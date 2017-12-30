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
  val aiPlayers = List(0)

  def main(args: Array[String]): Unit = {
    val started = Game.start(Graphs.original, players)
    Await.ready(move(started), Duration.Inf)
  }

  def move(game: Game): Future[Unit] = game match {
    case con @ Continued(graph, history, scores) =>
      val n = history.size
      val player = n % players
      val humanEmptyPicker: Game.EmptyPicker = { es => pickEmpty(player, es) }
      val humanDirectionPicker: Game.DirectionPicker = { (put, dirs) =>
        println(board(put))
        pickDirection(player, dirs)
      }
      val resultHandler: Game.ResultHandler = { (entry, graph, scores) =>
        Future.successful {
          println(board(graph))
          println(historyEntry(entry) + ", " + display(scores))
        }
      }
      val next =
        if (aiPlayers.contains(player)) {
          val (e, d) = Game.bestMove(con)
          Game.move(con, _ => Future.successful(e.get), { (put, _) =>
            println(board(put))
            Future.successful(d.get)
          }, resultHandler)
        } else Game.move(con, humanEmptyPicker, humanDirectionPicker, resultHandler)
      next.flatMap(move)
    case NoMoreMoves(winner, graph, history, scores) => Future.successful {
      println(s"\nNo valid moves left - ${colors(winner.p)}Player ${winner.p}${Console.RESET} WINS")
      println(s"Final score: ${display(scores)}")
    }
    case Eleven(winner, graph, history, scores) => Future.successful {
      println(s"\nELEVEN - ${colors(winner.p)}Player ${winner.p}${Console.RESET} WINS")
    }
  }

  def pickEmpty(player: Int, es: IndexedSeq[Index]): Future[Index] = {
    val ask = Try {
      StdIn.readLine(s"""\n${colors(player)}Player $player${Console.RESET} Pick empty (${es.map(_.i).mkString(", ")}) > """)
        .toInt
    }
    ask.map(Index.apply) match {
      case Success(i) if es.contains(i) => Future.successful(i)
      case _ => pickEmpty(player, es)
    }
  }

  def pickDirection(player: Int, dirs: IndexedSeq[Direction]): Future[Direction] = {
    val ask = Try(StdIn.readLine(s"\n${colors(player)}Player $player${Console.RESET} Direction (${directions(dirs)}) > ").toInt)
    ask.map(Direction.apply) match {
      case Success(d) if dirs.contains(d) => Future.successful(d)
      case _ => pickDirection(player, dirs)
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
      .sliding(4, 4).map(_.mkString(" ")) // original
      //.sliding(2, 2).map(_.mkString(" ")) // twoByTwo
      .mkString("\n", "\n", "")

  def historyEntry(entry: Game.HistoryEntry): String = {
    val put = entry._1.map(_.i).getOrElse("no pick")
    val dir = entry._2.map(_.d).getOrElse("no pick")
    s"\nPut: $put, Dir: $dir"
  }
}