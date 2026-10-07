package lidraughts.tournament
package itaswiss

private[tournament] object FinishService {

  def finish(tour: Tournament, state: TournamentState): Funit = {
    require(tour.system == System.ItaSwiss, "not an ItaSwiss tournament")
    require(state.finished, "Italian Swiss tournament rounds are not finished")
    TournamentRepo.setStatus(tour.id, Status.Finished) >>
      PairingRepo.removePlaying(tour.id)
  }
}
