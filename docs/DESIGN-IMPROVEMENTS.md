# Design improvement suggestions

Reviewed 2026-08-18 against `consistent_lib/core.clj` (now `src/consistent_lib/core.clj`
after stage 01), the test suite, the README, and
the stage roadmap in `docs/stages/README.md`. Where a suggestion is already scheduled
by a stage prompt, that stage is cross-referenced; the point here is the *design
rationale*, which the stage prompts mostly leave implicit.

## 1. State an explicit inclusion rule — and apply it

The current 18 wrappers mix three different situations:

1. **Coll-second core fns** (`partition`, `take`, `drop-while`, …) — the reason this
   library exists. Keep.
2. **F-first core fns** (`group-by`, `some`, `remove`) — same problem, but the current
   set is arbitrary: `remove` is wrapped while `map`, `filter`, `keep`, `mapcat`, and
   `every?` — the most-used f-first functions — are not. The README's own showcase
   example threads `(map inc)` through `->`, which silently calls
   `(map coll inc)` and breaks. Stage 04 adds these; the design point is that the
   library is *misleading* until the set is principled, because a reader assumes any
   common sequence fn is covered.
3. **Already-consistent core fns** (`interleave`, `concat`, `subvec`) — these add
   nothing. `interleave` and `concat` are variadic with no argument-order problem, and
   the wrappers are literally `(apply clojure.core/f colls)`. `clojure.core/subvec` is
   already coll-first, and the wrapper *drops* its 2-arity `(subvec v start)`.
   Suggest removing all three (pre-0.1.0, before there are consumers), or at minimum
   documenting why identity wrappers exist (e.g. "one namespace for everything" — but
   then the set must be complete, which is a much bigger surface).

Suggested rule for the README: *a function is included iff its `clojure.core`
counterpart does not take the collection as its first argument.* Everything else
follows from that sentence.

## 2. Declare `:refer-clojure :exclude` in the namespace

> **RESOLVED 2026-08-18** — both halves of this section are done. `core.clj` declares
> `(:refer-clojure :exclude [...])` for all 18 names (`7c513c3`), and the test ns moved
> to `:as c` (`5ea0302`). Compiler warnings on `(require '[consistent-lib.core :as c])`:
> 18 → 0. `clj-kondo --lint src test` is 0/0 cold and warm with no config file at all.
> Retained below as the rationale; stage 04 must extend the `:exclude` vector when it
> adds wrappers, or `:redefined-var` will fire.

`consistent-lib.core` shadows 18 `clojure.core` names but never declares
`(:refer-clojure :exclude [partition take drop ...])`. Loading the namespace emits a
wall of "already refers to" warnings, and the warnings will grow with stage 04.
This is a one-line fix and also serves as a machine-checked manifest of exactly which
names the library intentionally shadows.

Relatedly, the test namespace (and any docs) should model `(:require
[consistent-lib.core :as c])` rather than `:refer :all` — `:refer :all` in consumer
code shadows core unpredictably and is exactly the confusion this library tries to
remove. The tests currently use `:refer :all`.

## 3. Decide and document the multi-operand ordering rule

"Collection first" is unambiguous for 2-arity fns but not for the rest. Two cases in
the current code already answer the question differently and neither is documented:

- `reduce` chose `([coll f] [coll f init])`. Reasonable (threading works), but
  `(reduce coll f init)` reads oddly against core's `(reduce f init coll)`; the
  docstring should show both orders side by side.
