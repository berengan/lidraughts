package lidraughts.common

import com.typesafe.config.Config
import draughts.variant.{ Italian, Standard, Variant }
import scala.collection.JavaConverters._

/** Deployment policy for NEW tournaments; persisted historical records are unchanged. */
final case class FederationConfig(
    enabled: Boolean,
    enabledVariants: List[Variant],
    defaultVariant: Variant,
    tournamentSystems: Map[String, List[Variant]],
    defaultTournamentSystem: String,
    itaSwissFormats: List[String]
) {
  def allowsVariant(variant: Variant): Boolean =
    !enabled || enabledVariants.contains(variant)

  def variantsForSystem(system: String): List[Variant] =
    if (enabled) tournamentSystems.getOrElse(system, Nil) else Variant.allVariants

  def allowsTournament(system: String, variant: Variant): Boolean =
    !enabled || (allowsVariant(variant) && variantsForSystem(system).contains(variant))

  def allowsItaSwissFormat(format: String): Boolean =
    !enabled || itaSwissFormats.contains(format)
}

object FederationConfig {

  private val legacy = FederationConfig(
    enabled = false,
    enabledVariants = List(Standard),
    defaultVariant = Standard,
    tournamentSystems = Map.empty,
    defaultTournamentSystem = "arena",
    itaSwissFormats = Nil
  )

  lazy val current: FederationConfig = fromConfig(PlayApp.loadConfig)

  /** Pure parser: the Play application is not required by unit tests. */
  def fromConfig(config: Config): FederationConfig = {
    if (!config.hasPath("federation.enabled") || !config.getBoolean("federation.enabled")) legacy
    else {
      def variant(key: String): Variant =
        Variant(key).getOrElse(throw new IllegalArgumentException(s"Unknown federation variant: $key"))

      def variants(path: String): List[Variant] = {
        val keys = config.getStringList(path).asScala.toList
        require(keys.distinct == keys, s"Duplicate variants in $path")
        keys.map(variant)
      }

      val enabledVariants = variants("federation.variants.enabled")
      require(enabledVariants.nonEmpty, "Federation must enable at least one variant")
      val defaultVariant = variant(config.getString("federation.variants.default"))
      require(enabledVariants.contains(defaultVariant), "Default federation variant must be enabled")

      val systemsConfig = config.getConfig("federation.tournaments.systems")
      val systemKeys = systemsConfig.root.keySet.asScala.toList
      val implementedSystems = Set("arena", "itaSwiss")
      require(systemKeys.forall(implementedSystems), "Unsupported tournament system (roundRobin is not implemented yet)")
      val tournamentSystems = systemKeys.map { key =>
        val allowed = variants(s"federation.tournaments.systems.$key.variants")
        require(allowed.nonEmpty, s"Tournament system $key needs at least one variant")
        require(allowed.forall(enabledVariants.contains), s"Tournament system $key references a disabled variant")
        key -> allowed
      }.toMap

      // Extending this to international draughts requires pairing/scoring tests.
      require(
        tournamentSystems.getOrElse("itaSwiss", Nil).forall(_ == Italian),
        "ItaSwiss currently supports only the Italian variant"
      )
      val formats = if (tournamentSystems.contains("itaSwiss"))
        config.getStringList("federation.tournaments.systems.itaSwiss.formats").asScala.toList
      else Nil
      val knownFormats = Set("ITA_SWISS_FID_ART2", "ITA_SWISS_FID_ART8", "ITA_SWISS_FID_ART9")
      require(formats.distinct == formats, "Duplicate ItaSwiss formats")
      require(formats.forall(knownFormats), "Unknown ItaSwiss format")
      require(!tournamentSystems.contains("itaSwiss") || formats.nonEmpty, "ItaSwiss needs at least one format")

      val defaultSystem = config.getString("federation.tournaments.defaultSystem")
      require(
        tournamentSystems.get(defaultSystem).exists(_.contains(defaultVariant)),
        "Default tournament system must be enabled for the default variant"
      )
      require(
        enabledVariants.forall(v => tournamentSystems.values.exists(_.contains(v))),
        "Every enabled variant must have at least one tournament system"
      )

      FederationConfig(true, enabledVariants, defaultVariant, tournamentSystems, defaultSystem, formats)
    }
  }
}
