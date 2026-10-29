---
kind: research
id: framework-refactored-four
created_at: 2026-10-29T00:00:00Z
updated_at: 2026-10-29T00:00:00Z
status: measured-with-acceptance-limits
references:
  - .deus/research/framework-refactoring-plan.md
  - .deus/research/composed-generation-four.md
  - dependencies/deal-embedding/core/composition/execution.deal
  - dependencies/deal-embedding/ui/fragments/support/workflow.deal
  - android/tests/FrameworkE2eInstrumentation.kt
question: What was committed, preserved and measured after the framework refactoring?
mode: local
provider: source-inspection-and-device-measurement
sources:
  - Final packaged host verification and installed-byte receipts
  - Fresh four-task device cohort and inference-free metric derivation
  - Generated-only purge before/after archives
  - Committed implementation and benchmark assessor
gaps:
  - Ordinary full-task journeys across all six demos remain unverified.
  - Pixel-level visual acceptance and iOS/OHOS composition execution remain open.
---

# Framework refactoring checkpoint

## Code and verification

Embedding commit **6925e71** moves fragment instance composition into authored DEAL, structural workflow/read/quote blocks into `ui/fragments/`, and dependency ordering, workflow ownership/read sets and invalidation into `core/composition/execution.deal`. Parsed DEAL/Deal UI bindings instantiate concrete types, field ports, expressions and statement slots; the final program receives ordinary compiler/borrow checking. Kotlin remains the typed projection/lowering and platform adapter, not the maintained task-workflow author. Historical whole-behavior expansion moved to consuming test fixtures; the production generator no longer admits it. Explicit custom sections remain separate.

Demo/tooling commit **0e5acde** migrates all six apps to the shared bootstrap/assistance integration, splits Organizer contract/service/storage, removes shipping artificial delays and adds executable regressions. Neither repository was pushed. DEAL and Deal UI required no changes.

Final seven-APK build succeeded. Thirteen host suites passed: behavior/formulas, selection probe, boundaries, assistance, source agent, exact source repair, protocol, guidance, API declarations, gateway, transport, metrics and collector. Seven installed APK hashes matched the built bytes. Device Discovery, Compact, Protocol, AssistanceShell, RentalRead and DiscoveryCalendar output establishes their stated deterministic assertions, including cancellation, restoration, large lossless transfer, reset preservation and stale Calendar confirmation rejection. All six apps passed common Settings/Inspect/reset-cancel interaction checks.

Preserved failures include stale-package probes and a device helper's nonexistent instrumentation target. The first shell check expected the local-only suggestion label; the corrected assertion recognizes the configured cloud label. This was an assessor correction, not a product change or benchmark rerun.

## Cleanup and method

Before inference, all six apps were stopped and archived. Generated workspace source/state, saved links/dismissals, generated staging and historical benchmark outputs were removed across current and historical preference namespaces. After-archives verified zero generated keys/files and exact retained preference values. Domain records, consent/grants/disclosure and observation/failure evidence were preserved. No package-data clear occurred. The fresh benchmark subsequently created four new experiences; the device is not empty after measurement.

One fresh four-task cohort used DeepSeek Flash with thinking disabled and Cerebras `qwen-3.8-27b`, low reasoning and provider completion maximum. No builds overlapped inference. No candidate tests, semantic model feedback, runner retries, extra selector/verifier, retiming or selection of a better run occurred. Tracked input hashes were unchanged afterward.

The immutable clock stops at the first runnable generated frame committed to the swap chain, not physical pixel scanout. Later semantic actions are excluded. Assertions operated on newer settled snapshots and real provider replies.

## Results

Seconds; generation-to-frame includes source inference and checking.

| Task | Total to frame | Discovery stage | Generation to frame | Source model, host | Checking | Correctness |
|---|---:|---:|---:|---:|---:|---|
| Budget | 12.490 | 2.550 | 5.245 | 2.015 | 2.023 | Pass: edits, over/equal/under |
| Departure | 10.933 | 2.837 | 6.701 | 2.078 | 3.422 | Pass: actual travel, departure/inspection, start edit |
| Food | 14.001 | 2.438 | 10.420 | 3.768 | 5.529 | Pass: allocation, keyed edits and extra late-window invalidation |
| Equipment | 13.046 | 3.655 | 8.058 | 3.523 | 3.502 | Pass: actual package/separate quotes, deposit and budget edit |
| **Sum** | **50.470** | **11.480** | **30.424** | **11.384** | **14.476** | **4/4 supplied; extra food pass** |

All four used maintained fragments: **4 discovery + 4 generation + 0 chooser calls; zero source repairs, custom sections or fallback**. The cohort covers all eight fragment kinds.

Total is **27.40% lower** than the 69.522s v1.4 supplied-pass cohort and **37.00% lower** than the 80.116s composed settled all-four/late-food pass. It is 11.28% lower than the 56.884s hardened cohort, which failed departure semantics. These are observed cohort comparisons, not controlled causal estimates; model outputs, cache behavior and runtime conditions vary. The architecturally withdrawn 40.373s result is not an accepted baseline.

Source tokens: input **75,738**, completion **12,633**, reasoning **10,379**, cached **34,816**. Discovery: input **37,858**, completion **698**, cached **2,304**; reasoning count unknown. Cached tokens are included in input; reasoning tokens are included in completion.

## Network observations and bottlenecks

| Task | Discovery gateway round trip | Source gateway round trip | Source first meaningful receipt | Source gateway minus provider-reported duration |
|---|---:|---:|---:|---:|
| Budget | 1.909 | 1.970 | 1.260 | 0.515 |
| Departure | 2.442 | 2.029 | 0.636 | 0.523 |
| Food | 2.116 | 3.730 | 1.187 | 0.551 |
| Equipment | 3.222 | 3.471 | 0.593 | 0.492 |

These round trips include network, queuing and inference. The final column includes transport/server/accounting overhead; it is **not isolated network RTT**. Pure network latency and DeepSeek compute time were not separately measured.

Checking is the largest measured generation component: **14.476s**, versus **11.384s** source-model host time. Food checking alone is 5.529s; its DEAL compiler parent stage is 5.363s. Parent compiler phases overlap children and must not be added together. Discovery stages total 11.480s. Startup/catalogue/admission outside discovery and generation totals **8.566s**, including 4.695s on the first budget task; the cohort was not retimed to remove that cost. Remaining generation-to-frame time is **4.564s**, covering composition/orchestration/mount/publication and other unpartitioned work.

## Acceptance limits

This establishes a correct, faster four-task maintained-only cohort and the listed deterministic/device checks. It does **not** establish every ordinary suggestion/Open/edit/reopen/failure journey in all six apps. Common shell checks and benchmark instrumentation are not substitutes for those journeys. Pixel-level visual acceptance remains open, as does iOS/OHOS composition execution. Therefore the full plan's verification status is not relabelled complete.

Private source, original measurements, before/after archives, retained failures and derived receipts remain in ignored evidence. Original assessments were not rewritten.
