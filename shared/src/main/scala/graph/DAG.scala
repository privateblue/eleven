package graph

object DAG {
  def empty = DAG(0, Set.empty)
}

case class DAG(size: Int, edges: Set[(Index, Index)]) {
  lazy val connected: Set[Index] =
    edges.map(_._1) ++ edges.map(_._2)

  lazy val unconnected: Set[Index] =
    0.until(size).map(Index(_)).toSet diff connected

  lazy val sourceMap: Map[Index, Set[(Index, Index)]] =
    edges.groupBy(_._1)

  lazy val targetMap: Map[Index, Set[(Index, Index)]] =
    edges.groupBy(_._2)

  lazy val sources: Map[Index, Set[Index]] =
    targetMap.map(e => e._1 -> e._2.map(_._1))

  lazy val targets: Map[Index, Set[Index]] =
    sourceMap.map(e => e._1 -> e._2.map(_._2))

  lazy val roots: Set[Index] =
    (sourceMap.keySet diff targetMap.keySet) ++ unconnected

  lazy val tips: Set[Index] =
    (targetMap.keySet diff sourceMap.keySet) ++ unconnected

  def add(neighbours: Int*): DAG = {
    val es = edges ++ neighbours.filter(_ < size).map(Index(_) -> Index(size))
    DAG(size + 1, es)
  }

  def inverted: DAG =
    DAG(size, edges.map(e => (e._2, e._1)))

  def from(index: Index): Set[Index] =
    sources.get(index).getOrElse(Set.empty)

  def to(index: Index): Set[Index] =
    targets.get(index).getOrElse(Set.empty)

  def take(n: Int): DAG =
    DAG(n, edges.filterNot(e => e._1.i >= n || e._2.i >= n))
}