package lidraughts.tournament.itaswiss

import draughts.{ OpeningTable, StartingPosition }

/** Pure orchestration of one complete ItaSwiss round decision.
  *
  * Persistence and Game creation stay outside this class. This keeps the
  * certified pairing algorithm testable while producing one atomic plan
  * containing pairings, rests, rotation anchor and the round opening.
  */
object RoundPlanner {

  case class Plan(round: Round, nextState: TournamentState)

  def plan(
      tournamentState: TournamentState,
      pairingState: State,
      openingTable: Option[OpeningTable],
      chooseOpeningIndex: Int => Int = scala.util.Random.nextInt
  ): Plan = {
    require(tournamentState.canGenerateNextRound, "Italian Swiss round cannot be generated yet")
    require(pairingState.format == tournamentState.format, "Italian Swiss format mismatch")
    require(pairingState.roundCount == tournamentState.roundCount, "Italian Swiss round-count mismatch")
    require(pairingState.round == tournamentState.nextRoundNumber, "Italian Swiss round number mismatch")

    val result = PairingEngine.generate(pairingState)
    require(result.pairings.nonEmpty || result.rests.nonEmpty, "Italian Swiss pairing generation failed")

    val opening = openingTable.flatMap { table =>
      RoundOpening.draw(table, tournamentState.usedOpeningCodes, chooseOpeningIndex).map(_.position)
    }

    val round = Round(
      number = result.round,
      pairingStartNumber = result.pairingStartNumber,
      opening = opening,
      pairings = result.pairings,
      rests = result.rests,
      retired = result.retired
    )

    Plan(round, tournamentState.append(round))
  }
}
