---
kind: research
id: authority-attributes-four
created_at: 2026-10-26T09:57:18Z
updated_at: 2026-10-26T09:57:18Z
status: reviewed
references:
  - dependencies/deal-embedding/android/src/discovery/Discovery.kt
  - dependencies/deal-embedding/core/source-agent.deal
  - android/apps/rental/src/RentalCapabilities.kt
  - android/apps/rental/src/RentalModel.kt
  - tools/native_source_tools.py
  - tools/generation-gateway.py
question: What happens after separating discovery proposal authority, exposing typed Rental evidence, batching initial writes and adding ranged reads?
mode: local
provider: Cerebras qwen-3.8-27b low reasoning; DeepSeek Flash discovery
sources:
  - build/authority-attributes-four/revisions.json
  - build/authority-attributes-four/cohort/results.json
  - build/authority-attributes-four/cohort/numbers.json
  - build/authority-attributes-four/cohort/native-tool-observations.json
  - build/authority-attributes-four/verification.json
  - build/authority-attributes-four/historical-integrity.json
  - build/authority-attributes-four/cohort/clean-state.json
  - build/authority-attributes-four/discovery-tests-final.log
  - build/authority-attributes-four/rental-tests.log
  - build/authority-attributes-four/gateway-tests.log
gaps:
  - One fresh sample per scenario and four simultaneous changes; no causal or population speedup claim.
  - Advisory proposal separation does not enforce semantic fidelity; departure still follows an incorrect proposed formula.
  - Ranged reads worked but equipment reread the complete file over five calls; no demonstrated context reduction.
  - Supplied behavioral checks are not exhaustive semantic or visual acceptance.
  - Operation-blind repair telemetry still includes read-only calls; report counts actual edit batches separately.
---

# Changes and verification

Embedding a2cdb6d replaces discovery brief-as-intent with a host-authored instruction over frozen supporting facts; the unchanged brief is interactionProposal data. It also preflights paired initial writes while both files are absent, preserves single-write recovery, and provides inclusive numbered line reads with null bounds for full source. Root 6dd1fbe enables provider batching only from core workspace metadata and exposes Rental-owned typed attributes and package component IDs/quantities. Catalogue serialization and stock consumption share authored package composition. Quote descriptions specify whole-basket hire/deposit/availability/expiry; generated DEAL still owns task-specific selection, not embedding or renderer.

Compiled native-agent, exact-edit and guidance checks pass, as do gateway/streaming, metrics, API and package-boundary tests. Installed Discovery, RentalRead, GenerationGateway and assessment-waiting instrumentation pass. Discovery regression inspects real generated request input for both planned and explicit manual-selection tasks, not model fidelity. Rental regression covers attribute serialization independent of display names, 11000/100 mm lengths, unavailable stock, component links, basket deposits, malformed inputs and shared stock. No exhaustive full-suite claim.

The first build was interrupted by an outer command timeout; a completed rebuild is retained. An expanded discovery test initially retained an old assertion about the latest proposal text; correcting that test assertion produced the passing rerun. All builds/tests/install completed before live inference. Implementation commits preceded the cohort.

# Procedure

All six apps were archived and only generated state purged. Receipts verify zero generated files/keys and unchanged remaining domain/authority preferences. No pm clear, build overlap, provider switch or semantic feedback. The same Organizer test host/grants were used, not ordinary consent/navigation journeys. The existing four task facts and evaluator were unchanged.

Timing starts before foreground setup/discovery and stops at the first fault-free generated frame commit (swap-chain submission, not physical scanout). Admission/compiler repair is included; provider reads, semantic actions, screenshots and cleanup follow the frozen timestamp. No candidateTest, semantic repair, regeneration or retiming. Fresh source does not establish cold caches.

# Results

Seconds:

| Scenario | Discovery | Generation to frame | Total to frame | Checking | Model calls | Supplied checks |
|---|---:|---:|---:|---:|---:|---|
| Budget | 1.971 | 5.259 | 11.575 | 1.974 | 1 | Passed |
| Departure | 2.200 | 13.693 | 17.112 | 2.322 | 1 | Failed buffer formula |
| Food | 2.304 | 21.607 | 25.039 | 2.206 | 2 | Passed |
| Equipment | 3.053 | 28.772 | 32.880 | 2.268 | 8 | Passed |

Total 86.606 s versus previous native cohort 90.292 s: 3.686 s / 4.1% lower in this sample. Budget 12.619→11.575; departure 12.632→17.112; food 37.063→25.039; equipment 27.978→32.880. Supplied passes 2/4→3/4. This neither isolates the four changes nor establishes general reliability/speedup.

Twelve native responses, all protocol-valid. Every initial response contains both writes: four provider turns instead of eight initial turns. Eight writes, three edit batches and five ranged reads. Two edit batches applied exactly; one rejected atomically for PATCH_MULTIPLE_MATCHES. Six complete-pair checks; all four latest telemetry summaries complete; no missing captures. Food repaired E1007 (reserved local name); equipment repaired E1007 (reserved field), first hit ambiguous searches, read ranges then supplied unique exact edits. No invalid pair activated.

Equipment's five reads span 1–50, 50–130, 130–220, 220–320, 320–473: the model effectively reread the entire file, with overlap, across five provider turns. Ranged access works but this run demonstrates no context/latency win from it. Existing telemetry calls these reads repairs; actual edit admission is 2/3, not 2/8.

# Behavior and limitations

Food invokes actual menu/delivery and computes six pizzas with at least two vegan before manual edits. Supplied total/edit checks pass. Its generated initial allocation is a task-specific cheapest-item strategy; this is not proof of a general planner.

Equipment reads typed attributes, compares lengthMm, resolves package components and issues exactly two quotes: package:1 and stand:1,cable10:1. Actual returned hire amounts are 1200 and 1300 cents, each with 5000 refundable deposit; supplied budget edit checks pass. Display strings remain presentation, though generated code still uses substring checks on canonical connector labels. Held-out generated behavior was not evaluated.

Departure reads actual Vehicle29min but computes departure=start−travel−inspection, sets inspection equal to departure, and separately computes unloading=start−unload. It displays 15:21/15:21 instead of 15:16/15:06. The retained discovery proposal contains that same incorrect formula; generation copies it despite its new advisory status. Thus separation removes false provenance but does not enforce semantic authority. This is direct evidence against claiming that the handoff change solves requirement fidelity. No post-measurement repair was performed.

Model-operation/checking time: budget 2.558/1.974 s, departure 10.615/2.322, food 18.355/2.206, equipment 24.553/2.268. These are wall/receipt measurements, not isolated CPU or billing.

# Usage and retention

Provider-reported source tokens; reasoning is a subset of completion and cached input a subset of input:

| Scenario | Input | Completion | Reasoning | Cached input |
|---|---:|---:|---:|---:|
| Budget | 15125 | 2487 | 1668 | 0 |
| Departure | 15237 | 17285 | 15178 | 14336 |
| Food | 34502 | 26702 | 22811 | 14336 |
| Equipment | 190996 | 28783 | 21618 | 162816 |
| Source total | 255860 | 75257 | 61275 | 191488 |

Discovery separately: 31101 input, 711 completion, 10752 cached; reasoning unknown. Source usage rises despite fewer provider turns; no token-saving claim.

All 870 fingerprinted prior evidence files remain unchanged. New private captures, exact accepted sources, progress/final archives, trees/screenshots, purge receipts, test/build logs, revisions and APK hashes remain under build/authority-attributes-four. Hidden reasoning and credentials are not retained. Screenshots are capture, not visual review.
