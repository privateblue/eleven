package engine

import graph._

import org.scalatest._

class WriteBackReducerSpec extends FlatSpec with Matchers {
  val c1 = Color(0)
  val c2 = Color(1)
  val c3 = Color(2)
  val d1 = Direction(0)

  val _0 = Value.empty
  val _1 = _0.next.paint(c1)
  val _2 = _1.next.paint(c1)
  val _3 = _2.next.paint(c1)

  val g = Graph.empty[Value]

  def testSingleDirection(dag: DAG, a: IndexedSeq[Value], b: IndexedSeq[Value], score: Score, next: Color = c1) = {
    val before = g.update(a).add(dag)
    val after = g.update(b).add(dag)
    val (reduced, scored) = WriteBackReducer.reduce(next, d1, before)
    reduced.values shouldBe after.values
    scored shouldBe score
  }

  // 1 - 1 - 0 - 2 - 1 - 0 - 2 - 0 - 2 - 0 - 0 - 0  ===>  0 - 0 - 0 - 0 - 0 - 0 - 0 - 0 - 2 - 2 - 1 - 3

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
  val values2a = IndexedSeq(_0,_2,_1,_0,_1,_2,_0,_1)
  val values2b = IndexedSeq(_0,_0,_1,_2,_0,_2,_0,_2)

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
    testSingleDirection(dag3, values3a, values3b, Score.Zero)
  }



  // 1*- 1 - 0 - 2 - 1 - 0 - 2*- 0 - 2 - 0 - 0 - 0  ===>  0 - 2*- 0 - 2 - 1 - 0 - 0 - 0 - 3*- 0 - 0 - 0

  val dag4 = DAG.empty
    .add().add(0).add(1).add(2).add(3).add(4)
    .add(5).add(6).add(7).add(8).add(9).add(10)
  val values4a = IndexedSeq(_1.paint(c2),_1          ,_0,_2,_1,_0,_2.paint(c2),_0,_2          ,_0,_0,_0)
  val values4b = IndexedSeq(_0          ,_2.paint(c2),_0,_2,_1,_0,_0          ,_0,_3.paint(c2),_0,_0,_0)

  "WriteBackReducer" should "reduce multiplayer trivial steal correctly" in {
    testSingleDirection(dag4, values4a, values4b, _3.toScore + _2.toScore, c2)
  }



  // 1*           0
  //   \            \
  // 0 - 1  ===>  0 - 2*
  //   /            /
  // 0            0

  val dag5 = DAG.empty
    .add()      // 0
    .add()      // 1
    .add()      // 2
    .add(0,1,2) // 3
  val values5a = IndexedSeq(_1.paint(c2),_0,_0,_1)
  val values5b = IndexedSeq(_0          ,_0,_0,_2.paint(c2))

  "WriteBackReducer" should "reduce multiplayer merge steal correctly" in {
    testSingleDirection(dag5, values5a, values5b, _2.toScore, c2)
  }



  //     0            1*
  //   /            /
  // 1*- 1  ===>  0 - 2*
  //   \            \
  //     0            1*

  val dag6 = DAG.empty
    .add()      // 0
    .add(0)     // 1
    .add(0)     // 2
    .add(0)     // 3
  val values6a = IndexedSeq(_1.paint(c2),_0          ,_1          ,_0)
  val values6b = IndexedSeq(_0          ,_1.paint(c2),_2.paint(c2),_1.paint(c2))

  "WriteBackReducer" should "reduce multiplayer branch steal correctly" in {
    testSingleDirection(dag6, values6a, values6b, _2.toScore, c2)
  }



  //     0                0
  //   /   \            /   \
  // 1*- 0 - 1  ===>  0 - 0 - 2*
  //   \   /            \   /
  //     0                0

  val dag7 = DAG.empty
    .add()      // 0
    .add(0)     // 1
    .add(0)     // 2
    .add(0)     // 3
    .add(1,2,3) // 4
  val values7a = IndexedSeq(_1.paint(c2),_0,_0,_0,_1)
  val values7b = IndexedSeq(_0          ,_0,_0,_0,_2.paint(c2))

  "WriteBackReducer" should "reduce multiplayer branch-and-merge steal correctly 1" in {
    testSingleDirection(dag7, values7a, values7b, _2.toScore, c2)
  }

  //     0                0
  //   /   \            /   \
  // 1*-(2)- 1  ===>  1*-(2)- 1
  //   \   /            \   /
  //     0                0

  val dag8 = DAG.empty
    .add()      // 0
    .add(0)     // 1
    .add(0)     // 2
    .add(0)     // 3
    .add(1,2,3) // 4
  val values8a = IndexedSeq(_1.paint(c2),_0,_2.paint(c3),_0,_1)
  val values8b = IndexedSeq(_1.paint(c2),_0,_2.paint(c3),_0,_1)

  "WriteBackReducer" should "reduce multiplayer branch-and-merge steal correctly 2" in {
    testSingleDirection(dag8, values8a, values8b, Score.Zero, c2)
  }
}