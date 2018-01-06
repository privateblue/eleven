package js

import graph._
import engine._
import games._

import scala.concurrent.Future
import scala.collection.mutable

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js
import scala.scalajs.js.annotation._
import scala.scalajs.js.JSConverters._

@JSExportTopLevel("Eleven")
@JSExportAll
object Eleven {
  def graphOriginal = Graphs.original

  def start(graph: Graph[Value], players: Int) = Game.start(graph, players)

  def move(
    state: Continued,
    emptyPicker: Game.EmptyPicker,
    directionPicker: Game.DirectionPicker,
    resultHandler: Game.ResultHandler
  ) = Game.move(state, emptyPicker, directionPicker, resultHandler)

  def randomEmpty(state: Continued) = Game.randomEmpty(state)

  def bestMove(state: Continued) = Game.bestMove(state)

  def nextToJs(next: Future[Game]) = next.toJSPromise // TODO check if this is needed

  def gameToJs(game: Game) = game match {
    case Continued(g, h, ss) => new JsGame {
      val state = "continued"
      val winner = js.undefined
      val graph = graphToJs(g)
      val history = historyToJs(h)
      val scores = scoresToJs(ss)
    }
    case NoMoreMoves(w, g, h, ss) => new JsGame {
      val state = "nomoremoves"
      val winner = playerToJs(w)
      val graph = graphToJs(g)
      val history = historyToJs(h)
      val scores = scoresToJs(ss)
    }
    case engine.Eleven(w, g, h, ss) => new JsGame {
      val state = "eleven"
      val winner = playerToJs(w)
      val graph = graphToJs(g)
      val history = historyToJs(h)
      val scores = scoresToJs(ss)
    }
  }

  def gameOf(game: JsGame) = game.state match {
    case "continued" => Continued(
      graph = graphOf(game.graph),
      history = historyOf(game.history),
      scores = scoresOf(game.scores)
    )
    case "nomoremoves" => NoMoreMoves(
      winner = playerOf(game.winner.toOption.get),
      graph = graphOf(game.graph),
      history = historyOf(game.history),
      scores = scoresOf(game.scores)
    )
    case "eleven" => engine.Eleven(
      winner = playerOf(game.winner.toOption.get),
      graph = graphOf(game.graph),
      history = historyOf(game.history),
      scores = scoresOf(game.scores)
    )
  }

  def historyToJs(history: List[Game.HistoryEntry]) =
    history.map(historyEntryToJs).toJSArray

  def historyOf(history: js.Array[JsHistoryEntry]) =
    history.toList.map(historyEntryOf)

  def scoresToJs(scores: IndexedSeq[Score]) =
    scores.map(scoreToJs).toJSArray

  def scoresOf(scores: js.Array[Int]) =
    scores.toIndexedSeq.map(scoreOf)

  def graphToJs(graph: Graph[Value]) = new JsGraph[JsValue] {
    val values = graph.values.map(valueToJs).toJSArray
    val edges = graph.edges.map(_.map(edgeToJs).toJSArray).toJSArray
  }

  def graphOf(graph: JsGraph[JsValue]) = Graph(
    values = graph.values.toIndexedSeq.map(valueOf),
    edges = graph.edges.toIndexedSeq.map(_.toSet.map(edgeOf))
  )

  def colorToJs(c: Color) = c.c

  def colorOf(c: Int) = Color(c)

  def playerToJs(p: Player) = p.p

  def playerOf(p: Int) = Player(p)

  def scoreToJs(s: Score) = s.s

  def scoreOf(s: Int) = Score(s)

  def valueToJs(value: Value) = new JsValue {
    val v = value.v
    val c = value.c.map(colorToJs).orUndefined
  }

  def valueOf(v: JsValue) =
    Value(v.v, v.c.toOption.map(colorOf))

  def edgeToJs(e: (Index, Index)) = new JsEdge {
    val from = e._1.i
    val to = e._2.i
  }

  def edgeOf(e: JsEdge) =
    (indexOf(e.from), indexOf(e.to))

  def emptiesToJs(es: IndexedSeq[Index]) =
    es.map(indexToJs).toJSArray

  def directionsToJs(dirs: IndexedSeq[Direction]) =
    dirs.map(directionToJs).toJSArray

  def historyEntryToJs(entry: Game.HistoryEntry) = new JsHistoryEntry {
    val put = entry._1.map(indexToJs).orUndefined
    val dir = entry._2.map(directionToJs).orUndefined
  }

  def historyEntryOf(entry: JsHistoryEntry) =
    (entry.put.toOption.map(indexOf), entry.dir.toOption.map(directionOf))

  def indexToJs(i: Index) = i.i

  def indexOf(i: Int) = Index(i)

  def directionToJs(d: Direction) = d.d

  def directionOf(d: Int) = Direction(d)

  def emptyPickerOf(
    picker: js.Function1[IndexedSeq[Index], js.Promise[Index]]
  ) =
    picker andThen (_.toFuture)

  def directionPickerOf(
    picker: js.Function2[Graph[Value], IndexedSeq[Direction], js.Promise[Index]]
  ) =
    { (g: Graph[Value], ds:  IndexedSeq[Direction]) => picker(g, ds).toFuture }

  def resultHandlerOf(
    handler: js.Function3[Game.HistoryEntry, Graph[Value], IndexedSeq[Score], js.Promise[Unit]]
  ) =
    { (entry: Game.HistoryEntry, g: Graph[Value], ss: IndexedSeq[Score]) => handler(entry, g, ss).toFuture }
}