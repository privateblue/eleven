package js

import scala.scalajs.js
import scala.scalajs.js.annotation._

@ScalaJSDefined
abstract class JsEdge extends js.Object {
  val from: Int
  val to: Int
}

@ScalaJSDefined
abstract class JsHistoryEntry extends js.Object {
  val put: js.UndefOr[Int]
  val dir: js.UndefOr[Int]
}

@ScalaJSDefined
abstract class JsDAG extends js.Object {
  val size: Int
  val edges: js.Array[JsEdge]
}

@ScalaJSDefined
abstract class JsGraph[T] extends js.Object {
  val values: js.Array[T]
  val dirs: js.Array[JsDAG]
}

@ScalaJSDefined
abstract class JsValue extends js.Object {
  val v: Int
  val c: js.UndefOr[Int]
}

@ScalaJSDefined
abstract class JsGame extends js.Object {
  val state: String
  val winner: js.UndefOr[Int]
  val graph: JsGraph[JsValue]
  val history: js.Array[JsHistoryEntry]
  val scores: js.Array[Int]
}