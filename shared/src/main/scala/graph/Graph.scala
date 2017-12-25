package graph

object Graph {
  def empty[V] = Graph(IndexedSeq.empty[V], IndexedSeq.empty)
}

case class Graph[V](values: IndexedSeq[V], edges: IndexedSeq[Set[(Index, Index)]]) {
  val indices: IndexedSeq[Index] =
    0 until values.size map Index.apply toIndexedSeq

  val directions: IndexedSeq[Direction] =
    0 until edges.size map Direction.apply toIndexedSeq

  def add(dag: DAG): Graph[V] =
    Graph(values, edges :+ dag.cut(Index(values.size - 1)).edges)

  def update(vs: IndexedSeq[V]): Graph[V] =
    Graph(vs ++ values.drop(vs.size), edges)

  def direction(dir: Direction): DAG =
    DAG(Index(values.size), edges(dir.d))

  def at(index: Index): V =
    values(index.i)

  def set(index: Index, value: V): Graph[V] =
    Graph(values.updated(index.i, value), edges)

  def fill[W](v: W): Graph[W] =
    Graph(IndexedSeq.fill(values.size)(v), edges)
}