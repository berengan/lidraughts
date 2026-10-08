# Federation game-creation policy (increment 2)

This branch extends the HOCON federation policy introduced in
`feature/federation-tournament-config` to human games.

## Covered

- Hook/lobby and friend/challenge forms display only configured variants.
- Setup form mappings reject variants disabled in the federation policy.
- Challenge API defaults to `federation.variants.default` when no variant is sent,
  and rejects a disabled variant on POST.
- The central challenge creation service refuses disallowed new challenges,
  including rematches and external challenges.
- Lobby hook/seek creation and joining reject disabled variants.
- Standard-only quick-pairing pools are hidden and cannot be joined when
  Standard is disabled.
- Lobby filter defaults to the enabled variants.
- Automatic Arena tournament scheduling skips variants not enabled for Arena;
  the tournament creation service checks the same policy as a second guard.
- AI choices are intersected with the **existing engine-supported** set.
  Italian draughts is **not** exposed against the AI engine until verified.
  If no supported AI variant is enabled, the AI entry on the lobby is hidden.
- Custom positions are checked against their underlying variant; Italian
  custom-position support is not enabled by this increment.

Historical games and already created tournaments remain readable; disabling a
variant prevents **new** games and challenges.

## Still to audit before production

- Admin-created tournaments, independent Swiss module, simuls and bot-specific
  pairing entry points.
- Other direct game creation paths and matchmaking integrations.
- Persisted user setup preferences that refer to now-disabled variants:
  forms filter their choices but may need a friendly default-selection UX.
- Browser-level smoke tests for the lobby, challenge forms and API responses.
- Existing scheduled tournaments in the database are not removed or cancelled.
  The legacy scheduler does not define Italian draughts plans, so with only
  Italian enabled no automatic tournaments will be added until those plans
  are designed and implemented.

## VM checks

Fetch the branch explicitly if `remote.origin.fetch` only tracks one branch.
Then run:

```bash
SBT_OPTS="-Xms1G -Xmx6G -XX:+UseG1GC" \
  sbt "common/testOnly *FederationConfigTest" compile
```

Review `git status --short` after sbt; the formatter may rewrite unrelated
Scala sources. Preserve those changes separately instead of committing them
into this branch.
