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
      val sit = situation(White,
        22 -> (White - King),
        1 -> (Black - Man)
      )

      destinations(sit, 22) must_== List(17, 18, 25, 26)
    }

    "allow men to capture forward" in {
      val sit = situation(White,
        22 -> (White - Man),
        18 -> (Black - Man)
      )

      destinations(sit, 22) must_== List(15)
    }

    "not allow men to capture backwards" in {
      val sit = situation(White,
        22 -> (White - Man),
        26 -> (Black - Man)
      )

      destinations(sit, 22) must not contain 29
    }

    "not allow a man to capture a king" in {
      val sit = situation(White,
        22 -> (White - Man),
        18 -> (Black - King)
      )

      destinations(sit, 22) must not contain 15
    }

    "prefer a king capture over a man capture when capture lengths are equal" in {
      val sit = situation(White,
        22 -> (White - Man),
        24 -> (White - King),
        18 -> (Black - Man),
        19 -> (Black - Man)
      )

      sit.validMoves.keySet must_== Set(pos(24))
    }
  }
}
