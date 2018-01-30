package engine

import graph._

object Color {
  def apply(c: Int) = new Color(c)
}
class Color(val c: Int) extends AnyVal {
  override def toString = c.toString
}

object Player {
  def apply(p: Int) = new Player(p)
}
class Player(val p: Int) extends AnyVal {
  override def toString = p.toString
}

object Score {
  def apply(s: Int) = new Score(s)
  val Max = apply(Int.MaxValue)
  val Zero = apply(0)
  implicit val ordering = math.Ordering.by[Score, Int](_.s)
}
class Score(val s: Int) extends AnyVal {
  def +(that: Score): Score = Score(s + that.s)
  def -(that: Score): Score = Score(s - that.s)
  override def toString = s.toString
}

object Value {
  val empty = Value(1, None)
  val ordering = math.Ordering.by[Value, Int](_.v)
}
case class Value(v: Int, c: Option[Color]) {
  def eleven = v == Value.empty.next.next.next.next.next.next.next.next.next.next.next.v
  def next = Value(v * 2, c)
  def previous = if (this == Value.empty || this == Value.empty.next) Value.empty else Value(v / 2, c)
  def paint(nc: Color) = Value(v, Some(nc))
  def toScore = if (this == Value.empty) Score.Zero else Score(v)
}