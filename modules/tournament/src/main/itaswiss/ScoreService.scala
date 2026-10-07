package lidraughts.tournament
package itaswiss

private[tournament] object ScoreService {

  def sync(tour: Tournament, state: TournamentState): Funit = {
    require(tour.system == System.ItaSwiss, "not an ItaSwiss tournament")
    require(state.hasCompetitionNumbers, "missing Italian Swiss competition numbers")

    val completedGameIds = state.rounds.filter(_.complete).flatMap(_.gameIds)
    PairingRepo.byIds(completedGameIds).flatMap { games =>
      require(games.map(_.gameId).toSet == completedGameIds.toSet, "missing completed Italian Swiss game")
      val scores = RoundResults.scores(state, state.competitionNumbers, games)
      scores.toList.map {
        case (userId, score) => PlayerRepo.setScore(tour.id, userId, score)
      }.sequenceFu.void
    }
  }
}
