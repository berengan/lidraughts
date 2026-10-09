package lidraughts.tournament
package itaswiss

/**
 * Synchronizes Lidraughts Player.withdraw with the persistent FID retirement
 * round. The first observed withdrawal round is immutable.
 */
private[tournament] object RetirementService {

  def retire(tour: Tournament, userId: lidraughts.user.User.ID): Fu[TournamentState] = {
    require(tour.system == System.ItaSwiss, "not an ItaSwiss tournament")
    val state = tour.itaSwiss.getOrElse(sys.error("missing ItaSwiss tournament state"))
    require(state.hasCompetitionNumbers, "missing Italian Swiss competition numbers")
    val retirementRound = state.currentRound match {
      case Some(round) if !round.complete => round.number
      case _ => state.nextRoundNumber
    }
    val updated = state.retire(userId, retirementRound)
    if (updated == state) fuccess(state)
    else TournamentRepo.setItaSwissState(tour.id, updated) inject updated
  }

  def sync(tour: Tournament): Fu[TournamentState] = {
    require(tour.system == System.ItaSwiss, "not an ItaSwiss tournament")
    val state = tour.itaSwiss.getOrElse(sys.error("missing ItaSwiss tournament state"))
    require(state.hasCompetitionNumbers, "missing Italian Swiss competition numbers")

    PlayerRepo.byTourAndUserIds(tour.id, state.competitionPlayers.map(_.userId)).flatMap { players =>
      val retirementRound = state.currentRound match {
        case Some(round) if !round.complete => round.number
        case _ => state.nextRoundNumber
      }
      val updated = players.filter(_.withdraw).foldLeft(state) { (s, player) =>
        s.retire(player.userId, retirementRound)
      }
      if (updated == state) fuccess(state)
      else TournamentRepo.setItaSwissState(tour.id, updated) inject updated
    }
  }
}
