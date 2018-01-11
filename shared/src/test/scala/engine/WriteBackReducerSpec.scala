package engine

import graph._

import org.scalatest._

class WriteBackReducerSpec extends FlatSpec with Matchers {
  val c1 = Color(0)
  val d1 = Direction(0)

  val _0 = Value.empty
  val _1 = _0.next.paint(c1)
  val _2 = _1.next.paint(c1)
  val _3 = _2.next.paint(c1)

  val g = Graph.empty[Value]

  def testSingleDirection(dag: DAG, a: IndexedSeq[Value], b: IndexedSeq[Value], score: Score) = {
    val before = g.update(a).add(dag)
    val after = g.update(b).add(dag)
    val (reduced, scored) = WriteBackReducer.reduce(c1, d1, before)
    (reduced, scored) shouldBe (after, score)
  }

  val dag1 = DAG.empty
    .add().add(0).add(1).add(2).add(3).add(4)
    .add(5).add(6).add(7).add(8).add(9).add(10)
  val values1a = IndexedSeq(_1,_1,_0,_2,_1,_0,_2,_0,_2,_0,_0,_0)
  val values1b = IndexedSeq(_0,_0,_0,_0,_0,_0,_0,_0,_2,_2,_1,_3)

  "WriteBackReducer" should "reduce trivial case correctly" in {
    testSingleDirection(dag1, values1a, values1b, _3.toScore + _2.toScore)
  }



  // 0 - 1                    0 - 1
  //       \                        \
  //         1 - 0 - 1  ===>          0 - 0 - 2
  //       /                        /
  // 2 - 0                    0 - 2
  //       \                        \
  //         0                        0

  val dag2 = DAG.empty
    .add()     // 0
    .add()     // 1
    .add(0)    // 2
    .add(1)    // 3
    .add(2, 3) // 4
    .add(3)    // 5
    .add(4)    // 6
    .add(6)    // 7
  val values2a = IndexedSeq(_0,_2,_1,_0,_1,_0,_0,_1)
  val values2b = IndexedSeq(_0,_0,_1,_2,_0,_0,_0,_2)

  "WriteBackReducer" should "reduce simple branching dag correctly" in {
    testSingleDirection(dag2, values2a, values2b, _2.toScore)
  }



  // 1 ----- 1
  //   \   /
  // 1 - 0 -----
  //   /   \     \
  // 2 ----- 2     3
  //   \   /     /
  // 2 - 0 -----
  //       \
  //         1

  val dag3 = DAG.empty
    .add()       // 0
    .add()       // 1
    .add()       // 2
    .add()       // 3
    .add(0,1,2)  // 4
    .add(2,3)    // 5
    .add(0,4)    // 6
    .add(4,2,5)  // 7
    .add(4,5)    // 8
    .add(5)      // 9
  val values3a = IndexedSeq(_1,_1,_2,_2,_0,_0,_1,_2,_3,_1)
  val values3b = IndexedSeq(_1,_1,_2,_0,_0,_2,_1,_2,_3,_1)

  "WriteBackReducer" should "reduce classic branching example correctly" in {
    testSingleDirection(dag3, values3a, values3b, Score(0))
  }
}