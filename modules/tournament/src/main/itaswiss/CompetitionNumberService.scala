package lidraughts.tournament
package itaswiss

/** Assigns the FID competition-number draw once, before round one. */
private[tournament] object CompetitionNumberService {

  def assign(tour: Tournament): Fu[TournamentState] = {
    require(tour.system == System.ItaSwiss, "not an ItaSwiss tournament")
    val state = tour.itaSwiss.getOrElse(sys.error("missing ItaSwiss tournament state"))
    require(!state.hasCompetitionNumbers, "Italian Swiss competition numbers are already assigned")
    require(state.rounds.isEmpty, "Italian Swiss pairing already started")

    PlayerRepo.userIds(tour.id).flatMap { userIds =>
      val assigned = state.withCompetitionPlayers(CompetitionNumbers.draw(userIds))
      TournamentRepo.setItaSwissState(tour.id, assigned) inject assigned
    }
  }
}
