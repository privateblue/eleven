package engine

import graph._

import scala.util.Random
import scala.concurrent.Future
import scala.concurrent.ExecutionContext

sealed trait Game

case class Continued(
  graph: Graph[Value],
  history: List[Game.HistoryEntry],
  scores: IndexedSeq[Score]
) extends Game

case class NoMoreMoves(
  winner: Player,
  graph: Graph[Value],
  history: List[Game.HistoryEntry],
  scores: IndexedSeq[Score]
) extends Game

case class Eleven(
  winner: Player,
  graph: Graph[Value],
  history: List[Game.HistoryEntry],
  scores: IndexedSeq[Score]
) extends Game

object Game {
  type HistoryEntry = (Option[Index], Option[Direction])
  type EmptyPicker = IndexedSeq[Index] => Future[Index]
  type DirectionPicker = (Graph[Value], IndexedSeq[Direction]) => Future[Direction]
  type ResultHandler = (HistoryEntry, Graph[Value], IndexedSeq[Score]) => Future[Unit]

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
      entry = (e, dir)
      reduced = dir.map(WriteBackReducer.reduce(color, _, put)).getOrElse(put)
      score = scored(put, reduced)
      added = updatedScores(state.scores, next, score)
      _ <- resultHandler(entry, reduced, added)
      elevs = elevens(reduced)
      moves = 0 until players flatMap (c => reducibles(Color(c), reduced))
    } yield {
      if (elevs.size > 0)
        Eleven(elevs.head, reduced, entry :: state.history, added)
      else if (empties(reduced).size == 0 && moves.size == 0)
        NoMoreMoves(leader(added), reduced, entry :: state.history, added)
      else
        Continued(reduced, entry :: state.history, added)
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
      depth: Int = 0
    ): (Option[Index], Option[Direction], IndexedSeq[Score]) = {
      val elevs = elevens(graph)
      if (elevs.size > 0) (None, None, scores.updated(elevs.head.p, Score.Max))
      else if (depth >= 2) (None, None, scores)
      else {
        val color = Color(player.p)
        val starter = Value.empty.paint(color).next
        val next = Player((player.p + 1) % players)
        val p = graph.indices.collect {
          case i if graph.at(i) == Value.empty => Some(i) -> graph.set(i, starter)
        }
        val put = if (p.isEmpty) IndexedSeq(None -> graph) else p
        val childScores = put.flatMap { case (e, gp) =>
          val reds = 0.until(gp.edges.size).flatMap { dir =>
            val d = Direction(dir)
            val gr = WriteBackReducer.reduce(color, d, gp)
            if (gr.values != gp.values) IndexedSeq(Some(d) -> gr) else IndexedSeq()
          }
          val reduced = if (reds.isEmpty) IndexedSeq(None -> gp) else reds
          reduced.map { case (d, gr) =>
            val added = updatedScores(scores, player, scored(gp, gr))
            //println(("\t" * depth) + s"depth $depth player ${player.p} empty ${e.map(_.i)} dir ${d.map(_.d)} scores $added")
            val (_, _, ss) = best(next, gr, added, depth + 1)
            (e, d, ss, evaluate(player, ss))
          }
        }
        // sort desc, so top is at head
        val (e, d, ss, _) = childScores.sortBy(-_._4).head
        (e, d, ss)
      }
    }
    def evaluate(player: Player, scores: IndexedSeq[Score]): Int =
      // player's score minus sum of other players' score
      2 * scores(player.p).s - scores.map(_.s).sum

    val player = Player(state.history.size % players)
    val f = System.currentTimeMillis
    val (e, d, _) = best(player, state.graph, state.scores)
    println(System.currentTimeMillis - f)
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