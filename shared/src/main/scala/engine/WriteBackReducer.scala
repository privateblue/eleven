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

    def move(acc: Acc, from: Index, to: Index): Acc = {
      val v = graph.at(from)
      val tos = acc.moves(from.i)
      if (tos.contains(to) || tos.exists(dag.reachable(_, to))) acc
      else {
        val (cv, cs) =
          if (acc.values(to.i).v == v.v) (v.next, v.next.toScore)
          else (v, Score.Zero)
        val cmove = Acc(
          values = acc.values.updated(to.i, cv),
          moves = acc.moves.updated(from.i, to :: tos),
          score = acc.score + cs
        )
        tos.filter(dag.reachable(to, _)).foldLeft(cmove) { (a, t) =>
          val tfroms = acc.moves.filter(_.contains(t))
          val (uv, us) =
            if (tfroms.size > 1 && a.moves(t.i).contains(t))
              (a.get(t).previous.paint(graph.at(t).c.get), a.get(t).toScore)
            else if (tfroms.size > 1)
              (a.get(t).previous, a.get(t).toScore)
            else
              (Value.empty, Score.Zero)
          Acc(
            values = a.values.updated(t.i, uv),
            moves = a.moves.updated(from.i, a.moves(from.i).filterNot(_ == t)),
            score = a.score - us
          )
        }
      }
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
        //println(s"$i PRUNE $p $focused $ffroms $acc")
        acc
      } else if (v == Value.empty) { // SKIP
        //println(s"$i SKIP $p $focused $ffroms $acc")
        dag.sourcesOf(i).foldLeft(acc)((a, c) => reduce0(c, a, p, stay))
      } else if (ffroms.isEmpty) { // INSERT
        val updated = move(acc, i, focused)
        //println(s"$i INSERT $p $focused $ffroms $updated")
        dag.sourcesOf(i).foldLeft(updated)((a, c) => reduce0(c, a, p, stay))
      } else if (!ffroms.forall(p.contains)) { // IGNORE
        //println(s"$i IGNORE $p $focused $ffroms $acc")
        dag.sourcesOf(i).foldLeft(acc)((a, c) => reduce0(c, a, p, step))
      } else if (fv.v == v.v) { // MERGE
        val updated = move(acc, i, focused)
        //println(s"$i MERGE $p $focused $ffroms $updated")
        dag.sourcesOf(i).foldLeft(updated)((a, c) => reduce0(c, a, p, step))
      } else { // STACK
        val focused2 = if (p.isDefinedAt(f + 1)) p(f + 1) else i
        val updated = move(acc, i, focused2)
        //println(s"$i STACK $p $focused $ffroms $updated")
        dag.sourcesOf(i).foldLeft(updated)((a, c) => reduce0(c, a, p, step))
      }
    }

    val zero = Acc.empty(graph.values.size)
    val path = IndexedSeq.empty
    val reduced = dag.tips.foldLeft(zero)((a, t) => reduce0(t, a, path, 0))
    (Graph(reduced.values, graph.dirs), reduced.score)
  }
}