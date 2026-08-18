# Stage 03 — Cover all non-transducer core arities; document the transducer policy

## Motivation (measured)

Wrappers silently drop documented `clojure.core` arities:

- `partition` wraps only `[coll n]`; core supports `[n coll]`, `[n step coll]`,
  `[n step pad coll]`.
- `partition-all` drops the step arity; `subvec` requires `end` while core also has
  `[v start]`; audit the rest — the report must contain the full table.

A user migrating `(partition 2 1 xs)` to this library today hits an arity error with
no documented reason.

## The change

1. **Audit table first**: for each of the public fns in `consistent-lib.core`, list
   core's arities vs ours (this table goes in the report and drives the work).
2. Extend every wrapper to cover ALL non-transducer arities of its core counterpart:
   coll stays argument 1, remaining args keep core's relative order (e.g.
   `(partition coll n)`, `(partition coll n step)`, `(partition coll n step pad)`).
3. **Transducer policy (decided, not yours to revisit):** transducer arities are out
   of scope for 0.1.0. Each wrapper whose core counterpart has a transducer arity gets
   one docstring sentence: "Transducer arity not provided; use clojure.core/<name>
   directly." If, while implementing, you conclude this policy is untenable (e.g. it
   makes a wrapper incoherent), say so under Open questions — do not implement
   transducer support.
4. **Collision rule:** if any extended arity would collide with an existing coll-first
   arity (same argument count, different meaning), STOP via the Blocked protocol —
   naming/keyword-arg workarounds are a coordinator decision (guardrail 2).

## Ground rules

- Behavior identical to core for every supported arity (guardrail 1). Existing arities
  keep working unchanged (guardrail 5).
- No new runtime dependencies. Do not read `docs/stages/forecasts/`.

## Allowed files

- `src/consistent_lib/core.clj`
- `src/consistent_lib/impl.clj` (only if stage 02 landed the macro)
- `test/consistent_lib/core_test.clj`
- `test/consistent_lib/metadata_test.clj` (arglists-count updates only)
- `docs/stages/stage-03-REPORT.md`

## Tests (enumerated)

For every arity added, at least one assertion comparing the wrapper's result to the
equivalent direct `clojure.core` call. At minimum:

- `partition` 3-arity (step) and 4-arity (step + pad)
- `partition-all` 3-arity (step)
- `subvec` 2-arity (`[coll start]`)
- one test per additional arity your audit table uncovers (the enumerated list in the
  report must match the table)
- all pre-existing tests pass unchanged

## Definition of Done

- `clojure -X:test` → 0 failures, 0 errors.
- `clj-kondo --lint src test` → 0 errors, 0 warnings.
- Report contains the complete arity audit table (core arities vs wrapped arities vs
  excluded-as-transducer).

## Commit

Single implementation commit, message exactly:

    Cover all non-transducer core arities in wrappers

## Report

`docs/stages/stage-03-REPORT.md`: Summary; the audit table; verbatim gate outputs;
Deviations; Open questions (including any doubts about the transducer policy).

## Blocked protocol

If a ground rule, guardrail, or missing information blocks you: STOP. Do not
improvise. Write the report with `status: BLOCKED`, the exact blocker, and what you
need; commit only the report.
