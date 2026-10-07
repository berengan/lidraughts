package lidraughts.tournament.itaswiss

import draughts.StartingPosition
import lidraughts.user.User

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
    gameIds: List[String] = Nil,
    complete: Boolean = false
)

case class CompetitionPlayer(number: Int, userId: User.ID)

case class TournamentState(
    format: Format,
    roundCount: Int,
    rounds: List[Round] = Nil,
    usedOpeningCodes: Set[String] = Set.empty,
    competitionPlayers: List[CompetitionPlayer] = Nil
) {
  require(roundCount > 0, "Italian Swiss tournament must have at least one round")
  require(competitionPlayers.map(_.number).distinct.size == competitionPlayers.size, "duplicate Italian Swiss competition number")
  require(competitionPlayers.map(_.userId).distinct.size == competitionPlayers.size, "duplicate Italian Swiss player")

  def nextRoundNumber: Int = rounds.size + 1
  def finished: Boolean = rounds.size >= roundCount
  def currentRound: Option[Round] = rounds.lastOption
  def canGenerateNextRound: Boolean =
    !finished && currentRound.forall(_.complete)

  def competitionNumbers: Map[Int, User.ID] = competitionPlayers.map(p => p.number -> p.userId).toMap
  def playerCount: Int = competitionPlayers.size
  def hasCompetitionNumbers: Boolean = competitionPlayers.nonEmpty

  def withCompetitionPlayers(players: List[CompetitionPlayer]): TournamentState = {
    require(rounds.isEmpty, "Italian Swiss competition numbers cannot change after pairing starts")
    require(competitionPlayers.isEmpty, "Italian Swiss competition numbers are already assigned")
    require(players.nonEmpty, "Italian Swiss requires competition players")
    require(players.map(_.number) == (1 to players.size).toList, "Italian Swiss competition numbers must be contiguous from one")
    require(players.map(_.userId).distinct.size == players.size, "duplicate Italian Swiss player")
    copy(competitionPlayers = players)
  }

  def append(round: Round): TournamentState = {
    require(canGenerateNextRound, "previous Italian Swiss round is not complete")
    require(round.number == nextRoundNumber, "invalid Italian Swiss round number")
    require(!rounds.exists(_.number == round.number), "Italian Swiss round already exists")
    val openingCodes = round.opening.fold(usedOpeningCodes)(p => usedOpeningCodes + p.code)
    copy(rounds = rounds :+ round, usedOpeningCodes = openingCodes)
  }

  def withGameIds(roundNumber: Int, gameIds: List[String]): TournamentState = {
    require(currentRound.exists(_.number == roundNumber), "only the current Italian Swiss round can receive games")
    require(gameIds.size == currentRound.get.pairings.size, "wrong number of Italian Swiss game ids")
    copy(rounds = rounds.map(r => if (r.number == roundNumber) r.copy(gameIds = gameIds) else r))
  }

  def complete(roundNumber: Int): TournamentState = {
    require(rounds.exists(_.number == roundNumber), "Italian Swiss round does not exist")
    require(currentRound.exists(_.number == roundNumber), "only the current Italian Swiss round can be completed")
    copy(rounds = rounds.map(r => if (r.number == roundNumber) r.copy(complete = true) else r))
  }
}
