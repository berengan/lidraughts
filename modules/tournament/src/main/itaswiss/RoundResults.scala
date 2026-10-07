package lidraughts.tournament.itaswiss

import lidraughts.tournament.{ Pairing => TournamentPairing }
import lidraughts.user.User

/** Builds the next pure PairingEngine state from completed Lidraughts games. */
object RoundResults {

  def nextState(
      tournamentState: TournamentState,
      playerCount: Int,
      competitionNumbers: Map[Int, User.ID],
      completedPairings: List[TournamentPairing],
      retiredAt: Map[Int, Int] = Map.empty
  ): State = {
    require(tournamentState.currentRound.exists(_.complete), "Italian Swiss current round is not complete")
    require(!tournamentState.finished, "Italian Swiss tournament is finished")
    require(competitionNumbers.keySet == (1 to playerCount).toSet, "invalid ItaSwiss competition numbers")

    val numberByUser = competitionNumbers.map(_.swap)
    val scores = (1 to playerCount).map(_ -> 0d).toMap ++ accumulatedGameScores(numberByUser, completedPairings)
    val restScores = tournamentState.rounds.foldLeft(Map.empty[Int, Double]) { (acc, round) =>
      round.rests.foldLeft(acc) { (a, rest) =>
        val points = rest.restType match {
          case RestType.RM | RestType.RR => 2d
          case RestType.RT => 0d
        }
        a.updated(rest.player, a.getOrElse(rest.player, 0d) + points)
      }
    }
    val totalScores = (1 to playerCount).map { p =>
      p -> (scores.getOrElse(p, 0d) + restScores.getOrElse(p, 0d))
    }.toMap

    State(
      playerCount = playerCount,
      round = tournamentState.nextRoundNumber,
      roundCount = tournamentState.roundCount,
      scores = totalScores,
      history = tournamentState.rounds.map(r => r.number -> RoundHistory(r.pairings, r.rests)).toMap,
      format = tournamentState.format,
      retiredAt = retiredAt
    )
  }

  private def accumulatedGameScores(
      numberByUser: Map[User.ID, Int],
      pairings: List[TournamentPairing]
  ): Map[Int, Double] =
    pairings.foldLeft(Map.empty[Int, Double]) { (scores, pairing) =>
      require(pairing.finished, "unfinished game in completed ItaSwiss rounds")
      val white = numberByUser.getOrElse(pairing.user1, sys.error("unknown ItaSwiss white player"))
      val black = numberByUser.getOrElse(pairing.user2, sys.error("unknown ItaSwiss black player"))
      val whitePoints = if (pairing.wonBy(pairing.user1)) 2d else if (pairing.draw) 1d else 0d
      val blackPoints = if (pairing.wonBy(pairing.user2)) 2d else if (pairing.draw) 1d else 0d
      scores
        .updated(white, scores.getOrElse(white, 0d) + whitePoints)
        .updated(black, scores.getOrElse(black, 0d) + blackPoints)
    }
}
