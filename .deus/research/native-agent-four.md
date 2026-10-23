---
kind: research
id: native-agent-four
created_at: 2026-10-23T16:05:00Z
updated_at: 2026-10-23T16:05:00Z
status: reviewed
references:
  - dependencies/deal-embedding/core/source-agent.deal
  - dependencies/deal-embedding/core/source-repair.deal
  - tools/native_source_tools.py
  - tools/gateway_transport.py
  - tools/test-source-agent.js
question: What changes when native restricted source tools replace raw framing in a fresh four-scenario first-render measurement?
mode: local
provider: Cerebras qwen-3.8-27b low reasoning; DeepSeek Flash discovery
sources:
  - build/native-agent-four/revisions.json
  - build/native-agent-four/verification.json
  - build/native-agent-four/cohort/results.json
  - build/native-agent-four/cohort/numbers.json
  - build/native-agent-four/cohort/native-tool-observations.json
  - build/native-agent-four/cohort/clean-state.json
  - build/native-agent-four/cohort/artifact-retention.json
  - build/native-agent-four/historical-integrity.json
  - build/native-agent-four/native-gateway.log
  - build/native-agent-four/native-protocol.log
  - build/native-agent-four/native-hardening.log
gaps:
  - One sample per scenario; no population or causal latency claim.
  - Supplied semantic assertions are not exhaustive semantics or visual review.
  - Existing telemetry counts any post-pair model turn as repair, including read-only calls.
  - Discovery and other previously identified limits remain; no blanket unlimited-compliance claim.
---

# Replacement

The live path now uses provider-native strict write_file, edit_sources, read_file and check_sources calls. Raw framing is removed: source-framing.deal, markers, prompt rules, gateway assumptions and old fixtures are gone, without a fallback. Historical reports/captures remain unchanged. The preceding comparison harness was archived under ignored experiment output before removal because it depended on the retired parser.

Portable source-agent.deal owns staging, native assistant/tool feedback, retries and checking. Gateway supplies provider tool definitions, assembles streamed tool arguments and forwards complete calls; it never executes source operations. write_file creates one missing file and cannot overwrite it. read_file returns only deal/dealui. edit_sources applies a nonempty atomic batch of unique disjoint exact edits; core supplies the current base digest, not the model. No shell, arbitrary filesystem, credentials, new grants or provider reads are exposed. Both files must exist before the ordinary full checker runs. Successful admission returns the existing candidate identity to the existing activation/runtime path. No attempt ceiling, fuzzy matching, implicit file relocation or full-rewrite fallback.

Initial staging therefore normally takes two model calls, not one pair response. Read/check feedback remains native multi-turn conversation. All provider-specific request construction and credentials stay gateway-side; the model/provider and reasoning settings are unchanged.

# Verification before measurement

Packaged compiled agent tests pass separate writes, tool-result transcript feedback, restricted reads, overwrite denial, unchanged base on rejected edits, exact edits, malformed-message recovery and complete checking. Compiled exact-edit, generation-guidance, gateway/streaming transport, metrics/cache, API/boundaries and real compiler/VM toolkit checks pass. Android GenerationGateway, Hardening, Protocol and assessment-waiting selftest pass; Protocol includes cancellation before/during inference, a noncooperative mechanism and the 121-second generation fixture. Stream tests reconstruct fragmented native arguments and preserve receipt offsets while discarding reasoning text.

An early implementation build exposed two DEAL typing errors; a deterministic agent test then exposed E8001 for a dynamic table member in a result literal. Typed locals fixed it before final packaging and any live inference. Logs are retained. The final build/test/install completed before measurement. One toolkit invocation hit the outer command timeout; its separately retained completed rerun passed. No claim of exhaustive suite coverage.

The packaged AAR contains source-agent.js and no source-framing module. Model response captures establish the native channel actually used, not merely configuration. Credentials and hidden reasoning are absent from retained provider artifacts.

# Measurement

