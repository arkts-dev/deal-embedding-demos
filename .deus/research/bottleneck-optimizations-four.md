---
kind: research
id: bottleneck-optimizations-four
created_at: 2026-10-26T10:34:00Z
updated_at: 2026-10-26T10:34:00Z
status: reviewed
references:
  - dependencies/deal-embedding/android/src/discovery/Discovery.kt
  - dependencies/deal-embedding/core/source-repair.deal
  - dependencies/deal-embedding/core/repair-guidance.deal
  - dependencies/deal-embedding/core/generation-guidance.deal
  - dependencies/deal-embedding/tools/GenerationGuidance.java
  - android/tests/DiscoveryInstrumentation.kt
  - android/tests/FrameworkE2eInstrumentation.kt
question: Do proposal-independent synthesis, precise exact-edit feedback and compiler-derived identifier guidance improve fresh first-frame generation and behavior?
mode: local
provider: Cerebras qwen-3.8-27b low reasoning; DeepSeek Flash discovery
sources:
  - build/bottleneck-optimizations-four/revisions.json
  - build/bottleneck-optimizations-four/cohort/numbers.json
  - build/bottleneck-optimizations-four/cohort/results.json
  - build/bottleneck-optimizations-four/cohort/native-tool-observations.json
  - build/bottleneck-optimizations-four/cohort/clean-state.json
  - build/bottleneck-optimizations-four/capture-validation.json
  - build/bottleneck-optimizations-four/verification.json
  - build/bottleneck-optimizations-four/historical-integrity.json
  - build/bottleneck-optimizations-four/baseline-additional.log
  - build/bottleneck-optimizations-four/discovery-tests.log
  - build/bottleneck-optimizations-four/gateway-tests.log
  - build/authority-attributes-four/cohort/numbers.json
gaps:
  - One new sample per scenario with several simultaneous changes; not a causal ablation or population reliability estimate.
  - No ambiguous edit occurred live; occurrence-context latency benefit remains unmeasured.
  - Reserved-name guidance did not prevent a fresh reserved field; compiler admission and targeted repair remain necessary.
  - Additional behavioral coverage exercises late delivery only; reordered or unavailable catalogues and general planning remain untested on fresh generated programs.
  - Semantic checks and screenshots do not establish exhaustive or visual acceptance.
  - Existing repair telemetry counts read-only calls as repairs; actual operations are reported separately.
  - Discovery usage is not retained in these unified receipts; unknown, not zero.
---

# Implementation

Embedding a4359c4 removes discovery's interactionProposal from generation context and its schema. The host instruction, original facts and admitted references remain; the exact brief remains captured in discovery observation/Inspect. Discovery title and selected references are still model-derived: this isolates implementation-plan text, not every possible discovery influence. Deterministic real-handoff tests hold title/references/facts fixed, vary contradictory plans, and verify identical actual generation input for both automatic-planning and explicit-manual tasks, with no provider reads.

Exact-edit ambiguity feedback now lists every overlapping occurrence's 1-based line span and literal numbered context for the first eight. Additional occurrences remain locatable through read_file; this is a preview bound, not an edit admission limit. Rejection still preserves the complete original pair, unique/disjoint requirements, digest and atomicity. Compiled tests cover overlapping matches, supplementary Unicode, CR/blank lines, multiline searches, more than eight occurrences, stale revisions, wrong files and unchanged exact application. An initial test exposed empty-string split returning zero elements; first-line location handling was corrected before inference.

GenerationGuidance extracts reserved spellings mechanically through the compiled lexer/token inventory, without a maintained keyword list or compiler behavior change. Guidance prohibits keyword identifiers while preserving provider keys. E1007 feedback identifies an offending reserved token at the diagnostic column with an identifier boundary; compiled tests distinguish `from` from `fromInstant`. Stale model instructions requesting digest-bound patch output are replaced by edit_sources with host-owned revision binding.

Root fec02f2 adds separate untimed late-delivery assessment. It sets valid quantities through issued controls, selects an actual late delivery Option and checks feasibility status. Unsupported control/presentation shapes are inconclusive. The historical supplied assertions remain unchanged. Before purge, no-model reassessment of the exact previous saved food program reports Feasible for late delivery and fails this additional check. That is a newly measured old defect, not a retiming or repair of old evidence.

# Verification and procedure

Compiled source-agent, exact-edit and guidance tests pass. Python gateway/streaming, metrics/cache, API and package-boundary tests pass. Installed Discovery, GenerationGateway and assessment-waiting tests pass. The first build failed on unavailable Android JSONObject.keySet; using its keys iterator fixed the test. A complete rebuilt Organizer/tests pair was installed before inference. No exhaustive suite claim.

All six apps were archived before generated-only purge. Receipts verify zero generated files/keys and unchanged remaining domain/authority preferences. Same four task facts, Organizer host/grants, source model and low reasoning setting, independent Flash discovery. No build overlap, candidateTest, provider switch, semantic feedback, model retry outside framework recovery, regeneration or retiming. The clock starts before foreground setup/discovery and freezes at the first fault-free generated frame committed to the swap chain, not physical scanout. Semantic actions, screenshots and additional checks follow that timestamp. Fresh generated state does not mean cold caches.

