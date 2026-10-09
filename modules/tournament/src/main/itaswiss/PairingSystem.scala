package lidraughts.tournament
package itaswiss

import lidraughts.tournament.{ PairingSystem => AbstractPairingSystem }

/**
 * ItaSwiss is round driven.
 *
 * The generic StartedOrganizer asks systems for continuously available
 * pairings. Returning no pairings here is intentional: an ItaSwiss round
 * must be generated atomically from TournamentState by the round service.
 */
private[tournament] object PairingSystem extends AbstractPairingSystem {
  def createPairings(tour: Tournament, users: WaitingUsers, ranking: Ranking): Fu[Pairings] =
    fuccess(Nil)
}
