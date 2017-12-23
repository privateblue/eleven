package engine

import graph._

case class Player(p: Int)

case class Score(s: Int) {
  def +(that: Score): Score = Score(s + that.s)
}