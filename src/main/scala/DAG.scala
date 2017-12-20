object DAG {
  def empty[V] = DAG(IndexedSeq.empty[V], Set.empty)
}

case class DAG[V](values: IndexedSeq[V], edges: Set[(Int, Int)]) {
  def add(value: V, neighbours: Set[Int] = Set.empty): DAG[V] = {
    val vs = values :+ value
    val i = vs.size - 1
    val es = edges ++ neighbours.filter(_ < i).map(_ -> i)
    DAG(vs, es)
  }

  val vertices: Set[Int] = edges.map(_._1) ++ edges.map(_._2)

  val sourceMap: Map[Int, Set[(Int, Int)]] = edges.groupBy(_._1)

  val targetMap: Map[Int, Set[(Int, Int)]] = edges.groupBy(_._2)

  val starts: Set[Int] = sourceMap.keySet diff targetMap.keySet

  val ends: Set[Int] = targetMap.keySet diff sourceMap.keySet

  def inverted = DAG(values, edges.map(e => (e._2, e._1)))

  def at(index: Int): V =
    values(index)

  def set(index: Int, value: V): DAG[V] =
    DAG(values.updated(index, value), edges)

  def from(index: Int): Set[Int] =
    targetMap.get(index).getOrElse(Set.empty).map(_._1)

  def to(index: Int): Set[Int] =
    sourceMap.get(index).getOrElse(Set.empty).map(_._2)

  def before(root: Int): DAG[V] = {
    def before0(r: Int): Set[(Int, Int)] =
      targetMap.get(r).getOrElse(Set.empty) ++ from(r).flatMap(before0)
    DAG(values.take(root + 1), before0(root))
  }

  def beforeTree(root: Int): Tree[V] =
    Tree(root, at(root), from(root).map(beforeTree))

  def after(root: Int): DAG[V] = {
    def after0(r: Int): Set[(Int, Int)] =
      sourceMap.get(r).getOrElse(Set.empty) ++ to(r).flatMap(after0)
    DAG(values, after0(root))
  }
}

case class Tree[V](index: Int, value: V, children: Set[Tree[V]]) {
  def prepend(i: Int, v: V): Tree[V] = Tree(i, v, Set(this))
}