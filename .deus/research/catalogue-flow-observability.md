---
kind: research
id: catalogue-flow-observability
created_at: 2026-10-08T16:00:00Z
updated_at: 2026-10-08T20:25:00Z
status: verified-with-limits
references:
  - dependencies/deal-embedding/core/catalogue-source.deal
  - dependencies/deal-embedding/core/candidate-check.deal
  - dependencies/deal-embedding/core/generation-guidance.deal
  - tools/verify-core-migration.js
  - dependencies/deal-embedding/android/src/bindings/CapabilityBindings.kt
  - dependencies/deal-embedding/android/src/bindings/ExperienceGenerator.kt
  - dependencies/deal-embedding/core/generation.deal
  - android/tests/CatalogueInstrumentation.kt
  - tools/verify-organizer-catalogue.py
question: Does a selected Boom stand requirement produce useful rental options and native review without provider writes, and what failures reveal observability gaps?
mode: local
provider: local-source-and-device-tests
sources:
  - android/tests/CatalogueInstrumentation.kt
  - android/tests/WorkspaceInstrumentation.kt
  - tools/verify-organizer-catalogue.py
  - tools/verify-boundaries.py
  - tools/test-build-cache.py
gaps:
  - Provider confirmation is not connected to Organizer review.
  - Live provider death requires rediscovery; an ordinary retry retains the stale endpoint.
  - Unicode lowercase and JSON-shape conversion remain narrow native data primitives.
  - JSON shape normalization and dynamic context-key access require narrow native mechanisms on the current language/backend.
  - General generation instrumentation changes were compiled but not executed against real inference.
---

# Acceptance evidence

Deterministic Android instrumentation passed all eight supported presentation combinations through trusted orchestration and ordinary source checking. It verified typed period forwarding, matching catalogue rows, prices, unavailable-row disabling, selection publication and exact staging arguments. Provider handlers were not called during generation. Unsupported aliases preserved fallback reasons. Injected transport failures and defective template declarations stopped without inference repair. The existing independent-capability-session test also passed.

One real generation through Organizer's native Boom stand request selected the rental catalogue and passed checking without source fallback or repair. Actual remote Rental reads displayed the boom stand and vocal package at €8.00 and €12.00. Selection and descriptor staging worked. The first end-to-end verifier then failed to find native Review: a long generated title had displaced the controls. After fixing the header, saved-source replay through actual controls passed native review and provider-persistence invariance. No second inference was used.

Native review displayed the selected item, specification, quantity, period, price and deadline. Rental reservation/proposal state remained unchanged. This proves preparation, not provider confirmation or a completed reservation. Review explicitly states that provider confirmation is not connected.

# Failure ledger

| Observed failure | Cause and disposition | Observability lesson |
| --- | --- | --- |
| Test rejected the initial chooser input as a source-repair call | Test searched for a JSON protocol string without accounting for JSON slash escaping; corrected the stage marker | Stage identity should be explicit, not inferred from prompt text |
| CTE E2003: capability module not found | JSON quoting emitted escaped slashes into DEAL imports; replaced with DEAL-specific literal escaping | Correct source and declaration artifacts are needed alongside diagnostics |
| CTE E1010/E1011/E1029 in a generated host declaration | Wire parameter `from` is a DEAL keyword; declarations now use safe positional names | Generated boundary code needs tests independent of model repairs |
| RTE while creating a second runtime in one test | JavaScriptSandbox service was already bound; reused the same test runtime with scoped configuration | Fixture/runtime lifecycle faults are not product-source errors |
| PROVIDER_DENIED | Rental owner consent was off; enabled it through native Access controls | Host grants and provider consent are independent and must remain visible |
| PROVIDER_DIED after forcibly restarting Rental during inspection | Organizer held a stale provider endpoint; cold replay rediscovered it | Retry UI currently cannot recover a dead endpoint by itself |
| Native verifier could not find the requirement | Stale fixture label in verifier; corrected to actual Boom stand control | Inspect actual UI hierarchy instead of assuming labels or coordinates |
| Native Review absent after successful staging | Long generated title consumed horizontal header space; weighted/ellipsized title restored controls | Successful runtime snapshots do not prove native usability |
| Boundary verifier rejected chooser inventory | Stale host/file/module assertions; updated inventory and checked the current Organizer APK against AAR assets | Verification must describe the shipped generation path |
| Inspection ENOENT for RentalStore.kt and capability bindings | Implementation lives in RentalModel.kt and bindings/, respectively; refreshed paths | File-layout assumptions are hypotheses, not evidence |

