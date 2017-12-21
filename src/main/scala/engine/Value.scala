package engine

import graph._

import cats.kernel.Order
import cats.implicits._

object Value {
  val empty = Value(0, None)

  implicit val order: Order[Value] = Order.by(_.v)
}

case class Value(v: Int, c: Option[Color]) {
  def next = Value(v + 1, c)
  def prev = Value(v - 1, c)
  def paint(nc: Color) = Value(v, Some(nc))
}
