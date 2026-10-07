package lidraughts.tournament
package itaswiss

import lidraughts.game.Game
import lidraughts.tournament.{ Pairing => TournamentPairing }
import lidraughts.user.UserRepo

/** Executes an already validated ItaSwiss round plan using Lidraughts'
  * native Pairing/Game infrastructure.
  *
  * Competition numbers come only from persistent TournamentState. They are
  * drawn once and are never inferred again from Arena ranking order.
  */
private[tournament] final class RoundService(autoPairing: AutoPairing) {

  case class Started(round: Round, games: List[Game])

  def startFirstRound(tour: Tournament, ranking: Ranking): Fu[Started] = {
    val state = stateOf(tour)
    require(state.hasCompetitionNumbers, "Italian Swiss competition numbers are not assigned")
    require(state.rounds.isEmpty, "Italian Swiss first round already exists")
    val pairingState = State(
      playerCount = state.playerCount,
      round = 1,
      roundCount = state.roundCount,
      scores = state.competitionNumbers.keys.map(_ -> 0d).toMap,
      format = state.format
    )
    startRound(tour, pairingState, ranking)
  }

  def startNextRound(tour: Tournament, ranking: Ranking): Fu[Started] =
    RetirementService.sync(tour).flatMap { synced =>
      val syncedTour = tour.copy(itaSwiss = Some(synced))
      NextRoundState.load(syncedTour, synced.competitionNumbers, synced.retiredAt).flatMap { pairingState =>
        startRound(syncedTour, pairingState, ranking)
      }
    }

  private def startRound(tour: Tournament, pairingState: State, ranking: Ranking): Fu[Started] = {
    require(tour.system == System.ItaSwiss, "not an ItaSwiss tournament")
    val state = stateOf(tour)
    val competitionNumbers = state.competitionNumbers
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
          }.sequenceFu.map(games => Started(persistedState.currentRound.get, games))
      }
    }
  }

  private def stateOf(tour: Tournament) =
    tour.itaSwiss.getOrElse(sys.error("missing ItaSwiss tournament state"))

  private def makePairings(
      tour: Tournament,
      round: Round,
      competitionNumbers: Map[Int, lidraughts.user.User.ID]
  ): Fu[List[TournamentPairing]] =
    round.pairings.map { p =>
      TournamentPairing
        .prep(tour, competitionNumbers(p.white), competitionNumbers(p.black))
        .toPairing(firstGetsWhite = true)
    }.sequenceFu
}
