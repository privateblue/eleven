package games

import graph._
import engine._

import scala.io.StdIn
import scala.util.Random
import scala.util.Try
import scala.util.Success
import scala.concurrent.Future
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global

object OriginalConsole {
  val colors = IndexedSeq(Console.RED, Console.GREEN, Console.BLUE)

  val players = 2

  def main(args: Array[String]): Unit = {
    val started = Game.start(Graphs.original, players)
    Await.ready(move(started), Duration.Inf)
  }

  def move(game: Game): Future[Unit] = game match {
    case con @ Continued(graph, history, scores) =>
      val n = history.size
      val player = n % players
      println(s"\n\n\n$n. move by ${colors(player)}Player $player${Console.RESET}:")
      println(board(graph))
      val next = Game.move(
        state = con,
        emptyPicker = { es =>
          pickEmpty(es)
        },
        directionPicker = { (put, dirs) =>
          println(board(put))
          pickDirection(dirs)
        },
        resultHandler = { (entry, graph, scores) =>
          Future {
            println(board(graph))
            println(display(scores))
          }
        }
      )
      next.flatMap(move)
    case NoMoreMoves(winner, graph, history, scores) => Future {
      println(s"\nNo valid moves left - ${colors(winner.p)}Player ${winner.p}${Console.RESET} WINS")
      println(s"Final score: ${display(scores)}")
    }
    case Eleven(winner, graph, history, scores) => Future {
      println(s"\nELEVEN - ${colors(winner.p)}Player ${winner.p}${Console.RESET} WINS")
    }
  }

  def pickEmpty(es: IndexedSeq[Index]): Future[Index] = {
    val ask = Try {
      StdIn.readLine(s"""\nPick an empty for 1 (${es.map(_.i).mkString(", ")}) > """)
        .toInt
    }
    ask.map(Index.apply) match {
      case Success(i) if es.contains(i) => Future.successful(i)
      case _ => pickEmpty(es)
    }
  }

  def pickDirection(dirs: IndexedSeq[Direction]): Future[Direction] = {
    val ask = Try(StdIn.readLine(s"\nDirection (${directions(dirs)}) > ").toInt)
    ask.map(Direction.apply) match {
      case Success(d) if dirs.contains(d) => Future.successful(d)
      case _ => pickDirection(dirs)
    }
  }

  def directions(dirs: IndexedSeq[Direction]): String =
    Map(0 -> "right", 1 -> "left", 2 -> "down", 3 -> "up")
      .filterKeys(dirs.map(_.d).contains)
      .map { case (d, desc) => s"$d - $desc" }
      .mkString(", ")

  def display(scores: IndexedSeq[Score]): String =
    scores.zipWithIndex.map {
      case (s, c) => s"${colors(c)}${s.s}${Console.RESET}"
    }.mkString("\n", " : ", "")

  def board(graph: Graph[Value]): String =
    graph.values
      .map {
        case Value(v, Some(Color(c))) => s"${colors(c)}$v${Console.RESET}"
        case Value(v, _) => s"$v"
      }
      .sliding(4, 4).map(_.mkString(" "))
      .mkString("\n", "\n", "")
}