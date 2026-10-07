package lidraughts.tournament
package itaswiss

import lidraughts.user.User

/** FID competition numbers are drawn once and then remain stable for the
  * whole tournament. Randomness is injected so the draw is deterministic in
  * tests and does not depend on Arena ranking.
  */
object CompetitionNumbers {

  def draw(userIds: List[User.ID], shuffle: List[User.ID] => List[User.ID] = scala.util.Random.shuffle[User.ID] _): List[CompetitionPlayer] = {
    require(userIds.size > 1, "Italian Swiss requires at least two players")
    require(userIds.distinct.size == userIds.size, "duplicate Italian Swiss player")
    shuffle(userIds).zipWithIndex.map {
      case (userId, index) => CompetitionPlayer(index + 1, userId)
    }
  }
}
