---
kind: research
id: settled-four
created_at: 2026-10-23T10:20:00Z
updated_at: 2026-10-23T10:20:00Z
status: reviewed
references:
  - android/tests/FrameworkE2eInstrumentation.kt
  - android/tests/AGENTS.md
question: Does the corrected asynchronous evaluator avoid premature closure, and what bottlenecks remain after fresh generation of all four scenarios?
mode: local
provider: native instrumentation and retained live artifacts
sources:
  - build/settled-four/waiting-selftest-final.log
  - build/settled-four/results.json
  - build/settled-four/numbers.json
  - build/settled-four/events.json
  - build/settled-four/distributions.json
  - build/settled-four/reassessments.json
  - build/settled-four/semantic-assessment.json
  - build/settled-four/final.tar
  - build/settled-four/reassessed.tar
  - build/settled-four/clean-state.json
  - build/settled-four/historical-integrity.json
gaps:
  - One fresh sample per scenario, not controlled model comparison or population latency.
  - Functional assertions are not exhaustive semantics or visual approval.
  - UI-based pending detection requires generated progress presentation; not a general runtime-idle API.
  - Frame commit is swap-chain submission, not physical display scanout.
---

# Evaluator fixes and verification

The evaluator now records the snapshot version before dispatch, rejects old quiet frames as acknowledgement, requires a newer committed snapshot, then waits through progress/spinner frames and a stability interval before asserting or closing. A fast action may skip the loading frame but still must commit a newer version. A 60-second assessment-only watchdog produces INCONCLUSIVE, never a product failure or generation repair. It does not bound production generation or execution. Deterministic fake-clock tests cover dispatch delayed beyond the old stability interval, delayed provider completion, skipped loading frames and absent acknowledgements. Tests passed through native instrumentation before purge and again after the follow-up evaluator build.

The first post-fix cohort exposed two further assessment issues: departure used valid separate hour/minute controls rather than one minute-of-day field; equipment's combined Load & Compare action could be matched again by the quote search. The evaluator now accepts both clock representations, supports explicit refresh after an edit, and excludes the original action slot when selecting a distinct quote follow-up. Unsupported clock representations are inconclusive. A saved-source-only reassessment mode disables inference entirely and retains separate receipts.

Departure and equipment were reassessed on their exact saved source with the corrected evaluator. No regeneration or new timed result occurred. Source-model calls during reassessment were zero. Original results.json remains immutable; semantic-assessment.json incorporates the separate reassessment. Departure passed; equipment still failed after one completed catalogue read, with no pending/cancelled capability. The initial equipment assessment had two completed catalogue reads due to duplicate action selection; those were outside timing and remain recorded, not silently removed. This cohort contains no capability-cancelled event.

# Setup and method

Only consuming test code changed; embedding, DEAL and Deal UI remained at the preceding revisions. Gateway/transport, compiled patch/guidance checks passed; updated test APK build/install and deterministic native wait selftest completed before purge. Seven APK hashes retained. The installed host/provider APKs were unchanged; only tests.apk needed rebuilding. A second test build/install after the cohort corrected clock/duplicate-action assessment and did not overlap measured generation.

With explicit authorization, all six apps were archived and generated assistance files/source/drafts/links were purged. Preservation checks verified retained domain, authority, disclosure/capture preferences and zero generated state. Historical932files and all three preceding first-render archives remained unchanged. Purge does not mean cold compiler/library/provider caches. Cerebras qwen-3.8-27b low reasoning, unlimited client source output default, strict pair/patch schemas and DeepSeek Flash discovery were unchanged; no provider switching or expected-answer injection.

Unchanged timing: start before foreground Organizer/protocol setup, discover and admit once, include model/admission/check/patch recovery, stop at the first fault-free generated frame commit through real ExperienceSurface/renderer. Semantic reads/edits and screenshots occur after the fixed endpoint; no candidateTest callback or semantic repair. All four ran explicit test host/grants, not ordinary app navigation/consent journeys.

# Fresh first-render timings

Seconds. All checking includes every attempted candidate and is nested inside generation/total. DEAL/UI spans are nested inside checking.