All six apps were archived, then only generated files/state were purged. Zero generated files/keys and preservation of remaining domain/authority preferences were verified before inference. No pm clear, provider switch, overlapping build/test or semantic feedback. Explicit Organizer test host/grants, same as prior cohorts; not four ordinary consent/navigation journeys. Fresh source does not imply cold provider/compiler caches.

Clock remains before foreground setup/discovery through the first fault-free generated frame commit: swap-chain submission, not physical scanout. Admission/compile correction is inside timing. Actual capability reads, semantic actions, screenshots and cleanup follow the frozen timestamp. No candidateTest or semantic repair.

Seconds:

| Scenario | Discovery | Generation→frame | Total→frame | Checking | Source calls | Supplied semantics |
|---|---:|---:|---:|---:|---:|---|
| Budget | 2.803 | 5.288 | 12.619 | 1.943 | 2 writes | Passed |
| Departure | 2.179 | 9.299 | 12.632 | 2.284 | 2 writes | Passed |
| Food | 2.767 | 33.273 | 37.063 | 2.681 | 2 writes, 1 read, 4 edits | Failed initial allocation |
| Equipment | 2.945 | 23.862 | 27.978 | 2.867 | 2 writes, 1 edit | Failed matching |

Total **90.292 s**, versus raw-framing **137.001 s** (46.709 s / 34.1% lower in this sample). Food improved 89.470→37.063 s; equipment worsened 22.904→27.978 s; budget 10.950→12.619 s; departure 13.677→12.632 s. This is an observed cohort difference, not demonstrated general speedup.

Fourteen model responses: eight writes, one read, five edit batches. **Zero protocol failures; all five edit batches applied exactly.** Nine complete-pair checks. Food E1007→UI1015→UI2030→UI2024→pass; equipment UI2024→pass. No invalid source activated.

Telemetry currently counts the food read-only post-pair call as a repair attempt with patchOutcome not_completed, so aggregate admission is misleadingly 5/6. Actual edit admission is **5/5**. Four latest request summaries are complete; all referenced captures are retained. Existing attempt counts describe model turns, not edits or full rewrites.

# Semantics and cost

Budget arithmetic/edits and departure actual Vehicle-read/edit assertions passed. Food fetched actual data but initialized every menu quantity to zero and only recomputed those quantities; no initial feasible allocation was implemented. Equipment used brittle case-sensitive substring conjunctions against specification/included fields, e.g. lowercase balanced versus actual Balanced, and stand phrases absent from the specification; it reported missing components after catalogue completion, without reaching quotes. These are generated-program failures, not native protocol failures. Supplied passes decreased from 3/4 to 2/4; this interface improvement is not an overall semantic reliability improvement.

Provider model time versus checking: budget 2.551/1.943 s; departure 6.214/2.284; food 28.741/2.681; equipment 19.804/2.867. Provider timing is receipt/wall time, not isolated compute or billing.

Provider-reported tokens; reasoning is within completion, cached input within input:

| Scenario | Input | Completion | Reasoning subset | Cached input |
|---|---:|---:|---:|---:|
| Budget | 29070 | 1011 | 295 | 13312 |
| Departure | 30104 | 8700 | 6844 | 27648 |
| Food | 138918 | 27077 | 22601 | 101376 |
| Equipment | 52395 | 27310 | 22073 | 41984 |
| Total source | 250487 | 64098 | 51813 | 184320 |

Discovery separately: 28845 input, 656 completion, 28160 cached; reasoning unknown. No unknown usage was converted to zero. Fourteen native responses versus nineteen raw-framing source responses, but initial native staging needs eight calls versus four initial pair calls: compare operations explicitly.

# Retention

All 460 files in the selected prior-cohort integrity manifest remain unchanged. Private source, tool/provider captures, compiler evidence, trees/screenshots, progress/final archives, purge receipts, revisions, APK hashes, tests and implementation interruptions remain under build/native-agent-four. Screenshots are capture, not visual acceptance. Implementation commits: embedding e797aca; root ac5d25a. DEAL and Deal UI are unchanged.
