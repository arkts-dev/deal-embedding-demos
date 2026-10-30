---
kind: research
id: framework-speed
created_at: 2026-10-30
updated_at: 2026-10-30
status: measured
references:
  - recipe-ui-four.md
question: "Can embedding composition overhead be reduced without changing generated behavior or compiler internals?"
mode: local
provider: local-tools
sources:
  - dependencies/deal-embedding/core/composition/lowering-model.deal
  - dependencies/deal-embedding/ui/fragments/support/builder.deal
  - dependencies/deal-embedding/android/src/bindings/SandboxProgram.kt
  - android/tests/FrameworkE2eInstrumentation.kt
  - build/framework-speed/benchmark-2/analysis.json
gaps:
  - Fresh plans differ, so cross-cohort timings are not equal-source causal estimates.
  - Cold protocol construction is measured but not internally decomposed.
  - Provider latency and reasoning variability prevent an end-to-end speedup claim.
---

# Framework overhead fix

## Diagnosis and changes

A supplied-plan local profile isolated dependencies, expansion and lowering without inference or compiler execution. Warm Food lowering took approximately 1.8–1.9 s. Runtime samples showed repeated deep boundary validation while growing dynamic node/record graphs and accumulated source were passed as fields of the Model to many small helpers. No compiler source or generated compiler implementation was inspected or modified for this work.

Embedding now owns request-local typed accessor closures for node/record lookup, provider lookup and accumulated declaration/helper source. Builder likewise keeps its growing plan behind request-local append/root/snapshot operations. This changes the data passed to helpers, not language/runtime admission: selected records, arrays, strings and contracts still cross ordinary checked boundaries. There are no unchecked casts, shared mutable composition stores, removed validation, hidden regeneration or app-specific native decisions.

Additional small changes use Object.hasOwn for membership and fixed ordered string replacements for source escaping instead of enumerating keys or parsing carriage return once per character. Four maintained supplied-plan output pairs remain byte-identical to pre-change outputs (`source-equivalence.json`). Local warm Food lowering after the Model changes measured 0.71–0.78 s; host load/JIT vary, so device evidence is primary.

SandboxBundle caches immutable APK binding/module text by AssetManager and asset root. It never caches generated filesystem output, isolates, executed modules, authority or sessions. Every isolate still evaluates fresh factories. This avoids rereading packaged code; its isolated contribution is not established.

Production observation now records generation bundle/init and dependencies/expand/view-binding/lower/whole-assembly elapsed. The benchmark separately records activity startup, registry construction, protocol construction, authority setup and request handoff. A first run exposed missing registration of new observation stage names: durations were retained but normalized to logger. Stage registration was corrected; original receipts remain unchanged.

## Verification and preservation

Both builds passed 13 host checks and seven device suites; all seven installed APK bytes matched. The real maintained tests cover malformed plans/cycles/unknown references, provider sequencing, invalidation, pending/failure, allocation/stock, restoration and editable bindings. Added focused portable tests cover two independent Model/Builder stores, node lookup, record removal and source escaping. Initial test-fixture failures (untyped empty array and invariant unknown[] assertion) were corrected; production semantics were not relaxed. Final focused test passed.

Before each fresh cohort all six app archives and generated-file classification completed before any mutation. Generated workspaces/selections/preparation records were purged; retained domain/authority/preferences/history files compared exactly. No package-data reset, replayed UI or retained generated experience supplied the benchmark. Each cohort made four fresh discovery and four fresh generation calls, no exact-source repair or fallback. Same explicit Cerebras generation / DeepSeek-flash discovery; DEAL remains at 6ae488cb throughout.

Receipts under `build/framework-speed/`: `host-{1,2}/results.json`, `device-{1,2}/results.json`, installed-byte receipts, `source-equivalence.json`, `isolation-check-3.log`, `benchmark{,-2}/` archives/events/source/analysis. Captured evidence collection reports no missing artifacts. Screenshots captured, not visually accepted.

## Fresh cohort results

Seconds, baseline is the prior post-rebase cohort. LLM columns are gateway request elapsed; checking includes Deal UI and DEAL. Rest is total minus both gateway phases and checking, not pure CPU.

