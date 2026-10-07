package lidraughts.tournament
package itaswiss

import lidraughts.tournament.{ Score => AbstractScore }
import lidraughts.tournament.{ ScoringSystem => AbstractScoringSystem }

private[tournament] object ScoringSystem extends AbstractScoringSystem {

  case class Score(value: Int) extends AbstractScore
  case class Sheet(scores: List[Score]) extends ScoreSheet {
    val total = scores.foldLeft(0)(_ + _.value)
    val onFire = false
  }

  val emptySheet = Sheet(Nil)

  def sheet(userId: String, pairings: Pairings, streakable: Streakable): Sheet =
    Sheet(pairings.foldLeft(List.empty[Score]) { (scores, pairing) =>
      val value =
        if (!pairing.finished) 0
        else if (pairing.wonBy(userId)) 2
        else if (pairing.draw) 1
        else 0
      Score(value) :: scores
    })
}
