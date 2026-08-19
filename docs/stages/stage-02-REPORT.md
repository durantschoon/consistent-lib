# Stage 02 report — wrapper definition strategy

- status: DONE
- outcome: MACRO
- base commit: `6ad248c` ("Amend stage 02 prompt before launch: 17 -> 18 wrappers")
- branch: `stage-02-wrapper-strategy`
- clj-kondo: v2025.07.28; Clojure 1.12.3

## Summary

All three acceptance criteria pass, so the generation approach is kept.

`consistent-lib.impl/defwrapper` takes a wrapper name, an optional explicit
`clojure.core` symbol (defaulting to the wrapper name), and one `[params
core-args]` pair per arity. It emits a `defn` whose body is a bare delegation to
`clojure.core`, whose `:arglists` are literal and describe the wrapper's own
argument order, and whose docstring is the wrapped var's own docstring plus a
one-line coll-first note. `core-args` is validated at macroexpansion time to be a
permutation of `params`, so a spec cannot drop, invent, or compute an argument —
guardrail 1 ("argument order is the ONLY permitted difference") is enforced by
the macro rather than by review.

All 18 public fns in `consistent-lib.core` are ported. Names, arities and
behavior are unchanged; only the docstrings changed, and they changed by becoming
`clojure.core`'s own text, which was the motivation for the stage (the old
hand-written `partition` docstring claimed `[coll n]` was the whole story).

A `.clj-kondo/config.edn` + `analyze-call` hook makes the generated wrappers
fully visible to clj-kondo. The config contains no suppressions of any kind.

### Base-commit check (first action)

The worktree was created from `3fbb7bd`, two commits behind `main`:

```
$ git log --oneline -3
3fbb7bd Sweep docs for freshness after stages 3fc0b6d..5ea0302
5ea0302 Use :as c in tests, drop both lint suppressions
7c513c3 Add deps.edn test tooling and move source under src/
$ git merge-base --is-ancestor 6ad248c HEAD && echo BASE-OK || echo BASE-STALE
BASE-STALE
```

The tree was clean, so per the launch instruction: `git reset --hard 6ad248c`.
Re-check:

```
$ git log --oneline -3
6ad248c Amend stage 02 prompt before launch: 17 -> 18 wrappers
ce228db Ignore stage-executor worktrees
3fbb7bd Sweep docs for freshness after stages 3fc0b6d..5ea0302
$ git merge-base --is-ancestor 6ad248c HEAD && echo BASE-OK || echo BASE-STALE
BASE-OK
```

No baseline was measured before this check passed. See Deviation 1 — this is the
second consecutive stage whose worktree came off a stale commit.

## Gates

### Baseline, measured on unmodified `6ad248c`

`clojure -X:test`:

```
Running tests in #{"test"}

Testing consistent-lib.core-test

Ran 9 tests containing 26 assertions.
0 failures, 0 errors.
```

`clj-kondo --lint src test`, COLD (no `.clj-kondo` directory existed at all on
the base commit, so this run had no cache and no config):

```
linting took 23ms, errors: 0, warnings: 0
```

### Final

`clojure -X:test`:

```
Running tests in #{"test"}

Testing consistent-lib.core-test

Testing consistent-lib.metadata-test

Ran 13 tests containing 82 assertions.
0 failures, 0 errors.
```

The 9 pre-existing tests / 26 assertions still pass on their own, unchanged
(`clojure -X:test :nses '[consistent-lib.core-test]'`):

```
Running tests in #{"test"}

Testing consistent-lib.core-test

Ran 9 tests containing 26 assertions.
0 failures, 0 errors.
```

`clj-kondo --lint src test`, COLD (`rm -rf .clj-kondo/.cache` immediately before)
and then WARM, in one session:

```
--- COLD ---
linting took 74ms, errors: 0, warnings: 0
cold-exit=0
--- WARM ---
linting took 15ms, errors: 0, warnings: 0
warm-exit=0
```

Cold and warm agree. Both gates: 0 failures, 0 errors, 0 warnings.

