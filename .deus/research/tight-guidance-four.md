---
kind: research
id: tight-guidance-four
created_at: 2026-10-26T13:49:00Z
updated_at: 2026-10-26T13:49:00Z
status: reviewed
references:
  - dependencies/deal-embedding/ui/generation-guidance.md
  - dependencies/deal-embedding/ui/composition.md
  - dependencies/deal-embedding/core/repair-guidance.deal
  - dependencies/deal-embedding/android/src/compiler/ExperienceCompiler.kt
  - dependencies/deal/deal/module/CompilationTiming.java
  - android/tests/FrameworkE2eInstrumentation.kt
question: Does shorter authoring guidance plus clustered diagnostic feedback improve fresh generation latency and correctness?
mode: local
provider: Cerebras qwen-3.8-27b low reasoning; DeepSeek Flash discovery
sources:
  - build/v14-first-frame-four/cohort/numbers.json
  - build/tight-guidance-four/cohort/numbers.json
  - build/tight-guidance-four/cohort/results.json
  - build/tight-guidance-four/food-additional-only.log
  - build/tight-guidance-four/compiler-tests.log
  - build/tight-guidance-four-repeat/cohort/numbers.json
  - build/tight-guidance-four-repeat/cohort/results.json
  - build/tight-guidance-four-repeat/compiler-phases.json
  - build/tight-guidance-four-repeat/preservation.json
  - build/tight-guidance-four-repeat/historical-integrity.json
gaps:
  - Two changed-guidance samples per scenario versus one preceding baseline; not causal attribution or population reliability.
  - Multiple simultaneous changes; prompt shortening and clustered feedback are not separately ablated.
  - Both fresh cohorts fail departure and food supplied behavioral assertions.
  - Additional food reassessment on the first cohort is inconclusive as delivery-specific evidence because it reports Over budget and disables late options.
  - First cohort phase JSON was truncated in the logger target; only repeat artifact-based receipts are complete.
  - Emission timing includes staging/deployment; publication and uninstrumented compiler overhead remain residual.
  - Discovery token usage unknown; screenshots are not visual approval.
---

# Changes and verification

Authoring guidance shrank from 14,398 to 7,105 bytes (50.7%); composition from 4,387 to 2,764. Combined prose shrank 47.5%, preserving the renderer pack and both compiler-checked source examples. Added generic recomputation rules using current inputs and selected provider records, not a task-specific answer. Stable guidance/contracts remain before variable intent. Actual budget initial input shrank 18,168 to 16,395 provider tokens (9.8%); source input text shrank 57,851 to 48,939 characters (15.4%). The compiler language guide, contracts and examples remain substantial input.

Repair feedback lists every diagnostic location and merges overlapping nearby source lines once per file. Repeated code advice is deduplicated; a 120-line preview bound explicitly lists omissions and leaves read_file available. Exact-edit admission is unchanged. Tests cover adjacent diagnostics, compiler range shapes, duplicate-line/advice prevention, omission notices and operational parser failure.

Compiler instrumentation adds scoped monotonic parse/signature/typecheck/planning/lowering/emission receipts. Nested observers restore and exceptions do not alter compilation. Unified logging stores complete phase JSON as a captured artifact. The initial implementation used the bounded target string, truncating JSON; this was fixed before the repeat, without changing generation guidance.

Commits: DEAL 890d9dfc; embedding 905e224 plus receipt fix 8d269f5; demo tests a8ecd2b. DEAL full suite passed, as did packaged source-agent/repair/guidance/protocol checks and APK boundaries. Installed Toolkit, Discovery and assessment-waiting passed. GenerationGateway initially asserted the old feedback heading; migrated assertion then passed on the real installed APK. No model inference was used for those deterministic checks.

# Procedure

Two fresh four-scenario cohorts, same task facts/model/reasoning/backend and existing first-frame instrumentation. Archive then generated-only purge across all six apps before each run; clean-state receipts verify no generated keys/files/drafts/links and unchanged remaining preference records. Observation history stays on device. Post-repeat domain/authority preferences remain unchanged; only capture instrumentation changed its own preference. Organizer/tests rebuilt and installed before each relevant run. No build or source edit overlapped live measurement; no candidateTest, semantic feedback, retries, repaired reruns or retiming. Repeat differs only in complete phase receipt capture. Source failures recover within the original framework request.

# Fresh numbers

Seconds; checks are a subset, not additive to total. Timing stops at first fault-free committed frame, then supplied semantic actions execute.

