---
kind: research
id: embedding-protocol-delivery
created_at: 2026-10-12T21:00:00Z
updated_at: 2026-10-12T21:45:00Z
status: reproduced-with-limits
references:
  - .deus/design/organizer-assistance.md
question: Does the Android demonstration implement the agreed embedding and app-linked policy protocol and preserve the ordinary equipment workflow?
mode: local
provider: local-tools
sources:
  - dependencies/deal-embedding/api/embedding.d.deal
  - dependencies/deal-embedding/api/catalogue-policy.d.deal
  - dependencies/deal-embedding/api/README.md
  - dependencies/deal-embedding/core/generation.deal
  - dependencies/deal-embedding/android/src/bindings/PublicProtocolBinding.kt
  - android/apps/organizer/policy/app-policy.deal
  - android/tests/ProtocolInstrumentation.kt
  - tools/verify-demo-reproduction.py
gaps:
  - Independent user acceptance remains separate from assistant verification.
  - Android JavaScript only; other backends and general portability are not established.
  - Compiler execution lacks independent resource isolation.
  - Visual and accessibility certification is not established.
  - AI product value and future inference correctness or reliability are not established.
---

## Implementation evidence

The exact public declaration surface is packaged and linked through compiler-derived nominal ABI metadata. The public native binding covers every embedding export. Actual compiled DEAL consumer calls exercised load/run/poll/close/forget/events and synchronous saved linkage against Android mechanisms, including nominal Deal UI tree/prop conversion. This is stronger than declaration-only or synthetic return fixtures, but not exhaustive public-export device conformance.

Embedding orchestration executes compiled `core/generation.deal`. Organizer implements typed offer/lower in app-linked DEAL; equipment eligibility, question descriptions, domain validation and source synthesis no longer live in the library. Native adapters supply generic validation, checking, JSON/UTF-16 primitives, inference, storage, transport and execution. Generated UI does not receive privileged embedding declarations. Inspect uses protocol source/events/artifact/health operations rather than directly reading storage/preferences.

Protocol instrumentation passed frozen optional contexts, forged handles, preparation versus saving, one live session, close/cold recheck/forget, pre-cancelled single-use requests and actual public linkage. Authored no-network source fixtures exercised absent-policy fallback and repeated-response stopping through real compiled core/checker. Logging instrumentation passed bounded capture, storage-failure noninterference and incomplete-history recovery. Policy regressions passed 11 fixtures/132 decisions and 16 exact source pairs. Boundary, gateway/Jev and build-cache tests passed. Organizer, Calendar and tests were rebuilt and installed; provider builds remained independent.

## Fresh ordinary-tap evidence

The rewrite verification spent **five Jev choices and four full-source calls**, within the authorized six/six ceiling. No workflow exceeded three source attempts. Counts exclude historical audits/probes and authored no-network fixtures.

- Initial comparison: one choice, zero source calls; checked catalogue, explicit Rental read, selection and local review.
- Initial editable: one choice, three source calls; accepted third after candidate diagnostics. Explicit budget edits produced an honest empty result at €4 and a real stand result once raised. Selection/preparation reached €8 Not booked review.
- Post-fix comparison: one choice, zero source calls; accepted catalogue. The verification process timed out after acceptance during Inspect navigation; this is not counted as a complete verifier pass.
- Final editable verifier: one choice, one source call; AI-built source, editable quantity two, actual Rental result and €16 Not booked review, unchanged provider persistence, no observer warnings. Observed choice 417 ms and source 137639 ms, not a latency benchmark.
- Final comparison verifier: one choice, zero source calls; catalogue, actual Rental read and €8 Not booked review, unchanged provider persistence, no observer warnings. Observed choice 485 ms.

The final ordinary-tap receipts are supporting delivery evidence, not independent acceptance. A failed earlier reproduction stopped before inference because refreshed contracts were not reaching the internal runtime; the bootstrap shadowing defect was fixed and a local capability-import regression added. No failed workflow was silently substituted with replay.

## Recovery and retained source

Final installed Organizer cold-opened retained assistance with no stock read or stale review entrance; explicit Load options/selection/preparation again reached native Not booked review. A stopped gateway produced a failed request with controls restored and previous options retained; Keep previous options returned to usable assistance. The gateway error mapping was tightened for empty/disconnected responses. This does not establish a new Retry-to-success inference run.

Native clarity fixtures passed normal and dark/grayscale/enlarged-text modes with settings restored; renderer omitted/true/false defaults and payload tests passed. These are authored presentation tests, not fresh model or full visual/a11y evidence. Earlier interrupted test/build commands were inspected and completed explicitly, not treated as passes.

Delivered services use Jev choice plus Cortex source in demo mode, without audit ceilings or automatic retries. USB forwarding was restored and capture left off. Plan/provider data was retained. Obsolete runtime consumers/instrumentation and unused generated fixtures were purged rather than kept as compatibility surfaces. The frozen assistance design was not amended.
