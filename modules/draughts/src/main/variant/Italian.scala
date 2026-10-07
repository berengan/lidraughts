package draughts
package variant

import scala.collection.breakOut

/**
 * Italian draughts (Dama Italiana).
 *
 * Phase 1 deliberately keeps tournament/drawing rules separate from move
 * legality. The rules implemented here are the board rules that determine
 * legal moves and captures.
 */
case object Italian extends Variant(
  id = 13,
  gameType = 27,
  key = "italian",
  name = "Italian",
  shortName = "Italian",
  title = "Italian draughts: short-range kings and mandatory capture priorities.",
  standardInitialPosition = false,
  boardSize = Board.D64
) {

  def pieces = Russian.pieces
  def initialFen = Russian.initialFen
  def startingPosition = StartingPosition("---", initialFen, "", "Initial position".some)

  def captureDirs = Standard.captureDirs
  def moveDirsColor = Standard.moveDirsColor
  def moveDirsAll = Standard.moveDirsAll

  override def kingMovesLongRange = false
  override def kingCapturesLongRange = false
  override def captureEndsOnPromotion = true

  // Men capture forward only. Kings capture in both directions.
  override def captureDirsFor(actor: Actor): Directions =
    actor.piece.role match {
      case Man  => moveDirsColor(actor.color)
      case King => moveDirsAll
      case _    => Nil
    }

  // In Italian draughts a man cannot capture a king.
  override def canCapture(actor: Actor, piece: Piece): Boolean =
    super.canCapture(actor, piece) &&
      !(actor.piece.role == Man && piece.role == King)

  /**
   * Italian capture priority, applied in order:
   *   1. capture the greatest number of pieces;
   *   2. with equal length, capture with a king rather than a man;
   *   3. then capture the greatest number of kings;
   *   4. then capture a king as early as possible in the sequence.
   *
   * If several lines are still equal, all remain legal.
   */
  override def validMoves(situation: Situation, finalSquare: Boolean = false): Map[Pos, List[Move]] = {
    val captures = situation.actors.flatMap { actor =>
      actor.getCaptures(finalSquare).map(actor -> _)
    }

    if (captures.nonEmpty) {
      val maxTaken = captures.map(captureLength).max
      val byLength = captures.filter(captureLength(_) == maxTaken)

      val kingCaptures = byLength.filter(_._1.piece.role == King)
      val byCapturingPiece = if (kingCaptures.nonEmpty) kingCaptures else byLength

      val maxKings = byCapturingPiece.map {
        case (actor, move) => capturedKings(actor.board, move)
      }.max
      val byCapturedValue = byCapturingPiece.filter {
        case (actor, move) => capturedKings(actor.board, move) == maxKings
      }

      val earliestKing = byCapturedValue.map {
        case (actor, move) => firstCapturedKing(actor.board, move)
      }.min
      val selected = byCapturedValue.filter {
        case (actor, move) => firstCapturedKing(actor.board, move) == earliestKing
      }

      selected.groupBy(_._1.pos).map {
        case (pos, lines) => pos -> lines.map(_._2).toList
      }
    } else situation.actors.collect {
      case actor if actor.noncaptures.nonEmpty =>
        actor.pos -> actor.noncaptures
    }(breakOut)
  }

  override def validMovesFrom(situation: Situation, pos: Pos, finalSquare: Boolean = false): List[Move] =
    validMoves(situation, finalSquare).getOrElse(pos, Nil)

  private def captureLength(capture: (Actor, Move)): Int =
    capture._2.taken.fold(0)(_.length)

  private def capturedKings(board: Board, move: Move): Int =
    move.taken.toList.flatten.count { pos =>
      board(pos).exists(_.role == King)
    }

  private def firstCapturedKing(board: Board, move: Move): Int = {
    // Capture lists are accumulated in reverse order by the move generator.
    val chronological = move.taken.toList.flatten.reverse
    val index = chronological.indexWhere { pos =>
      board(pos).exists(_.role == King)
    }
    if (index < 0) Int.MaxValue else index
  }

  def maxDrawingMoves(board: Board): Option[Int] = None

  // Draw/repetition rules will be added from the FID technical regulation.
  def updatePositionHashes(board: Board, move: Move, hash: draughts.PositionHash): PositionHash =
    Hash(Situation(board, !move.piece.color))

  override def validSide(board: Board, strict: Boolean)(color: Color) = {
    val roles = board rolesOf color
    (roles.count(_ == Man) > 0 || roles.count(_ == King) > 0) &&
      (!strict || roles.size <= 12) &&
      !menOnPromotionRank(board, color)
  }
}
