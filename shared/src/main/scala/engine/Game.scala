package engine

import graph._

import scala.util.Random
import scala.concurrent.Future
import scala.concurrent.ExecutionContext

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
    emptyPicker: (IndexedSeq[Index]) => Future[Index],
    directionPicker: (Graph[Value], IndexedSeq[Direction]) => Future[Direction],
    resultHandler: (Graph[Value], IndexedSeq[Score]) => Future[Unit]
  )(implicit ec: ExecutionContext): Future[Game] = {
    val next = Player(state.history.size % state.scores.size)
    val color = Color(next.p)
    val starter = Value.empty.paint(color).next
    val es = empties(state.graph)
    for {
      e <- if (!es.isEmpty) emptyPicker(es).map(Option.apply)
           else Future.successful(None)
      put = e.map(state.graph.set(_, starter)).getOrElse(state.graph)
      reds = reducibles(color, put)
      dir <- if (!reds.isEmpty) directionPicker(put, reds).map(Option.apply)
             else Future.successful(None)
      reduced = dir.map(WriteBackReducer.reduce(color, _, put)).getOrElse(put)
      score = scored(put, reduced)
      added = updatedScores(state.scores, next, score)
      _ <- resultHandler(reduced, added)
      elevs = elevens(reduced)
      moves = 0 until state.scores.size flatMap (c => reducibles(Color(c), reduced))
    } yield {
      if (elevs.size > 0)
        Eleven(elevs.head, reduced, (e, dir) :: state.history, added)
      else if (empties(reduced).size == 0 && moves.size == 0)
        NoMoreMoves(reduced, (e, dir) :: state.history, added)
      else
        Continued(reduced, (e, dir) :: state.history, added)
    }
  }

  def randomMove(
    state: Continued,
    directionPicker: (Graph[Value], IndexedSeq[Direction]) => Future[Direction],
    resultHandler: (Graph[Value], IndexedSeq[Score]) => Future[Unit]
  )(implicit ec: ExecutionContext): Future[Game] =
    move(state, {es => Future.successful(Random.shuffle(es).head)}, directionPicker, resultHandler)


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