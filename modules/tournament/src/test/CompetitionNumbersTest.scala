package lidraughts.tournament.itaswiss

import org.specs2.mutable.Specification

class CompetitionNumbersTest extends Specification {

  "FID Italian Swiss competition numbers" should {

    "number the one-time draw contiguously from one" in {
      val drawn = CompetitionNumbers.draw(List("alice", "bob", "carol"), _.reverse)
      drawn must_== List(
        CompetitionPlayer(1, "carol"),
        CompetitionPlayer(2, "bob"),
        CompetitionPlayer(3, "alice")
      )
    }

    "reject duplicate players" in {
      CompetitionNumbers.draw(List("alice", "alice"), identity) must throwA[IllegalArgumentException]
    }

    "freeze assigned numbers once pairing identity exists" in {
      val state = TournamentState(Format.Art2, 3)
        .withCompetitionPlayers(List(CompetitionPlayer(1, "alice"), CompetitionPlayer(2, "bob")))

      state.competitionNumbers must_== Map(1 -> "alice", 2 -> "bob")
      state.withCompetitionPlayers(List(CompetitionPlayer(1, "bob"), CompetitionPlayer(2, "alice"))) must
        throwA[IllegalArgumentException]
    }
  }
}
