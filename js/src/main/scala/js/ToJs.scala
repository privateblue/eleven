package js

import graph._
import engine._

import scala.scalajs.js
import scala.scalajs.js.JSConverters._

trait ToJs[T] {
  def toJs(v: T): js.Any
}

object ToJs {
  def to[T](v: T)(implicit tToJs: ToJs[T]) = tToJs.toJs(v)

  implicit val indexToJs = new ToJs[Index] {
    def toJs(i: Index) = i.i
  }

  implicit val directionToJs = new ToJs[Direction] {
    def toJs(d: Direction) = d.d
  }

  implicit val colorToJs = new ToJs[Color] {
    def toJs(c: Color) = c.c
  }

  implicit val edgeToJs = new ToJs[Tuple2[Index, Index]] {
    def toJs(e: (Index, Index)) =
      js.Dynamic.literal(from = to(e._1), to = to(e._2))
  }

  implicit val historyEntryToJs = new ToJs[Tuple2[Option[Index], Option[Direction]]] {
    def toJs(he: (Option[Index], Option[Direction])) = he match {
      case (Some(e), Some(d)) => js.Dynamic.literal(put = to(e), dir = to(d))
      case (Some(e), None) => js.Dynamic.literal(put = to(e))
      case (None, Some(d)) => js.Dynamic.literal(dir = to(d))
      case _ => js.Dynamic.literal()
    }
  }

  implicit def setToJs[T](implicit tToJs: ToJs[T]) = new ToJs[Set[T]] {
    def toJs(set: Set[T]) = set.map(tToJs.toJs).toSeq.toJSArray
  }

  implicit def indexedSeqToJs[T](implicit tToJs: ToJs[T]) = new ToJs[IndexedSeq[T]] {
    def toJs(seq: IndexedSeq[T]) = seq.map(tToJs.toJs).toJSArray
  }

  implicit def listToJs[T](implicit tToJs: ToJs[T]) = new ToJs[List[T]] {
    def toJs(list: List[T]) = list.map(tToJs.toJs).toJSArray
  }

  implicit def graphToJs[T: ToJs] = new ToJs[Graph[T]] {
    def toJs(graph: Graph[T]) =
      js.Dynamic.literal(values = to(graph.values), edges = to(graph.edges))
  }

  implicit val valueToJs = new ToJs[Value] {
    def toJs(v: Value) = v.c match {
      case Some(c) => js.Dynamic.literal(v = v.v, c = to(c))
      case _ => js.Dynamic.literal(v = v.v)
    }
  }

  implicit val scoreToJs = new ToJs[Score] {
    def toJs(s: Score) = s.s
  }

  implicit val playerToJs = new ToJs[Player] {
    def toJs(p: Player) = p.p
  }

  implicit val gameToJs = new ToJs[Game] {
    def toJs(game: Game) = game match {
      case Continued(graph, history, scores) =>
        js.Dynamic.literal(
          state = "continued",
          graph = to(graph),
          history = to(history),
          scores = to(scores)
        )
      case NoMoreMoves(graph, history, scores) =>
        js.Dynamic.literal(
          state = "nomoremoves",
          graph = to(graph),
          history = to(history),
          scores = to(scores)
        )
      case Eleven(winner, graph, history, scores) =>
        js.Dynamic.literal(
          state = "eleven",
          winner = to(winner),
          graph = to(graph),
          history = to(history),
          scores = to(scores)
        )
    }
  }
}