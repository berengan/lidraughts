# LiFiDama – Integrazione sperimentale Dama Italiana

**Ramo di sviluppo, non installare in produzione.** Il pulsante per giocare
contro il computer rimane disabilitato fino alla verifica end-to-end.

## Componenti

- Scala: separa i lavori Italian destinati a `LiFiDama-Italian` dai lavori
  degli altri client Draughtsnet.
- Scala: aggiunge `currentFen` ai lavori di tipo `move`.
- Python 3: `worker.py` esegue il motore nativo, in un processo separato,
  con un limite di 8 secondi per chiamata.
- `engine-levels.ini`: quattro profili di ricerca. I livelli del sito
  1–2/3–4/5–6/7–8 corrispondono a
  Beginner/Intermediate/Professional/Ultra.
- `LIFIDAMA_ITALIAN_AI_ENABLED=true` abilita la variante nella form di
  gioco contro il computer. **Non impostarla in produzione ora.**

## Test sulla VM, senza modificare il servizio

```bash
cd /opt/lidraughts
git fetch origin feature/lifidama-italian-engine
git worktree add --detach /tmp/lifidama-engine-test FETCH_HEAD
cd /tmp/lifidama-engine-test/lifidama-engine
python3 -m unittest -v test_worker.py
```

La prova del motore richiede il binario Linux già compilato in precedenza.
Il worker presume che il binario accetti:

```text
dama-linux --move --fen 'W:B1-12:W21-32' --level beginner --config engine-levels.ini
```

**Questa interfaccia CLI è stata verificata sulla VM il 10 ottobre 2026.**
Il binario compilato è `/usr/src/lifidama-engine/dama-linux`.
Controllarla con `--probe`, specificando il percorso reale del binario:

```bash
python3 worker.py --probe --engine /usr/src/lifidama-engine/dama-linux \
  --config ./engine-levels.ini
```

`--probe` non richiede chiavi, non interroga il server e non invia mosse.

## Regressione completa del motore nativo (solo locale)

La VM ha già prodotto i seguenti riferimenti: 7 mosse iniziali,
`perft(1..5) = 7, 49, 302, 1469, 7361`; cattura singola
`W:W22:B18 -> 22x13`; cattura multipla
`W:W22:B18,10 -> 22x6`; cattura di dama
`W:WK22:B18,10 -> 22x6`.

Dopo aver aggiornato il worktree di prova al ramo corrente, eseguire:

```bash
cd /opt/lidraughts
git fetch origin feature/lifidama-italian-engine
git -C /tmp/lifidama-engine-test fetch origin feature/lifidama-italian-engine
git -C /tmp/lifidama-engine-test switch --detach FETCH_HEAD
cd /tmp/lifidama-engine-test/lifidama-engine
LIFIDAMA_ENGINE=/usr/src/lifidama-engine/dama-linux \\
  python3 -m unittest -v test_worker.py test_engine_integration.py
```

I test nativi sono opt-in: senza `LIFIDAMA_ENGINE` vengono saltati.
Non inviano mosse al sito. Verificano esplicitamente che il worker
**rifiuti ancora le catture** finché il campo `taken` non è validato.

## Draughtsnet: solo dopo la verifica della CLI

L'applicazione espone già `POST /draughtsnet/acquire` e
`POST /draughtsnet/move/<workId>`. Il client deve usare una chiave
Draughtsnet con permesso `move`, creabile tramite la CLI interna
`draughtsnet client create <userId> move`.

Il test in sola lettura del flusso (non pubblica mosse) è:

```bash
LIFIDAMA_DRAUGHTSNET_KEY=CHIAVE_TEST python3 worker.py --once \
  --url http://127.0.0.1:9663 --engine /PERCORSO/DEL/BINARIO \
  --config ./engine-levels.ini
```

Attenzione: anche il test `--once` acquisisce temporaneamente un lavoro
nella coda del server, che verrà riproposto alla scadenza del lease.

## Blocchi ancora aperti

1. Verificare la compilazione Scala della branch.
2. Confrontare FEN e numerazione caselle con la libreria del sito, oltre
   ai test locali già superati.
3. Verificare UCI, multi-catture e il campo `taken`: al momento il worker
   **rifiuta esplicitamente tutte le catture**.
4. Testare una partita completa su istanza di prova, incluso il tempo limite.
5. Chiarire i termini di ridistribuzione del motore originale.

Il worker resta in dry-run per impostazione predefinita. Non usare `--post`
prima di aver completato i controlli sopra.
