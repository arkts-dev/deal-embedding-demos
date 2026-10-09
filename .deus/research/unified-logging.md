---
kind: research
id: unified-logging
created_at: 2026-10-09T10:00:00Z
updated_at: 2026-10-09T10:00:00Z
status: implemented-with-limits
references:
  - dependencies/deal-embedding/core/logging.deal
  - dependencies/deal-embedding/android/src/bindings/EmbeddingLog.kt
  - android/apps/organizer/src/DiagnosticsActivity.kt
  - tools/embedding-log.py
question: Can trusted DEAL logging unify embedding-owned activity and expose live drilldown without depending on generated code or changing communication authority?
mode: local
provider: compiled-deal-and-android-device-tests
sources:
  - android/tests/LoggingInstrumentation.kt
  - android/tests/CatalogueInstrumentation.kt
  - tools/test-generation-gateway.py
  - dependencies/deal-embedding/android/src/capabilities/CapabilityRegistry.kt
  - dependencies/deal-embedding/android/src/compiler/ExperienceCompiler.kt
gaps:
  - Provider internals and build-time compiler stdout are not intercepted; only embedding-owned runtime boundaries are instrumented.
  - The console is an append stream, not a full indexed query service or replay engine.
  - Logger failure/overflow reporting is implemented, but physical disk-full, hard process death and queue-saturation fault injection were not exercised.
  - Model provider exchange envelope was tested without network; no fresh real inference was spent.
  - Capture settings survive process restart until explicitly disabled; captured personal data is private but not application-layer encrypted.
  - Per-event artifacts may be evicted independently and do not provide indefinitely complete histories.
---

# Delivered boundary

The logging schema, stage vocabulary, metadata bounds and event/artifact/store limits are defined in core/logging.deal. Its compiled JS is packaged independently and executed in a trusted logger isolate. Generated application source has no logger capability. Android EmbeddingLog supplies asynchronous admission, clock, sequential writer, bounded storage, hashing and isolated execution. All former StageReceipts call sites now use it; GenerationTrace and the old Kotlin receipt implementation were deleted. Legacy files/flag are cleaned rather than maintained as compatibility sinks.

The one JSONL event stream carries schema version, trace/span/parent, sequence/time, operation, candidate/workspace, target, outcome/code, duration, produced/published version, content status, artifact identity and stored-byte digest. Routine events exclude raw content. Explicit capture stores prompt/context/source/response/diagnostic/provider payload artifacts, capped per artifact and in aggregate. Omitted/truncated/evicted content is not presented as complete. Emergency Android logging is restricted to failure of the logger itself; health metadata records incomplete capture. Event loss must not retry compiler/model/provider work.

Separate Deal UI and DEAL compiler invocations are recorded, including structured diagnostics and input/entry source references in capture mode. Generation choice/source calls record input, previous result, diagnostics and responses. The development gateway no longer writes parallel request/response/usage logs: it returns an authentication-free exact provider request/response envelope to the embedding logger. The gateway's actual repair instruction and model settings can therefore be inspected, not merely inferred from the embedding request. Credential loading remains confined to the gateway; authorization headers never enter the envelope.

Discovery/contract exchange, grants, local/remote calls, remote invocation, callback validation outcome, cancellation, stale replies, sandbox effect delivery and native publication enter the same logging contract. Sandbox and native transport share capability request span identity; action span parents identify the originating interaction. Discovery and grants use registry-level traces; they are separate from individual generation traces. Host failures and storage/session lifecycle also use the logger. Internal provider execution is not fabricated.

# Live observation

Organizer has an Execution activity reachable while generation is running. It refreshes activity records and lets the user inspect structured event fields and a captured artifact. It says Live activity — not AI reasoning. Capture content requires explicit confirmation, and Clear removes events/artifacts without deleting workspaces. Incomplete logger health is visible.

The terminal companion supports follow, raw JSON, trace filtering, artifact retrieval and a shell-permission-protected debug capture switch. This provides a live work stream analogous to agent tool activity, not hidden chain-of-thought. Consumers must treat all captured source/prompt/provider data as untrusted text.

# Reproduced failures and fixes

The first logger isolation approach attempted a second connected Android JavaScriptSandbox service in the same process. Standalone logger tests passed, but catalogue tracing failed during compilation. Android disallows separately binding that service twice; logger and application now lease a single process sandbox engine while retaining independent isolates and schedules. Runtime shutdown releases a lease; fatal sandbox death invalidates the engine. Native logger admission never waits for generated application execution.

A truncation regression added an admission-only property to the closed DEAL class envelope. The logger correctly rejected that extra field, leaving capture incomplete. The native mechanism now removes its internal flag before invoking the canonical DEAL function and marks truncated content afterward. Final logging instrumentation passed truncation and bounded artifact checks.

# Verification

Logging instrumentation passed metadata omission, explicit captured artifacts, no raw content in event records, parent spans, truncation and concurrent producers. Catalogue instrumentation passed correlated schema events and source/compiler failure ownership. Workspace shutdown/reopen and Deal UI lifecycle instrumentation passed with the shared process engine. Core source parity/policy, artifact boundaries, build-cache and no-network gateway-envelope tests passed.

Native replay of retained unchanged AI source passed editable Compose controls, real Rental reads, selection, populated native review, live Resume and cold source reopen. Provider persistence remained unchanged. The terminal export showed compiler-ui/compiler-deal, model choice/source, grants/discovery/contract and sandbox/native transport events. Captured real replay artifacts were inspected through the same stream. Execution activity was opened through native controls and its live-activity label verified. No new inference was used.

# Remaining discipline

This is an initial unified implementation, not a proof of exhaustive telemetry under every crash. Add deterministic queue/storage/engine-loss tests before relying on it as a lossless audit log. No application-layer encryption, remote telemetry backend or provider logging protocol was introduced. The event stream observes calls; it does not route transport or authorize capabilities. Keep full capture off outside explicit debugging and clear sensitive history afterward.
