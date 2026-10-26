---
kind: research
id: compact-generation-four
created_at: 2026-10-26T17:40:00Z
updated_at: 2026-10-26T17:40:00Z
status: measured-regression
references:
  - dependencies/deal-embedding/android/src/compiler/CompactCandidate.kt
  - dependencies/deal-embedding/core/source-agent.deal
  - tools/native_source_tools.py
  - android/tests/CompactInstrumentation.kt
  - tools/collect-generation-cohort.py
question: Does compact deterministic initial-source composition improve the four-scenario first-frame and task outcomes?
mode: local
provider: local-artifacts
sources:
  - build/compact-four/cohort/numbers.json
  - build/compact-four-repeat/cohort/numbers.json
  - build/compact-four-final/cohort/numbers.json
  - build/compact-four-final/raw-evidence-integrity.json
  - build/compact-four-repeat/test-Compact.log
  - build/compact-four-repeat/test-GenerationGateway.log
  - .deus/research/v14-first-frame-four.md
  - .deus/research/tight-guidance-four.md
gaps:
  - Final installed revision did not complete a fresh four-scenario cohort.
  - Third cohort lost early detailed captures and summaries to log retention; unknown usage is not zero.
  - Semantic checks cover supplied assertions, not arbitrary task correctness or visual approval.
  - No controlled ablation or population-level speed conclusion.
---

# Conclusion

**Implemented and independently committed the compact-generation infrastructure; the requested performance and four-scenario reliability outcome was not achieved.** The sole complete cohort took **232.603 s**, versus v1.4's **69.522 s** (+234.6%), and passed 3/4 supplied semantic checks. It exposed an assembler numeric-emission defect, fixed before two subsequent fresh cohorts. Neither subsequent cohort completed all four scenarios. Do not present the earlier complete run as final-revision acceptance.

No semantic feedback, candidateTest, automatic runner retries or retiming was used inside any cohort. Two separately archived additional cohorts were run explicitly during this implementation: the first after a concrete assembler fix, the second seeking a complete measurement after a discovery failure. These are additional live attempts, not a retry-free single-trial success claim. All failures remain evidence.

# Delivered implementation

Embedding commit **8be646b** adds closed compact-plan validation and deterministic initial canonical `.deal`/`.dealui` emission. Native submit_compact replaces full-source emission; there is no added LLM selector/verifier. Typed records/state, explicit initializers, dependency sorting/cycle rejection, numeric controls, fresh outer-state copying, recomputation, custom actions/helpers/views, async request/completion/loading/error plumbing are emitted generically. Capability imports must be disclosed; providers still own records, prices, eligibility, stock and quotes. The generic assembler contains no scenario fixtures or domain solvers.

Core alone stages the complete pair. Malformed composition leaves it absent. Rejected compilation returns the exact materialized bytes; later read/edit/check operations use the existing revision-bound unique/disjoint atomic edit machinery. No plan regeneration follows materialization. Explicit source_fallback(reason) opens initial paired full-source writes and is observed; prior work stays in elapsed time. Catalogue policy remains app-owned and may select maintained experiences in other flows. Actual gateway counters show **zero choice calls** in all three cohorts; no claim that all production paths are selector-free.

Replaced full-source-first instructions and removed full-program examples from the initial prompt; retained compiler-checked example assets and explicit fallback fixtures, not an obsolete alternate protocol. Historical source, rejected candidates and domain data were not deleted. Custom DEAL remains compiler/borrow checked; a fresh outer State does **not** make nested borrowed tables mutable. Collection computations still require explicit fresh rows/arrays.

The first run found exponent-form numeric bounds rejected by Deal UI. Final emission uses plain decimal UI literals and tests a large bound. Final admission also rejects full view/import declarations inside inner view content and framework-owned declarations inside custom helpers. These fixes changed the build between cohorts 1 and 2, not during a measurement. Cohorts 2 and 3 use identical APK hashes.

# Procedure and verification

Each cohort archived all six apps before purging generated state only. Clean-state receipts establish zero generated keys/files; retained domain/authority/disclosure preference values were unchanged after each cohort. Historical archive/log fingerprints: **272 checked, zero raw evidence changes**. Five derived JSON receipts from cohort 1 were regenerated with the new collector; the original comparison records those changes explicitly. Raw archives and logs were not rewritten.

