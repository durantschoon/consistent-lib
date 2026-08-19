# Stage 01 — REPORT

status: COMPLETE
branch: `stage-01-deps-src-layout`
base commit: `3fc0b6d` ("Fix unbalanced parens so source and tests read")

## Summary

The repo now builds and tests as a conventional `deps.edn` project.

1. **`deps.edn` (new)** — `:paths ["src"]`, a single `:deps` entry
   (`org.clojure/clojure {:mvn/version "1.12.3"}`), and a `:test` alias carrying
   `:extra-paths ["test"]` plus `io.github.cognitect-labs/test-runner` (git tag
   `v0.5.1`, sha `dfb30dd…`) as `:extra-deps`, exposed via
   `:exec-fn cognitect.test-runner.api/test` so that `clojure -X:test` runs.
   The test runner is `:test`-alias-scoped, so guardrail 6 (zero runtime
   dependencies) holds: the library's own `:deps` is Clojure and nothing else.
2. **`git mv consistent_lib/core.clj src/consistent_lib/core.clj`** — git reports
   the change as `rename … (100%)`. The move itself was byte-for-byte content
   preserving; the file was subsequently edited in exactly one place under a
   coordinator amendment (see Deviation 1). The emptied `consistent_lib/` directory
   was removed (`git mv` leaves the empty dir behind on this platform; `rmdir`
   finished the job).
3. **`src/consistent_lib/core.clj` ns form** — gained
   `(:refer-clojure :exclude [...])` naming all 18 intentionally shadowed core vars,
   per the coordinator amendment. No function body, docstring, signature, ordering,
   or the trailing `(comment …)` block was touched.
4. **`test/consistent_lib/core_test.clj`** — `partition-all-test`'s two `testing`
   forms are now siblings rather than nested. One `)` moved from the end of the
   deftest to the end of the first `testing` form. Assertion text is untouched;
   the suite still reports 26 assertions.
5. **`.clj-kondo/config.edn` (new)** — one namespace-scoped exclusion pair on the
   test ns only (`:refer-all`, `:type-mismatch`); both itemised under Deviations.
   `:redefined-var` is deliberately left **on**.

Both gates pass and the test counts match the pre-change baseline exactly.

## Gate results

| Gate | Baseline (at `3fc0b6d`) | Final |
|---|---|---|
| `clojure -X:test` | 9 tests, 26 assertions, 0 failures, 0 errors | 9 tests, 26 assertions, 0 failures, 0 errors |
| `clj-kondo --lint src test` | 1 error, 2 warnings (`src` did not exist) — 0 errors / 20 warnings on the real paths | 0 errors, 0 warnings (cold cache and warm) |

Secondary measure requested by the amendment — JVM compiler warnings at require time:

| | Baseline | Final |
|---|---|---|
| `require` of `consistent-lib.core` alone (what a consumer sees) | 18 | **0** |
| Full `clojure -X:test` run (loads the test ns too) | 36 | 18 |

### Baseline — test gate

There is no `deps.edn` at `3fc0b6d`, so `clojure -X:test` cannot run at all on the
base commit. The equivalent baseline was taken with an ad-hoc classpath (the command
the coordinator specified):

```
$ clojure -Sdeps '{:paths ["." "test"]}' -M -e "(require 'consistent-lib.core-test)(clojure.test/run-tests 'consistent-lib.core-test)"
[14 JVM "already refers to" WARNING lines elided — see note below]

Testing consistent-lib.core-test

Ran 9 tests containing 26 assertions.
0 failures, 0 errors.
{:test 9, :pass 26, :fail 0, :error 0, :type :summary}
```

### Baseline — lint gate

The Definition-of-Done command, run verbatim on the unmodified base, fails on the
missing `src` directory — expected, since creating `src/` is this stage's job:

```
$ clj-kondo --lint src test
src:0:0: error: file does not exist
test/consistent_lib/core_test.clj:2:34: warning: use alias or :refer [deftest is testing]
test/consistent_lib/core_test.clj:3:41: warning: use alias or :refer [drop drop-while partition partition-all partition-by split-at take take-nth take-while]
linting took 59ms, errors: 1, warnings: 2
```

Pointing the same linter at the paths that actually existed at `3fc0b6d` gives the
meaningful baseline — 0 errors, 20 warnings:

