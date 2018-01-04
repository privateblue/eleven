package engine

import graph._

import cats.kernel.Order
import cats.implicits._

trait MapAccumulator {
  type Out = Map[Index, Value]
  val empty = Map.empty[Index, Value]

  def get(out: Out, i: Index): Value =
    out.get(i).getOrElse(Value.empty)
  def set(out: Out, i: Index, v: Value): Out =
    out + (i -> v)
  def add(out1: Out, out2: Out): Out =
    (out1.keySet ++ out2.keySet)
      .map { k => k -> Order.max(get(out1, k), get(out2, k)) }
      .toMap
  def combine(zero: Out, outs: Set[Out]): Out =
    outs.foldLeft(zero)(add)
}

trait NonEmptyValueSearch {
  implicit def toDogsSet[T: Order](xs: Set[T]): dogs.Set[T] =
    dogs.Set.fromList(xs.toList)

  def values(
    col: Color,
    dir: DAG,
    graph: Graph[Value],
    index: Index
  ): dogs.Set[Value] =
    dir.from(index).map {
      case i if graph.at(i) === Value.empty =>
        val cvs = values(col, dir, graph, i)
        if (cvs.size == 1) cvs.toScalaSet.head else Value.empty
      case i if graph.at(i).c == Some(col) => graph.at(i)
      case i => Value.empty
    }.filterNot(_ === Value.empty)
}

object WriteBackReducer extends MapAccumulator with NonEmptyValueSearch {
  def reduce(col: Color, dir: Direction, graph: Graph[Value]): Graph[Value] = {
    val dag = graph.direction(dir)
    def reduce0(i: Index, out: Out, path: IndexedSeq[Index], focus: Int): Out = {
      val v = graph.at(i)
      val foreign = v =!= Value.empty && v.c != Some(col)
      val mergeb = values(col, dag, graph, i).size > 1
      val splitb =
        dag.to(i)
          .filterNot(path.contains)
          .exists(c => values(col, dag, graph, c).size > 1 || dag.ends.contains(c))
      val p = path :+ i
      val f = if (splitb || foreign) p.size - 1 else focus
      val focused = p(f)
      val fv = get(out, focused)
      val stay = if (mergeb) p.size else f
      val step = if (mergeb) p.size else f + 1
      if (v === Value.empty) {
        val cs = dag.from(i).map(c => reduce0(c, out, p, stay))
        combine(out, cs)
      } else if (fv === Value.empty) {
        val updated = set(out, focused, v)
        val cs = dag.from(i).map(c => reduce0(c, updated, p, stay))
        combine(updated, cs)
      } else if (fv === v) {
        val updated = set(out, focused, v.next.paint(col))
        val cs = dag.from(i).map(c => reduce0(c, updated, p, step))
        combine(updated, cs)
      } else {
        val nf = p.drop(f + 1).headOption.getOrElse(i)
        val updated = set(out, nf, v)
        val cs = dag.from(i).map(c => reduce0(c, updated, p, step))
        combine(updated, cs)
      }
    }
    val reduced = dag.ends.map { r => reduce0(r, empty, Vector.empty, 0) }
    val combined = combine(empty, reduced)
    val blank = graph.fill(Value.empty)
    combined.foldLeft(blank)((g, v) => g.set(v._1, v._2))
  }
}