| Scenario | Discovery LLM before → after | UI LLM before → after | Checking before → after | Rest before → after | Total before → after |
|---|---:|---:|---:|---:|---:|
| Budget | 1.697 → 1.783 | 1.324 → 2.723 | 1.507 → 1.479 | 7.537 → 7.075 | 12.065 → 13.060 |
| Departure | 2.214 → 2.200 | 2.485 → 3.644 | 1.767 → 1.950 | 4.567 → 3.880 | 11.033 → 11.674 |
| Food | 2.167 → 2.275 | 4.881 → 9.442 | 2.779 → 2.616 | 7.714 → 4.616 | 17.541 → 18.950 |
| Equipment | 2.513 → 2.874 | 4.237 → 20.135 | 1.930 → 1.902 | 7.102 → 4.290 | 15.782 → 29.201 |
| **Sum** | **8.591 → 9.133** | **12.927 → 35.944** | **7.983 → 7.947** | **26.920 → 19.861** | **56.421 → 72.885** |

**Observed non-LLM/non-checking residual improved 7.059 s (26.2%). Total got worse, not better.** Latest semantics: **4/4**, including additional Food checks, versus baseline 2/4. No semantic feedback entered generation. Fresh output differences prevent attributing the pass-rate change causally to the mechanical optimization.

Latest directly measured composition stages:

| Scenario | Dependencies | Expansion | View binding | Lowering | Whole assembly |
|---|---:|---:|---:|---:|---:|
| Budget | 0.137 | 0.155 | 0.021 | 0.270 | 0.599 |
| Departure | 0.061 | 0.319 | 0.018 | 0.553 | 0.965 |
| Food | 0.082 | 0.561 | 0.025 | 0.939 | 1.617 |
| Equipment | 0.104 | 0.490 | 0.016 | 0.802 | 1.420 |
| **Sum** | **0.384** | **1.525** | **0.080** | **2.564** | **4.601** |

Whole assembly includes transport overhead and is a parent of those stages; do not add the parent to its children. Before, last model completion → composition assembled gaps summed to 11.199 s. Latest equivalent gaps are retained in events; direct assembly is a narrower measure, not an identical before/after denominator. The first optimized cohort's equivalent gaps summed to 5.512 s, with four semantic passes.

### What startup actually costs

Latest four-scenario pre-generation-outside-discovery total: **8.829 s**. Its measured components:

- Activity startup: **2.798 s** (benchmark cold/open-screen cost, not hidden LLM work).
- Registry construction: **0.005 s**.
- Protocol construction: **3.793 s**; cold first scenario **3.176 s**, later **0.246/0.206/0.165 s**.
- Authority setup: **1.290 s**; grants/capture/connection setup remain explicit and preserved.
- Handoff: **0.943 s**.

This identifies cold protocol construction as the remaining startup target; it does not prove its internal cause. No cross-session mutable authority reuse or artificial benchmark prewarm was introduced. Repeated generation bundle construction itself now measures only 5–25 ms, initialization 91–179 ms; it is not the remaining multi-second bottleneck.

## Why total worsened

Latest source provider totals: **33.797 s**, including **11.993 s queue**, **1.762 s prompt**, **19.976 s completion**. Equipment alone reported **11.132 s provider queue**. Source gateway minus provider total was 2.147 s across four calls, not isolated network RTT. Reasoning 15,297 of 17,462 completion tokens; cached input 34,816 of 74,422. No prompt/model/effort settings were changed to claim a framework win.

The first optimized cohort is also retained: total **81.186 s**, four semantic passes, rest **21.350 s**. Budget source headers took 10.585 s while provider total was 1.433 s; discovery headers took 10.649 s. Departure discovery headers took 5.573 s. These request-latency anomalies were not silently excluded or retried within that cohort. A second separately archived/purged cohort followed the logging/instrumentation correction, not a cherry-picked replacement.

## Conclusion

The composition fix materially reduces observed framework overhead and preserves deterministic output. It does **not** fix provider queue/reasoning variability or establish faster end-to-end delivery. Remaining measured targets are cold protocol initialization and still-significant composition/bridge overhead; compiler internals remain outside scope. All evidence and failures remain private under ignored build/. No push requested.
