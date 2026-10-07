package lidraughts.tournament.itaswiss

import lidraughts.tournament.{ Pairing => TournamentPairing }

/** Validates the game side of a round before marking its domain state complete. */
object RoundCompletion {

  def complete(state: TournamentState, pairings: List[TournamentPairing]): TournamentState = {
    val round = state.currentRound.getOrElse(sys.error("missing Italian Swiss round"))
    require(!round.complete, "Italian Swiss round is already complete")
    require(pairings.size == round.pairings.size, "wrong number of Italian Swiss games")
    if (round.gameIds.nonEmpty)
      require(pairings.map(_.gameId).toSet == round.gameIds.toSet, "wrong games for Italian Swiss round")
    require(pairings.forall(_.finished), "Italian Swiss round still has unfinished games")
    state.complete(round.number)
  }
}
