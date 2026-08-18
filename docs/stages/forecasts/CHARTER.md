# Goal charter — consistent-lib maturity push

Recorded 2026-08-18 from the coordinator's goal-charter interview (AskUserQuestion,
single round). Answers are the selected options, verbatim. Amendments are append-only
with dates; never rewrite an answer.

## Destination milestone

**"Published to Clojars"** — Done = version 0.1.0 live on Clojars with cljdoc
building, CI green. Stage 8 does the actual publish.

(Note: the executor cannot hold Clojars credentials, so stage 08's Definition of Done
is deploy-READY — verified jar + documented deploy command; the human runs the final
`deploy`, which completes this milestone.)

## ClojureScript scope

**"Stretch goal"** — Stage 6 attempts it; if it forces awkward restructuring,
deferring to a later version is an acceptable outcome, not a failure.

## Quality bar (sacred gates)

**"Tests + lint, props later"** — Every merge: full test suite + clj-kondo clean.
Property-based equivalence tests become sacred once stage 5 lands.

## Appetite / timebox

**"A few sessions, no deadline"** — Run stages as time permits; the pipeline can pause
between stages indefinitely.

## Known unknowns

None volunteered by the user. Coordinator-flagged (not user-confirmed): the charter is
silent on transducer support — the interview did not ask, so a transducer-driven pivot
counts as an elicitation gap (`overlooked`), not an external goal move.

## Amendments

(none yet)
