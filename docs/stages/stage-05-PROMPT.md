# Stage 05 — Generative equivalence tests against clojure.core

## Motivation (measured)

Equivalence with `clojure.core` is this library's entire contract, but the
example-based suite asserts a handful of hand-picked cases per fn (and, before stage
04, covered 9 of 17 fns). A wrapper that mis-permutes arguments for one arity would
pass today's suite if no example hits that arity. Property-based tests check the
contract across generated inputs.

## The change

1. Add `org.clojure/test.check` to the `:test` alias's `:extra-deps` in `deps.edn`
   (test-scoped only — guardrail 6 governs runtime deps).
2. New test ns `consistent-lib.equivalence-test`: for EVERY public var in
   `consistent-lib.core`, a `defspec` asserting the wrapper's result equals the
   corresponding direct `clojure.core` call with arguments in core's order.
   - Collection generators: vectors, lists, lazy seqs, and empty collections of small
     ints; maps/sets where the fn accepts them (`group-by`, `reduce-kv`).
   - Function arguments drawn from a fixed pool of pure fns
     (`odd?`, `even?`, `pos?`, `inc`, `identity`, `str`, `-` for reduce, …).
   - Sizes/indices generated within valid ranges for fns with constraints
     (`subvec`, `split-at`); the property, not the generator, must fail on a
     genuine mismatch — do not clamp results, only inputs.
   - Variadic fns (`interleave`, `concat`, multi-coll `map`/`mapcat`): generate 0–3
     collections where core allows it.
   - ≥ 100 cases per property.
3. Enumerate the public vars programmatically (`ns-publics`) and assert the spec count
   equals the public-var count, so a future fn cannot ship without a property.

## Ground rules

- No changes to `src/` — if a property finds a real wrapper bug, that is a STOP via
  the Blocked protocol (the fix is a coordinator decision, likely its own stage).
- Do not read `docs/stages/forecasts/`.

## Allowed files

- `deps.edn` (`:test` alias deps only)
- `test/consistent_lib/equivalence_test.clj` (new)
- `docs/stages/stage-05-REPORT.md`

## Tests (enumerated)

- One `defspec` per public var (report states the count and it matches `ns-publics`).
- All pre-existing tests pass unchanged.

## Definition of Done

- `clojure -X:test` → 0 failures, 0 errors, and the runner output shows the
  equivalence specs ran (paste summary).
- `clj-kondo --lint src test` → 0 errors, 0 warnings.
- Per the charter, this suite is a sacred gate from this stage forward — the report
  must note total property count and runtime.

## Commit

Single implementation commit, message exactly:

    Add generative equivalence tests against clojure.core

## Report

`docs/stages/stage-05-REPORT.md`: Summary; spec count vs public-var count; verbatim
gate outputs with suite runtime; Deviations; Open questions.

## Blocked protocol

If a ground rule, guardrail, or missing information blocks you: STOP. Do not
improvise. Write the report with `status: BLOCKED`, the exact blocker, and what you
need; commit only the report. A genuine equivalence failure discovered by a property
is ALWAYS a block, never something to paper over in the generator.
