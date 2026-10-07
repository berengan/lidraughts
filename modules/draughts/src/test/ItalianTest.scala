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
