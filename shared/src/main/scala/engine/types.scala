package engine

import graph._

case class Player(p: Int)

object Score {
  val Max = Score(Int.MaxValue)

  implicit val ordering = math.Ordering.by[Score, Int](_.s)
}

case class Score(s: Int) {
  def +(that: Score): Score = Score(s + that.s)
}

object Value {
  val empty = Value(0, None)

  val ordering = math.Ordering.by[Value, Int](_.v)
}

case class Value(v: Int, c: Option[Color]) {
  val eleven = v == 11
  def next = Value(v + 1, c)
  def prev = Value(v - 1, c)
  def paint(nc: Color) = Value(v, Some(nc))
  def toScore = Score(v)
}