Source/build inputs were frozen during every cohort; no overlapping build. Discovery remained DeepSeek Flash; generation remained Cerebras qwen-3.8-27b, low reasoning, provider completion maximum, automatic retries disabled. Credentials stayed in the loopback gateway. Fresh generated state does not establish cold process/compiler/provider caches; cached tokens are reported below.

Final build/install succeeded. Real-device Compact and GenerationGateway tests passed on final bytes. Compact checks deterministic emission, malformed shapes/imports/references/cycles/reserved names, atomic plan rejection, actual compilation, materialized exact-edit equality, no initial provider reads, explicit read success/failure/retry and local recomputation without rereading. Earlier settled build also passed Discovery and Toolkit device tests. Packaged agent/repair/protocol/guidance tests, Python gateway/transport/metrics, boundary and exact API checks passed. New archive collector has a synthetic test for latest-summary selection, missing captures, unknown counters and unrendered results. These tests do not establish four-task semantic acceptance.

Times stop at the first fault-free generated frame committed to the swap chain, **not physical scanout**. Discovery-only excludes setup; total includes setup, discovery and all generation/repair work. Semantics follow the immutable timing stop. No visual approval was established.

# Cohort 1: complete, before final assembler fix

Seconds; source calls include reads and edit/check turns, not only edits.

| Scenario | Discovery only | Generation→frame | Total→frame | Checking | Model | Calls | Supplied semantics |
|---|---:|---:|---:|---:|---:|---:|---|
| Budget | 1.810 | 10.694 | 17.421 | 1.900 | 7.627 | 4 | Pass |
| Departure | 17.175 | 10.818 | 29.192 | 2.957 | 7.044 | 1 | Pass |
| Food | 2.629 | 129.058 | 132.704 | 7.315 | 112.074 | 34 | Fail |
| Equipment | 2.414 | 49.737 | 53.286 | 3.726 | 42.822 | 15 | Pass |
| Sum | 24.028 | 200.307 | **232.603** | **15.898** | **169.567** | **54** | **3/4** |

Four compact plans assembled, zero explicit full-source fallbacks, 50 post-source repair calls. Summary-admitted literal edits: budget 2, departure 0, food 80, equipment 11. The retained native responses show 23 read calls, 18 edit batches and one compact submission; six response captures were evicted. Those retained-operation counts are lower bounds, not the full 54-call inventory. Latest request summaries preserve complete source usage and aggregate timing even where captures are missing (48 referenced capture artifacts missing).

Budget needed repair for root/import material inside view content and source syntax, including the assembler's exponent-bound emission. Equipment needed view/comment/string/parser and typed-table repairs. Food repeatedly encountered immutable borrowed-state/helper diagnostics and reread source. Its final screen exposed separate menu and delivery buttons and an empty allocation; the supplied single initial read did not establish six pizzas/at least two vegan. This is a failure of the observed generated workflow, not proof that no later manual interaction could produce a basket. No semantic repair or rerun of that result was used to improve its figure.

## Source usage

Cached input is a subset of input; reasoning is a subset of completion. Discovery usage remains unknown.

| Scenario | Input | Completion | Reasoning | Cached input |
|---|---:|---:|---:|---:|
| Budget | 71,661 | 4,725 | 3,310 | 17,408 |
| Departure | 16,352 | 10,184 | 8,990 | 15,360 |
| Food | 1,958,535 | 95,386 | 53,908 | 1,787,904 |
| Equipment | 475,396 | 38,495 | 32,214 | 403,456 |
| Total | **2,521,944** | **148,790** | **98,422** | **2,224,128** |

Initial budget input was 16,323 tokens, versus 16,395 in the prior shortened guidance and 18,168 in the initial v1.4 cohort. This version did not substantially reduce input relative to the immediately preceding guidance; custom code and recovery dominated output/work. There is no demonstrated latency win.

# Cohort 2: final build, equipment discovery failure

