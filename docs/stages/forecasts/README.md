# Sealed branch forecasts — convention

For stages with genuine branch uncertainty, the coordinator predicts likely pivots
before launch and seals the prediction so it cannot contaminate the stage or its
review:

- `stage-NN-FORECAST.sealed` + `stage-NN-FORECAST.sealed.sha256` are committed in the
  SAME commit as `stage-NN-PROMPT.md`, BEFORE stage NN launches. The `.sha256` is a
  commitment to the plaintext; the git commit timestamp proves the prediction preceded
  the evidence.
- The sealed file MUST NOT be unsealed until stage NN is merged or abandoned. Stage
  prompts never mention forecasts, branches, or probabilities; executors must not read
  anything in this directory.
- After the merge (or abandon) verdict: unseal, annotate each prediction with outcome
  `1 | 0 | unresolved | invalid` plus a verbatim evidence quote, run the mandatory
  unmodeled-pivot sweep, and commit the readable
  `stage-NN-FORECAST-RESOLVED.md` beside the stage REPORT. Every row is also appended
  to the global ledger at `~/.claude/branching-stages/ledger.csv`.

Forecasts are scored against `CHARTER.md` in this directory: pivots that contradict a
recorded charter answer void the affected predictions rather than counting against
calibration. Charter amendments are append-only and dated.
