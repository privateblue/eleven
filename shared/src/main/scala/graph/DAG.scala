package graph

object DAG {
  def empty = DAG(0, IndexedSeq.empty)
}

case class DAG(size: Int, edges: IndexedSeq[(Index, Index)]) {
  lazy val indices: IndexedSeq[Index] =
    0.until(size).map(Index(_)).toIndexedSeq

  lazy val sources: IndexedSeq[IndexedSeq[Index]] =
    indices.map(t => edges.filter(_._2 == t).map(_._1))

  lazy val targets: IndexedSeq[IndexedSeq[Index]] =
    indices.map(s => edges.filter(_._1 == s).map(_._2))

  lazy val roots: IndexedSeq[Index] =
    indices.filter(t => sources(t.i).isEmpty)

  lazy val tips: IndexedSeq[Index] =
    indices.filter(s => targets(s.i).isEmpty)

  def add(neighbours: Int*): DAG = {
    val es = neighbours
      .filter(_ < size)
      .map(Index(_) -> Index(size))
      .filterNot(edges.contains)
    DAG(size + 1, edges ++ es)
  }

  def inverted: DAG =
    DAG(size, edges.map(e => (e._2, e._1)))

  def sourcesOf(index: Index): IndexedSeq[Index] =
    sources(index.i)

  def before(index: Index): IndexedSeq[Index] =
    sourcesOf(index) ++ sourcesOf(index).flatMap(before)

  def targetsOf(index: Index): IndexedSeq[Index] =
    targets(index.i)

  def after(index: Index): IndexedSeq[Index] =
    targetsOf(index) ++ targetsOf(index).flatMap(after)

  def take(n: Int): DAG =
    DAG(n, edges.filterNot(e => e._1.i >= n || e._2.i >= n))
}