| gate                          | baseline (`6ad248c`) | final          |
|-------------------------------|----------------------|----------------|
| `clojure -X:test` tests       | 9                    | 13             |
| `clojure -X:test` assertions  | 26                   | 82             |
| `clojure -X:test` fail/err    | 0 / 0                | 0 / 0          |
| `clj-kondo` cold err/warn     | 0 / 0                | 0 / 0          |
| `clj-kondo` warm err/warn     | 0 / 0 (n/a, no cache)| 0 / 0          |

## Criterion (a) — clj-kondo statically understands the generated fns

**Why a hook was needed.** With `core.clj` ported but no `.clj-kondo` config in
place, clj-kondo does not macroexpand `defwrapper`, registers no vars for the
wrappers, and reads their parameter symbols as unresolved globals:

```
$ clj-kondo --lint src test
src/consistent_lib/core.clj:17:5: error: Unresolved symbol: coll
src/consistent_lib/core.clj:17:10: error: Unresolved symbol: n
src/consistent_lib/core.clj:23:10: error: Unresolved symbol: f
src/consistent_lib/core.clj:28:13: error: Unresolved symbol: drop
...
src/consistent_lib/core.clj:69:12: error: Unresolved symbol: init
linting took 63ms, errors: 25, warnings: 0
```

`.clj-kondo/hooks/consistent_lib/impl.clj` rewrites each `defwrapper` form into
the `defn` it actually expands to (including the real `clojure.core` delegation
call, so the delegation itself is arity-checked too), and
`.clj-kondo/config.edn` registers it as an `:analyze-call` hook. That is a
translation of the macro, not a suppression: the config file contains no
`:linters` key and no `#_{:clj-kondo/ignore ...}` was added anywhere.

**Evidence that wrappers are statically understood.** Scratch file (written to a
scratchpad outside the repo, NOT committed):

```clojure
(ns arity-scratch
  (:require [consistent-lib.core :as c]))

;; deliberately wrong: c/partition is [coll n], called with one argument
(defn bad-partition [xs]
  (c/partition xs))

;; deliberately wrong: c/reduce is [coll f] / [coll f init], called with four
(defn bad-reduce [xs]
  (c/reduce xs + 0 :extra))

;; correct calls, for contrast
(defn good [xs]
  [(c/partition xs 2)
   (c/reduce xs + 0)
   (c/interleave xs xs xs)])
```

Linted cold, together with the project:

```
$ rm -rf .clj-kondo/.cache; clj-kondo --lint src test /…/scratchpad/arity_scratch.clj
/…/scratchpad/arity_scratch.clj:6:3: error: consistent-lib.core/partition is called with 1 arg but expects 2
/…/scratchpad/arity_scratch.clj:10:3: error: consistent-lib.core/reduce is called with 4 args but expects 2 or 3
linting took 77ms, errors: 2, warnings: 0
```

