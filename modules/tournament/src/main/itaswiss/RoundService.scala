package lidraughts.tournament.itaswiss

import lidraughts.tournament.{ Pairing => TournamentPairing, _ }
import lidraughts.user.{ User, UserRepo }

/** Executes an already validated ItaSwiss round plan using Lidraughts'
  * native Pairing/Game infrastructure.
  *
  * Competition numbers are deliberately supplied explicitly: they are the
  * stable FID identity used by PairingEngine and must never be inferred from
  * Arena ranking order.
  */
private[tournament] final class RoundService(autoPairing: AutoPairing) {

  def startRound(
      tour: Tournament,
      pairingState: State,
      competitionNumbers: Map[Int, User.ID],
      ranking: Ranking
  ): Fu[Round] = {
    require(tour.system == System.ItaSwiss, "not an ItaSwiss tournament")
    val state = tour.itaSwiss.getOrElse(sys.error("missing ItaSwiss tournament state"))
    require(competitionNumbers.keySet == (1 to pairingState.playerCount).toSet, "invalid ItaSwiss competition numbers")

    val plan = RoundPlanner.plan(state, pairingState, tour.openingTable)
    val userIds = plan.round.pairings.flatMap(p => List(competitionNumbers(p.white), competitionNumbers(p.black)))

    UserRepo.idsMap(userIds).flatMap { users =>
      makePairings(tour, plan.round, competitionNumbers).flatMap { pairings =>
        val persistedState = plan.nextState.withGameIds(plan.round.number, pairings.map(_.gameId))
        TournamentRepo.setItaSwissState(tour.id, persistedState) >>
          pairings.map { pairing =>
            PairingRepo.insert(pairing) >>
              autoPairing(tour, pairing, users, ranking, plan.round.opening)
          }.sequenceFu.void
      }
    } inject plan.round
  }

  private def makePairings(
      tour: Tournament,
      round: Round,
      competitionNumbers: Map[Int, User.ID]
  ): Fu[List[TournamentPairing]] =
    round.pairings.map { p =>
      TournamentPairing
        .prep(tour, competitionNumbers(p.white), competitionNumbers(p.black))
        .toPairing(firstGetsWhite = true)
    }.sequenceFu
}
