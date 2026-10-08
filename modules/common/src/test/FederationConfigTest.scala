package lidraughts.common

import com.typesafe.config.ConfigFactory
import draughts.variant.{ Italian, Standard }
import org.specs2.mutable.Specification

class FederationConfigTest extends Specification {

  private val sample = """
    federation {
      enabled = true
      variants {
        enabled = ["italian", "standard"]
        default = "italian"
      }
      tournaments {
        defaultSystem = "arena"
        systems {
          arena.variants = ["italian", "standard"]
          itaSwiss {
            variants = ["italian"]
            formats = ["ITA_SWISS_FID_ART2", "ITA_SWISS_FID_ART8"]
          }
        }
      }
    }
  """

  private def parse(s: String) = FederationConfig.fromConfig(ConfigFactory.parseString(s))

  "FederationConfig" should {
    "preserve legacy behavior when disabled or absent" in {
      val legacy = parse("federation.enabled = false")
      legacy.enabled must beFalse
      legacy.allowsVariant(Standard) must beTrue
      legacy.allowsTournament("itaSwiss", Standard) must beTrue
      parse("").enabled must beFalse
    }

    "allow only configured variants, systems and formats" in {
      val policy = parse(sample)
      policy.defaultVariant must_== Italian
      policy.allowsVariant(Standard) must beTrue
      policy.allowsTournament("arena", Standard) must beTrue
      policy.allowsTournament("itaSwiss", Italian) must beTrue
      policy.allowsTournament("itaSwiss", Standard) must beFalse
      policy.allowsItaSwissFormat("ITA_SWISS_FID_ART9") must beFalse
      policy.allowsGameVariant(Italian) must beTrue
      policy.allowsGameVariant(draughts.variant.Russian) must beFalse
      policy.allowsGameVariant(draughts.variant.FromPosition) must beTrue
      policy.allowsGameVariant(draughts.variant.FromPosition, Some(Italian)) must beFalse
    }

    "reject a custom position when its base variant is disabled" in {
      val italianOnly = parse(sample.replace(
        "enabled = [\"italian\", \"standard\"]",
        "enabled = [\"italian\"]"
      ).replace(
        "arena.variants = [\"italian\", \"standard\"]",
        "arena.variants = [\"italian\"]"
      ))
      italianOnly.allowsGameVariant(draughts.variant.FromPosition) must beFalse
      italianOnly.allowsGameVariant(Italian) must beTrue
    }

    "reject unsupported systems" in {
      parse(sample.replace("arena.variants", "roundRobin.variants")) must throwA[IllegalArgumentException]
    }

    "reject ItaSwiss on unvalidated variants" in {
      val invalid = sample.replace("variants = [\"italian\"]", "variants = [\"italian\", \"standard\"]")
      parse(invalid) must throwA[IllegalArgumentException]
    }

    "reject invalid default variants" in {
      parse(sample.replace("default = \"italian\"", "default = \"russian\"")) must throwA[IllegalArgumentException]
    }
  }
}
