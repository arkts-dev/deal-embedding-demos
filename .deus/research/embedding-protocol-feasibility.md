---
kind: research
id: embedding-protocol-feasibility
created_at: 2026-10-13T10:26:56Z
updated_at: 2026-10-13T10:26:56Z
status: verified-android-js
references:
  - tools/verify-protocol-api.py
  - dependencies/deal-embedding/api/embedding.d.deal
  - dependencies/deal-embedding/api/catalogue-policy.d.deal
question: Does an ordinary compiled DEAL consumer execute the agreed protocol without injected synchronous answers or native consumer bypasses?
mode: local
provider: local compiler and Android instrumentation
sources:
  - dependencies/deal-embedding/tools/protocol-consumer.deal
  - dependencies/deal-embedding/android/assets/embedding/bindings/public-protocol.js
  - dependencies/deal-embedding/android/src/bindings/ProtocolController.kt
  - android/tests/ProtocolInstrumentation.kt
  - android/tests/ProtocolPolicyInstrumentation.kt
  - android/tests/LoggingInstrumentation.kt
  - android/apps/organizer/policy/app-policy.deal
  - android/apps/organizer/src/OrganizerCapabilities.kt
gaps:
  - Finite executable evidence is not exhaustive proof of every possible interleaving or resource failure.
  - Verification uses deterministic inference fixtures; it does not establish fresh remote inference reliability.
  - The ordinary Organizer shell continues using the native facade; the public DEAL consumer uses the reusable production executor.
  - Other execution backends and portability remain outside this evidence.
---

# Result

The authoritative user document's two declaration blocks match shipped API and core mirrors token-for-token, including defaults and sync/async markers. `tools/verify-protocol-api.py` takes the reference document as an explicit argument; comments and formatting are excluded, declarations are not weakened.

A production-compiled DEAL consumer executes every public export through `ProtocolController`. It contains ordinary sequences of synchronous context/request operations followed by awaited generation and execution. There are no individual injected answers, `NATIVE_TURN_REQUIRED`, forced route, or Kotlin lifecycle calls replacing the consumer's sequence.

The architectural correction is local trusted synchronous state. `public-protocol.js` validates and copies schemas/values, issues context/request IDs, validates public references, and maintains returned source/session/saved state. Explicit async operations reach native mechanisms through a nonblocking pump. Generated UI executes in separate isolates without the administration inventory. Kotlin continues to enforce native resource references and capability security independently. App policy and generation orchestration remain compiled DEAL; native controller code does not select an equipment route or synthesize domain source.

# Contract evidence

| Rule | Executable evidence |
| --- | --- |
| Handles | DEAL rejects forged contexts and released/closed resources. Constructed Experience metadata with a valid ID cannot change persisted title/origin/attempts. Instance seed and local registries govern context/request issuance; native resources are independently checked. |
| Context | DEAL tests original-schema mutation and accessor-copy mutation, exact JSON, closed-record rejection, optional absent field, invalid absent-branch schema and UTF-16 bound. |
| Wire types | Local validation admits only the six kinds, validates all schema branches, bounds/depth/cycles, closed records and array metadata. Existing capability validators and schema-directed bindings enforce argument/result shape. |
| Capabilities | Real checked UI action awaits an actual native probe through generated declarations/transport; no probe call occurs during load/mount. Revocation prevents a further provider invocation. Organizer now authors its local contract with WireType/WireField/Parameter and shared function/contract records; native conversion is transport only. Provider authentication/consent mechanisms are retained, not replaced by grants. |
| Lifecycle | Actual checker, UI isolate and private storage: prepare does not save, run saves/starts, repeated run retains one session, poll/dispatch/publication, close retains, cold run rechecks without inference, forget deletes, release invalidates. A new native/bootstrap instance reconstructs saved source and runs it without inference. A physically closed UI isolate produces failure, invalidates session bookkeeping and permits explicit cold run. |
| Policy validation | Actual compiled Organizer offer/lower and core: valid catalogue uses one choice/no source; unsupported and unissued choices use one choice/one source; absent policy uses no choice/one source. Typed numeric choice coercion rejects. Deliberately invalid compiled app-policy source reaches the real checker and stops with no source inference repair. |
| Chooser | Provider failure propagates its code, one choice/no source fallback. Zero confidence does not reject an otherwise issued answer or authorize application calls. Generic validation remains distinct from Organizer domain validation. |
| Observation | Public consumer exercises empty-trace/start paging, continuation, artifact unavailable, capture/health/clear. Native logger tests actual storage failure without replacing values/errors, incomplete recovery, malformed observation and truncation status after event eviction. |
| Failures/cancellation | DEAL catches CANCELLED and INVALID_HANDLE. Error code/message survive trusted generation. Cancellation before and during generation works; a noncooperative inference fixture remains blocked while the DEAL wait rejects, proving waiting/acceptance cancellation is independent of remote termination. |
| Bootstrap | Library builds without app source; app policy builds separately. Compiler-derived nominal records and typed UI trees cross actual native linkage. Executor, inference, compiler, storage, transport and renderer remain host mechanisms. |

# Observed verification

All three Android suites completed with instrumentation code zero: public consumer/lifecycle, policy routes/failures and logging. The public consumer uses fixture inference through real compiled core, checker, mounting and storage—not live remote inference. Policy cases counted calls independently and asserted zero application calls during generation/mount.

Policy parity covered eleven fixtures and 132 answer decisions; core source parity covered sixteen exact source pairs and seven invalid envelopes. Jev, gateway, build-cache and library-boundary regressions passed. Organizer, Calendar, Rental, Todo, Vehicle and tests built independently with the changed library. Demo inference was not invoked or budget spent.

# Scrutiny

Initial attempts failed because instrumentation target assets were still from the old Organizer installation, and because JavaScriptSandbox lacks TextEncoder. Both failures were inspected and corrected; neither was counted as a pass. DEAL fixture compilation also exposed forbidden module globals, reserved `from`, and literal inference requiring typed locals; fixtures were corrected without changing the authoritative API/compiler. An additional terminated-isolate recovery test exposed stale-session recovery and was fixed using typed mechanism failure classification. A provider-only dependency leak in capability authoring conversion was removed before independent provider builds.

The previous demo-only completion claim was invalid. This evidence establishes an executable Android/JavaScript implementation of the unchanged interface and its tested contract paths. It does not claim exhaustive verification, cross-backend support, or fresh model reliability. The native Organizer shell is not itself migrated into a DEAL application.
