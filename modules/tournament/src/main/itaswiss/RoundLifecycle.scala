package lidraughts.tournament
package itaswiss

import lidraughts.tournament.{ PairingRepo, Tournament, TournamentRepo }

/** Repository-backed transition that closes exactly the current ItaSwiss round. */
private[tournament] object RoundLifecycle {

  def completeCurrent(tour: Tournament): Fu[TournamentState] = {
    require(tour.system == System.ItaSwiss, "not an ItaSwiss tournament")
    val state = tour.itaSwiss.getOrElse(sys.error("missing ItaSwiss tournament state"))
    val round = state.currentRound.getOrElse(sys.error("missing Italian Swiss round"))
    require(round.gameIds.size == round.pairings.size, "Italian Swiss round games are not bound")

    PairingRepo.byIds(round.gameIds).flatMap { games =>
      require(games.map(_.gameId).toSet == round.gameIds.toSet, "missing Italian Swiss game")
      val completed = RoundCompletion.complete(state, games)
      TournamentRepo.setItaSwissState(tour.id, completed) inject completed
    }
  }
}