# Fresh results

Seconds; historical supplied checks remain directly comparable:

| Scenario | Discovery | Generation to frame | Total to frame | Checking | Model turns | Supplied checks |
|---|---:|---:|---:|---:|---:|---|
| Budget | 1.872 | 4.822 | 11.207 | 1.961 | 1 | Pass |
| Departure | 2.219 | 8.813 | 12.192 | 2.207 | 1 | Pass |
| Food | 2.044 | 29.158 | 32.270 | 3.026 | 4 | Pass |
| Equipment | 2.507 | 19.057 | 22.552 | 2.099 | 1 | Pass |

Previous to new total: 86.606 to 78.221 seconds, 8.385 seconds / 9.7% lower in this sample. Per scenario: budget 11.575 to 11.207; departure 17.112 to 12.192; food 25.039 to 32.270; equipment 32.880 to 22.552. Supplied checks rise from 3/4 to 4/4. Additional late-delivery check: previous exact food source fails; new food source passes. This is not exhaustive acceptance.

Seven native replies, zero observed protocol rejections. All four initial replies contain paired writes. Eight writes, two exact edit batches, one ranged read (deal lines 253–295); both edit batches applied, no ambiguous/no-match rejection. Six complete-pair checks. Food: E1007 reserved field from, exact rename, E3003 typed table reads, focused read, exact repair, pass. Thus reserved-name prevention remains incomplete even though focused feedback worked. Unlike the previous five file-spanning reads, this read retrieves only 43 of 431 lines.

# Effect assessment

Model-operation wall time falls 56.081 to 48.658 seconds (13.2%); checking rises 8.770 to 9.293. Generation-to-frame falls 69.331 to 61.850 seconds. Discovery inference totals 8.642 versus 9.528 seconds. Remaining non-overlapping overhead is 11.628 seconds, not independently attributed CPU.

Critically, aggregate initial provider calls are essentially unchanged: 44.148 to 44.404 seconds. Subsequent provider calls fall 11.367 to 3.991 seconds. The observed gain is concentrated in avoiding the equipment recovery chain, not demonstrated faster initial synthesis. New equipment initial synthesis is slower (13.612 to 16.224 seconds); new food initial synthesis is also slower (17.430 to 20.293). Departure initial synthesis falls 10.580 to 5.755. These contrasts prevent claiming a general reasoning-speed improvement.

No live ambiguity occurred, so deterministic occurrence-context tests establish mechanics, not live latency savings. Removing proposal text has direct boundary evidence and successful fresh departure behavior, but the new discovery proposal itself also has the correct formula. Therefore the behavioral difference cannot be attributed solely to proposal removal; changing both prompts and sampled outputs is confounded.

# Behavioral evidence

Departure invokes actual Vehicle estimate(12), receives 29, computes arrival=start−unloading, departure=arrival−travel, inspection=departure−inspection buffer, and passes original/edit checks. Food calls actual menu/delivery, computes initial quantities and includes selected delivery meetsRequirement in feasibility; both original edits/totals and late-window assessment pass. Equipment uses typed millimetres and exact canonical connector equality, invokes equipment for 14:15Z–20:30Z and quotes package:1 versus stand:1,cable10:1. Actual results are 1200/1300 cents with 5000 deposit per basket. Budget supplied signed/equal/edit checks pass.

Remaining generated limitations are not repaired: food still substring-matches size containing 30, and equipment's selected-component/package strategy is not held-out alternative-search proof. Reordered/unavailable variants and all error/manual cases remain separate work.

# Usage and retained evidence

Source provider tokens; reasoning is a completion subset and cached input an input subset:

| Scenario | Input | Completion | Reasoning | Cached input |
|---|---:|---:|---:|---:|
| Budget | 15138 | 2137 | 1375 | 2048 |
| Departure | 15167 | 10461 | 8456 | 14336 |
| Food | 80505 | 32599 | 26842 | 69632 |
| Equipment | 15200 | 26893 | 23083 | 14336 |
| Total | 126010 | 72090 | 59756 | 100352 |

Against the previous cohort: input −50.8%, completion −4.2%, reasoning −2.5%. Reduced repeated context, not eliminated initial reasoning, is the largest token change. Unknown discovery usage is not included. Provider times/counters are receipts, not isolated compute or billing.

462 unified events; every referenced artifact retained; four complete latest request summaries. Source captures verify the same model, no interactionProposal, initial-only batch permission and no retained hidden reasoning. All 554 fingerprinted historical files remain unchanged (excluding the prior gateway's formerly open log/count files). Private archives/captures, source pairs, trees/screenshots, test logs, hashes and receipts remain under build/bottleneck-optimizations-four. No credentials or hidden reasoning are included.

# Next decisions

Keep proposal isolation, precise feedback and paired writes. Do not declare reserved-name generation solved. Prioritize remaining typed-read/name errors and broader untimed behavioral variants; measure any further initial-synthesis change with repeated controlled trials. Compiler caching, prompt/example reduction and operation-aware telemetry were not implemented in this cohort and must not be credited for its results.
