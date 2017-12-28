package engine

import graph._

case class Player(p: Int)

object Score {
  val Max = Score(Int.MaxValue)
}

case class Score(s: Int) {
  def +(that: Score): Score = Score(s + that.s)
}