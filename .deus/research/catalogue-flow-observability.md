---
kind: research
id: catalogue-flow-observability
created_at: 2026-10-08T16:00:00Z
updated_at: 2026-10-08T16:00:00Z
status: verified-with-limits
references:
  - dependencies/deal-embedding/android/src/bindings/ChoiceCatalogue.kt
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
  - Private generation traces still contain source and answers, not just structured production telemetry.
  - Choice inventory and lowering policy remain Android-owned.
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

# Diagnostic changes

Generation traces now correlate runs with elapsed time and retain accepted/fallback outcomes. Trace append failures are logged rather than silently discarded. Candidate rejection exceptions include checker details, so no-model replay retains the cause. Template checker rejection is terminal in trusted DEAL orchestration; inference transport/lowering exceptions propagate rather than masquerading as model-source repair. Source fallback still receives choice diagnostics even without a previous source response. Organizer publication now includes fault changes even when snapshot version is unchanged.

# Limits and verification scope

The catalogue template uses name/specification terms to identify candidates; it does not establish technical compatibility, reserve stock or enforce arbitrary user constraints. Full portable-policy migration is still outstanding. Generated descriptor arguments are bounded by the published native contract; oversized values remain runtime-rejected rather than silently truncated.

Build-cache tests, current boundary/artifact checks, Python syntax checks and both repository diff checks passed. Organizer/tests built and installed successfully. No DEAL compiler or Deal UI implementation changes were needed. No commit was made in this work loop.