| Scenario | Discovery only | Start to suggestion | Generation to frame | Total to frame | Checking | DEAL stage | UI stage | Repair calls |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| Budget | 1.719 | 6.312 | 4.679 | 11.218 | 1.918 | 1.842 | 0.022 | 0 |
| Departure | 2.075 | 2.984 | 9.157 | 12.389 | 2.403 | 2.326 | 0.033 | 0 |
| Food | 1.996 | 2.955 | 35.596 | 38.717 | 2.894 | 2.620 | 0.086 | 4 |
| Equipment | 2.726 | 3.624 | 73.880 | 77.712 | 2.396 | 2.282 | 0.042 | 5 |

Total140.036seconds. Thirteen source calls: four initial plus nine exact patches; five patches applied and four rejected. All four latest summaries telemetryComplete=true with no missing fields; nine repair records; zero malformed/oversized metric events. Referenced artifacts retained across progress/final archives. Prior cohort total91.505seconds was lower: fixing assessment does not establish faster generation, and this sample required more generation repairs.

# Source tokens

Provider-reported generation plus all repairs. Completion includes reasoning; input includes cached input. Subsets must not be added again.

| Scenario | Input | Completion | Reasoning subset | Cached input |
|---|---:|---:|---:|---:|
| Budget | 13875 | 1816 | 1142 | 0 |
| Departure | 13961 | 9977 | 7955 | 13312 |
| Food | 92164 | 39648 | 32826 | 65536 |
| Equipment | 108784 | 116704 | 108916 | 100352 |
| Total | 228784 | 168145 | 150839 | 179200 |

Discovery separately28845input/658completion, cached28160; reasoning usage unknown. Receipt phases are not isolated model compute or billing. Gateway counts are cumulative, not cohort-only.

# Separate semantic results

- Budget: passed supplied arithmetic, edits, equality and signed-under-budget assertions.
- Departure: actual completed Vehicle read and correct initial15:16/15:06. Original evaluator rejected its separate16-hour/0-minute controls. Same saved experience with a25-minute edit and explicit refresh passed15:41/15:31; this was an oracle representation issue, not missing editable start.
- Food: passed supplied initial allocation and keyed quantity edit assertions. Actual menu/delivery returned; generated size matching accepted returned30cm records, selected initial quantities and later edits produced7100/8100cents. These supplied checks do not prove all feasible-allocation cases or exhaustive vegan constraints.
- Equipment: incorrect. After completed catalogue read, source reports missing matching items and never reaches quotes. Same saved experience retest confirms this after one read without cancellations. Source requires XLR in the item name, but the suitable returned item is named Vocal cable10m, with XLR in specification/included fields. It also requires category package or capitalized Package/kit in the name, while the actual Vocal package has category Microphones. Both predicates can reject valid returned options. Cable-length matching is also a brittle substring list, not robust unit parsing. No renderer/domain workaround was introduced.

# Bottleneck analysis

1. Equipment: model69.537seconds versus checking2.396seconds. Initial reply again embedded escaped view text inside DEAL (E1001). Four exact searches found no match; fifth patch removed contamination and compiled. Repair calls alone57.429seconds; source completion116704tokens, of which108916reasoning. The model repeatedly failed source/patch escaping despite current instructions. Final source still failed semantic catalogue matching, outside timing.
2. Food: model30.960seconds versus checking2.894seconds. Four patches applied but sequential checking exposed UI1012 unsupported escape, UI1009 unsupported indexed view expression, E1052 unsupported Unicode escape, then E1044 unrecognized compiler directive. Multiple syntax defects required separate calls; no compiler correctness defect established. Final supplied semantic checks passed.
3. Departure: model6.045seconds versus checking2.403seconds; no repair. Budget: model2.161seconds versus checking1.918seconds; initial host setup before discovery4.593seconds. Neither demonstrates a recovery bottleneck.

Model calls, especially rejected exact patches and serial syntax repairs, dominate difficult-case latency. Compilation is not the largest observed cost. Provider latency and semantic evaluator work remain outside the first-render endpoint and are not presented as user-path bottlenecks. No model-change benefit can be inferred without comparable repeated samples.
