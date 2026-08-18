# Stage 07 — GitHub Actions CI running the gates

## Motivation (measured)

The gates (`clojure -X:test`, `clj-kondo --lint src test`) run only on the developer
machine; nothing guards `main` on push. The charter milestone requires "CI green".

## The change

Create `.github/workflows/ci.yml`:

- Trigger: `push` and `pull_request` on `main`.
- One `ubuntu-latest` job: checkout → `actions/setup-java` (temurin LTS) →
  `DeLaGuardo/setup-clojure` (clojure CLI + clj-kondo + cljfmt, pinned versions) →
  dependency cache keyed on `deps.edn`.
- Blocking steps, in order: `clojure -X:test`; `clj-kondo --lint src test`.
- If (and only if) `deps.edn` contains a `:cljs-test` alias (stage 06 outcome A), a
  blocking `clojure -M:cljs-test` step.
- Advisory step (charter: cljfmt is never blocking): `cljfmt check` with
  `continue-on-error: true`.

## Ground rules

- No repo code changes — workflow file only. If the gates fail locally before you
  start, that is a Blocked situation, not something to fix here.
- Pin action versions by tag (e.g. `@v4`); no `@master`.
- Do not read `docs/stages/forecasts/`.

## Allowed files

- `.github/workflows/ci.yml` (new)
- `docs/stages/stage-07-REPORT.md`

## Tests (enumerated)

- Both gates re-run locally, green (this validates what CI will run).
- Workflow YAML parses: validate with `actionlint` if available on PATH, otherwise
  `python3 -c "import yaml,sys; yaml.safe_load(open('.github/workflows/ci.yml'))"`.
  State which validator ran in the report. (The repo may have no GitHub remote; a live
  CI run is NOT part of this stage's DoD — first push proves it later.)

## Definition of Done

- `clojure -X:test` → 0 failures, 0 errors.
- `clj-kondo --lint src test` → 0 errors, 0 warnings.
- Workflow validation output in the report.

## Commit

Single implementation commit, message exactly:

    Add GitHub Actions CI for tests and lint

## Report

`docs/stages/stage-07-REPORT.md`: Summary; validator used + output; verbatim gate
outputs; Deviations; Open questions.

## Blocked protocol

If a ground rule, guardrail, or missing information blocks you: STOP. Do not
improvise. Write the report with `status: BLOCKED`, the exact blocker, and what you
need; commit only the report.
