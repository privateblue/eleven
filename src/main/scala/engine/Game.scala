package engine

import graph._

sealed trait Game

case class Continued(
  graph: Graph[Value],
  history: List[(Option[Index], Option[Direction])],
  scores: IndexedSeq[Score]
) extends Game

case class NoMoreMoves(
  graph: Graph[Value],
  history: List[(Option[Index], Option[Direction])],
  scores: IndexedSeq[Score]
) extends Game

case class Eleven(
  winner: Player,
  graph: Graph[Value],
  history: List[(Option[Index], Option[Direction])],
  scores: IndexedSeq[Score]
) extends Game

object Game {
  def start(graph: Graph[Value], players: Int): Game =
    Continued(graph, List.empty, IndexedSeq.fill(players)(Score(0)))

  def move(
    state: Continued,
    emptyPicker: (IndexedSeq[Index]) => Index,
    directionPicker: (Graph[Value], IndexedSeq[Direction]) => Direction,
    resultHandler: (Graph[Value], IndexedSeq[Score]) => Unit
  ): Game = {
    val next = Player(state.history.size % state.scores.size)
    val color = Color(next.p)
    val starter = Value.empty.paint(color).next
    val es = empties(state.graph)
    val e = if (!es.isEmpty) Some(emptyPicker(es)) else None
    val put = e.map(state.graph.set(_, starter)).getOrElse(state.graph)
    val reds = reducibles(color, put)
    val dir = if (!reds.isEmpty) Some(directionPicker(put, reds)) else None
    val reduced = dir.map(WriteBackReducer.reduce(color, _, put)).getOrElse(put)
    val score = scored(put, reduced)
    val added = updatedScores(state.scores, next, score)
    resultHandler(reduced, added)
    val elevs = elevens(reduced)
    val moves = 0 until state.scores.size flatMap (c => reducibles(Color(c), reduced))
    if (elevs.size > 0)
      Eleven(elevs.head, reduced, (e, dir) :: state.history, added)
    else if (empties(reduced).size == 0 && moves.size == 0)
      NoMoreMoves(reduced, (e, dir) :: state.history, added)
    else
      Continued(reduced, (e, dir) :: state.history, added)
  }

  def empties(graph: Graph[Value]): IndexedSeq[Index] =
    graph.indices.filter(graph.at(_) == Value.empty)

  def reducibles(color: Color, g: Graph[Value]): IndexedSeq[Direction] =
    g.directions.filter(WriteBackReducer.reduce(color, _, g).values != g.values)

  def elevens(graph: Graph[Value]): IndexedSeq[Player] =
    graph.values.filter(_.eleven).flatMap(_.c).map(c => Player(c.c))

  def updatedScores(scores: IndexedSeq[Score], player: Player, score: Score): IndexedSeq[Score] =
    scores.updated(player.p, scores(player.p) + score)

  def scored(prev: Graph[Value], cur: Graph[Value]): Score = {
    val p = prev.values.map(_.v)
    val c = cur.values.map(_.v)
    val removed = p diff c
    val appeared = c diff p
    val s = (appeared sum) + (removed diff (appeared flatMap (v => IndexedSeq(v - 1, v - 1))) sum)
    Score(s)
  }
}