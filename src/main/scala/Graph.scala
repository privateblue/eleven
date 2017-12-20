object Graph {
  def empty[V] = Graph(IndexedSeq.empty[V], Map.empty)
}

case class Graph[V](values: IndexedSeq[V], edges: Map[Int, Set[(Int, Int)]]) {
  def add(dir: Int, dag: DAG): Graph[V] =
    Graph(values, edges + (dir -> dag.cut(values.size - 1).edges))

  def update(vs: IndexedSeq[V]): Graph[V] =
    Graph(vs ++ values.drop(vs.size), edges)

  def direction(dir: Int): DAG =
    DAG(values.size, edges(dir))

  def at(index: Int): V =
    values(index)

  def set(index: Int, value: V): Graph[V] =
    Graph(values.updated(index, value), edges)
}