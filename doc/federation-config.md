# Federation tournament configuration

The HOCON `federation` section is **disabled by default**. Without it, Lidraughts
keeps its previous behavior. To enable the current tournament policy:

```hocon
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
        formats = ["ITA_SWISS_FID_ART2", "ITA_SWISS_FID_ART8", "ITA_SWISS_FID_ART9"]
      }
    }
  }
}
```

The user tournament form filters variants, systems and ItaSwiss formats.
JavaScript updates available systems when the variant changes, and Play
form validation also rejects disallowed combinations (including API creation).
Existing tournaments retain their persisted variant/system and can still be
edited without changing that combination. Restart the app after policy changes.

**Implementation boundaries:** ItaSwiss is validated only for Italian draughts.
Enabling it for international draughts is rejected until pairing and scoring
compatibility tests exist. Round Robin is a planned third system, not yet an
implemented engine; configuring it is rejected rather than exposing a broken
option. This first increment applies to user-created tournaments only. Lobby
game creation, challenges, AI, admin/scheduled tournaments and other entry
points are NOT yet globally restricted by this policy. Do not enable federation
mode in production as a complete site-wide variant restriction until those
integrations are finished.

Validation on the development VM:
`sbt "common/testOnly *FederationConfigTest"`
`sbt "tournament/testOnly *SystemTest"`
`sbt compile`
