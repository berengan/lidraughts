package lidraughts.tournament.itaswiss

import draughts.variant.Italian
import org.specs2.mutable.Specification

class TournamentStateTest extends Specification {

  "Italian Swiss tournament state" should {

    "not generate a second round before the current round is complete" in {
      val state = TournamentState(Format.Art2, roundCount = 4)
      val first = Round(1, 1, None, Nil, Nil, Nil)

      val afterFirst = state.append(first)
      afterFirst.canGenerateNextRound must beFalse
      afterFirst.complete(1).canGenerateNextRound must beTrue
    }

    "carry the official opening number into the used-opening set" in {
      val opening = Italian.openingTables.head.positions.head
      val state = TournamentState(Format.Art2, roundCount = 4)
        .append(Round(1, 1, opening.some, Nil, Nil, Nil))

      state.usedOpeningCodes must contain(opening.code)
    }

    "keep pairing_start_number as round state" in {
      val state = TournamentState(Format.Art8, roundCount = 4)
        .append(Round(1, 5, None, Nil, List(Rest(5, RestType.RM)), Nil))

      state.currentRound.map(_.pairingStartNumber) must beSome(5)
    }
  }
}
