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
  winner: Player,
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
  type EmptyPicker = IndexedSeq[Index] => Future[Index]
  type DirectionPicker = (Graph[Value], IndexedSeq[Direction]) => Future[Direction]
  type ResultHandler = (Graph[Value], IndexedSeq[Score]) => Future[Unit]

  def start(graph: Graph[Value], players: Int): Game =
    Continued(graph, List.empty, IndexedSeq.fill(players)(Score(0)))

  def move(
    state: Continued,
    emptyPicker: EmptyPicker,
    directionPicker: DirectionPicker,
    resultHandler: ResultHandler
  )(implicit ec: ExecutionContext): Future[Game] = {
    val players = state.scores.size
    val next = Player(state.history.size % players)
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
      moves = 0 until players flatMap (c => reducibles(Color(c), reduced))
    } yield {
      if (elevs.size > 0)
        Eleven(elevs.head, reduced, (e, dir) :: state.history, added)
      else if (empties(reduced).size == 0 && moves.size == 0)
        NoMoreMoves(leader(added), reduced, (e, dir) :: state.history, added)
      else
        Continued(reduced, (e, dir) :: state.history, added)
    }
  }

  def randomEmptyPicker(es: IndexedSeq[Index]): Future[Index] =
    Future.successful(Random.shuffle(es).head)

  def bestMove(
    state: Continued,
    resultHandler: ResultHandler
  )(implicit ec: ExecutionContext): Future[Game] = {
    val players = state.scores.size

    def best(
      player: Player,
      graph: Graph[Value],
      scores: IndexedSeq[Score],
      depth: Int
    ): (Option[Index], Option[Direction], IndexedSeq[Score]) = {
      val elevs = elevens(graph)
      val moves = 0 until players flatMap (c => reducibles(Color(c), graph))
      if (elevs.size > 0)
        (None, None, scores.updated(elevs.head.p, Score.Max))
      else if (depth == 0 || (empties(graph).size == 0 && moves.size == 0))
        (None, None, scores)
      else {
        val color = Color(player.p)
        val starter = Value.empty.paint(color).next
        val next = Player((player.p + 1) % players)
        val es = empties(graph)
        val put =
          if (!es.isEmpty) es.map(e => Some(e) -> graph.set(e, starter))
          else IndexedSeq(None -> graph)
        val childScores = for {
          (e, gp) <- put
          reds = reducibles(color, gp)
          reduced =
            if (!reds.isEmpty)
              reds.map(d => Some(d) -> WriteBackReducer.reduce(color, d, gp))
            else
              IndexedSeq(None -> gp)
          (d, gr) <- reduced
          added = updatedScores(scores, next, scored(gp, gr))
          (_, _, ss) = best(next, gr, added, depth - 1)
        } yield (e, d, ss)
        childScores
          // sort by player's score minus sum of other players' score
          .sortBy { case (e, d, ss) => 2 * ss(player.p).s - ss.map(_.s).sum }
          .head
      }
    }

    val player = Player(state.history.size % players)
    val (e, d, _) = best(player, state.graph, state.scores, 1)
    move(
      state = state,
      // if e is None, emptyPicker won't be called at all
      // same applies to d and directionPicker
      emptyPicker = _ => Future.successful(e.get),
      directionPicker = (_, _) => Future.successful(d.get),
      resultHandler = resultHandler
    )
  }

  def empties(graph: Graph[Value]): IndexedSeq[Index] =
    graph.indices.filter(graph.at(_) == Value.empty)

  def reducibles(color: Color, g: Graph[Value]): IndexedSeq[Direction] =
    g.directions.filter(WriteBackReducer.reduce(color, _, g).values != g.values)

  def elevens(graph: Graph[Value]): IndexedSeq[Player] =
    graph.values.filter(_.eleven).flatMap(_.c).map(c => Player(c.c))

  def updatedScores(
    scores: IndexedSeq[Score],
    player: Player,
    score: Score
  ): IndexedSeq[Score] =
    scores.updated(player.p, scores(player.p) + score)

  def scored(prev: Graph[Value], cur: Graph[Value]): Score = {
    val p = prev.values.map(_.v)
    val c = cur.values.map(_.v)
    val removed = p diff c
    val appeared = c diff p
    val appearedFrom = appeared.flatMap(v => IndexedSeq(v - 1, v - 1))
    val s = appeared.sum + (removed diff appearedFrom sum)
    Score(s)
  }

  def leader(scores: IndexedSeq[Score]): Player =
    Player(scores.indexOf(scores.map(_.s).max))
}