import scala.io.StdIn
import scala.util.Random
import scala.util.Try
import scala.util.Success

object Original {
  val right =
    DAG.empty
      .add(Set()).add(Set(0)).add(Set(1)).add(Set(2))
      .add(Set()).add(Set(4)).add(Set(5)).add(Set(6))
      .add(Set()).add(Set(8)).add(Set(9)).add(Set(10))
      .add(Set()).add(Set(12)).add(Set(13)).add(Set(14))

  val left = right.inverted

  val down =
    DAG.empty
      .add(Set()).add(Set()).add(Set()).add(Set())
      .add(Set(0)).add(Set(1)).add(Set(2)).add(Set(3))
      .add(Set(4)).add(Set(5)).add(Set(6)).add(Set(7))
      .add(Set(8)).add(Set(9)).add(Set(10)).add(Set(11))

  val up = down.inverted

  val zero =
    Graph.empty[Int]
      .update(IndexedSeq.fill(16)(0))
      .add(right).add(left).add(down).add(up)

  def directions(dirs: IndexedSeq[Int]): String =
    Map(0 -> "right", 1 -> "left", 2 -> "down", 3 -> "up")
      .filterKeys(dirs.contains)
      .map { case (d, desc) => s"$d - $desc" }
      .mkString(", ")

  def main(args: Array[String]): Unit =
    move(zero, List.empty, 0).fold(_ => (), _ => ())

  def move(graph: Graph[Int], history: List[(Int, Int)], ps: Int): Try[Unit] = {
    println(s"\n${history.size}. move:")
    for {
      e <- pickEmpty(empties(graph))
      filled = graph.set(e, 1)
      _ = println(board(filled))
      dir <- pickDirection(reducibles(filled))
    } yield {
      val reduced = WriteBackReducer.reduce(dir, filled)
      val p = points(filled, reduced)
      println(s"points: $ps + $p")
      move(reduced, (e, dir) :: history, ps + p)
    }
  }

  def reducibles(graph: Graph[Int]): IndexedSeq[Int] =
    0 until graph.edges.size filter (WriteBackReducer.reduce(_, graph).values != graph.values)

  def pickDirection(dirs: IndexedSeq[Int]): Try[Int] =
    Try(StdIn.readLine(s"Direction (${directions(dirs)}) > ").toInt)
      .filter(dirs.contains)

  def pickEmpty(es: IndexedSeq[Int]): Try[Int] =
    Try(Random.shuffle(es).head)

  def empties(graph: Graph[Int]): IndexedSeq[Int] =
    graph.values.zipWithIndex.filter(_._1 == 0).map(_._2)

  def points(prev: Graph[Int], cur: Graph[Int]): Int =
    cur.values.diff(prev.values).sum +
      prev.values.diff(cur.values)
        .diff(cur.values.diff(prev.values).flatMap(v => IndexedSeq(v - 1, v - 1)))
        .sum

  def board(graph: Graph[Int]): String =
    graph.values.sliding(4, 4).map(_.mkString(" ")).mkString("\n")
}