# Portable policy and stage receipts

The next extraction moved catalogue eligibility, context/parameter checks, issued options and answer validation into `core/choice-policy.deal`. Kotlin eligibility/answer policy was removed; at this stage source synthesis still remained native. Compiled DEAL matched frozen pre-migration decisions across nine fixtures and 108 answer decisions, including dynamic context keys and UTF-16 bounds.

Android instrumentation additionally passed six negative schema/context cases, deterministic source rejection/repair/mounting, exhaustion preserving a healthy workspace, template defects stopping without repair, and transport/cancellation ownership. Routine receipts correlate requests, policy, candidates, mounts, action slots, capability request IDs and completion, produced snapshots and native publication. Capability receipts retain their parent action. Payload-bearing replay traces are explicitly opt-in and separate from the two bounded shape-only receipt files.

One real generation after the extraction passed the native request, rental options, selection and review flow without source repair. Routine receipts showed the choice, lowering, check, mount, remote read and publication sequence. Final downstream validation replayed that accepted source without another inference. Provider persistent state remained unchanged.

Additional failures encountered during extraction:

| Failure | Verified cause and disposition |
| --- | --- |
| CTE: dynamic string table indexing rejected | Contrary to the earlier guide-based assumption, the current checker permits string-indexed table writes but not reads. Disclosed strings cross as neutral key/value entries; eligibility still executes in DEAL. No compiler extension was introduced. |
| CTE: reserved `from` field access | Current parser rejects that keyword after a dot. The same neutral context-entry representation provides the period value. |
| CTE: table assignment needs contextual target | Typed local reads work; direct assignments in these cases did not provide the required context. Explicit typed locals resolved the diagnostics. |
| RTE/decision mismatch: valid purpose rejected | Compiled std/json arrays remain marked Map tables, while this policy's generated loops expect ABI arrays. A narrow trusted JSON-shape conversion returns objects as tables and arrays as arrays; it makes no policy decisions. Parity and Android tests passed afterward. |
| Kotlin test CTE: cross-module smart cast rejected | Public nullable schema element needed an explicit non-null assertion in the fixture construction. |
| Negative fixture stopped before policy validation | Test used a literal backslash-n prompt delimiter. Corrected the delimiter and added fixture labels/causes to assertions. |

# Remaining generation-policy extraction

`core/catalogue-source.deal` now synthesizes the existing template, including presentation, filtering expressions, descriptor wording and source escaping. The Kotlin template was deleted. Unicode search-term lowercase remains a neutral declared host primitive to preserve previous behavior; emitted catalogue-record lowercase remains ASCII, as before. JSON normalization and context entries still work around the separately reported implementation defects.

`core/candidate-check.deal` validates the closed two-string source envelope before native compilation. The native checker now catches only typed candidate rejection; resource and unexpected implementation failures stop without model repair. `core/generation-guidance.deal` owns composition rules, component example formatting and prompt assembly. Java extracts language/renderer asset text and raw pack AST facts only.

Compiled-core verification passed 16 exact pre-migration source-pair comparisons (all eight presentations, ordinary and escaped/Unicode/quantity-bound context), seven invalid-envelope cases, fatal host propagation and exact prompt parity. Android instrumentation passed all eight presentations, an escaped/Unicode/quantity-bound template mount, six envelope repair cases, six negative policy fixtures and native oversized-source failure without repair, alongside existing interaction/isolation checks. Saved-source native replay reached rental options and populated review with unchanged provider persistence. No new real inference was used for this extraction.

