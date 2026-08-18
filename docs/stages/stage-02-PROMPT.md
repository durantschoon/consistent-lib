# Stage 02 — Wrapper definition strategy: metadata-preserving generation vs hand-written

## Motivation (measured)

- `src/consistent_lib/core.clj` hand-writes 17 wrappers whose docstrings and arglists
  already drift from `clojure.core` (e.g. our `partition` documents only `[coll n]`
  while core supports step and pad arities).
- Stage 04 plans roughly 14 more wrappers, and stage 03 multiplies arities. Whether
  wrappers are generated or hand-written decides the cost of everything downstream — this
  stage decides it empirically, not by taste.

## The change

Build the generation approach and measure it against acceptance criteria; keep it only
if ALL criteria pass.

1. New namespace `consistent-lib.impl` with a macro (suggested name `defwrapper`)
   that, given a `clojure.core` var and an argument-order spec, emits a `defn` with:
   - a docstring derived from the core var's docstring plus a one-line coll-first note;
   - literal, accurate `:arglists` metadata for the wrapped arities;
   - pure delegation to the core fn (guardrail 1).
2. Port ALL 17 existing public fns in `consistent-lib.core` to the macro. Public API
   (names, arities, behavior) must be byte-for-byte compatible with today — guardrail 5.
3. New test ns `consistent-lib.metadata-test`: every public var in
   `consistent-lib.core` has a non-nil docstring and non-nil `:arglists`. This test
   ships REGARDLESS of which outcome you land on.

**Acceptance criteria (all must hold to keep the macro):**

- (a) `clj-kondo --lint src test` is clean, AND clj-kondo statically understands the
  generated fns: lint a scratch file (do not commit it) containing a deliberately
  wrong-arity call to a generated fn and show the warning in the report. Custom
  `.clj-kondo` config or hooks to achieve this are allowed and committed.
- (b) In a REPL, `(clojure.repl/doc consistent-lib.core/partition)` shows the derived
  docstring and correct arglists — transcript in the report.
- (c) The full existing test suite passes unchanged.

**Acceptable outcome B (fallback):** if after honest attempts any criterion cannot be
met, revert `core.clj` to hand-written defns (improving docstrings/arglists by hand is
fine and encouraged), keep the metadata test, delete or don't create `impl.clj`, and
document in the report exactly which criterion failed and the evidence. Outcome B is a
legitimate result of this stage, not a failure.

## Ground rules

- No new runtime dependencies (guardrail 6). No behavior changes (guardrail 1).
- Do not read `docs/stages/forecasts/`.

## Allowed files

- `src/consistent_lib/core.clj`
- `src/consistent_lib/impl.clj` (new; absent under outcome B)
- `test/consistent_lib/metadata_test.clj` (new)
- `.clj-kondo/config.edn`, `.clj-kondo/hooks/**` (if needed for criterion a)
- `deps.edn` (only if the test alias needs the new test ns path/config; no deps)
- `docs/stages/stage-02-REPORT.md`

## Tests (enumerated)

- All stage-01 tests pass unchanged.
- `metadata-test`: docstring + `:arglists` present on every public var (must count 17
  vars — assert the count so future drift is caught).

## Definition of Done

- `clojure -X:test` → 0 failures, 0 errors.
- `clj-kondo --lint src test` → 0 errors, 0 warnings.
- Report states the outcome explicitly: `outcome: MACRO` or `outcome: HAND-WRITTEN`,
  with the criterion evidence either way.

## Commit

Single implementation commit, message exactly:

    Establish wrapper definition strategy with metadata fidelity tests

## Report

`docs/stages/stage-02-REPORT.md`: Summary (including the outcome line); verbatim gate
outputs; criterion (a) scratch-lint and criterion (b) REPL transcripts; Deviations;
Open questions.

## Blocked protocol

If a ground rule, guardrail, or missing information blocks you: STOP. Do not
improvise. Write the report with `status: BLOCKED`, the exact blocker, and what you
need; commit only the report.
