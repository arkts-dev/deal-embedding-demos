---
kind: research
id: repair-feedback-four
created_at: 2026-10-22T17:48:00Z
updated_at: 2026-10-22T17:48:00Z
status: reviewed
references:
  - dependencies/deal-embedding/core/source-repair.deal
  - dependencies/deal-embedding/core/repair-guidance.deal
  - dependencies/deal-embedding/android/src/bindings/GenerationTelemetry.kt
  - tools/test-source-repair.js
  - tools/verify-generation-guidance.js
  - android/tests/FrameworkE2eInstrumentation.kt
question: Do actionable exact-patch failures and decoded file-labelled repair context improve fresh first-render generation, and what semantics do the first results satisfy?
mode: local
provider: compiled-policy tests and live device execution
sources:
  - build/repair-feedback-four/results.json
  - build/repair-feedback-four/numbers.json
  - build/repair-feedback-four/events.json
  - build/repair-feedback-four/distributions.json
  - build/repair-feedback-four/semantic-assessment.json
  - build/repair-feedback-four/final.tar
  - build/repair-feedback-four/replay/results.json
  - build/repair-feedback-four/historical-integrity.json
  - build/first-render-four/numbers.json
gaps:
  - One new sample per scenario; before/after is observational, not controlled causal evidence.
  - First frame commit is swap-chain submission, not physical scanout or visual approval.
  - Fresh generated state does not mean cold compiler or provider caches.
  - Supplied semantic assertions are not exhaustive.
---

# Changes and checks

Embedding commit 0ae6c79 replaces conflated PATCH_AMBIGUOUS with PATCH_NO_MATCH and PATCH_MULTIPLE_MATCHES, reporting zero-based edit index, target file, overlapping match count and other-file count. Rejections remain atomic; no relocation, fuzzy matching, heuristic unescaping or full rewrite was introduced. Digest, shape, unique/disjoint searches and complete compiler admission remain mandatory. Routine metrics keep a stable rejection code plus source-free rejection detail.

The repair base is now displayed once as decoded, file-labelled source with lengths and end markers. Excerpts identify diagnostic filename, patch file and numbered lines, explicitly not replacement authority. Structured source-local guidance covers E1001 backslash/envelope contamination, E3017 read-only length, and E3003 typed table reads. Prompt captures verify the new context and actual rejection details reached live food repairs.

Compiled policy tests cover zero/multiple/overlapping matches, wrong-file counts, Unicode digest, stale base, atomic rejection, exact application, decoded base, diagnostic routing and operational-error propagation. Old retained food/equipment first patches replay as zero-match failures; food matches the other file once, equipment differs from both. Gateway/transport/metrics/API tests passed. Full app/test build and installation completed; native GenerationGateway suite returned code0 before purge. An early policy-test invocation hit ENOENT while the build had not yet produced assets; tests were rerun successfully after asset packaging. No build/native test overlapped the measured cohort.

# Method and preservation

Same first-render harness as the preceding cohort, now using its corrected signed-budget oracle: no candidateTest callback, semantic feedback or semantic repairs. Time starts before foreground Organizer host setup, includes discovery and generation/check recovery, and stops at the first fault-free generated frame commit through real ExperienceSurface/renderer. Screenshots, actual provider actions, edit assertions and closure follow the frozen timestamp. All four use explicit test grants; this is not ordinary navigation/consent timing.

Before inference all six apps were force-stopped and archived. Only generated assistance state was purged, with retained domain/authority/disclosure preferences compared and zero generated state verified. Existing caches were not purged. Backend unchanged: Cerebras qwen-3.8-27b, low reasoning, no client source output ceiling; strict pair/patch schemas; discovery DeepSeek Flash. Same gateway process/configuration as the preceding cohort; counts are cumulative, not cohort-only. Complete per-call receipts, not gateway global counts, establish cohort usage. Previous first-render archive checksum unchanged; 932 historical files verified without mismatches.

# Timings

Seconds. Compilation/checking sums all attempts and is already included in generation/total. Main DEAL/UI stages are nested, not additional.

| Scenario | Discovery only | Start to suggestion | Generation to frame | Total to frame | All checking | Main DEAL | UI stage | Repair calls |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| Budget | 1.702 | 5.553 | 5.338 | 11.108 | 1.879 | 1.810 | 0.023 | 0 |
| Departure | 1.920 | 2.767 | 8.084 | 11.119 | 1.627 | 1.505 | 0.035 | 1 |
| Food | 2.433 | 3.280 | 44.405 | 47.928 | 2.529 | 2.304 | 0.106 | 4 |
| Equipment | 2.881 | 3.689 | 13.603 | 17.597 | 2.015 | 1.953 | 0.027 | 0 |

Nine source calls: four initial plus five exact patches. Three patches applied and two rejected. All four latest summaries telemetryComplete=true; five repair records; no malformed/oversized metric records or captured health-failure file. Every referenced event artifact remains available across progress/final archives; final archive alone held all artifacts inspected for analysis.

# Tokens

Source generation plus all repair calls. Completion includes reasoning; input includes cached input. Do not add subset columns again.

| Scenario | Input | Completion | Reasoning subset | Cached input |
|---|---:|---:|---:|---:|
| Budget | 13505 | 1954 | 1295 | 0 |
| Departure | 29127 | 8636 | 6970 | 24576 |
| Food | 84506 | 61460 | 55666 | 72704 |
| Equipment | 13794 | 19000 | 15790 | 12288 |
| Total | 140932 | 91050 | 79721 | 109568 |

Discovery separately: 27941 input /731 completion, cached27136. Discovery reasoning usage unknown, not zero. No hidden reasoning text retained; receipt spans are not isolated model compute or billing evidence.

# Separate semantics of the first result

- Budget: correct under supplied approved/committed, edit, equality and signed-under-budget checks.
- Departure: incorrect. No provider-read action or actual Vehicle call. Source substitutes local `5 + 2 * distanceKm`; matching the sample arithmetic does not establish domain authority. Later edit assertions were not reached.
- Food: incorrect initial allocation after actual menu/delivery reads: zero pizzas/zero vegan instead of six/at least two. Later edit assertions were not reached.
- Equipment: correct under supplied real catalogue/package-versus-components, deposit, budget-edit and recompute assertions. Three actual capability reads occurred after the timer stopped.

These are supplied functional checks plus source review, not exhaustive or visual acceptance. No semantic failure was fed back into generation and no repaired source replaced a measured first result.

# Remaining bottlenecks and comparison

Food: model calls40.283s versus checking2.529s. Initial source again contained escaped UI inside DEAL. Both rejected repairs now targeted the correct file but searched text absent from either file, despite actionable rejection feedback. Third patch removed contamination; fourth fixed E3003 with explicit typed locals and passed. E3017 did not recur. Departure: E1052 Unicode escape repaired with one exact patch, then compiled; its later semantic failure remained untouched. Equipment compiled first call, model10.939s versus checking2.015s. Budget model2.872s versus checking1.879s, with3.851s host setup before discovery.

Against the preceding first-render sample: budget10.664→11.108s, departure13.383→11.119s, food228.861→47.928s, equipment56.588→17.597s. Total309.496→87.752s; repair calls25→5; source completion404379→91050. These are observed reductions, not a controlled attribution to the fix. Equipment never needed repair, so its reduction cannot demonstrate repair-context efficacy. Food still incurred two failed exact searches; the fix improves diagnosis and context, not guaranteed model compliance. Semantic count stays two supplied passes out of four, with equipment passing and departure failing in this sample.
