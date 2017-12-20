object Graph {
  def empty[V] = Graph(IndexedSeq.empty[V], Map.empty)
}

case class Graph[V](values: IndexedSeq[V], edges: Map[Int, Set[(Int, Int)]]) {
  def update(dir: Int, dag: DAG[V]): Graph[V] = Graph(
    values = dag.values ++ values.drop(dag.values.size),
    edges = edges + (dir -> dag.edges)
  )

  def direction(dir: Int): DAG[V] =
    DAG(values, edges(dir))

  def at(index: Int): V =
    values(index)

  def set(index: Int, value: V): Graph[V] =
    Graph(values.updated(index, value), edges)
}