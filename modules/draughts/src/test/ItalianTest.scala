package draughts

import draughts.variant.Italian
import org.specs2.mutable.Specification

class ItalianTest extends Specification {

  private def pos(field: Int): PosMotion = Pos64.posAt(field).get

  private def situation(color: Color, pieces: (Int, Piece)*): Situation =
    Situation(
      Board(
        pieces.map { case (field, piece) => pos(field) -> piece }.toMap,
        DraughtsHistory(),
        Italian
      ),
      color
    )

  private def destinations(situation: Situation, from: Int): List[Int] =
    situation.validMoves.getOrElse(pos(from), Nil).map(_.dest.fieldNumber).sorted

  private def play(situation: Situation, from: Int, to: Int): Situation =
    situation.validMoves(pos(from)).find(_.dest == pos(to)).get.situationAfter

  "Italian draughts" should {

    "use short-range kings" in {
      val sit = situation(
        White,
        22 -> (White - King),
        1 -> (Black - Man)
      )

      destinations(sit, 22) must_== List(17, 18, 25, 26)
    }

    "allow men to capture forward" in {
      val sit = situation(
        White,
        22 -> (White - Man),
        18 -> (Black - Man)
      )

      destinations(sit, 22) must_== List(15)
    }

    "not allow men to capture backwards" in {
      val sit = situation(
        White,
        22 -> (White - Man),
        26 -> (Black - Man)
      )

      destinations(sit, 22) must not contain 29
    }

    "not allow a man to capture a king" in {
      val sit = situation(
        White,
        22 -> (White - Man),
        18 -> (Black - King)
      )

      destinations(sit, 22) must not contain 15
    }

    "prefer the capture that takes the greatest number of pieces" in {
      val sit = situation(
        White,
        22 -> (White - King),
        17 -> (Black - Man),
        18 -> (Black - Man),
        11 -> (Black - Man)
      )

      val moves = sit.validMoves.getOrElse(pos(22), Nil)
      moves must not be empty
      moves.forall(_.taken.exists(_.length == 2)) must beTrue
    }

    "prefer capturing a king when otherwise equal" in {
      val sit = situation(
        White,
        22 -> (White - King),
        17 -> (Black - King),
        18 -> (Black - Man)
      )

      destinations(sit, 22) must_== List(13)
    }

    "prefer the line that captures a king earlier when value is otherwise equal" in {
      val sit = situation(
        White,
        22 -> (White - King),
        17 -> (Black - King),
        9 -> (Black - Man),
        18 -> (Black - Man),
        11 -> (Black - King)
      )

      destinations(sit, 22) must_== List(13)
    }

    "prefer kings earlier across the whole capture sequence" in {
      val sit = situation(
        White,
        2 -> (White - King),
        6 -> (Black - King),
        14 -> (Black - King),
        7 -> (Black - Man),
        15 -> (Black - Man)
      )

      val moves = sit.validMoves.getOrElse(pos(2), Nil)
      moves must haveSize(1)
      moves.head.taken.toList.flatten.reverse.map(_.fieldNumber) must_== List(6, 14, 15, 7)
    }

    "prefer a king capture over a man capture when capture lengths are equal" in {
      val sit = situation(
        White,
        22 -> (White - Man),
        24 -> (White - King),
        18 -> (Black - Man),
        19 -> (Black - Man)
      )

      sit.validMoves.keySet must_== Set(pos(24))
    }

    "promote a man that reaches the opponent base" in {
      val sit = situation(
        White,
        5 -> (White - Man),
        32 -> (Black - Man)
      )

      val move = sit.validMoves(pos(5)).find(_.dest == pos(1)).get
      move.situationAfter.board(pos(1)).map(_.role) must beSome(King)
    }

    "detect the third occurrence of the same position with the same side to move" in {
      var sit = situation(
        White,
        29 -> (White - King),
        4 -> (Black - King)
      )

      (1 to 3).foreach { _ =>
        sit = play(sit, 29, 25)
        sit = play(sit, 4, 8)
        sit = play(sit, 25, 29)
        sit = play(sit, 8, 4)
      }

      sit.threefoldRepetition must beTrue
    }

    "count 80 consecutive non-capturing king plies for the drawing rule" in {
      val sit = situation(
        White,
        29 -> (White - King),
        4 -> (Black - King)
      )
      val board79 = sit.board.withHistory(sit.board.history.setHalfMoveClock(79))
      val board80 = sit.board.withHistory(sit.board.history.setHalfMoveClock(80))

      Italian.maxDrawingMoves(sit.board) must beSome(80)
      board79.autoDraw must beFalse
      board80.autoDraw must beTrue
    }

    "continue the drawing counter after a non-capturing king move" in {
      val base = situation(
        White,
        29 -> (White - King),
        4 -> (Black - King)
      )
      val sit = Situation(base.board.withHistory(base.board.history.setHalfMoveClock(12)), White)
      val after = play(sit, 29, 25)

      after.board.history.halfMoveClock must_== 13
    }

    "reset the drawing counter when a man moves" in {
      val base = situation(
        White,
        22 -> (White - Man),
        4 -> (Black - King)
      )
      val sit = Situation(base.board.withHistory(base.board.history.setHalfMoveClock(12)), White)
      val after = play(sit, 22, 17)

      after.board.history.halfMoveClock must_== 0
    }

    "reset the drawing counter after a capture" in {
      val base = situation(
        White,
        22 -> (White - King),
        18 -> (Black - Man),
        4 -> (Black - King)
      )
      val sit = Situation(base.board.withHistory(base.board.history.setHalfMoveClock(12)), White)
      val after = play(sit, 22, 15)

      after.board.history.halfMoveClock must_== 0
    }

    "stop a capture when a man reaches the opponent base" in {
      val sit = situation(
        White,
        10 -> (White - Man),
        6 -> (Black - Man),
        7 -> (Black - Man)
      )

      val moves = sit.validMoves(pos(10))
      moves.map(_.dest.fieldNumber).sorted must_== List(1, 3)
      moves.map(_.taken.map(_.length).getOrElse(0)).sorted must_== List(1, 1)
      moves.forall { move =>
        move.taken.exists(_.length == 1) &&
        move.situationAfter.board(move.dest).exists(_.role == King)
      } must beTrue
    }
  }
}
