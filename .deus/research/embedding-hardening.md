---
kind: research
id: embedding-hardening
created_at: 2026-10-13
updated_at: 2026-10-13
status: verified-selected-paths
references:
  - embedding-surface
  - embedding-contract-rewrite
question: "Are the audited Android session-surface cleanup, observation, bounds and error gaps fixed?"
mode: local
provider: local-tools
sources:
  - dependencies/deal-embedding/android/src/rendering/ExperienceSurface.kt
  - dependencies/deal-embedding/android/src/UpdateCursor.kt
  - dependencies/deal-embedding/android/src/bindings/ApplicationProtocol.kt
  - dependencies/deal-embedding/android/src/bindings/ProtocolController.kt
  - dependencies/deal-embedding/android/src/storage/StagingReclamation.kt
  - android/apps/calendar/src/CalendarActivity.kt
  - android/apps/organizer/src/WorkspacePresentation.kt
  - android/tests/HardeningInstrumentation.kt
  - android/tests/SurfaceInstrumentation.kt
  - android/tests/ProtocolPolicyInstrumentation.kt
  - tools/verify-boundaries.py
gaps:
  - Exhaustive Binder permission/callback interleavings are not established.
  - Independent ordinary-user acceptance and fresh live inference were not performed.
  - Permanent filesystem failure cannot be repaired automatically; failed cleanup remains owned and reported.
  - Android/JavaScript only; no general backend portability claim.
---

## Changes and evidence

The routine surface migration remains intact. Calendar and Organizer now guard preparation acceptance through `withPreparation`: cancellation, changed context and failed activation release unused preparation while replacement decisions remain app-owned. Organizer still retains healthy owners. Failed retained owners explicitly close their terminal session and cold-reopen on activation without inference. Renderer callbacks are tied to the originating snapshot/session rather than a newly activated session.

Composition attachment now owns a closable observer, not execution. Detach removes its DEAL transport operation, native next future/waiter and result bookkeeping. Late main-thread posts are fenced by an observation epoch. Accepted actions remain independent of display detachment. Session progress still runs inside embedding.

Session-update cursors use authenticated session-local revisions with a fixed secret rather than an ever-growing issued-cursor map. Earlier valid cursors remain valid for the session; foreign, altered and future revisions fail. No expiration policy or DEAL signature change was introduced.

Capability admission no longer has a 128-request lifetime quota. Concurrent pending calls, buffered reply count and bytes are bounded, and local results are validated/bounded. Request sequence identity, provider authentication, consent, deadlines and permission epochs remain independent checks.

Synchronous release still retires public preparation authority immediately. Native release failures stay in controller reclamation ownership, with paced retry and explicit `flushReclamation` failure. Physical staging deletion checks its result and journals pending directories privately before deletion. Bootstrap/checking/shutdown retry journaled cleanup. Saved source is not staging. Cleanup errors do not replace original generation/compiler failures; shutdown attempts remaining resources despite individual cleanup failure.

Shared framework helpers now own retained-event paging, observed exchange correlation and copy-only display-prop traversal. Organizer owns domain date formatting and wording, not generic traversal or paging. Typed generation reasons and provider/transport codes survive the DEAL/native error boundary; ordinary presentation and Inspect no longer require native generator/gateway exception classes.

## Two scrutiny passes

First source scrutiny found callback fencing, reentrant waiter completion, retryable failed close, cleanup error masking and retained-owner recovery issues. These were corrected and exercised. Final source scrutiny checked dependency boundaries, unchanged declarations, preservation of valid old cursors, observer cancellation bookkeeping, healthy replacement preservation, saved-source retention and typed error presentation.

The fresh hardening instrumentation directly exercised 10,000 authenticated revisions, 309 registry admissions and a compiled DEAL effect making 140 sequential capability calls. Fifty attach/detach cycles retained no native waiter/controller next future and produced no callback after detach. Authored checked source with a forced catalogue revision mismatch failed actual mounting while the existing healthy session remained unchanged. Changed-context rejection removed preparation. Injected deletion failure retained native ownership and the private journal; restoration reclaimed it. Failed session-close deletion remained retryable and retained saved source. Fixtures verified repeated-response, attempt-limit, provider refusal, connection and timeout codes, plus presentation distinctions and shared helper behavior.

Fresh Compose instrumentation exercised accessibility clicks/publication, detached action execution with no surface reception, reattachment, actual isolate termination, cold recovery of the same owner, rejection of a stale callback, explicit close and source retention. Protocol, Organizer policy and logging suites also passed. A second serialized run of all five suites passed with instrumentation code zero. Declaration parity, boundary checks, policy/core fixture regressions and gateway/chooser/build-cache tests passed separately.

## Failed attempts and limits

An initial foreground build timed out. Subsequent builds exposed a star-projected map lookup and then mismatched library/test snapshots caused by edits during builds; final builds were completed before device runs. The first final surface run timed out searching only the first all-trace observation page after the hardening suite had produced many events. The assertion now uses session-scoped framework paging and flushes queued observation; both subsequent surface runs passed. These failed attempts are not counted as passes.

The retained build/device receipts are local generated evidence, not committed live logs: `build/hardening-build6.txt`, `build/hardening-tests7.txt`, and `build/hardening-review2-*.txt`. The executable tests are tracked evidence. All model inputs were deterministic fixtures or authored source; no fresh network inference was spent. Selected tests do not establish exhaustive lifecycle/resource isolation, remote callback races, production readiness or independent user acceptance. Frozen Organizer design and the unrelated PDFs were untouched.
