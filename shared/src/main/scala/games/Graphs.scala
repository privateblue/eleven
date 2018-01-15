package games

import graph._
import engine._

object Graphs {
  val theOriginal = {
    val right = DAG.empty
      .add().add(0).add(1).add(2)
      .add().add(4).add(5).add(6)
      .add().add(8).add(9).add(10)
      .add().add(12).add(13).add(14)

    val left = right.inverted

    val down = DAG.empty
      .add().add().add().add()
      .add(0).add(1).add(2).add(3)
      .add(4).add(5).add(6).add(7)
      .add(8).add(9).add(10).add(11)

    val up = down.inverted

    Graph.empty[Value]
      .update(IndexedSeq.fill(16)(Value.empty))
      .add(right).add(left).add(down).add(up)
  }

  val twoByTwo = {
    val right = DAG.empty
      .add().add(0)
      .add().add(2)

    val left = right.inverted

    val down = DAG.empty
      .add().add()
      .add(0).add(1)

    val up = down.inverted

    Graph.empty[Value]
      .update(IndexedSeq.fill(4)(Value.empty))
      .add(right).add(left).add(down).add(up)
  }

  val theEye = {
    val right = DAG.empty
      .add()
      .add(0).add(0)
      .add(1).add(1).add(2).add(2)
      .add(3).add(3).add(4).add(4).add(5).add(5).add(6).add(6)
      .add(7,8).add(9,10).add(11,12).add(13,14)
      .add(15,16).add(17,18)
      .add(19,20)

    val left = right.inverted

    val down = DAG.empty
      .add()
      .add().add(1)
      .add().add(3).add(4).add(5)
      .add().add(7).add(8).add(9).add(10).add(11).add(12).add(13)
      .add().add(15).add(16).add(17)
      .add().add(19)
      .add()

    val up = down.inverted

    Graph.empty[Value]
      .update(IndexedSeq.fill(22)(Value.empty))
      .add(right).add(left).add(down).add(up)
  }

  val theCourt = {
    val right = DAG.empty
      .add().add(0).add(1)
      .add().add(3).add(4).add(5)
      .add().add(7).add(8)
      .add().add(10).add(11).add(12)
      .add().add(14).add(15)

    val left = right.inverted

    val down = DAG.empty
      .add().add().add()
      .add(0).add(1).add(1).add(2)
      .add(3).add(4,5).add(6)
      .add(7).add(8).add(8).add(9)
      .add(10).add(11,12).add(13)

    val up = down.inverted

    Graph.empty[Value]
      .update(IndexedSeq.fill(17)(Value.empty))
      .add(right).add(left).add(down).add(up)
  }
}