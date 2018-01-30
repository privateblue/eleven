package engine

import graph._

object Acc {
  def empty(size: Int): Acc = Acc(
    values = IndexedSeq.fill(size)(Value.empty),
    moves = IndexedSeq.fill(size)(List.empty),
    score = Score.Zero
  )
}

case class Acc(
  values: IndexedSeq[Value],
  moves: IndexedSeq[List[Index]],
  score: Score
) {
  def move(dag: DAG, from: Index, to: Index, v: Value, s: Score): Acc = {
    val tos = moves(from.i)
    if (tos.contains(to) || tos.exists(dag.after(_).contains(to))) this
    else {
      val cmove = Acc(
        values = values.updated(to.i, v),
        moves = moves.updated(from.i, to :: tos),
        score = score + s
      )
      tos.filter(dag.after(to).contains).foldLeft(cmove) {
        case (a, t) if (moves.filter(_.contains(t)).size > 1) => Acc(
          values = a.values.updated(t.i, a.get(t).previous),
          moves = a.moves.updated(from.i, a.moves(from.i).filterNot(_ == t)),
          score = a.score - a.get(t).toScore
        )
        case (a, t) => Acc (
          values = a.values.updated(t.i, Value.empty),
          moves = a.moves.updated(from.i, a.moves(from.i).filterNot(_ == t)),
          score = a.score
        )
      }
    }
  }

  def froms(i: Index): IndexedSeq[Index] =
    moves.zipWithIndex.collect {
      case (ts, f) if ts.contains(i) => Index(f)
    }

  def get(i: Index): Value = values(i.i)
}

object WriteBackReducer {
  def reduce(col: Color, dir: Direction, graph: Graph[Value]): (Graph[Value], Score) = {
    val dag = graph.direction(dir)

    def values(index: Index): IndexedSeq[Value] =
      dag.sourcesOf(index).foldLeft(IndexedSeq.empty[Value]) {
        case (vs, i) if graph.at(i) == Value.empty =>
          val cvs = values(i)
          if (cvs.size == 1 && !vs.contains(cvs.head)) vs :+ cvs.head else vs
        case (vs, i) if graph.at(i).c == Some(col) && !vs.contains(graph.at(i)) =>
          vs :+ graph.at(i)
        case (vs, i) =>
          vs
      }

    def reduce0(i: Index, acc: Acc, path: IndexedSeq[Index], focus: Int): Acc = {
      val v = graph.at(i)
      val p = path :+ i
      val f = if (v.v != Value.empty.v && v.c != Some(col)) p.size - 1 else focus
      val focused = p(f)
      val ffroms = acc.froms(focused)
      val fv = acc.get(focused)
      val mergeb = values(i).size > 1
      val stay = if (mergeb) p.size else f
      val step = if (mergeb) p.size else f + 1
      if (ffroms.contains(i)) { // PRUNE
        acc
      } else if (v == Value.empty) { // SKIP
        dag.sourcesOf(i).foldLeft(acc)((a, c) => reduce0(c, a, p, stay))
      } else if (ffroms.isEmpty) { // INSERT
        val updated = acc.move(dag, i, focused, v, Score.Zero)
        dag.sourcesOf(i).foldLeft(updated)((a, c) => reduce0(c, a, p, stay))
      } else if (!ffroms.forall(p.contains)) { // IGNORE
        dag.sourcesOf(i).foldLeft(acc)((a, c) => reduce0(c, a, p, step))
      } else if (fv.v == v.v) { // MERGE
        val updated = acc.move(dag, i, focused, v.next.paint(col), v.next.toScore)
        dag.sourcesOf(i).foldLeft(updated)((a, c) => reduce0(c, a, p, step))
      } else { // STACK
        val focused2 = if (p.isDefinedAt(f + 1)) p(f + 1) else i
        val updated = acc.move(dag, i, focused2, v, Score.Zero)
        dag.sourcesOf(i).foldLeft(updated)((a, c) => reduce0(c, a, p, step))
      }
    }

    val zero = Acc.empty(graph.values.size)
    val path = IndexedSeq.empty
    val reduced = dag.tips.foldLeft(zero)((a, t) => reduce0(t, a, path, 0))
    (Graph(reduced.values, graph.dirs), reduced.score)
  }
}