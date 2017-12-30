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

  def bestMove(state: Continued): Game.HistoryEntry = {
    val players = state.scores.size
    val maxDepth = 1 + (math.sqrt(state.graph.values.size) / math.sqrt(math.max(1, empties(state.graph).size))).toInt
    val noempty = Option.empty[Index]
    val nodir = Option.empty[Direction]
    val initscores = Vector.fill(players)(Double.MinValue)
    def zerosum(scores: Vector[Double]): Vector[Double] =
      scores.map(_ - scores.sum / scores.size)
    def hypermax(
      player: Int,
      graph: Graph[Value],
      scores: Vector[Double],
      alpha: Vector[Double] = initscores,
      depth: Int = 0
    ): (Option[Index], Option[Direction], Vector[Double]) = {
      val elevs = elevens(graph)
      if (elevs.size > 0) {
        val won = scores.updated(elevs.head.p, Double.MaxValue)
        (None, None, zerosum(won))
      } else if (depth >= maxDepth) (None, None, zerosum(scores))
      else {
        val color = Color(player)
        val starter = Value.empty.paint(color).next
        val next = (player + 1) % players
        val eszero = (0, List.empty[(Option[Index], Graph[Value])])
        val (_, empties) = graph.values.foldLeft(eszero) {
          case ((i, es), v) if v == Value.empty =>
            val e = Index(i)
            val gp = graph.set(e, starter)
            (i + 1, (Some(e) -> gp) :: es)
          case ((i, es), v) =>
            (i + 1, es)
        }
        val put = if (empties.nonEmpty) empties else List(None -> graph)
        val children = put.flatMap { case (e, gp) =>
          val reds = 0.until(gp.edges.size).toList.flatMap { dir =>
            val d = Direction(dir)
            val gr = WriteBackReducer.reduce(color, d, gp)
            if (gr.values == gp.values) List()
            else {
              val added = scores.updated(player, scored(gp, gr).s.toDouble)
              List((e, Some(d), gr, added))
            }
          }
          if (reds.nonEmpty) reds else List((e, None, gp, scores))
        }
        val reszero = (true, false, noempty, nodir, initscores, alpha)
        val (_, _, e, d, ss, _) = children.foldLeft(reszero) {
          case ((first, prune, be, bd, bs, a), (e, d, gr, added)) if prune =>
            (first, prune, be, bd, bs, a)
          case ((first, prune, be, bd, bs, a), (e, d, gr, added)) =>
            val (_, _, ss) = hypermax(next, gr, added, a, depth + 1)
            if (a(player) < ss(player)) {
              val na = a.updated(player, ss(player))
              (false, na.sum >= 0, e, d, ss, na)
            } else (false, prune, be, bd, if (first) ss else bs, a)
        }
        (e, d, ss)
      }
    }
    val p = state.history.size % players
    val ss = state.scores.map(_.s.toDouble).toVector
    val s = System.currentTimeMillis
    val (e, d, _) = hypermax(p, state.graph, ss)
    println(s"HYPERMAX DEPTH: $maxDepth MOVE TIME: ${System.currentTimeMillis - s}")
    (e, d)
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