```
$ clj-kondo --lint consistent_lib test
consistent_lib/core.clj:6:1: warning: partition already refers to #'clojure.core/partition
consistent_lib/core.clj:11:1: warning: partition-all already refers to #'clojure.core/partition-all
consistent_lib/core.clj:16:1: warning: partition-by already refers to #'clojure.core/partition-by
consistent_lib/core.clj:21:1: warning: take already refers to #'clojure.core/take
consistent_lib/core.clj:26:1: warning: drop already refers to #'clojure.core/drop
consistent_lib/core.clj:31:1: warning: take-nth already refers to #'clojure.core/take-nth
consistent_lib/core.clj:36:1: warning: take-while already refers to #'clojure.core/take-while
consistent_lib/core.clj:41:1: warning: drop-while already refers to #'clojure.core/drop-while
consistent_lib/core.clj:46:1: warning: split-at already refers to #'clojure.core/split-at
consistent_lib/core.clj:51:1: warning: split-with already refers to #'clojure.core/split-with
consistent_lib/core.clj:56:1: warning: group-by already refers to #'clojure.core/group-by
consistent_lib/core.clj:61:1: warning: interleave already refers to #'clojure.core/interleave
consistent_lib/core.clj:66:1: warning: interpose already refers to #'clojure.core/interpose
consistent_lib/core.clj:71:1: warning: concat already refers to #'clojure.core/concat
consistent_lib/core.clj:76:1: warning: some already refers to #'clojure.core/some
consistent_lib/core.clj:81:1: warning: remove already refers to #'clojure.core/remove
consistent_lib/core.clj:86:1: warning: subvec already refers to #'clojure.core/subvec
consistent_lib/core.clj:91:1: warning: reduce already refers to #'clojure.core/reduce
test/consistent_lib/core_test.clj:2:34: warning: use alias or :refer [deftest is testing]
test/consistent_lib/core_test.clj:3:41: warning: use alias or :refer [drop drop-while partition partition-all partition-by split-at take take-nth take-while]
linting took 10ms, errors: 0, warnings: 20
```

18 × `redefined-var` + 2 × `refer-all`, confirmed via `--config '{:output {:format :json}}'`.

### Final — test gate

```
$ clojure -X:test

Running tests in #{"test"}
WARNING: reduce already refers to: #'clojure.core/reduce in namespace: consistent-lib.core-test, being replaced by: #'consistent-lib.core/reduce
WARNING: take-nth already refers to: #'clojure.core/take-nth in namespace: consistent-lib.core-test, being replaced by: #'consistent-lib.core/take-nth
WARNING: take already refers to: #'clojure.core/take in namespace: consistent-lib.core-test, being replaced by: #'consistent-lib.core/take
WARNING: take-while already refers to: #'clojure.core/take-while in namespace: consistent-lib.core-test, being replaced by: #'consistent-lib.core/take-while
WARNING: remove already refers to: #'clojure.core/remove in namespace: consistent-lib.core-test, being replaced by: #'consistent-lib.core/remove
WARNING: interleave already refers to: #'clojure.core/interleave in namespace: consistent-lib.core-test, being replaced by: #'consistent-lib.core/interleave
WARNING: group-by already refers to: #'clojure.core/group-by in namespace: consistent-lib.core-test, being replaced by: #'consistent-lib.core/group-by
WARNING: concat already refers to: #'clojure.core/concat in namespace: consistent-lib.core-test, being replaced by: #'consistent-lib.core/concat
WARNING: some already refers to: #'clojure.core/some in namespace: consistent-lib.core-test, being replaced by: #'consistent-lib.core/some
WARNING: drop already refers to: #'clojure.core/drop in namespace: consistent-lib.core-test, being replaced by: #'consistent-lib.core/drop
WARNING: split-at already refers to: #'clojure.core/split-at in namespace: consistent-lib.core-test, being replaced by: #'consistent-lib.core/split-at
WARNING: partition already refers to: #'clojure.core/partition in namespace: consistent-lib.core-test, being replaced by: #'consistent-lib.core/partition
WARNING: partition-all already refers to: #'clojure.core/partition-all in namespace: consistent-lib.core-test, being replaced by: #'consistent-lib.core/partition-all
WARNING: partition-by already refers to: #'clojure.core/partition-by in namespace: consistent-lib.core-test, being replaced by: #'consistent-lib.core/partition-by
WARNING: subvec already refers to: #'clojure.core/subvec in namespace: consistent-lib.core-test, being replaced by: #'consistent-lib.core/subvec
WARNING: split-with already refers to: #'clojure.core/split-with in namespace: consistent-lib.core-test, being replaced by: #'consistent-lib.core/split-with
WARNING: interpose already refers to: #'clojure.core/interpose in namespace: consistent-lib.core-test, being replaced by: #'consistent-lib.core/interpose
WARNING: drop-while already refers to: #'clojure.core/drop-while in namespace: consistent-lib.core-test, being replaced by: #'consistent-lib.core/drop-while

Testing consistent-lib.core-test

Ran 9 tests containing 26 assertions.
0 failures, 0 errors.
```

