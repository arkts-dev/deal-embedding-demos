---
kind: research
id: framework-speed-retrace
created_at: 2026-11-01
updated_at: 2026-11-01
status: measured
references:
  - framework-speed.md
question: "Which framework costs remain, and can they be reduced without compiler changes?"
mode: local
provider: local-tools
sources:
  - dependencies/deal-embedding/ui/fragments/support/builder.deal
  - dependencies/deal-embedding/android/src/capabilities/CapabilityDiscovery.kt
  - dependencies/deal-embedding/android/src/bindings/SandboxProgram.kt
  - build/framework-speed-2/benchmark-2/full-phases.json
gaps:
  - Fresh plans differ; cohort deltas are observations, not isolated causal estimates.
  - Food fails semantic acceptance; screenshots are captured, not independently visually accepted.
  - Identical-plan device replay has repeated timings but no old-binary device control.
  - Two deterministic Discovery runs timed out flushing the logger; a subsequent unchanged run passed.
---

# Framework retrace

## Changes and evidence

Host JFR again sampled repeated dynamic boundary validation. Builder still exposed growing ports, workflow steps, view mappings, provider contracts and authored views at every helper boundary. Its append operation decoded/validated the entire growing array on each append.

`ui/fragments/support/builder.deal` now keeps these stores behind request-local typed accessors, and appends directly to typed arrays captured by the request. Selected records/arrays still undergo ordinary checked extraction. Dependent fragment/recipe modules use those accessors. No validation bypass, cross-request sharing, provider execution or task-answer inference was introduced. Four maintained fixture source pairs remain byte-identical (`build/framework-speed-2/source-equivalence.json`). Isolation tests now cover ports/steps/views/authored storage as well as Model/plan isolation.

`SandboxProgram.evaluateString` returns a small result in one JSON-enveloped bridge call instead of length + chunk + cleanup calls. Large results retain lossless paging and one execution of destructive producers. Compact device tests cover empty, Unicode/control characters, 16,384/16,385-character boundary and >512 KiB output; small transfers assert one crossing. Added bridge counters report calls/elapsed/UTF-8 bytes without retaining content.

`ProtocolController` now measures sandbox connect, isolate creation, saved loading, capability discovery, bundle creation and evaluation, with native-thread CPU alongside elapsed. `EmbeddingLog` timestamps admission instead of asynchronous persistence, so log queue delay no longer shifts operation timestamps. Persisted sequence still denotes admission/persistence ordering, not a new task clock.

First retraced live cohort identified cold capability discovery at **3.161 s**, versus **0.079 s** sandbox connection. `CapabilityDiscovery` previously started and waited for each provider serially. It now posts all trusted independent binds before waiting, preserving original service order for contract admission, per-bind five-second deadlines, schema validation, Binder liveness and cleanup. The second cohort measured cold discovery **0.872 s**. Warm discovery measured 0.119/0.130/0.156 s. This is observed with cold force-stopped providers, not a controlled equal-load estimate.

## Verification and retained failures

Two coherent seven-APK builds, thirteen host suites and seven device suites completed. Installed APK SHA-256 receipts match. Both builds passed all host suites. First device cycle passed all seven suites. In the second cycle, Discovery's semantic work reached its final logger flush, which timed out twice; the third unchanged invocation passed. No timeout was raised, evidence deleted or code changed to force that pass. Other six suites passed first invocation. This remains an observation-throughput reliability gap, not an unconditional clean-suite claim.

Compact now repeats deterministic composition three times per fixture, checks source equality and records CPU/elapsed and subphases. Supplied-plan device CPU closely tracked total (e.g. composed Budget final repeat 320 ms CPU / 324 ms elapsed); portable computation remains material. Device replay is separate from live inference. Host equal-plan expansion final repeats before → after: Budget 30.5 → 19.1 ms, Departure 61.1 → 50.4 ms, Food 120.7 → 50.7 ms, Equipment 135.7 → 60.4 ms. JIT/load and three samples limit those estimates; no compiler internals were inspected or modified.

Before both live cohorts: archive all six apps, classify before mutation, purge generated experiences only, compare retained preferences/files exactly. Both receipts report zero retained generated experiences and preserved domain/authority/history. No package reset or build/inference overlap. Explicit backends unchanged: Cerebras generation / DeepSeek Flash discovery. Provider caches remain warm where reported. Input hashes unchanged during measurement, no missing captured artifacts.

A wrapper path error before the final cohort refused to overwrite an earlier purge archive and aborted before any data mutation/inference. The wrapper was corrected to a new cohort directory; original refusal retained. Before that refusal the wrapper overwrote the earlier cohort's verification/revision/input-hash metadata with second-build metadata; those earlier metadata files are not valid cohort identity receipts and are excluded from claims. Its archives/live outputs were not replaced. Final cohort metadata is independently complete and unchanged.

