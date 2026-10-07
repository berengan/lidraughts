package lidraughts.tournament.itaswiss

import draughts.variant.Italian
import org.specs2.mutable.Specification

class RoundPlannerTest extends Specification {

  "Italian Swiss round planner" should {

    "atomically combine pairing and one round opening" in {
      val table = Italian.openingTables.head
      val tournament = TournamentState(Format.Art2, roundCount = 3)
      val pairing = State(
        playerCount = 4,
        round = 1,
        roundCount = 3,
        scores = (1 to 4).map(_ -> 0d).toMap,
        format = Format.Art2
      )

      val plan = RoundPlanner.plan(tournament, pairing, table.some, _ => 2)

      plan.round.pairings must_== List(Pairing(1, 2), Pairing(3, 4))
      plan.round.opening.map(_.code) must beSome(table.positions(2).code)
      plan.nextState.usedOpeningCodes must contain(table.positions(2).code)
      plan.nextState.canGenerateNextRound must beFalse
    }

    "draw no opening for free-opening tournaments" in {
      val tournament = TournamentState(Format.Art2, roundCount = 3)
      val pairing = State(4, 1, 3, (1 to 4).map(_ -> 0d).toMap, format = Format.Art2)

      RoundPlanner.plan(tournament, pairing, None).round.opening must beNone
    }

    "refuse to advance while the preceding round is incomplete" in {
      val firstState = TournamentState(Format.Art2, roundCount = 3)
      val firstPairing = State(4, 1, 3, (1 to 4).map(_ -> 0d).toMap, format = Format.Art2)
      val afterFirst = RoundPlanner.plan(firstState, firstPairing, None).nextState
      val secondPairing = State(
        4,
        2,
        3,
        (1 to 4).map(_ -> 0d).toMap,
        history = Map(1 -> RoundHistory(List(Pairing(1, 2), Pairing(3, 4)), Nil)),
        format = Format.Art2
      )

      RoundPlanner.plan(afterFirst, secondPairing, None) must throwA[IllegalArgumentException]
    }

    "never reuse the opening drawn in the preceding round" in {
      val table = Italian.openingTables.head
      val initial = TournamentState(Format.Art2, roundCount = 3)
      val firstPairing = State(4, 1, 3, (1 to 4).map(_ -> 0d).toMap, format = Format.Art2)
      val first = RoundPlanner.plan(initial, firstPairing, table.some, _ => 0)
      val completed = first.nextState.complete(1)
      val secondPairing = State(
        4,
        2,
        3,
        Map(1 -> 2d, 2 -> 0d, 3 -> 1d, 4 -> 1d),
        history = Map(1 -> RoundHistory(first.round.pairings, first.round.rests)),
        format = Format.Art2
      )

      val second = RoundPlanner.plan(completed, secondPairing, table.some, _ => 0)
      second.round.opening.map(_.code) must not(beSome(first.round.opening.get.code))
    }
  }
}
