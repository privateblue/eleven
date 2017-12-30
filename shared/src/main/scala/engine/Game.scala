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
    val maxDepth = players * state.graph.values.size / math.max(1, empties(state.graph).size) + 1
    println(s"MAX DEPTH: $maxDepth")

    def best(
      player: Player,
      graph: Graph[Value],
      scores: IndexedSeq[Score],
      depth: Int = 0
    ): (Option[Index], Option[Direction], IndexedSeq[Score]) = {
      val elevs = elevens(graph)
      if (elevs.size > 0) (None, None, scores.updated(elevs.head.p, Score.Max))
      else if (depth >= maxDepth) (None, None, scores)
      else {
        val color = Color(player.p)
        val starter = Value.empty.paint(color).next
        val next = Player((player.p + 1) % players)
        val (_, pute, putd, putss, _) =
          graph.values.foldLeft((0, Option.empty[Index], Option.empty[Direction], IndexedSeq.empty[Score], Int.MinValue)) {
            case ((i, be, bd, bs, beval), v) if v == Value.empty =>
              val e = Index(i)
              val gp = graph.set(e, starter)
              val (redd, redss, redeval) =
                0.until(gp.edges.size).foldLeft((Option.empty[Direction], IndexedSeq.empty[Score], Int.MinValue)) {
                  case ((bbd, bbs, bbeval), dir) =>
                    val d = Direction(dir)
                    val gr = WriteBackReducer.reduce(color, d, gp)
                    if (gr.values == gp.values) (bbd, bbs, bbeval)
                    else {
                      val added = updatedScores(scores, player, scored(gp, gr))
                      //println(("\t" * depth) + s"depth $depth player ${player.p} empty $i dir $dir scores $added")
                      val (_, _, ss) = best(next, gr, added, depth + 1)
                      val eval = evaluate(player, ss)
                      if (eval > bbeval) (Some(d), ss, eval) else (bbd, bbs, bbeval)
                    }
                }
              val (d, ss, eval) =
                if (redd.isDefined) (redd, redss, redeval)
                else {
                  //println(("\t" * depth) + s"depth $depth player ${player.p} empty $i dir no dir scores $scores")
                  val (_, _, noredss) = best(next, gp, scores, depth + 1)
                  val noredeval = evaluate(player, noredss)
                  (None, noredss, noredeval)
                }
              if (eval > beval) (i + 1, Some(e), d, ss, eval)
              else (i + 1, be, bd, bs, beval)
            case ((i, be, bd, bs, beval), v) =>
              (i + 1, be, bd, bs, beval)
          }
        if (pute.isDefined) (pute, putd, putss)
        else {
          //println(("\t" * depth) + s"depth $depth player ${player.p} empty no empty dir no dir scores $scores")
          val (_, _, noputss) = best(next, graph, scores, depth + 1)
          (None, None, noputss)
        }
      }
    }
    def evaluate(player: Player, scores: IndexedSeq[Score]): Int =
      2 * scores(player.p).s - scores.map(_.s).sum

    val player = Player(state.history.size % players)
    val s = System.currentTimeMillis
    val (e, d, _) = best(player, state.graph, state.scores)
    println(s"MOVE TIME: ${System.currentTimeMillis - s}")
    move(
      state = state,
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