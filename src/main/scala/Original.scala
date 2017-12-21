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
    Graph.empty[Value]
      .update(IndexedSeq.fill(16)(Value.zero))
      .add(right).add(left).add(down).add(up)

  def directions(dirs: IndexedSeq[Direction]): String =
    Map(0 -> "right", 1 -> "left", 2 -> "down", 3 -> "up")
      .filterKeys(dirs.map(_.d).contains)
      .map { case (d, desc) => s"$d - $desc" }
      .mkString(", ")

  def main(args: Array[String]): Unit =
    move(zero, List.empty, 0).fold(_ => (), _ => ())

  def move(graph: Graph[Value], history: List[(Index, Direction)], ps: Int): Try[Unit] = {
    println(s"\n${history.size}. move:")
    for {
      e <- pickEmpty(empties(graph))
      filled = graph.set(e, Value.zero.next)
      _ = println(board(filled))
      dir <- pickDirection(reducibles(filled))
    } yield {
      val reduced = WriteBackReducer.reduce(dir, filled)
      val p = score(filled, reduced)
      println(s"score: $ps + $p")
      move(reduced, (e, dir) :: history, ps + p)
    }
  }

  def reducibles(graph: Graph[Value]): IndexedSeq[Direction] =
    graph.directions.filter(WriteBackReducer.reduce(_, graph).values != graph.values)

  def pickDirection(dirs: IndexedSeq[Direction]): Try[Direction] =
    Try(StdIn.readLine(s"Direction (${directions(dirs)}) > ").toInt)
      .map(Direction.apply)
      .filter(dirs.contains)

  def pickEmpty(es: IndexedSeq[Index]): Try[Index] =
    Try(Random.shuffle(es).head)

  def empties(graph: Graph[Value]): IndexedSeq[Index] =
    graph.indices.filter(graph.at(_) == Value.zero)

  def score(prev: Graph[Value], cur: Graph[Value]): Int =
    cur.values.diff(prev.values).map(_.v).sum +
      prev.values.diff(cur.values)
        .diff(cur.values.diff(prev.values).flatMap(v => IndexedSeq(v.prev, v.prev)))
        .map(_.v).sum

  def board(graph: Graph[Value]): String =
    graph.values.map(_.v).sliding(4, 4).map(_.mkString(" ")).mkString("\n")
}