| Scenario | Baseline total | Short run 1 total | Short run 2 total | Run 2 generation → frame | Run 2 checking | Turns 1 / 2 | Supplied 1 / 2 |
|---|---:|---:|---:|---:|---:|---:|---|
| Budget | 11.443 | 12.267 | 10.995 | 4.764 | 1.985 | 1 / 1 | Pass / Pass |
| Departure | 13.382 | 15.066 | 18.019 | 14.721 | 2.893 | 2 / 1 | Fail / Fail |
| Food | 25.833 | 35.343 | 27.684 | 24.262 | 4.056 | 2 / 5 | Fail / Fail |
| Equipment | 18.864 | 25.319 | 26.466 | 22.318 | 2.398 | 3 / 1 | Pass / Pass |
| Sum | 69.522 | 87.995 | 83.164 | 66.065 | 11.332 | 8 / 8 | 2/4 / 2/4 |

Cohort 1 is 26.6% slower than baseline; cohort 2 is 19.6% slower. Mean cohort total 85.580s, 23.1% slower. These samples demonstrate no optimization win; they do not isolate shortening as the cause.

Departure calls the actual 12km provider and gets 29 minutes; departure 15:16 is correct, but inspection is computed from event end as 17:10 rather than ten minutes before departure (15:06), in both runs. Food initializes only three pizzas/one vegan instead of six/at least two, in both runs. Thus concise constraints are not sufficient for faithful synthesis. No generated source was fixed after assessment.

Food additional-only reassessment of run 1, without inference/retiming, returns a failed assertion with Over budget. The generated source also disables late delivery options; this is not clean evidence that a reachable late selection remains feasible. It is retained separately, not counted as proof of the baseline's Ready defect or of successful constraint propagation. The normal benchmark skips this additional check when supplied food checks already fail.

# Tokens and recovery

All source calls; cached input and reasoning are subsets, not additional tokens. Discovery excluded as unknown.

| Scenario | Run 1 input / completion / reasoning / cached | Run 2 input / completion / reasoning / cached |
|---|---|---|
| Budget | 16,395 / 2,229 / 1,503 / 0 | 16,395 / 2,466 / 1,733 / 16,384 |
| Departure | 35,647 / 12,487 / 10,006 / 30,720 | 16,424 / 12,837 / 10,753 / 0 |
| Food | 41,981 / 37,540 / 30,470 / 30,720 | 110,958 / 24,591 / 17,559 / 99,328 |
| Equipment | 62,000 / 27,131 / 22,953 / 52,224 | 16,457 / 28,755 / 25,071 / 16,384 |
| Total | 156,023 / 79,387 / 64,932 / 113,664 | 160,234 / 68,649 / 55,116 / 132,096 |

Baseline totals were 143,501 / 57,096 / 45,786 / 93,184. Input reduction per initial request did not reduce aggregate tokens or completion work.

Run 1: eight native replies, eight initial file writes, one read, three edit batches, seven checks. Departure repairs unsupported Unicode escapes; food repairs eleven contextual table-read errors; equipment reads lines 81–130 then repairs eight contextual reads. Run 2: eight replies, eight writes, one read, three edit batches, seven checks; recovery is concentrated in food (syntax, reserved field and typed reads). Clustered feedback did not eliminate read roundtrips. Zero observed invalid-tool replies and no semantic feedback to generation. All referenced artifacts retained: 273/263 trace-filtered events and four complete request summaries each.

# Compiler result

Complete run-2 receipts cover five DEAL compiler invocations (some UI parse failures occur before DEAL invocation). Aggregate milliseconds:

| Phase | ms |
|---|---:|
| Parse/discovery | 154.984 |
| Signatures | 10.877 |
| Type checking | 140.681 |
| Checked project + requirement/construction/dependency planning | 184.304 |
| Semantic lowering | 4,557.934 |
| JS emission/staging/deployment | 5,728.650 |

Codegen parent is 10,287.253ms and overlaps the last two rows; do not sum it again. Recorded compiler-deal stages total 10,838ms. Lowering and JS emission/staging dominate, not dependency parsing/typechecking. This argues for profiling those owners next, not a speculative declaration/typecheck cache.

# Conclusion

Guidance is shorter and committed, diagnostics are clustered, and checking is now attributable. Fresh results are slower and behaviorally worse in both samples. Keep the evidence and report the regression; prompt brevity alone is not demonstrated quality or speed. Next experiments should isolate guidance structure/content and address explicit requirement preservation, while compiler investigation targets lowering/emission rather than frontend caching. No language semantics changed, no push, no visual-acceptance claim.
