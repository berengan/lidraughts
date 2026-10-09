# LiFiDama FID visual theme (first iteration)

The `lifidama-fid` theme is a separate, reversible visual layer. It does not modify board geometry, game rules, tournament pairing, or gameplay JavaScript.

## Enable

In the VM's ignored `conf/application.conf`:

```hocon
lifidama.theme.enabled = true
```

The default is `false`; missing configuration keeps the original presentation. Restart the Play application after changing configuration, then rebuild the CSS assets with the project's existing UI build process.

## Structure

- `ui/common/css/lifidama/_theme.scss`: header, navigation, buttons, focus states, mobile adaptation, and light/dark support.
- `ui/lobby/css/_lifidama-fid.scss`: homepage cards, calls to action, and now-playing layout.
- `app/views/base/layout.scala`: opt-in `lifidama-fid` class on the body.

The styles are scoped to `body.lifidama-fid` and are loaded through the existing SCSS bundles. No external stylesheets, additional dependencies, or changes to gameplay are required.

## Brand

Initial palette: blue `#18407C`, turquoise `#00A99C`, light canvas `#F4F6FA`. Confirm these against the current FID brand guide before production approval. The header reuses the existing FID logo.

## Verification

1. With the flag absent/false, check that the site still looks unchanged.
2. With the flag true, check header, menus, lobby, game page, and forms on desktop and mobile.
3. Check dark theme, keyboard focus, blind mode, and a live game.
4. Run the project SCSS build and `sbt compile` on the VM. GitHub commits alone do not validate these builds.
