package lidraughts.tournament

import org.specs2.mutable.Specification
import org.joda.time.DateTime

import draughts.variant.{ Italian, Standard }
import lidraughts.common.FederationConfig

class SystemTest extends Specification {

  "Tournament systems" should {

    "keep Arena as the backwards-compatible default" in {
      System.default must_== System.Arena
      System.orDefault(999) must_== System.Arena
    }

    "exclude disabled variants from new scheduled tournaments" in {
      val policy = FederationConfig(
        enabled = true,
        enabledVariants = List(Italian),
        defaultVariant = Italian,
        tournamentSystems = Map("arena" -> List(Italian)),
        defaultTournamentSystem = "arena",
        itaSwissFormats = Nil
      )
      val standard = Schedule(
        Schedule.Freq.Hourly, Schedule.Speed.Blitz,
        Standard, Standard.startingPosition, DateTime.now
      )
      val italian = standard.copy(variant = Italian, position = Italian.startingPosition)

      (italian.allowedByFederation(policy) &&
        !standard.allowedByFederation(policy) &&
        standard.allowedByFederation(policy.copy(enabled = false))) must beTrue
    }

    "persist ItaSwiss with its own stable id" in {
      System(2) must beSome(System.ItaSwiss)
      System.ItaSwiss.default must beFalse
      System.ItaSwiss.berserkable must beFalse
      System.ItaSwiss.key must_== "itaSwiss"
      System.Arena.key must_== "arena"
    }
  }
}
