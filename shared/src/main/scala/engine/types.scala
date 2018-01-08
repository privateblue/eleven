package engine

import graph._

case class Color(c: Int)

case class Player(p: Int)

object Score {
  val Max = Score(Int.MaxValue)

  implicit val ordering = math.Ordering.by[Score, Int](_.s)
}

case class Score(s: Int) {
  def +(that: Score): Score = Score(s + that.s)
}

object Value {
  val empty = Value(1, None)

  val ordering = math.Ordering.by[Value, Int](_.v)
}

case class Value(v: Int, c: Option[Color]) {
  def eleven = v == Value.empty.next.next.next.next.next.next.next.next.next.next.next.v
  def next = Value(v * 2, c)
  def paint(nc: Color) = Value(v, Some(nc))
  def toScore = Score(v)
}