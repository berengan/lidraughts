package lidraughts.tournament
package itaswiss

import lidraughts.tournament.{ Pairing => TournamentPairing, PairingRepo, Tournament }
import lidraughts.user.User

/**
 * Loads the exact games recorded by completed ItaSwiss rounds and builds the
 * pairing state for the next round.
 */
private[tournament] object NextRoundState {

  def load(
    tour: Tournament,
    competitionNumbers: Map[Int, User.ID],
    retiredAt: Map[Int, Int] = Map.empty
  ): Fu[State] = {
    require(tour.system == System.ItaSwiss, "not an ItaSwiss tournament")
    val state = tour.itaSwiss.getOrElse(sys.error("missing ItaSwiss tournament state"))
    require(state.currentRound.exists(_.complete), "Italian Swiss current round is not complete")
    require(!state.finished, "Italian Swiss tournament is finished")

    val ids = state.rounds.flatMap { round =>
      require(round.complete, "unfinished Italian Swiss round before next-round generation")
      require(round.gameIds.size == round.pairings.size, "Italian Swiss round games are not bound")
      round.gameIds
    }

    PairingRepo.byIds(ids).map { games =>
      require(games.map(_.gameId).toSet == ids.toSet, "missing Italian Swiss game")
      RoundResults.nextState(state, competitionNumbers.size, competitionNumbers, games, retiredAt)
    }
  }
}
