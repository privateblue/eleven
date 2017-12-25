package graph

object DAG {
  def empty = DAG(Index(0), Set.empty)
}

case class DAG(next: Index, edges: Set[(Index, Index)]) {
  val vertices: Set[Index] = edges.map(_._1) ++ edges.map(_._2)

  val sourceMap: Map[Index, Set[(Index, Index)]] = edges.groupBy(_._1)

  val targetMap: Map[Index, Set[(Index, Index)]] = edges.groupBy(_._2)

  val starts: Set[Index] = sourceMap.keySet diff targetMap.keySet

  val ends: Set[Index] = targetMap.keySet diff sourceMap.keySet

  def add(neighbours: Set[Int] = Set.empty): DAG = {
    val es = edges ++ neighbours.filter(_ < next.i).map(Index(_) -> next)
    DAG(Index(next.i + 1), es)
  }

  def inverted: DAG =
    DAG(next, edges.map(e => (e._2, e._1)))

  def from(index: Index): Set[Index] =
    targetMap.get(index).getOrElse(Set.empty).map(_._1)

  def to(index: Index): Set[Index] =
    sourceMap.get(index).getOrElse(Set.empty).map(_._2)

  def before(root: Index): DAG = {
    def before0(r: Index): Set[(Index, Index)] =
      targetMap.get(r).getOrElse(Set.empty) ++ from(r).flatMap(before0)
    DAG(Index(root.i + 1), before0(root))
  }

  def after(root: Index): DAG = {
    def after0(r: Index): Set[(Index, Index)] =
      sourceMap.get(r).getOrElse(Set.empty) ++ to(r).flatMap(after0)
    DAG(next, after0(root))
  }

  def cut(index: Index): DAG =
    DAG(Index(index.i + 1), edges.filterNot(e => e._1.i > index.i || e._2.i > index.i))
}