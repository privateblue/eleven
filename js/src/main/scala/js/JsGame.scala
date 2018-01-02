package js

import graph._
import engine._
import games._

import scala.concurrent.Future

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js
import scala.scalajs.js.annotation._
import scala.scalajs.js.JSConverters._

@JSExportTopLevel("Game")
object JsGame {
  @JSExport
  def graphOriginal = Graphs.original

  @JSExport
  def start(graph: Graph[Value], players: Int) = Game.start(graph, players)

  @JSExport
  def move(
    state: Continued,
    emptyPicker: Game.EmptyPicker,
    directionPicker: Game.DirectionPicker,
    resultHandler: Game.ResultHandler
  ) = Game.move(state, emptyPicker, directionPicker, resultHandler)

  @JSExport
  def randomEmpty(state: Continued) = Game.randomEmpty(state)

  @JSExport
  def bestMove(state: Continued) = Game.bestMove(state)

  @JSExport
  def gameToJs(game: Game) = ToJs.to(game)

  @JSExport
  def graphToJs(graph: Graph[Value]) = ToJs.to(graph)

  @JSExport
  def emptiesToJs(es: IndexedSeq[Index]) = ToJs.to(es)

  @JSExport
  def directionsToJs(dirs: IndexedSeq[Direction]) = ToJs.to(dirs)

  @JSExport
  def historyEntryToJs(entry: Game.HistoryEntry) = ToJs.to(entry)

  @JSExport
  def scoresToJs(scores: IndexedSeq[Score]) = ToJs.to(scores)

  @JSExport
  def nextToJs(next: Future[Game]) = next.toJSPromise

  @JSExport
  def indexOf(i: Int) = Index(i)

  @JSExport
  def directionOf(d: Int) = Direction(d)

  @JSExport
  def emptyPickerOf(
    picker: js.Function1[IndexedSeq[Index], js.Promise[Index]]
  ) =
    picker andThen (_.toFuture)

  @JSExport
  def directionPickerOf(
    picker: js.Function2[Graph[Value], IndexedSeq[Direction], js.Promise[Index]]
  ) =
    { (g: Graph[Value], ds:  IndexedSeq[Direction]) => picker(g, ds).toFuture }

  @JSExport
  def resultHandlerOf(
    handler: js.Function3[Game.HistoryEntry, Graph[Value], IndexedSeq[Score], js.Promise[Unit]]
  ) =
    { (entry: Game.HistoryEntry, g: Graph[Value], ss: IndexedSeq[Score]) => handler(entry, g, ss).toFuture }
}