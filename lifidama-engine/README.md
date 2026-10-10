# LiFiDama: staged Italian engine integration

**Draft / not ready for production.** The existing "Play computer" Italian
option remains disabled while the native engine and capture protocol are validated.

Changes:
- Draughtsnet routes Italian move jobs exclusively to engine name `LiFiDama-Italian`.
- Existing Draughtsnet clients continue to receive non-Italian jobs.
- The move work JSON now includes `currentFen`, avoiding move-history replay.
- `worker.py` polls Draughtsnet and runs the separately built `dama-linux` CLI
  with `engine-levels.ini`. It does **not** bundle the original engine source.
- Levels 1–2 = Beginner; 3–4 = Intermediate; 5–6 = Professional; 7–8 = Ultra.

## Safety gates
- Worker defaults to dry-run and does not post moves.
- Captures are rejected until Draughtsnet's `taken` field is verified.
- Current FEN must match the 32-square notation accepted by the engine.
- Engine hard wall-clock timeout is 8 seconds per subprocess; search uses
  separate per-level depth/time settings.
- Engine source licensing and a full site game must be reviewed before rollout.

## Local checks

```bash
cd lifidama-engine
python3 -m unittest -v test_worker.py
LIFIDAMA_DRAUGHTSNET_KEY=YOUR_TEST_KEY python3 worker.py \
  --url http://127.0.0.1:9663 \
  --engine ./dama-linux --config ./engine-levels.ini --once
```

Generate a move-only test key using the site's existing
`draughtsnet client create <user> move` CLI. Never commit the key.

**Do not run `--post` on production.**
