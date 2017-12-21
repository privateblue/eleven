package games

import graph._
import engine._

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
      .update(IndexedSeq.fill(16)(Value.empty))
      .add(right).add(left).add(down).add(up)

  def directions(dirs: IndexedSeq[Direction]): String =
    Map(0 -> "right", 1 -> "left", 2 -> "down", 3 -> "up")
      .filterKeys(dirs.map(_.d).contains)
      .map { case (d, desc) => s"$d - $desc" }
      .mkString(", ")

  def main(args: Array[String]): Unit =
    move(zero, 2, List.empty, IndexedSeq.fill(2)(0))
      .fold(_ => (), _ => ())

  def move(graph: Graph[Value], players: Int, history: List[(Option[Index], Option[Direction])], scores: IndexedSeq[Int]): Try[Unit] = {
    val player = history.size % players
    val color = Color(player)
    println(s"\n\n\n${history.size}. move by ${colors(player)}Player $player${Console.RESET}:")
    println(board(graph))
    for {
      e <- if (!empties(graph).isEmpty) pickEmpty(empties(graph)).map(Option.apply) else Success(None)
      filled = e.map(graph.set(_, Value.empty.paint(color).next)).getOrElse(graph)
      _ = e.foreach(_ => println(board(filled)))
      dir <- if (!reducibles(color, filled).isEmpty) pickDirection(reducibles(color, filled)).map(Option.apply) else Success(None)
    } yield {
      val reduced = dir.map(WriteBackReducer.reduce(color, _, filled)).getOrElse(filled)
      dir.foreach(_ => println(board(reduced)))
      val p = score(filled, reduced)
      val added = scores.updated(player, scores(player) + p)
      println(display(added))
      move(reduced, players, (e, dir) :: history, added)
    }
  }

  def reducibles(color: Color, graph: Graph[Value]): IndexedSeq[Direction] =
    graph.directions.filter(WriteBackReducer.reduce(color, _, graph).values != graph.values)

  def pickDirection(dirs: IndexedSeq[Direction]): Try[Direction] =
    Try(StdIn.readLine(s"\nDirection (${directions(dirs)}) > ").toInt)
      .map(Direction.apply)
      .flatMap {
        case d if dirs.contains(d) => Success(d)
        case _ => pickDirection(dirs)
      }

  def pickEmpty(es: IndexedSeq[Index]): Try[Index] =
    Try(StdIn.readLine(s"""\nPick an empty for 1 (${es.map(_.i).mkString(", ")}) > """).toInt)
      .map(Index.apply)
      .flatMap {
        case e if es.contains(e) => Success(e)
        case _ => pickEmpty(es)
      }

  def empties(graph: Graph[Value]): IndexedSeq[Index] =
    graph.indices.filter(graph.at(_) == Value.empty)

  def display(scores: IndexedSeq[Int]): String =
    scores.zipWithIndex.map {
      case (s, c) => s"${colors(c)}$s${Console.RESET}"
    }.mkString("\n", " : ", "")

  def score(prev: Graph[Value], cur: Graph[Value]): Int = {
    val p = prev.values.map(_.v)
    val c = cur.values.map(_.v)
    val removed = p diff c
    val appeared = c diff p
    (appeared sum) + (removed diff (appeared flatMap (v => IndexedSeq(v - 1, v - 1))) sum)
  }

  val colors = IndexedSeq(Console.RED, Console.GREEN, Console.YELLOW, Console.MAGENTA, Console.BLUE, Console.CYAN)

  def board(graph: Graph[Value]): String =
    graph.values
      .map {
        case Value(v, Some(Color(c))) => s"${colors(c)}$v${Console.RESET}"
        case Value(v, _) => s"$v"
      }
      .sliding(4, 4).map(_.mkString(" "))
      .mkString("\n", "\n", "")
}