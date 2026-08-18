# Stage 01 — Real project: deps.edn, src/ layout, runnable gates

## Motivation (measured)

- The repo has **no `deps.edn` and no `project.clj`** (verified 2026-08-18), yet
  `README.md:85-100` documents `lein test` and `clj -X:test`. Neither command can run.
- Source lives at `consistent_lib/core.clj` in the repo root — not on any conventional
  classpath — so `test/consistent_lib/core_test.clj` cannot resolve its
  `consistent-lib.core` require.

## The change

1. Create `deps.edn`:
   - `:paths ["src"]`
   - `:aliases {:test {...}}` using `io.github.cognitect-labs/test-runner` with
     `:extra-paths ["test"]`, exposed as `clojure -X:test`.
   - No runtime dependencies beyond `org.clojure/clojure` (guardrail 6).
2. `git mv consistent_lib/core.clj src/consistent_lib/core.clj`; the now-empty
   `consistent_lib/` directory must not survive.
3. In `test/consistent_lib/core_test.clj`, rebalance `partition-all-test` so its two
   `testing` forms are siblings — currently the second is nested inside the first
   (lines 14-17). No assertion changes anywhere.
4. Make `clj-kondo --lint src test` pass with zero errors and zero warnings. The test
   ns uses `:refer :all` and intentionally shadows `clojure.core` names; prefer a
   minimal committed `.clj-kondo/config.edn` (e.g. `:refer-all` / shadowing excludes
   scoped to the test ns) over rewriting the test namespace. Record whatever you
   exclude, and why, in the report.

## Ground rules

- No changes to any function's behavior or signature in `core.clj`. This stage is
  layout and tooling only.
- Guardrails in `docs/stages/README.md` apply; STOP-AND-ASK via the Blocked protocol.

## Allowed files

- `deps.edn` (new)
- `src/consistent_lib/core.clj` (moved from `consistent_lib/core.clj`; content
  unchanged)
- `consistent_lib/core.clj` (deletion via the move)
- `test/consistent_lib/core_test.clj` (nesting fix only)
- `.clj-kondo/config.edn` (new, minimal)
- `docs/stages/stage-01-REPORT.md`

## Tests (enumerated)

All nine existing deftests pass under `clojure -X:test`: `partition-test`,
`partition-all-test`, `partition-by-test`, `take-test`, `drop-test`, `take-nth-test`,
`take-while-test`, `drop-while-test`, `split-at-test`.

## Definition of Done

- `clojure -X:test` → 0 failures, 0 errors (paste the runner summary in the report —
  this is the baseline test count for later stages).
- `clj-kondo --lint src test` → 0 errors, 0 warnings.

## Commit

Single implementation commit, message exactly:

    Add deps.edn test tooling and move source under src/

## Report

Write `docs/stages/stage-01-REPORT.md`: Summary; verbatim gate outputs; Deviations
(every departure from this prompt, however small — including each clj-kondo exclusion
with rationale); Open questions.

## Blocked protocol

If a ground rule, guardrail, or missing information blocks you: STOP. Do not
improvise. Write the report with `status: BLOCKED`, the exact blocker, and what you
need; commit only the report.