Same 9 tests and same 26 assertions as the baseline — no test was gained, lost, or
silently skipped by the `partition-all-test` rebalance.

**Note on the 18 remaining WARNING lines.** Every one now names
`in namespace: consistent-lib.core-test` — **none** come from `consistent-lib.core`
any more. They are emitted by the *test* file's own `:refer :all`, not by the library.
What a downstream consumer actually sees is clean:

```
$ clojure -M -e "(require 'consistent-lib.core)(println :loaded-ok)"
:loaded-ok
```

Zero warnings, which is the outcome the amendment was aimed at. Removing the last 18
would mean adding `(:refer-clojure :exclude …)` to the test namespace — a rewrite of
the test file that both the original prompt and the amendment's scope note rule out.
Carried as Open question 4.

### Final — lint gate

Run from a **cold** cache (`.clj-kondo/.cache` deleted first) and again warm, to prove
the result does not depend on cache state (see Deviation 3):

```
$ command rm -rf .clj-kondo/.cache
$ clj-kondo --lint src test
linting took 10ms, errors: 0, warnings: 0
exit=0

$ clj-kondo --lint src test
linting took 18ms, errors: 0, warnings: 0
exit=0
```

### Post-amendment regression check (not a required gate)

`(:refer-clojure :exclude …)` could in principle have broken a wrapper that relies on
an excluded name. `core.clj` calls every delegate fully qualified, so it should not —
verified for the 9 wrappers the suite does *not* cover:

```
$ clojure -M -e "(require '[consistent-lib.core :as c]) (println (c/reduce [1 2 3] +)) …"
6
16
{true [1 3], false [2 4]}
true
(2 4)
[2 3]
(1 :x 2 :x 3)
(1 2 3)
(1 3 2 4)
[(1) (2 3 1)]
```

All correct (`reduce` 2- and 3-arity, `group-by`, `some`, `remove`, `subvec`,
`interpose`, `concat`, `interleave`, `split-with`).

## `git diff 3fc0b6d --stat`

```
$ git diff 3fc0b6d -M --stat
 {consistent_lib => src/consistent_lib}/core.clj | 6 +++++-
 test/consistent_lib/core_test.clj               | 4 ++--
 2 files changed, 7 insertions(+), 3 deletions(-)

$ git diff 3fc0b6d -M --summary
 rename {consistent_lib => src/consistent_lib}/core.clj (90%)
```

The whole of that 6-line delta in `core.clj` is the `ns` form:

```
-(ns consistent-lib.core)
+(ns consistent-lib.core
+  (:refer-clojure :exclude [partition partition-all partition-by take drop
+                            take-nth take-while drop-while split-at split-with
+                            group-by interleave interpose concat some remove
+                            subvec reduce]))
```

Plus two new files not tracked at diff time, both committed in this stage:
`deps.edn` and `.clj-kondo/config.edn`. `.clj-kondo/.cache/` is already covered by
`.gitignore` and is not committed. The exact post-commit figures are reproduced by
`git diff 3fc0b6d HEAD -M --stat`.

## Deviations

