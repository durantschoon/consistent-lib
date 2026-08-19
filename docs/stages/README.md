# Stage pipeline — consistent-lib

## Numbering and files

One global stage sequence. Each stage `NN` has:

- `stage-NN-PROMPT.md` — committed to `main` before launch; the text at launch time is
  canonical. Stages 01–08 were batch-authored on 2026-08-18 as the maturity roadmap;
  a not-yet-launched prompt may be amended by a follow-up commit (e.g. after an earlier
  stage's outcome), but never after its executor launches.
- `stage-NN-REPORT.md` — written by the executor, audited at review.
- `forecasts/stage-NN-FORECAST.sealed` (+ `.sha256`) — for stages with sealed branch
  forecasts; see `forecasts/README.md`.

## Gates

The repo had no build tooling before stage 01 (no `deps.edn`, no `project.clj`);
stage 01 establishes the gates. From stage 01 onward, every stage's Definition of Done
includes, verbatim:

- Full test suite: `clojure -X:test`
- Static check: `clj-kondo --lint src test`

Per the charter (`forecasts/CHARTER.md`): once stage 05 lands, the generative
equivalence suite runs inside `clojure -X:test` and is equally sacred. `cljfmt` is
advisory, never a blocking gate.

## Route sketch (planning horizon)

Milestone: **consistent-lib 0.1.0 live on Clojars, cljdoc building, CI green.**

1. Stage 01 — real project: `deps.edn`, conventional `src/` layout, runnable gates
   — **DONE** (`7c513c3`, merged 2026-08-18; report: `stage-01-REPORT.md`)
2. Stage 02 — wrapper definition strategy (metadata-preserving generation vs hand-written)
3. Stage 03 — cover all non-transducer core arities; transducer policy documented
4. Stage 04 — coll-first wrappers for f-first core fns; consumer lint config
5. Stage 05 — generative equivalence tests against `clojure.core` (test.check)
6. Stage 06 — ClojureScript via `.cljc` (charter: stretch goal; deferral acceptable)
7. Stage 07 — GitHub Actions CI running the gates
8. Stage 08 — release build tooling, accurate README, Clojars deploy readiness

Sealed forecasts exist for stages 02, 03, and 06 — the stages with genuine branch
uncertainty. Stages 01, 04, 05, 07, 08 are near-mechanical; their constraints live in
their prompts, not in forecasts.

## Guardrails (STOP-AND-ASK form)

Unifying principle for this library: **a wrapper is a pure, transparent rename of its
`clojure.core` counterpart — argument order is the ONLY permitted difference.**

1. **Zero behavior delta.** Every wrapper delegates to `clojure.core`; any semantic
   deviation beyond argument order (coercion, extra validation, changed laziness)
   ⇒ STOP and ask.
2. **Coll-first invariant.** The primary collection is always argument 1. If which
   argument is "primary" is ambiguous for a fn, do not guess ⇒ STOP and ask.
3. **Metadata fidelity.** Every public var carries a docstring and accurate
   `:arglists`. A definition approach that loses either ⇒ STOP and ask.
4. **No I/O, no side effects** in library namespaces. Ever.
5. **Public API is append-only.** Never rename or remove a public var once merged;
   a breaking change ⇒ STOP and ask.
6. **Zero runtime dependencies.** A new dependency outside `:test`-scoped aliases
   ⇒ STOP and ask.

## Coordinator practices

- Stage prompts land on `main` before launch (canonical text + number reservation).
- At most one in-flight stage touches any shared registration file. As of stage 01
  the only one this repo has is `deps.edn` (`.clj-kondo/config.edn` was added by
  stage 01 and deleted again in `5ea0302` once the test ns moved to `:as c`, which
  removed the need for every suppression it held).
- Executors attempt their own push. Credentials ARE available in the worktree
  (stage 01's executor pushed its own branch successfully), so treat a push as
  likely to succeed; the coordinator still owns the merge to `main` and the push
  of `main` itself. Never force-push.
- Review = diff + independent gate rerun in the executor's worktree, never
  report-reading alone.
- **Verify the executor's worktree base before trusting any baseline.** Stage 01's
  worktree was created from `132ef10` while `main` was already at `3fc0b6d`; the
  executor caught it and reset, but a stale base means measurements are taken against
  the wrong tree. Confirm `git merge-base --is-ancestor <expected-base> HEAD` in the
  worktree as the first review step, and state the expected base commit in the launch
  message so the executor can check it too.
- **Re-run lint on a COLD cache.** `clj-kondo` results can depend on whether
  `.clj-kondo/.cache` exists (stage 01 hit 44 cold-only false positives). CI always
  starts cold, so a gate verified only warm is not verified.
- Retro every 5 stages (before authoring stages 05, 10, …): re-read the last five
  REPORTs' Deviations and Open-questions, fix systemic prompt/practice bugs, and run
  `calibrate.py` for a calibration takeaway — all in the same commit as the new prompt.
- Batch-authored prompts are re-read (and amended if stale) immediately before launch;
  amendments are ordinary commits made before the executor starts.
- Executor agent name resolves from `MODELS.md` → `## Pipeline agents`, never
  hard-coded.