- `partition`'s missing step/pad arities (stage 03) will force the same decision:
  `(partition coll n)`, `(partition coll n step)`, `(partition coll n step pad)` is the
  natural extension — state the rule once ("coll first, remaining args in core's
  order") so stage 03/04 don't decide it per-function.

Put the rule in the README under a "Design rules" heading; it is the library's actual
specification.

## 4. Document the transducer trade-off prominently

Every wrapped fn loses core's transducer arity: `(c/take 3)` would be an error, not a
transducer. That is probably the right call (a coll-less arity can't be coll-first),
but it is the single biggest semantic difference from core and deserves a README
section, not just the stage 03 policy note. Recommend: state that transducer contexts
should use `clojure.core` directly, and that this is intentional, e.g.
`(into [] (clojure.core/take 3) coll)`.

## 5. Preserve core's semantics exactly — including edge arities and docs

- Docstrings currently drift from core (noted in stage 02): our `partition` documents
  only `[coll n]`; `take-nth`'s docstring ("takes every nth element") doesn't mention
  it always includes the first element; `some`'s docstring says "predicate" but core
  accepts any fn and returns its truthy result, which is the idiom `(some coll #{:a})`
  relies on.
- Laziness must be preserved by construction. Pure delegation (stage 02 guardrail 1)
  guarantees it; the design doc / README should still *say* the wrappers are exactly
  as lazy as core, since "wrapper library" often implies eagerness to readers.
- Suggest the equivalence property (stage 05) be stated in the README as a user-facing
  guarantee: *for every wrapped arity, `(c/f coll args…)` ≡ the corresponding core
  call, for all inputs.* That one sentence is the library's contract.

## 6. Fix the README's incorrect examples now, not at stage 08

Beyond the broken `(map inc)` thread (see §1), the "complex example" claims

```clojure
(-> [1 2 3 4 5 6 7 8 9 10] (c/partition-by odd?) (c/take 3) (c/partition-all 2) flatten)
;; => (1 2 3 3 5)   ; actual result: (1 2 3)
```

`partition-by odd?` on 1..10 alternates every element, so the pipeline yields
`(1 2 3)`. Wrong examples in a correctness-pitch library are uniquely damaging;
suggest making every README example REPL-verified (stage 05's test.check harness could
double as a doc-example checker, or add a small `README`-examples test ns). Also: the
README's "Available Functions" list shows 9 of the 18 fns, and the install snippet
references `project.clj`/Leiningen while the roadmap builds on `deps.edn` — align on
one story (git dep + `deps.edn` until Clojars at stage 08).

## 7. Close the unit-test coverage gap

`group-by`, `split-with`, `some`, `remove`, `interpose`, `interleave`, `concat`,
`subvec`, and `reduce` have no tests at all today. Stage 05's generative equivalence
suite will subsume most of this, but until then a failure in half the public API is
invisible. Cheap interim fix: one `deftest` per untested fn with two or three
examples, written in the same style as the existing tests. (The `partition-all-test`
paren is RESOLVED: it was not merely misnested — the unbalanced paren meant the whole
test ns failed to read, so the suite had never run. Fixed in `3fc0b6d`; siblings
restored in `7c513c3`. `core.clj` had the same defect, fixed in the same commit.)

## 8. Smaller points

- **Layout**: RESOLVED — moved to `src/consistent_lib/core.clj` by stage 01 (`7c513c3`).
- **Naming collision as a feature**: keeping core's names is the right design (the
  library is a drop-in reading of core), but the README should explicitly warn that
  `:refer :all`-ing this namespace is unsupported and `:as c` is the intended usage.
- **Error behavior**: with swapped arguments, failures surface as confusing core
  errors (e.g. `(c/take 3 [1 2 3])` → core tries to count a number). The consumer
  clj-kondo config (stage 04) is the right fix; runtime asserts would cost more than
  they buy in a pure-delegation library. Worth stating that trade-off in the README.
- **`split-at`/`split-with` returns**: core returns a two-element vector; keep it, but
  the docstrings should say "returns `[taken dropped]`" so users don't destructure
  blind.

## Priority order

1. §2 `:refer-clojure :exclude` + `:as c` convention (one-line fix, removes warnings)
2. §6 README example corrections (broken examples actively mislead)
3. §1 inclusion rule + drop/justify the identity wrappers (API surface is hardest to
   change after 0.1.0)
4. §3 multi-operand rule written down (unblocks stages 03–04 from re-deciding it)
5. §7 interim unit tests (safety net until stage 05)
6. §4–§5 documentation of semantics (can ride along with stage 08)
