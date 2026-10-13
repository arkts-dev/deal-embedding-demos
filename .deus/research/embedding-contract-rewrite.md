---
kind: research
id: embedding-contract-rewrite
created_at: 2026-10-13T18:02:00+03:00
updated_at: 2026-10-13T18:02:00+03:00
status: verified-selected-android-js-paths
references:
  - dependencies/deal-embedding/api/embedding.d.deal
  - dependencies/deal-embedding/android/assets/embedding/bindings/public-protocol.js
  - dependencies/deal-embedding/android/src/Embedding.kt
  - dependencies/deal-embedding/android/src/bindings/ProtocolController.kt
  - dependencies/deal-embedding/android/src/bindings/ApplicationProtocol.kt
  - android/tests/ProtocolInstrumentation.kt
  - android/tests/ProtocolPolicyInstrumentation.kt
question: Does the rewritten Android implementation exercise the revised automatic-progress and synchronous-permission contract?
mode: local
provider: local-source-and-device-tests
sources:
  - tools/verify-protocol-api.py
  - tools/verify-boundaries.py
  - android/tests/ProtocolInstrumentation.kt
  - android/tests/ProtocolPolicyInstrumentation.kt
  - android/tests/LoggingInstrumentation.kt
gaps:
  - Exhaustive concurrent remote callback and revocation interleavings are not tested.
  - No fresh live inference or independent ordinary-user acceptance was performed.
  - No cross-platform or production-readiness claim is established.
---

# Rewrite and evidence

The public declarations and core mirrors match the user-supplied authoritative document, including declaration order, defaults and asynchronous markers. Public poll is removed; dispatch returns null; next returns SessionUpdate.

The trusted controller owns synchronous permission epochs. Grant/revoke no longer enqueue native controls. The native registry has no independent grant set: admission queries controller state, and remote pending operations retain the admitted epoch for sending, deadline processing and reply acceptance. Provider authentication, consent and bounded argument/result checks remain. Completed operations are not rolled back.

Embedding starts its own driver and maintains current snapshots and session-scoped cursors. Reading current state and registering waiters share the state lock. Close and shutdown reject pending waits. Terminal isolate failure produces a fault update; explicit run cold-rechecks retained source. Publication records renderer acceptance, not pixel presentation, and does not gate the driver.

Organizer and Calendar use ApplicationProtocol and the production compiled DEAL application consumer. Their application progress timers and direct native dispatch/snapshot paths are removed. Rendering observations run inside embedding. Local Organizer review authority derives from the invoking capability session's issued Experience, not the activity's current screen. PublicProtocolBinding and native grant-all/revoke-all authority are deleted.

## Direct observations

- Organizer, Calendar, Rental, Todo, Vehicle and instrumentation targets built separately.
- Exact declaration parity and executable ownership/artifact boundary checks passed.
- Protocol instrumentation passed on Android: copied context, forged handles, lifecycle, selected nested defaults, DEAL-visible invalid grant/cursor errors, synchronous grant/revoke followed immediately by native admission, effect completion without next or application pumping, pending next rejection on close/shutdown, cancellation including noncooperative inference, cold reopening and isolate fault recovery.
- Policy instrumentation passed through actual compiled core and Organizer policy. Catalogue selection used one fixture choice and no source inference; unsupported/invalid/absent offers used the documented fallback. Deterministic invalid catalogue source stopped without source repair. Preparation and mount made no provider calls.
- Both assertion-bearing and production application consumers exercised explicit Load, selection and preparation to a €8.00 Not booked review. Exact equipment-period arguments and preparation detail/deadline were checked. Production-consumer cold reopening reset the UI without inference or another provider read.
- Logging instrumentation passed storage-failure noninterference, incomplete recovery, retained truncation and malformed-observation cases.
- Existing policy/core parity, Jev, gateway and build-cache regressions passed. Inference used deterministic fixtures; no new model network calls were spent.

## Failed attempts retained as limits

An initial fault regression reused the dead session because next failure did not reconcile controller ownership; reconciliation and live run were corrected. A default probe rejected an invented ViewNode key field; the default now constructs the actual nominal tree. A workflow fixture used assumed action slots and failed; it now reads compiler-issued slots from the checked tree. A failed workflow left test source in a fixed storage namespace; scenario namespaces are now unique. These failed runs are not counted as passing evidence.

## Scope

This report supersedes the old poll-based evidence for the paths above. It does not establish exhaustive contract conformance, remote-provider reliability, visual/accessibility acceptance, live inference quality or independent ordinary Organizer acceptance. Permission epoch checks are inspected code; the device suite establishes immediate local admission changes, not every remote callback ordering.
