package lidraughts.tournament.itaswiss

import draughts.StartingPosition

/** Persistent domain data needed by a round-based Italian Swiss tournament.
  *
  * This deliberately lives outside the Arena waiting-user model: ItaSwiss
  * advances by completed rounds, not by continuously pairing idle users.
  */
case class Round(
    number: Int,
    pairingStartNumber: Int,
    opening: Option[StartingPosition],
    pairings: List[Pairing],
    rests: List[Rest],
    retired: List[Int],
    complete: Boolean = false
)

case class TournamentState(
    format: Format,
    roundCount: Int,
    rounds: List[Round] = Nil,
    usedOpeningCodes: Set[String] = Set.empty
) {
  require(roundCount > 0, "Italian Swiss tournament must have at least one round")

  def nextRoundNumber: Int = rounds.size + 1
  def finished: Boolean = rounds.size >= roundCount
  def currentRound: Option[Round] = rounds.lastOption
  def canGenerateNextRound: Boolean =
    !finished && currentRound.forall(_.complete)

  def append(round: Round): TournamentState = {
    require(canGenerateNextRound, "previous Italian Swiss round is not complete")
    require(round.number == nextRoundNumber, "invalid Italian Swiss round number")
    require(!rounds.exists(_.number == round.number), "Italian Swiss round already exists")
    val openingCodes = round.opening.fold(usedOpeningCodes)(p => usedOpeningCodes + p.code)
    copy(rounds = rounds :+ round, usedOpeningCodes = openingCodes)
  }

  def complete(roundNumber: Int): TournamentState =
    copy(rounds = rounds.map(r => if (r.number == roundNumber) r.copy(complete = true) else r))
}