1. **Coordinator amendment to the prompt, received mid-stage and applied in full.**
   The user-authorized amendment relaxed the "Allowed files" entry for
   `src/consistent_lib/core.clj` — which the committed prompt marks *"content
   unchanged"* — in exactly one respect: the `ns` form MAY gain a
   `(:refer-clojure :exclude [...])` clause listing the intentionally shadowed core
   names; "Nothing else in core.clj may change. No function bodies, no docstrings, no
   signatures, no reordering, no formatting sweep, no touching the trailing
   `(comment ...)` block. The ns form is the entire permitted edit." The amendment's
   stated reasoning: the `.clj-kondo` config route "silences only the linter. All 18
   compiler warnings still fire at require time for every downstream consumer — it
   would turn your gate green while leaving the actual defect in place", and
   `:redefined-var {:level :off}` "would mask a genuine accidental redefinition, which
   matters because stage 04 adds more wrappers (map, filter, keep, mapcat, every?) to
   this same namespace." It therefore directed: do NOT disable `:redefined-var`.
   Applied exactly — the ns form is the only edit to `core.clj`, and `:redefined-var`
   is left on. **I verified the amendment's premise rather than assuming it**, and it
   holds: consumer-visible compiler warnings went 18 → 0, and both lint warnings and
   compiler warnings for `consistent-lib.core` are now genuinely absent rather than
   suppressed. Nothing in the amendment conflicts with the six guardrails in
   `docs/stages/README.md` — `:refer-clojure :exclude` changes no behavior, no
   signature, and no public var name, so guardrails 1, 3 and 5 are untouched.
   As instructed I derived the 18 names myself from the `defn` forms
   (`grep -n '^(defn '`) and machine-checked each against `clojure.core` via
   `ns-resolve` rather than trusting the supplied list. My derivation matched the
   coordinator's 18 exactly, in the same order, so there is **no discrepancy** to
   report and the file did not change under us.
2. **The worktree was branched from `132ef10`, not the `3fc0b6d` I was told was my
   base — I reset to `3fc0b6d` before doing any work.** On arrival `git log` showed
   HEAD at `132ef10` ("Add design improvement suggestions doc") and
   `git merge-base --is-ancestor 3fc0b6d HEAD` answered NO; `main` was at `3fc0b6d`,
   one commit ahead of me. `132ef10` is the *pre*-paren-fix tree, where both source
   files are unreadable, so measuring a baseline there was impossible and step 3's
   description would not have matched the file. Since my worktree was clean and I had
   made no changes, I ran `git reset --hard 3fc0b6d` to land on the declared base, then
   `git branch -m stage-01-deps-src-layout`. This is a worktree-provisioning
   discrepancy rather than a prompt contradiction, and resetting to the base the
   coordinator named was the minimal resolution — but the coordinator should confirm
   the worktree-creation step, because a stale worktree could silently mislead a
   future stage. **No content from `132ef10` is in this branch**; `3fc0b6d` is its
   parent.
3. **`:type-mismatch` is off in the test namespace — an exclusion neither the prompt
   nor the amendment anticipated, and the amendment did not remove the need for it.**
   Once `core.clj` sits under `src/`, `clj-kondo --lint src test` produces **44 errors
   on a cold cache and 0 on a warm one** — the gate's verdict depended on whether
   `.clj-kondo/.cache` happened to exist. Verified by alternating
   `command rm -rf .clj-kondo/.cache` with repeat runs. Cause: on a cold cache
   clj-kondo lints the test file before it has analysed `src/consistent_lib/core.clj`,
   so the `:refer :all` names fall back to `clojure.core` and `clojure.core`'s built-in
   argument-type specs are applied to our deliberately arg-swapped wrappers. Every one
   of the 44 is a false positive — e.g. `(partition [1 2 3 4] 2)` is flagged
   `Expected: integer, received: vector` because it is being checked against
   `clojure.core/partition`'s `[n coll]` order; the tests themselves pass. **I re-tested
   this after applying the amendment on the hypothesis that the exclude would fix it
   too: it does not** — the cold run still produced the same 44 errors with
   `:refer-clojure :exclude` in place, because the mis-resolution happens in the test
   namespace, which the exclude does not touch. Scoping `:type-mismatch {:level :off}`
   to `consistent-lib.core-test` is what makes the gate deterministic from a cold clone.
   This is a real loss of coverage (the linter is fully off in that ns, not narrowed),
   accepted because it is currently 100% false-positive there — but it matters for
   stage 07, where CI always starts cold. See Open questions 1 and 2.
4. **Clojure version pinned to `1.12.3`, which the prompt did not specify.** The prompt
   said only "no runtime dependencies beyond `org.clojure/clojure`". I chose 1.12.3
   (present in the local `~/.m2` cache, so the build works offline; 1.12.4 was also
   available). Nothing in the library depends on a 1.12-only feature, so this is a
   floor, not a requirement.
