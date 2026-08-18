# Stage 08 — Release build tooling and accurate docs

## Motivation (measured)

- No build tooling exists: nothing can produce a jar or pom, and `pom.xml` is even
  gitignored from the Leiningen era.
- `README.md` misleads: the install snippet gives Leiningen coords for a library that
  now uses deps.edn tooling; the "Available Functions" list names 9 fns while the API
  is far larger; `README.md:44-48` threads bare core `map` — invalid before stage 04
  gave the library its own `map`.
- Charter milestone: 0.1.0 live on Clojars with cljdoc building. This stage makes the
  repo deploy-READY; the human runs the final deploy (credentials never reach an
  executor).

## The change

1. `build.clj` with tools.build (`:build` alias, tool-scoped deps only):
   - targets `jar`, `install`, and `deploy` (deploy via `slipset/deps-deploy`, reading
     `CLOJARS_USERNAME` / `CLOJARS_PASSWORD` from env — you will NOT run deploy).
   - Coordinates: `io.github.durantschoon/consistent-lib`, version `0.1.0`.
   - pom includes SCM URL `https://github.com/durantschoon/consistent-lib`, license
     (EPL-1.0, matching `LICENSE`), and description.
   - Un-ignore the generated pom path if tools.build writes it in-tree
     (`.gitignore` edit) or keep pom generation inside the jar — your call; state it.
2. README rewrite:
   - correct deps.edn AND Leiningen install coords for the new group/artifact;
   - regenerate the function list from the actual public API (group by theme; every
     name must exist — cross-check against `ns-publics`);
   - every code example verified in a REPL — full transcript in the report; the `->`
     examples must use the library's own wrappers throughout (`c/map`, not `map`);
   - Development section documents the real gates and (if present) the cljs gate;
   - add a "Releasing" section: exact `clojure -T:build jar/install/deploy` commands
     and required env vars;
   - cljdoc badge.
3. `CHANGELOG.md` with a 0.1.0 entry summarizing the library surface.

## Ground rules

- No changes to `src/` or `test/`. Do NOT run the `deploy` target (guardrail: no
  credentials, no publishing from an executor).
- Do not read `docs/stages/forecasts/`.

## Allowed files

- `build.clj` (new), `deps.edn` (`:build` alias only)
- `README.md`, `CHANGELOG.md` (new), `.gitignore` (pom lines only, if needed)
- `docs/stages/stage-08-REPORT.md`

## Tests (enumerated)

- `clojure -T:build jar` then `clojure -T:build install` succeed.
- Consumption smoke test: in a scratch directory OUTSIDE the repo (do not commit it),
  a minimal `deps.edn` depending on the locally installed
  `io.github.durantschoon/consistent-lib {:mvn/version "0.1.0"}` runs one README
  example successfully — transcript in the report.
- Full gate suite still green (no source changed, but prove it).

## Definition of Done

- `clojure -X:test` → 0 failures, 0 errors.
- `clj-kondo --lint src test` → 0 errors, 0 warnings.
- jar + install + consumption transcripts in the report.
- README REPL transcript covers every code example.

## Commit

Single implementation commit, message exactly:

    Add release build tooling and accurate docs

## Report

`docs/stages/stage-08-REPORT.md`: Summary; build + consumption transcripts; README
example transcript; verbatim gate outputs; Deviations; Open questions (including
anything the human must do to complete the Clojars publish).

## Blocked protocol

If a ground rule, guardrail, or missing information blocks you: STOP. Do not
improvise. Write the report with `status: BLOCKED`, the exact blocker, and what you
need; commit only the report.
