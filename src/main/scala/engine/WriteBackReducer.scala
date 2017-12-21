package engine

import graph._

object WriteBackReducer {
  type Out = Map[Index, Value]

  val empty = Map.empty[Index, Value]
  def get(out: Out, i: Index): Value = out.get(i).getOrElse(Value.zero)
  def set(out: Out, i: Index, v: Value): Out = out + (i -> v)
  def add(out1: Out, out2: Out): Out =
    out1.keySet ++ out2.keySet map (k => k -> Value.max(get(out1, k), get(out2, k))) toMap

  def reduce(dir: Direction, graph: Graph[Value]): Graph[Value] = {
    val dag = graph.direction(dir)
    def valuesBefore(i: Index): Set[Value] =
      dag.from(i).map {
        case c if graph.at(c) == Value.zero =>
          val cvs = valuesBefore(c)
          if (cvs.size == 1) cvs.head else Value.zero
        case c => graph.at(c)
      }.filterNot(_ == Value.zero)
    def reduce0(i: Index, out: Out, path: IndexedSeq[Index], focus: Int): Out = {
      val mergeb = valuesBefore(i).size > 1
      val splitb =
        dag.to(i)
          .filterNot(path.contains)
          .exists(c => valuesBefore(c).size > 1 || dag.ends.contains(c))
      val p = path :+ i
      val f = if (splitb) p.size - 1 else focus
      val focused = p(f)
      val v = graph.at(i)
      val fv = get(out, focused)
      if (v == Value.zero) {
        val cs = dag.from(i).map(c => reduce0(c, out, p, if (mergeb) p.size else f))
        resolve(out, cs)
      } else if (fv == Value.zero) {
        val updated = set(out, focused, v)
        val cs = dag.from(i).map(c => reduce0(c, updated, p, if (mergeb) p.size else f))
        resolve(updated, cs)
      } else if (fv == v) {
        val updated = set(out, focused, v.next)
        val cs = dag.from(i).map(c => reduce0(c, updated, p, if (mergeb) p.size else f + 1))
        resolve(updated, cs)
      } else {
        val nf = p.drop(f + 1).headOption.getOrElse(i)
        val updated = set(out, nf, v)
        val cs = dag.from(i).map(c => reduce0(c, updated, p, if (mergeb) p.size else f + 1))
        resolve(updated, cs)
      }
    }
    val reduced = dag.ends.map { r => reduce0(r, empty, Vector.empty, 0) }
    val resolved = resolve(empty, reduced)
    val blank = graph.fill(Value.zero)
    resolved.foldLeft(blank)((g, v) => g.set(v._1, v._2))
  }

  def resolve(parent: Out, cs: Set[Out]): Out = cs.foldLeft(parent)(add)
}