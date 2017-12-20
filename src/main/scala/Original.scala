import scala.io.StdIn
import scala.util.Random
import scala.util.Try
import scala.util.Success

object Original {
  val right =
    DAG.empty[Int]
      .add(0, Set()).add(0, Set(0)).add(0, Set(1)).add(0, Set(2))
      .add(0, Set()).add(0, Set(4)).add(0, Set(5)).add(0, Set(6))
      .add(0, Set()).add(0, Set(8)).add(0, Set(9)).add(0, Set(10))
      .add(0, Set()).add(0, Set(12)).add(0, Set(13)).add(0, Set(14))

  val left = right.inverted

  val down =
    DAG.empty[Int]
      .add(0, Set()).add(0, Set()).add(0, Set()).add(0, Set())
      .add(0, Set(0)).add(0, Set(1)).add(0, Set(2)).add(0, Set(3))
      .add(0, Set(4)).add(0, Set(5)).add(0, Set(6)).add(0, Set(7))
      .add(0, Set(8)).add(0, Set(9)).add(0, Set(10)).add(0, Set(11))

  val up = down.inverted

  val zero =
    Graph.empty[Int]
      .update(0, right).update(1, left).update(2, down).update(3, up)

  def directions(dirs: Set[Int]): String =
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

  def reducibles(graph: Graph[Int]): Set[Int] =
    graph.edges.keySet.collect {
      case d if WriteBackReducer.reduce(d, graph).values != graph.values => d
    }

  def pickDirection(dirs: Set[Int]): Try[Int] =
    Try(StdIn.readLine(s"Direction (${directions(dirs)}) > ").toInt)
      .filter(dirs.contains)

  def pickEmpty(es: Set[Int]): Try[Int] =
    Try(Random.shuffle(es).head)

  def empties(graph: Graph[Int]): Set[Int] =
    graph.values.zipWithIndex.filter(_._1 == 0).map(_._2).toSet

  def points(prev: Graph[Int], cur: Graph[Int]): Int =
    cur.values.diff(prev.values).sum +
      prev.values.diff(cur.values)
        .diff(cur.values.diff(prev.values).flatMap(v => IndexedSeq(v - 1, v - 1)))
        .sum

  def board(graph: Graph[Int]): String =
    graph.values.sliding(4, 4).map(_.mkString(" ")).mkString("\n")
}