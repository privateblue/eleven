package engine

import graph._

trait MapAccumulator {
  case class Out(values: IndexedSeq[Value], score: Score) {
    def scored(sc: Score): Out = Out(values, score + sc)
  }

  def empty(size: Int): Out =
    Out(IndexedSeq.fill(size)(Value.empty), Score(0))
  def get(out: Out, i: Index): Value =
    out.values(i.i)
  def set(out: Out, i: Index, v: Value): Out =
    Out(out.values.updated(i.i, v), out.score)
  def add(out1: Out, out2: Out): Out = Out(
    out1.values.zip(out2.values).map { case (v1, v2) => Value.ordering.max(v1, v2) },
    out1.score + out2.score
  )
  def combine(zero: Out, outs: Set[Out]): Out =
    outs.foldLeft(zero)(add)
}

trait NonEmptyValueSearch {
  def values(
    col: Color,
    dir: DAG,
    graph: Graph[Value],
    index: Index
  ): Set[Value] =
    dir.from(index).map {
      case i if graph.at(i) == Value.empty =>
        val cvs = values(col, dir, graph, i)
        if (cvs.size == 1) cvs.head else Value.empty
      case i if graph.at(i).c == Some(col) => graph.at(i)
      case i => Value.empty
    }.filterNot(_ == Value.empty)
}

object WriteBackReducer extends MapAccumulator with NonEmptyValueSearch {
  def reduce(col: Color, dir: Direction, graph: Graph[Value]): (Graph[Value], Score) = {
    val dag = graph.direction(dir)
    def reduce0(i: Index, out: Out, path: IndexedSeq[Index], focus: Int): Out = {
      val v = graph.at(i)
      val foreign = v.v != Value.empty.v && v.c != Some(col)
      val mergeb = values(col, dag, graph, i).size > 1
      val splitb =
        dag.to(i)
          .filterNot(path.contains)
          .exists(c => values(col, dag, graph, c).size > 1 || dag.tips.contains(c))
      val p = path :+ i
      val f = if (splitb || foreign) p.size - 1 else focus
      val focused = p(f)
      val fv = get(out, focused)
      val stay = if (mergeb) p.size else f
      val step = if (mergeb) p.size else f + 1
      if (v == Value.empty) {
        val cs = dag.from(i).map(c => reduce0(c, out, p, stay))
        combine(out, cs)
      } else if (fv == Value.empty) {
        val updated = set(out, focused, v)
        val cs = dag.from(i).map(c => reduce0(c, updated, p, stay))
        combine(updated, cs)
      } else if (fv.v == v.v) {
        val updated = set(out, focused, v.next.paint(col))
        val cs = dag.from(i).map(c => reduce0(c, updated, p, step))
        combine(updated, cs).scored(v.next.toScore)
      } else {
        val nf = p.drop(f + 1).headOption.getOrElse(i)
        val updated = set(out, nf, v)
        val cs = dag.from(i).map(c => reduce0(c, updated, p, step))
        combine(updated, cs)
      }
    }
    val blank = empty(graph.values.size)
    val reduced = dag.tips.map { r => reduce0(r, blank, IndexedSeq.empty, 0) }
    val combined = combine(blank, reduced)
    (Graph(combined.values, graph.edges), combined.score)
  }
}