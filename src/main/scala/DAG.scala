object DAG {
  def empty = DAG(0, Set.empty)
}

case class DAG(next: Int, edges: Set[(Int, Int)]) {
  val vertices: Set[Int] = edges.map(_._1) ++ edges.map(_._2)

  val sourceMap: Map[Int, Set[(Int, Int)]] = edges.groupBy(_._1)

  val targetMap: Map[Int, Set[(Int, Int)]] = edges.groupBy(_._2)

  val starts: Set[Int] = sourceMap.keySet diff targetMap.keySet

  val ends: Set[Int] = targetMap.keySet diff sourceMap.keySet

  def add(neighbours: Set[Int] = Set.empty): DAG = {
    val es = edges ++ neighbours.filter(_ < next).map(_ -> next)
    DAG(next + 1, es)
  }

  def inverted: DAG =
    DAG(next, edges.map(e => (e._2, e._1)))

  def from(index: Int): Set[Int] =
    targetMap.get(index).getOrElse(Set.empty).map(_._1)

  def to(index: Int): Set[Int] =
    sourceMap.get(index).getOrElse(Set.empty).map(_._2)

  def before(root: Int): DAG = {
    def before0(r: Int): Set[(Int, Int)] =
      targetMap.get(r).getOrElse(Set.empty) ++ from(r).flatMap(before0)
    DAG(root + 1, before0(root))
  }

  def after(root: Int): DAG = {
    def after0(r: Int): Set[(Int, Int)] =
      sourceMap.get(r).getOrElse(Set.empty) ++ to(r).flatMap(after0)
    DAG(next, after0(root))
  }

  def cut(index: Int): DAG =
    DAG(index + 1, edges.filterNot(e => e._1 > index || e._2 > index))
}