| Scenario | Discovery only | Generation→frame | Total→frame | Checking | Model | Calls | Supplied semantics |
|---|---:|---:|---:|---:|---:|---:|---|
| Budget | 1.783 | 6.801 | 13.567 | 2.094 | 3.890 | 2 | Pass |
| Departure | 2.325 | 10.412 | 13.908 | 3.141 | 6.524 | 1 | Pass |
| Food | 2.052 | 34.322 | 37.634 | 4.731 | 27.120 | 8 | Fail: empty initial allocation |
| Equipment | unknown | no frame | no frame | n/a | no source call | 0 | Not assessed: invalid discovery output |

Three plans assembled, two earlier compact submissions rejected before staging, zero full-source fallback. All 11 native response captures retained: five compact submissions, four edit batches, two reads; five literal source edits admitted. Three rendered tasks total **65.109 s**; **this is not a four-scenario aggregate**. For the same first three tasks the v1.4 baseline totals 50.658 s (+28.5% observed here).

| Scenario | Input | Completion | Reasoning | Cached input |
|---|---:|---:|---:|---:|
| Budget | 33,298 | 3,508 | 2,348 | 15,360 |
| Departure | 16,352 | 9,668 | 8,426 | 15,360 |
| Food | 220,580 | 28,452 | 20,400 | 180,224 |
| Source total | **270,230** | **41,628** | **31,174** | **210,944** |

Gateway saw four discovery requests, 11 source calls, zero choice calls. Invalid equipment discovery occurred before a source request; it must not be assigned zero end-to-end latency or counted as a semantic pass.

# Cohort 3: same final build, operational failure

Budget: discovery 1.731 s, generation→frame 6.652 s, total 12.645 s, two source attempts, supplied pass. Departure: discovery 2.499 s, generation→frame 21.246 s, total 24.969 s, six source attempts, supplied fail: correct departure 15:16 but inspection 16:10 instead of 15:06.

Food began after discovery 2.165 s, but did not render. Recovery ended with `EvaluationResultSizeLimitExceededException`: bridge result length must be at most 524,288 bytes. This is an actual operational failure, not a source diagnostic or semantic result. Equipment was not reached. No complete aggregate exists. The source counters record 48 requests (budget 2, departure 6, food 40), three discovery requests, zero choice calls.

Food's surviving final summary records **145.574 s model**, **0.548 s checking**, 2 initial-phase calls and 38 repair calls; known source usage **2,362,901 input / 140,690 completion / 94,546 reasoning / 2,165,760 cached**. This is a **lower bound for cohort 3 usage**, not its total: budget/departure detailed usage and summaries were evicted before the final archive. No frames or time-to-success are invented for food/equipment. Capture polling began too late in this cohort to rescue early artifacts. The retained failed source responses do not establish a complete compact/fallback inventory; counts are unknown where evicted. The bridge's resource limitation was not silently raised, and there is no claim that unlimited logical recovery bypasses host resource limits.

# Comparison

Complete cohort 1 versus previous complete cohorts, total→frame seconds:

| Scenario | v1.4 | Short guidance 1 | Short guidance 2 | Compact 1 |
|---|---:|---:|---:|---:|
| Budget | 11.443 | 12.267 | 10.995 | 17.421 |
| Departure | 13.382 | 15.066 | 18.019 | 29.192 |
| Food | 25.833 | 35.343 | 27.684 | 132.704 |
| Equipment | 18.864 | 25.319 | 26.466 | 53.286 |
| Total | **69.522** | **87.995** | **83.164** | **232.603** |
| Supplied passes | 4/4 | 2/4 | 2/4 | 3/4 |

Compact 1 is +234.6% versus v1.4, +164.3% versus short 1 and +179.7% versus short 2. Initial v1.4's separate late-delivery check failed despite 4/4 supplied passes; this is not universal correctness comparison. Compact final-build cohorts are incomplete and cannot replace this table with a favorable aggregate.

The structural seam works and exact source remains authoritative. It did **not** remove collection ownership/type mistakes or natural-language interpretation errors; custom helpers/views still generate significant code, and error histories can grow enough to hit the existing bridge limit. Further work would need real reduction of custom-code/recovery burden and safe transport/history handling, not a claim that compact JSON itself solved synthesis latency. No successful final four-scenario acceptance, compiler optimization, provider switch, app-specific solver patch or visual approval is asserted.