| Additional failure | Cause and disposition |
| --- | --- |
| CTE E3002 in generated pack facts | Empty arrays in untyped tables had no element target. Build extraction now emits a typed empty-table-array helper. |
| JS compiler internal NoSuchElementException in emitElseChain | Chained else-if generation failed. Equivalent independent conditions with deliberate precedence avoid it; no upstream edit was made. |
| Test assertion treated DEAL host error as a JavaScript Error | Runtime exposes code/message as a DEAL object; regression now checks those fields. |
| Test invoked synchronous DEAL exports as promises | Harness now wraps the return in Promise.resolve. |
| Policy/boundary regressions described removed native inventory | Updated host lowercase metadata, checker string arguments and artifact ownership checks. |
| Intermittent receipt test could not find mount | Test read only the current bounded file; a run can cross rotation. It now reads current and previous pages, consistent with retention policy. |

# Deal UI lifecycle and wire-data boundary

Embedding's `ui-session.js` now supplies module loading and Promise scheduling only. Deal UI `runtime-js/session.js` owns mounted-root coordination, using generated queue/store `startDrain` and completion admission rather than a duplicate draining/disposed flag. `ui/session.deal` owns observation version/fault, physical effect accounting and replacement readiness through compiler-issued exports. Runtime packaging and consumer fingerprints include the Deal UI JS asset.

The core's scalar-iteration UTF-16 helper was replaced by the declared `policy-data.utf16Length` primitive. Android implements code-unit length mechanically; eligibility remains core policy. The frozen policy oracle passed eleven fixtures and 132 decisions, including supplementary exact/over bounds and combining sequences. Android catalogue instrumentation passed seven negative cases, including the supplementary wire-bound case.

Production-generated JS fixture tests passed overlap/completion order, atomic update rejection, effect failures, reentrant drain, discarded late completion, physical-exit readiness, synchronous scheduler failure, restored state and invalid-slot rejection. Dedicated Android instrumentation passed controlled overlapping completions, replacement blocking, failure snapshots, state restoration and disposal. Catalogue/workspace device regressions and native saved-source review replay also passed; provider persistence remained unchanged. No new inference was used.

JSON/context bridge removal remains conditional and was not performed. Both existing production reproducers still fail: dynamic string table reads are rejected with E3007; decoded JSON-array iteration compiles but executes zero iterations and fails its count assertion. The upstream implementation was left unchanged.

| Failure during this extraction | Disposition |
| --- | --- |
| Kotlin compiler process killed with exit 137 | First build stopped during compilation. A bounded JVM heap build succeeded; the kill cause was not established. |
| JS regression resolved runtime asset one directory too high | Corrected the test repository-root calculation. |
| Fault assertion expected ordinary JS Error text | DEAL error objects and JS errors retain the runtime's existing reification/string behavior. Tests assert state/version invariants and actual fault representation rather than inventing a new one. |

# Diagnostic changes

Generation traces now correlate runs with elapsed time and retain accepted/fallback outcomes. Trace append failures are logged rather than silently discarded. Candidate rejection exceptions include checker details, so no-model replay retains the cause. Template checker rejection is terminal in trusted DEAL orchestration; inference transport/lowering exceptions propagate rather than masquerading as model-source repair. Source fallback still receives choice diagnostics even without a previous source response. Organizer publication now includes fault changes even when snapshot version is unchanged.

# Limits and verification scope

The catalogue template uses name/specification terms to identify candidates; it does not establish technical compatibility, reserve stock or enforce arbitrary user constraints. This extraction covers the existing catalogue synthesis and generation policy, not every possible portable concern across the embedding. Generated descriptor arguments are bounded by the published native contract; oversized values remain runtime-rejected rather than silently truncated.

Build-cache tests, current boundary/artifact checks, compiled policy parity, Python/shell syntax checks and both repository diff checks passed. Organizer/tests built and installed successfully. No DEAL compiler or Deal UI implementation changes were needed. General real-inference source-generation instrumentation was not rerun; this slice exercised slow-path failure/repair deterministically and one real catalogue choice end to end.
