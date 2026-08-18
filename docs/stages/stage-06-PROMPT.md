# Stage 06 — ClojureScript support via .cljc (stretch goal)

## Motivation (measured)

The source is JVM-only `.clj`, but every wrapped fn (audit needed — that's part of
this stage) appears to exist in ClojureScript's core. The charter records cljs as a
stretch goal for 0.1.0: attempted here, deferrable without shame.

## The change

1. **Audit first**: table of every public var vs its cljs.core counterpart —
   exists? same arities? known semantic differences? This table drives (and bounds)
   the work and goes in the report.
2. Rename `src/consistent_lib/core.clj` → `core.cljc` (and `impl.clj` → `impl.cljc`
   if the macro strategy is in place — macros will need `:refer-macros`-friendly
   structure or `#?(:clj ...)` guards). Reader conditionals ONLY where the audit
   shows they are required; a diff full of gratuitous `#?` is a review failure.
3. Add a cljs test gate: an alias (suggested `:cljs-test`) using
   `olical/cljs-test-runner`, running the existing example-based tests (port
   `core_test.clj` → `.cljc` as needed). Porting the test.check equivalence suite to
   cljs is in scope only if it is low-friction; otherwise note it as an Open question.

**Acceptable outcome B (charter-sanctioned deferral):** if the cljs toolchain or the
cljc conversion cannot reach green with honest effort inside this stage, revert to a
state where BOTH JVM gates are green (reverting the rename entirely is acceptable),
and write the report documenting the exact blocker, with `outcome: DEFERRED`. The
defer/retry decision then returns to the coordinator. Outcome A's report line is
`outcome: CLJC`.

## Ground rules

- JVM behavior must be completely unchanged either way (guardrails 1, 5).
- New deps only inside test/dev aliases (guardrail 6).
- Do not read `docs/stages/forecasts/`.

## Allowed files

- `src/consistent_lib/core.clj[c]`, `src/consistent_lib/impl.clj[c]` (renames + reader
  conditionals)
- `test/consistent_lib/**` (`.cljc` ports)
- `deps.edn` (aliases only)
- `.clj-kondo/config.edn` (cljc awareness if needed)
- `docs/stages/stage-06-REPORT.md`

## Tests (enumerated)

- Outcome A: full existing JVM suite green AND `clojure -M:cljs-test` green running
  the ported example-based tests; any cljs-vs-JVM behavioral divergence found by the
  audit gets an explicit conditional test plus a report note.
- Outcome B: full existing JVM suite green; no cljs gate.

## Definition of Done

- `clojure -X:test` → 0 failures, 0 errors.
- `clj-kondo --lint src test` → 0 errors, 0 warnings.
- Outcome A additionally: `clojure -M:cljs-test` → 0 failures, 0 errors.
- Report contains the audit table and the outcome line.

## Commit

Single implementation commit, message exactly:

    Support ClojureScript via cljc conversion

(Outcome B with a full revert commits only the report; use the Blocked-style
report-only commit in that case.)

## Report

`docs/stages/stage-06-REPORT.md`: Summary with outcome line; audit table; verbatim
gate outputs; Deviations; Open questions.

## Blocked protocol

If a ground rule, guardrail, or missing information blocks you: STOP. Do not
improvise. Write the report with `status: BLOCKED`, the exact blocker, and what you
need; commit only the report.
