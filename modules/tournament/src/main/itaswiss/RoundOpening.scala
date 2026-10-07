package lidraughts.tournament.itaswiss

import draughts.{ OpeningTable, StartingPosition }

/** Round-level opening draw for FID Italian Swiss tournaments.
  *
  * The opening is selected once for the round and then reused by every game
  * in that round. StartingPosition.code is the official FID opening number,
  * so uniqueness is independent from OpeningTable.randomOpening's shuffled index.
  */
object RoundOpening {

  case class Draw(position: StartingPosition, usedCodes: Set[String])

  def available(table: OpeningTable, usedCodes: Set[String]): Vector[StartingPosition] =
    table.positions.iterator.filterNot(p => usedCodes(p.code)).toVector

  def draw(
      table: OpeningTable,
      usedCodes: Set[String],
      chooseIndex: Int => Int = scala.util.Random.nextInt
  ): Option[Draw] = {
    val choices = available(table, usedCodes)
    if (choices.isEmpty) none
    else {
      val position = choices(chooseIndex(choices.size))
      Draw(position, usedCodes + position.code).some
    }
  }
}