5. **Test runner pinned by both `:git/tag` and `:git/sha`.** `v0.5.1` /
   `dfb30dd6605cb6c0efc275e1df1736f6e90d4d73`, chosen because that sha was already in
   `~/.gitlibs`, so the gate runs without network access. The prompt named the library
   but not a version.
6. **`git mv` did not remove the emptied `consistent_lib/` directory.** The prompt
   requires it "must not survive", so I ran `rmdir consistent_lib` afterwards. Git
   itself does not track empty directories, so this affects the working tree only.
7. **`deps.edn` and `.clj-kondo/config.edn` carry explanatory comments.** The prompt
   asked for a "minimal" config; I read that as minimal in *effect* (narrowest possible
   exclusions) rather than minimal in bytes, and each exclusion is commented with its
   rationale in-file so the next reader does not have to open this report. If the
   coordinator wants them stripped, that is a trivial follow-up.
8. **Superseded, recorded for the audit trail.** Before the amendment arrived I had
   scoped a `:redefined-var {:level :off}` exclusion to `consistent-lib.core` (not to
   the test ns, as the prompt's step-4 wording implies — all 18 `redefined-var`
   warnings come from the library namespace, and only the 2 `refer-all` warnings come
   from the test ns; the prompt mis-attributes them). That exclusion has been **removed**
   in favour of the amendment's `(:refer-clojure :exclude …)`, and `:redefined-var`
   is now active repo-wide. Noting it because the prompt's step-4 wording is still
   worth correcting for future stages.

## Open questions

Noticed but deliberately **not** acted on, being outside this stage's allow-list:

1. **Stage 07 (CI) will hit the cold-cache problem head-on.** Deviation 3 neutralises it
   for the `:type-mismatch` linter specifically, but the underlying fragility — lint
   results depending on analysis order — is still there and could resurface as a
   different linter when stages 03/04 add wrappers. The standard clj-kondo remedy is a
   cache-warming pre-pass (`clj-kondo --lint "$(clojure -Spath)" --dependencies`) before
   the real lint. That belongs in the CI workflow, not in this stage's allow-list.
2. **Downstream consumers will see the same 44 false positives, and they will not have
   this repo's `.clj-kondo/config.edn`.** The exclude in `core.clj` fixes the *compiler*
   side for them but not the *lint* side: anyone who does
   `(:require [consistent-lib.core :refer :all])` gets clojure.core's arg-type specs
   applied to the swapped wrappers. This is squarely stage 04's "consumer lint config"
   territory and is arguably the more important half of the issue — worth exporting a
   config via clj-kondo's `:config-paths` / config-exporting mechanism so consumers
   inherit it automatically.
3. **`core.clj` defines 18 public wrappers but only 9 have tests.** Untested:
   `split-with`, `group-by`, `interleave`, `interpose`, `concat`, `some`, `remove`,
   `subvec`, `reduce`. The prompt's "Tests (enumerated)" lists exactly the 9 that exist,
   so this is expected at this stage and is presumably what stage 05's generative
   equivalence suite is for. I smoke-tested all 9 untested wrappers by hand (output
   above) purely as a regression check on the ns edit; that check is throwaway and was
   not added to the suite.
4. **18 compiler WARNINGs still come from the test namespace** and will grow with each
   new wrapper. Adding `(:refer-clojure :exclude …)` to `core_test.clj` would silence
   them, but that is a test-file rewrite the prompt and the amendment both exclude.
   Cheap for stage 03 or 04 to fold in when they touch the tests anyway.
5. **`README.md` still documents `lein test`**, which remains unrunnable — there is no
   `project.clj`. `clj -X:test` now works as documented. `README.md` is not in this
   stage's allow-list and stage 08 owns "accurate README", so I left it alone.
6. **The exclude list is now a maintenance obligation.** As the amendment notes, it
   doubles as a manifest of intentional shadowing — which means stage 04 must add
   `map`, `filter`, `keep`, `mapcat`, `every?` to *both* the `defn` list and the
   `:exclude` vector. If they drift apart, `:redefined-var` (now left on, by design)
   will catch it — which is precisely the safety property the amendment was buying.
7. **`.gitignore` already contained `.clj-kondo/.cache/`** before `.clj-kondo/` existed —
   convenient, and no change was needed.

## Push

Attempted `git push -u origin stage-01-deps-src-layout`; the result is recorded in the
final executor message. Per `docs/stages/README.md`, the coordinator pushes and merges.
