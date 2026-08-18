# Stage 04 — Coll-first wrappers for f-first core fns; consumer lint config

## Motivation (measured)

- The library's premise (`README.md:3-9`) is a consistent coll-first interface, yet
  the highest-traffic fn-first core fns are absent: there is no `c/map` or `c/filter`.
  `README.md:44-48` even threads bare `clojure.core/map` inside a `->` pipeline as if
  it were coll-first — which only works because `map` is not wrapped, and breaks the
  library's own story.
- Consumers who `:refer` the library shadow `clojure.core` names and will drown in
  lint warnings without a shipped clj-kondo export config.

## The change

1. Add coll-first wrappers for exactly this list (coordinator-decided; do not add or
   drop names without the Blocked protocol):
   `map`, `filter`, `mapcat`, `keep`, `map-indexed`, `keep-indexed`, `every?`,
   `not-every?`, `not-any?`, `sort-by`, `take-last`, `drop-last`, `replace`,
   `reduce-kv`.
   - Multi-collection arities of `map`/`mapcat`: the shape is
     `(c/map coll f & more-colls)` — first coll first, fn second, remaining colls
     after.
   - `sort-by` optional comparator: `(c/sort-by coll keyfn)` and
     `(c/sort-by coll keyfn comp)`.
   - `reduce-kv`: `(c/reduce-kv coll f init)`.
   - All non-transducer arities, per the stage 03 convention; transducer-arity
     docstring sentence likewise.
2. Ship consumer lint config as a clj-kondo export:
   `resources/clj-kondo.exports/consistent-lib/consistent-lib/config.edn`, and add
   `"resources"` to `:paths` in `deps.edn`. The export should spare consumers
   shadowing warnings when they `(:require [consistent-lib.core :as c])` and, if
   feasible, when they `:refer` specific names.
3. Add a namespace docstring to `consistent-lib.core` recommending the alias style
   (`:as c`) and warning about `:refer :all` shadowing.

## Ground rules

- Guardrails 1, 2, 5, 6 as always. If a fn on the list turns out to have an ambiguous
  "primary collection" in some arity, STOP via the Blocked protocol.
- Do not read `docs/stages/forecasts/`.

## Allowed files

- `src/consistent_lib/core.clj`
- `src/consistent_lib/impl.clj` (if the macro strategy is in place)
- `resources/clj-kondo.exports/consistent-lib/consistent-lib/config.edn` (new)
- `deps.edn` (`:paths` addition only)
- `test/consistent_lib/core_test.clj`
- `test/consistent_lib/metadata_test.clj` (count update)
- `.clj-kondo/config.edn`
- `docs/stages/stage-04-REPORT.md`

## Tests (enumerated)

- One deftest per new fn (14 new deftests), each with at least: a basic case compared
  against the direct core call, an empty-collection case, and for `map`/`mapcat` a
  multi-coll case.
- Metadata test still passes with the count raised to the new public-var total (state
  the number in the report).
- All pre-existing tests pass unchanged.

## Definition of Done

- `clojure -X:test` → 0 failures, 0 errors.
- `clj-kondo --lint src test` → 0 errors, 0 warnings.
- Report demonstrates the export config working: transcript of linting a scratch
  consumer file (not committed) with and without the export on the classpath.

## Commit

Single implementation commit, message exactly:

    Add coll-first wrappers for f-first core fns and consumer lint config

## Report

`docs/stages/stage-04-REPORT.md`: Summary; new public-var count; export-config
demonstration transcript; verbatim gate outputs; Deviations; Open questions.

## Blocked protocol

If a ground rule, guardrail, or missing information blocks you: STOP. Do not
improvise. Write the report with `status: BLOCKED`, the exact blocker, and what you
need; commit only the report.
