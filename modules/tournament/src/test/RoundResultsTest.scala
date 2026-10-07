package lidraughts.tournament.itaswiss

import draughts.Status
import lidraughts.tournament.{ Pairing => TournamentPairing }
import org.specs2.mutable.Specification

class RoundResultsTest extends Specification {

  private def finished(id: String, white: String, black: String, winner: Option[String]) =
    TournamentPairing(id, "tour", Status.Mate, white, black, winner, Some(20), false, false)

  "Italian Swiss round results" should {

    "build cumulative 2-1-0 scores and history for the next round" in {
      val state = TournamentState(Format.Art2, 3)
        .append(Round(1, 1, None, List(Pairing(1, 2), Pairing(3, 4)), Nil, Nil))
        .complete(1)
      val games = List(
        finished("g1", "u1", "u2", Some("u1")),
        finished("g2", "u3", "u4", None)
      )

      val next = RoundResults.nextState(state, 4, Map(1 -> "u1", 2 -> "u2", 3 -> "u3", 4 -> "u4"), games)

      next.round must_== 2
      next.scores must_== Map(1 -> 2d, 2 -> 0d, 3 -> 1d, 4 -> 1d)
      next.history(1).pairings must_== List(Pairing(1, 2), Pairing(3, 4))
    }

    "score RM and RR as two points and RT as zero" in {
      val state = TournamentState(Format.Art8, 3)
        .append(Round(1, 1, None, List(Pairing(1, 2)), List(Rest(3, RestType.RM), Rest(4, RestType.RT)), Nil))
        .complete(1)
      val games = List(finished("g1", "u1", "u2", Some("u2")))

      val next = RoundResults.nextState(
        state,
        4,
        Map(1 -> "u1", 2 -> "u2", 3 -> "u3", 4 -> "u4"),
        games
      )

      next.scores must_== Map(1 -> 0d, 2 -> 2d, 3 -> 2d, 4 -> 0d)
    }

    "reject unfinished games when constructing the next round" in {
      val state = TournamentState(Format.Art2, 3)
        .append(Round(1, 1, None, List(Pairing(1, 2)), Nil, Nil))
        .complete(1)
      val game = TournamentPairing("g1", "tour", Status.Started, "u1", "u2", None, Some(2), false, false)

      RoundResults.nextState(state, 2, Map(1 -> "u1", 2 -> "u2"), List(game)) must
        throwA[IllegalArgumentException]
    }
  }
}