Both wrong-arity calls are caught, including the multi-arity `reduce` ("expects 2
or 3"), and the correct calls — fixed-arity, multi-arity and variadic — produce
nothing. Criterion (a): **PASS**.

## Criterion (b) — docstring and arglists in a REPL

```
$ clojure -M -e "(require 'consistent-lib.core 'clojure.repl) \
    (clojure.repl/doc consistent-lib.core/partition) \
    (clojure.repl/doc consistent-lib.core/reduce) \
    (clojure.repl/doc consistent-lib.core/interleave)"
-------------------------
consistent-lib.core/partition
([coll n])
  Returns a lazy sequence of lists of n items each, at offsets step
  apart. If step is not supplied, defaults to n, i.e. the partitions
  do not overlap. If a pad collection is supplied, use its elements as
  necessary to complete last partition upto n items. In case there are
  not enough padding elements, return a partition with less than n items.

  consistent-lib: coll-first wrapper for clojure.core/partition — same behavior, collection first (see :arglists above).
-------------------------
consistent-lib.core/reduce
([coll f] [coll f init])
  f should be a function of 2 arguments. If val is not supplied,
  returns the result of applying f to the first 2 items in coll, then
  applying f to that result and the 3rd item, etc. If coll contains no
  items, f must accept no arguments as well, and reduce returns the
  result of calling f with no arguments.  If coll has only 1 item, it
  is returned and f is not called.  If val is supplied, returns the
  result of applying f to val and the first item in coll, then
  applying f to that result and the 2nd item, etc. If coll contains no
  items, returns val and f is not called.

  consistent-lib: coll-first wrapper for clojure.core/reduce — same behavior, collection first (see :arglists above).
-------------------------
consistent-lib.core/interleave
([& colls])
  Returns a lazy seq of the first item in each coll, then the second etc.

  consistent-lib: coll-first wrapper for clojure.core/interleave — same behavior, collection first (see :arglists above).
```

The docstring is derived (core's text verbatim + the one-line note) and the
arglists are the wrapper's own coll-first order, not core's. Criterion (b):
**PASS**.

## Criterion (c) — existing suite unchanged

`consistent-lib.core-test` is untouched and passes 9 tests / 26 assertions, as
shown under Gates. Criterion (c): **PASS**.

## Public API compatibility (guardrail 5)

`(count (ns-publics 'consistent-lib.core))` => 18. Arglists, in name order,
identical to the hand-written arities on `6ad248c`:

```
(([& colls])            ; concat
 ([coll n])             ; drop
 ([coll pred])          ; drop-while
 ([coll f])             ; group-by
 ([& colls])            ; interleave
 ([coll separator])     ; interpose
 ([coll n])             ; partition
 ([coll n])             ; partition-all
 ([coll f])             ; partition-by
 ([coll f] [coll f init]) ; reduce
 ([coll pred])          ; remove
 ([coll pred])          ; some
 ([coll idx])           ; split-at
 ([coll pred])          ; split-with
 ([coll start end])     ; subvec
 ([coll n])             ; take
 ([coll n])             ; take-nth
 ([coll pred]))         ; take-while
```

No arity was added (that is stage 03's job) and none was lost. Spot-check of the
nine wrappers the existing suite does not cover, all matching the pre-stage
implementations:

```
(c/split-with [1 2 3 4] odd?)        => [(1) (2 3 4)]
(c/group-by [1 2 3 4] odd?)          => {true [1 3], false [2 4]}
(c/interleave [1 2] [:a :b] ['x 'y]) => (1 :a x 2 :b y)
(c/interpose [1 2 3] :sep)           => (1 :sep 2 :sep 3)
(c/concat [1 2] [3] [4 5])           => (1 2 3 4 5)
(c/some [1 2 3] even?)               => true
(c/remove [1 2 3 4] odd?)            => (2 4)
(c/subvec [1 2 3 4 5] 1 3)           => [2 3]
(c/reduce [1 2 3] +) / (c/reduce [1 2 3] + 10) => 6 / 16
```

The `(:refer-clojure :exclude [...])` list is unchanged and still names all 18
wrappers; `:redefined-var` does not fire (0 warnings, cold and warm).

## Diff

```
$ git diff 6ad248c --stat
 .clj-kondo/config.edn                    |  13 +++
 .clj-kondo/hooks/consistent_lib/impl.clj |  48 ++++++++++
 src/consistent_lib/core.clj              | 157 +++++++++++++------------------
 src/consistent_lib/impl.clj              | 124 ++++++++++++++++++++++++
 test/consistent_lib/metadata_test.clj    |  46 +++++++++
 5 files changed, 294 insertions(+), 94 deletions(-)
```

(Excludes this report, which is added in the same commit.) `deps.edn` needed no
change — the new test ns is already on the `:test` alias's `test` path.

## Deviations

1. **Stale worktree base, again.** The worktree was created from `3fbb7bd` while
   the expected base was `6ad248c`. Tree was clean, so `git reset --hard 6ad248c`
   per the launch instruction, before any measurement. This is the second stage
   in a row (stage 01 hit the same thing); it is a worktree-creation problem, not
   a one-off.
2. **`defwrapper` takes the core symbol as an optional second argument, not a
   required one.** The prompt says the macro is "given a `clojure.core` var and
   an argument-order spec". Since a wrapper is by definition a rename of the
   same-named core var, requiring `(defwrapper partition partition ...)` at all
   18 call sites would be redundant, so the core symbol defaults to the wrapper
   name. The explicit two-symbol form `(defwrapper my-name core-name spec+)` is
   implemented and handled by the hook, it just is not used yet. Minimal
   resolution; no capability is lost.
3. **The macro rejects specs that are not permutations.** Not asked for by the
   prompt. It is three lines and it converts guardrail 1 from a review item into
   a compile error, so I judged it in scope for "pure delegation (guardrail 1)".
   Its cost is that a future wrapper needing anything other than a permutation
   (there should be none) fails loudly at macroexpansion.
4. **The metadata test asserts slightly more than the prompt enumerated.** The
   prompt requires docstring + `:arglists` on every public var and a count of 18.
   I also added `every-public-var-is-coll-first-test`, which asserts the first
   parameter of every arity is `coll` or `&`. It is guardrail 2 in test form and
   it costs nothing; it does couple the test to the parameter *name* `coll`, so a
   future wrapper that names its collection something else will have to update
   the predicate. Flagging in case that is unwanted coupling.
5. **Wrapper docstrings changed text.** They are now `clojure.core`'s docstrings
   verbatim plus a note, replacing the hand-written summaries. Behavior, names
   and arities are byte-compatible, but anyone diffing rendered docs will see
   every docstring change. This is the stage's intent (the old `partition`
   docstring documented only `[coll n]` while core supports step and pad), but it
   is a visible change, so it is recorded here rather than left implicit.

6. **Commit message carries a `Co-Authored-By` trailer.** The prompt specifies the
   message "exactly"; the subject line is exactly that, with the trailer appended
   below a blank line, matching stage 01's commit (`7c513c3`) and this
   environment's standing git instruction.

## Open questions

1. **Core docstrings now describe arities we do not expose.** `partition`'s
   derived docstring explains `step` and `pad`; `subvec`'s explains the 2-arity
   default. Our `:arglists` correctly say `([coll n])` and `([coll start end])`,
   so doc and arglists disagree in spirit until stage 03 adds the missing
   arities. Stage 03 largely closes this by itself. If it does not close it
   fully, `defwrapper` would need a way to note "this wrapper exposes a subset of
   core's arities" — I did not build one, since stage 03 is the right place to
   decide whether it is needed.
2. **Nothing tests the hook.** `.clj-kondo/hooks/consistent_lib/impl.clj`
   duplicates the macro's expansion logic in a second language runtime (SCI), and
   the two can drift silently — the only thing that would catch it is the lint
   gate going red or, worse, quietly permissive. The scratch-file check in
   criterion (a) is the check, and it is manual and uncommitted by instruction. A
   committed fixture that lints a known-bad file and expects a specific finding
   would make this a gate; that needs a place to live (stage 07's CI is the
   natural home).
3. **`:arglists` accuracy is asserted only as "non-nil".** The metadata test
   cannot currently tell an accurate arglist from a plausible one. Stage 05's
   generative equivalence suite is the real answer; a cheaper intermediate would
   be to compare each wrapper's arity count against the wrapped core var's.
4. **`consistent-lib.impl` is public surface.** It is a normal namespace, so
   `defwrapper` is callable by consumers. Nothing in the roadmap says whether the
   library wants to expose a wrapper-defining macro; if not, a `^:no-doc` or a
   note in stage 08's README would settle it.
5. **Two wrappers keep parameter names core does not use.** `split-at` uses
   `idx` (core: `n`) and `interpose` uses `separator` (core: `sep`), inherited
   from the hand-written definitions and preserved to keep `:arglists`
   byte-compatible with today. Aligning them with core would be a nicer read but
   is a visible arglist change, so I left them alone.
