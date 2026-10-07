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

    "refuse to complete a round that does not exist" in {
      val state = TournamentState(Format.Art2, roundCount = 4)
      state.complete(1) must throwA[IllegalArgumentException]
    }

    "refuse to complete an older round instead of the current one" in {
      val state = TournamentState(Format.Art2, roundCount = 4)
        .append(Round(1, 1, None, Nil, Nil, Nil))
        .complete(1)
        .append(Round(2, 2, None, Nil, Nil, Nil))

      state.complete(1) must throwA[IllegalArgumentException]
    }

    "bind exactly one game id to every pairing in the current round" in {
      val state = TournamentState(Format.Art2, roundCount = 4)
        .append(Round(1, 1, None, List(Pairing(1, 2), Pairing(3, 4)), Nil, Nil))

      val bound = state.withGameIds(1, List("g1", "g2"))
      bound.currentRound.map(_.gameIds) must beSome(List("g1", "g2"))
      state.withGameIds(1, List("g1")) must throwA[IllegalArgumentException]
    }

    "persist the first withdrawal round and never move it later" in {
      val state = TournamentState(Format.Art2, roundCount = 4)
        .withCompetitionPlayers(List(CompetitionPlayer(1, "u1"), CompetitionPlayer(2, "u2")))

      val retired = state.retire("u2", 2)
      retired.retiredAt must_== Map(2 -> 2)
      retired.retire("u2", 3).retiredAt must_== Map(2 -> 2)
    }

    "reject retirement of a player outside the competition draw" in {
      val state = TournamentState(Format.Art2, roundCount = 4)
        .withCompetitionPlayers(List(CompetitionPlayer(1, "u1"), CompetitionPlayer(2, "u2")))

      state.retire("other", 2) must throwA[RuntimeException]
    }

    "keep pairing_start_number as round state" in {
      val state = TournamentState(Format.Art8, roundCount = 4)
        .append(Round(1, 5, None, Nil, List(Rest(5, RestType.RM)), Nil))

      state.currentRound.map(_.pairingStartNumber) must beSome(5)
    }

    "not be finished merely because the last round was generated" in {
      val state = TournamentState(Format.Art2, roundCount = 1)
        .withCompetitionPlayers(List(
          CompetitionPlayer(1, "u1"),
          CompetitionPlayer(2, "u2")
        ))
        .append(Round(1, 1, None, List(Pairing(1, 2)), Nil, Nil))

      state.allRoundsGenerated must beTrue
      state.finished must beFalse
      state.canGenerateNextRound must beFalse
      state.complete(1).finished must beTrue
    }
  }
}
