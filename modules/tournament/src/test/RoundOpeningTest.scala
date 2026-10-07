package lidraughts.tournament.itaswiss

import draughts.variant.Italian
import org.specs2.mutable.Specification

class RoundOpeningTest extends Specification {

  "FID Italian Swiss round opening draw" should {

    "draw one official opening and mark its official number as used" in {
      val table = Italian.openingTables.head
      val result = RoundOpening.draw(table, Set.empty, _ => 0).get

      result.position.code must_== table.positions.head.code
      result.usedCodes must contain(result.position.code)
    }

    "never offer an opening already used by an earlier round" in {
      val table = Italian.openingTables.head
      val first = table.positions.head
      val available = RoundOpening.available(table, Set(first.code))

      available.map(_.code) must not contain first.code
      available.size must_== table.positions.size - 1
    }

    "stop when the selected FID table has no unused openings" in {
      val table = Italian.openingTables.head
      val allUsed = table.positions.map(_.code).toSet

      RoundOpening.draw(table, allUsed, _ => 0) must beNone
    }

    "preserve the same drawn opening for all games of a round" in {
      val table = Italian.openingTables.head
      val drawn = RoundOpening.draw(table, Set.empty, _ => 3).get.position
      val roundGames = List.fill(4)(drawn)

      roundGames.map(_.code).distinct must_== List(drawn.code)
      roundGames.map(_.fen).distinct must_== List(drawn.fen)
    }
  }
}
