package js

import graph._
import engine._
import games._

import scala.concurrent.Future

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js
import scala.scalajs.js.annotation._

@JSExportTopLevel("Game")
object JsGame {
  @JSExport
  def graphOriginal = Graphs.original

  @JSExport
  def start(graph: Graph[Value], players: Int) = Game.start(graph, players)

  @JSExport
  def move(
    state: Continued,
    emptyPicker: (IndexedSeq[Index]) => Future[Index],
    directionPicker: (Graph[Value], IndexedSeq[Direction]) => Future[Direction],
    resultHandler: (Graph[Value], IndexedSeq[Score]) => Future[Unit]
  ) = Game.move(state, emptyPicker, directionPicker, resultHandler)

  @JSExport
  def gameToJs(game: Game) = ToJs.to(game)

  @JSExport
  def graphToJs(graph: Graph[Value]) = ToJs.to(graph)

  @JSExport
  def indexOf(i: Int) = Index(i)

  @JSExport
  def directionOf(d: Int) = Direction(d)

  @JSExport
  def emptyPickerOf(picker: js.Function1[IndexedSeq[Index], js.Promise[Index]]) =
    picker andThen (_.toFuture)

  @JSExport
  def directionPickerOf(picker: js.Function2[Graph[Value], IndexedSeq[Direction], js.Promise[Index]]) =
    { (g: Graph[Value], ds:  IndexedSeq[Direction]) => picker(g, ds).toFuture }
}