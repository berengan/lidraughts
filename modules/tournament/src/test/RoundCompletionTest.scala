package lidraughts.tournament.itaswiss

import draughts.Status
import lidraughts.tournament.{ Pairing => TournamentPairing }
import org.specs2.mutable.Specification

class RoundCompletionTest extends Specification {

  private def game(id: String, status: Status) =
    TournamentPairing(id, "tour", status, "u1", "u2", None, Some(20), false, false)

  "Italian Swiss round completion" should {

    "complete only when every expected game is finished" in {
      val state = TournamentState(Format.Art2, 3)
        .append(Round(1, 1, None, List(Pairing(1, 2)), Nil, Nil))

      RoundCompletion.complete(state, List(game("g1", Status.Mate))).currentRound.map(_.complete) must beSome(true)
      RoundCompletion.complete(state, List(game("g1", Status.Started))) must throwA[IllegalArgumentException]
    }

    "reject a game-count mismatch" in {
      val state = TournamentState(Format.Art2, 3)
        .append(Round(1, 1, None, List(Pairing(1, 2), Pairing(3, 4)), Nil, Nil))

      RoundCompletion.complete(state, List(game("g1", Status.Mate))) must throwA[IllegalArgumentException]
    }
  }
}