## Final full loop: seconds

All columns below are **exclusive/additive**. Startup includes activity + registry + protocol + authority. Discovery includes its gateway request and discovery framework overhead. UI LLM is summed gateway elapsed, including all native tool turns. Assembly includes failed and successful composition elapsed. Checking includes Deal UI/DEAL and surrounding compiler admission. Other is remaining generation initialization, model-host minus gateway, bridge/control work, scheduling and frame publication; it is not isolated CPU. Mount is runtime mount, not physical display scanout. Timed stop is first runnable frame committed to swap chain, before semantic actions.

| Phase | Budget | Departure | Food | Equipment | Sum |
|---|---:|---:|---:|---:|---:|
| Startup/authority | 2.404 | 1.024 | 1.141 | 1.273 | 5.842 |
| Discovery, full | 2.172 | 2.491 | 2.802 | 2.818 | 10.283 |
| Discovery→generation handoff | 0.264 | 0.402 | 0.227 | 0.326 | 1.219 |
| UI-generation gateway | 1.619 | 4.914 | 6.787 | 3.353 | 16.673 |
| Composition assembly | 0.509 | 0.884 | 1.107 | 1.169 | 3.669 |
| Checking | 1.501 | 2.080 | 2.264 | 1.905 | 7.750 |
| Mount | 0.108 | 0.170 | 0.195 | 0.132 | 0.605 |
| Other generation→frame | 0.909 | 0.961 | 1.086 | 2.277 | 5.233 |
| **First committed frame** | **9.486** | **12.926** | **15.609** | **13.253** | **51.274** |
| Native generation turns | 1 | 1 | 2 | 2 | 6 |
| Semantics | PASS | PASS | FAIL | PASS | **3/4** |

Discovery gateway alone: 1.550/2.072/2.391/2.346 s (8.359 s sum), included in full discovery, not additive. DEAL compile alone: 1.422/1.955/2.131/1.767 s, included in checking. No compiler changes.

Composition children (included in assembly, not additive to it):

| Stage | Budget | Departure | Food | Equipment |
|---|---:|---:|---:|---:|
| Dependencies | 0.137 | 0.075 | 0.063 | 0.099 |
| Expansion | 0.078 | 0.098 | 0.090 | 0.103 |
| View binding | 0.022 | 0.023 | 0.025 | 0.016 |
| Lowering | 0.255 | 0.673 | 0.856 | 0.803 |

Failed early assembly admission contributes to parent elapsed without necessarily reaching every child phase. Native bridge calls/elapsed: Budget 36/0.386 s, Departure 45/0.448 s, Food 67/0.552 s, Equipment 64/0.505 s. Bridge timing overlaps initialization and other generation work; never add it again.

Cold Budget protocol phases: connection 0.063 s, isolate 0.002 s, saved load <0.001 s, provider discovery 0.872 s, bundle 0.012 s, evaluation 0.210 s. The CPU counter covers the native calling thread only, not sandbox/provider process CPU.

## Outcome and limits

Versus immediately previous reported cohort: non-LLM/non-checking residual **19.861 → 18.493 s (−6.9%)**, composition assembly **4.601 → 3.669 s (−20.3%)**, total **72.885 → 51.274 s**. Most total improvement is provider variability, not these code changes: generation gateway fell **35.944 → 16.673 s**. Final provider queue summed 0.054 s versus 11.993 s previously. Final provider completion 12.432 s; reasoning 14,805 of 18,368 output tokens; cached input 90,112 of 113,092. Fresh plans/native turn counts differ and timestamp semantics improved, so aggregate reductions are not controlled causal claims.

The intermediate cohort before concurrent binding remains retained: total 53.510 s, residual 19.490 s, assembly 3.336 s, semantics 3/4. Its Food plan checked menu counts instead of allocating quantities. Final Food did allocate but failed the edit/recompute assertion requiring 3 Margherita + 3 Garden Vegan + timely 500-cent delivery to total 7,100 cents; it exposed a manual delivery-fee input rather than reliably using loaded delivery cost. No manual source edits or answer injection were made.

Final native first submissions were rejected for Food (expected pure formula with declared references) and Equipment (unknown port item.itemId); second native submissions succeeded. These are **not exact-source repairs**. Zero exact-source repair calls. Faster framing therefore does not establish four-scenario product acceptance.

Evidence: `build/framework-speed-2/` profiles, replay, equivalence, builds, host/device receipts; `benchmark/` and `benchmark-2/` contain complete archives, live events, accepted sources, provider timings and screenshots. `benchmark-2/full-phases.json` is the additive phase ledger. Nothing pushed.
