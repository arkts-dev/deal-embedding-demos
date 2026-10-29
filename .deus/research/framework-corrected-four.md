---
kind: research
id: framework-corrected-four
created_at: 2026-10-29T00:00:00Z
updated_at: 2026-10-29T00:00:00Z
status: measured-with-remaining-acceptance
references:
  - .deus/research/framework-refactoring-plan.md
  - .deus/research/framework-refactored-four.md
  - dependencies/deal-embedding/core/composition/lowering.deal
  - dependencies/deal-embedding/core/composition/view-binding.deal
  - dependencies/deal-embedding/android/src/compiler/BehaviorCandidate.kt
  - dependencies/deal/deal/codegen/jvm/JvmSemanticEmitter.java
question: Does the portable composition migration preserve the four maintained tasks, and what does the corrected implementation cost?
mode: local
provider: local-artifacts-and-device-execution
sources:
  - Packaged host compiler/runtime regressions
  - Installed APK byte comparison and device instrumentation
  - One fresh archived four-scenario cohort
  - Separate native Todo journey and configured screenshot review
gaps:
  - Remaining ordinary app journeys and visual acceptance are incomplete.
  - Non-Android execution is not established by this measurement.
---

# Portable composition: corrected four-task measurement

## Implementation

`core/composition/lowering*.deal` now owns graph validation, expression/collection lowering, provider codecs, state/control initialization, workflow construction and view emission. `view-binding.deal` owns lexical binding and recursive authored-view construction. `BehaviorCandidate.kt` and `FragmentComposition.kt` transport JSON and translate rejection; native compiler hosts transform parsed syntax and export parsed view trees. Legacy formula and selection adapters moved to consuming test fixtures, outside the shipping library.

Independent generated JVM composition programs use separate Java namespaces. The DEAL emitter shares per-body save/restore helpers instead of duplicating a callee's frame inventory at every call site. This resolves encountered JVM method-size failures while retaining reentrant restoration. A monolithic bundle attempt failed the constant-pool limit and was removed; failures remain retained.

## Deterministic verification

The final packaged implementation passes all thirteen host suites: maintained behavior, experimental selection, boundaries, assistance, source agent, exact repair, protocol, guidance, public API, gateway, transport, metrics and collector. The selection suite covers existing explicit-host-test operations; it does not introduce a shipping selection route.

Compiler suites pass: async export invocation **795/0**, production project emission **2443/0**, including assertions for shared frame helpers. Installed APK bytes match all seven build outputs. Device Discovery, Compact, Protocol, AssistanceShell, RentalRead and DiscoveryCalendar checks report their explicit pass results. Six-app Settings/Inspect/reset-cancellation checks pass. Those shell checks are not six complete task journeys or destructive reset approval.

## Fresh measurement

One fresh cohort used unchanged DeepSeek Flash discovery with thinking disabled and Cerebras `qwen-3.8-27b`, low reasoning, provider completion maximum. Generated-only cleanup followed archival; no package-data clear or domain/authority reset. No build overlapped inference. The runner did not retry, supply semantic feedback, invoke candidate testing, add a selector/verifier call, or retime an accepted frame. Semantics ran after each immutable first-frame stop.

| Task | First frame | Generation → frame | Discovery stage | Source-model host wait | Checking |
|---|---:|---:|---:|---:|---:|
| Budget | 12.627s | 5.289s | 2.643s | 1.962s | 1.986s |
| Departure | 11.659s | 7.638s | 2.517s | 3.106s | 3.216s |
| Food | 14.219s | 9.966s | 2.745s | 2.945s | 5.283s |
| Equipment | 19.622s | 9.930s | 8.332s | 3.878s | 4.372s |
| **Total** | **58.127s** | **32.823s** | **16.237s** | **11.891s** | **14.857s** |

All four pass supplied semantic assertions on attempt one; food also passes additional late-delivery assertions. All four routes are maintained-only: **four source calls, four discovery calls, zero chooser calls, zero custom characters, zero fallbacks, zero source repairs**. Captured source-input hashes remained unchanged through measurement.

Source tokens: **75,738 input / 12,976 completion / 10,746 reasoning / 52,224 cached**. Discovery: **37,858 input / 737 completion / 36,992 cached**; reasoning count unknown. Cached tokens are included in input; reasoning tokens are included in completion.

The total is **16.39% below 69.522s** in the accepted v1.4 supplied-pass baseline and **27.45% below 80.116s** in the prior all-four/late-pass cohort. It is **15.17% slower than the immediately preceding 50.470s cohort** and **2.19% slower than the 56.884s cohort that failed departure**. No faster replacement run was selected.

Against the immediately preceding cohort, discovery stages increased by 4.757s, generation-to-frame by 2.399s, and other pre-generation time by 0.501s. Source-model host wait increased by 0.507s and checking by 0.381s; these are components, not additional independent time. Equipment discovery alone took 8.332s. These are observed differences, not controlled causal estimates of the migration.

Gateway totals are 11.720s source and 14.219s discovery; source provider-reported total is 9.532s. The 2.187s source gateway/provider residual is not isolated network RTT. Host wait is not pure model compute; checking includes compiler/staging overhead. Compiler parent/child phase times overlap.

## Separate ordinary and visual evidence

A native Todo journey exercised actual suggestion/Open, menu/delivery loading, four Margherita plus two Garden Vegan, **€69.00 / Ready / 6 of 6**, increment to **€79.00 / Not feasible / 7 of 6**, decrement, reopen with retained €69.00, and Inspect. Initial offline discovery failed visibly before explicit retry after gateway availability; this was outside the measured cohort. Manual probe mistakes in an expected price and scroll position were corrected without source changes. This is one ordinary journey, not all six.

Configured screenshot review of all four measured screens reports hierarchy, redundant labels and editing/pending-state presentation weaknesses. No visual approval is claimed. The first review invocation failed because its reference document was absent; the corrected invocation used the existing library UI reference. Offscreen reachable controls and the external status overlay are not themselves product failures.

## Limits

The portable migration is implemented and the four functional fast paths are measured on corrected bytes. The refactoring plan remains **implementation-in-progress**: remaining ordinary app journeys, visual acceptance and any required non-Android execution evidence are not established. Existing cohorts and failures remain baselines, not replacement evidence. Private artifacts are retained under ignored build output, with a separate evidence archive; no raw logs, device identities or credentials are included here.
