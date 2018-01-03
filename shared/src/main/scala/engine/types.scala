package engine

import graph._

import cats.kernel.Order
import cats.implicits._

case class Player(p: Int)

object Score {
  val Max = Score(Int.MaxValue)

  implicit val order: Order[Score] = Order.by(_.s)
  implicit val ordering = order.toOrdering
}

case class Score(s: Int) {
  def +(that: Score): Score = Score(s + that.s)
}