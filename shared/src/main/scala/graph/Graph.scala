package graph

object Graph {
  def empty[V] = Graph(IndexedSeq.empty[V], IndexedSeq.empty)
}

case class Graph[V](values: IndexedSeq[V], dirs: IndexedSeq[DAG]) {
  lazy val indices: IndexedSeq[Index] =
    values.indices.map(Index.apply)

  lazy val directions: IndexedSeq[Direction] =
    dirs.indices.map(Direction.apply)

  def add(dag: DAG): Graph[V] =
    Graph(values, dirs :+ dag.take(values.size))

  def update(vs: IndexedSeq[V]): Graph[V] =
    Graph(vs ++ values.drop(vs.size), dirs)

  def direction(dir: Direction): DAG =
    dirs(dir.d)

  def at(index: Index): V =
    values(index.i)

  def set(index: Index, value: V): Graph[V] =
    Graph(values.updated(index.i, value), dirs)

  def fill[W](v: W): Graph[W] =
    Graph(IndexedSeq.fill(values.size)(v), dirs)
}