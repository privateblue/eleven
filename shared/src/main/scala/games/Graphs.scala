package games

import graph._
import engine._

object Graphs {
  val original = {
    val right = DAG.empty
      .add(Set()).add(Set(0)).add(Set(1)).add(Set(2))
      .add(Set()).add(Set(4)).add(Set(5)).add(Set(6))
      .add(Set()).add(Set(8)).add(Set(9)).add(Set(10))
      .add(Set()).add(Set(12)).add(Set(13)).add(Set(14))

    val left = right.inverted

    val down = DAG.empty
      .add(Set()).add(Set()).add(Set()).add(Set())
      .add(Set(0)).add(Set(1)).add(Set(2)).add(Set(3))
      .add(Set(4)).add(Set(5)).add(Set(6)).add(Set(7))
      .add(Set(8)).add(Set(9)).add(Set(10)).add(Set(11))

    val up = down.inverted

    Graph.empty[Value]
      .update(IndexedSeq.fill(16)(Value.empty))
      .add(right).add(left).add(down).add(up)
  }
}