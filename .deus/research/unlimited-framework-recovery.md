---
kind: research
id: unlimited-framework-recovery
created_at: 2026-10-22T15:20:00Z
updated_at: 2026-10-22T15:20:00Z
status: partial
references:
  - android/tests/FrameworkE2eInstrumentation.kt
  - dependencies/deal-embedding/core/generation.deal
question: Does framework-owned recovery execute and test the four generated scenarios without the previous attempt stops?
mode: local
provider: local inspection and device execution
sources:
  - build/unlimited-e2e/strict-cohort/results.json
  - build/unlimited-e2e/strict-cohort/distributions.json
  - build/unlimited-e2e/strict-cohort/final.tar
  - build/unlimited-e2e/compiler-all.log
  - build/unlimited-e2e/historical-integrity.json
gaps:
  - Global removal of all execution/resource limits is incomplete.
  - Visual acceptance remains unreviewed.
  - The historical Calendar cold-entry failure has no established root cause.
  - Passing supplied assertions is not exhaustive semantic verification.
---

# Result

Two independent fresh four-case framework cohorts passed their supplied runtime assertions, accepted mounting/publication and saved-workspace reconstruction. The later `strict-cohort` strengthened budget equal/under checks, initial pizza allocation checking and rental budget editing. No builds or native suites overlapped either measured cohort. Archive/purge receipts preserve domain data, grants, disclosure and capture choices; no application-data clear was used.

| Strict cohort | Discovery ms | Source calls | Runtime callbacks | Checked/tested ms | Mounted/published ms | Through saved reopen ms |
|---|---:|---:|---:|---:|---:|---:|
| Budget | 5479 | 1 | 2 | 17757 | 18167 | 22330 |
| Departure | 2748 | 4 | 3 | 29969 | 30362 | 35784 |
| Food | 3350 | 3 | 3 | 50169 | 50493 | 56646 |
| Equipment | 3531 | 5 | 4 | 100561 | 100930 | 107206 |

All table offsets share the per-scenario monotonic start before protocol construction/discovery (`FrameworkE2eInstrumentation.kt:114`). Thus checked and total times include discovery and all rounds; do not add discovery again. They include deliberately stable-snapshot waits, not just model latency. The four scenarios total 221966 ms, not total project effort.

The previous `verified-cohort` totals were 20848/34134/50257/55664 ms with 1/3/3/3 calls respectively; its weaker assertions are retained separately, not replaced by the later result.

# What actually ran

The trusted live harness supplies ordinary facts to discovery, admits its issued suggestion, generates fresh source, compiles it and executes candidate controls through issued action slots against real capabilities. Expectations are absent from the initial request. Only an observed failed runtime assertion becomes `BEHAVIOR_TEST` feedback for revision-bound exact-patch recovery. Speculative execution uses separate telemetry attachment; it does not mark accepted-request mounting/publication.

Budget exercised 180/197.50, edits to 181/198.50, equality and 200/198.50 under-budget arithmetic. Departure read Vehicle estimate(12) → 29 minutes and checked 15:16/15:06, then an edited start yielding 15:41/15:31. Food read actual menu/delivery, rejected the three-pizza/one-vegan default, then passed the initial-allocation assertion and keyed-row edits to 7100 and 8100 cents. Accepted-source inspection confirms six allocated pizzas including at least two vegan; the generic initial assertion itself is not a full independent allocation oracle. Equipment read actual catalogue/quotes, checked package1200/separate1300/deposit5000 and changed budget2000→1100→2000, rejecting missing over-budget feedback before recovery.

Each accepted candidate was also restored and tested from its settled checkpoint. Subsequently the accepted experience mounted, published, closed and reconstructed through a new protocol. Those final mount checks do not repeat every behavioral assertion. Explicit test-host read grants are intentional and separate from ordinary app permissions; no generated source gains authority.

# Attempts and telemetry

Strict cohort: **13 source calls = eight initial/admission calls + five exact-patch calls**, not twelve repairs or five initial calls. Four malformed initial envelopes caused fresh initial admission. All five patches were admitted; three immediately passed rechecking and two required another behavior repair. Feedback includes a missing explicit departure load action, wrong food allocation, wrong rental alternatives and missing rental over-budget feedback.

Known source usage: **208558 input, 87131 completion, 59693 reasoning, 156672 cached-input tokens**. Reasoning is included in completion, not added again. Repair duration n=5: median22063 ms, nearest-rank p9025424 ms, range5907–25424 ms. All four latest summaries and all five repair records were present, with telemetryComplete=true and no missing fields. There were zero malformed/oversized metric events, and the captured log directory contained no health-failure file. These observations do not guarantee a lossless provider billing ledger.

The standard cache-hit denominator is nine calls, because it aggregates one initial summary sample per request plus five repair records; it must not be reported as thirteen per-call cache observations. Use retained provider receipts for per-call questions. `all-runs-known-usage.json` inventories captured completed source receipts across interrupted and successful archives (119 observed completions; known subtotals2061511 input/1031400 completion/905677 reasoning/1528832 cached). It is incomplete project-effort accounting, not invoice cost. Interrupted runs, deterministic host/compiler failures and the earlier successes remain archived. Historical evidence integrity: **932 files checked, zero mismatches**.

# Changes and validation

Compiled core now owns indefinite generation/recovery until acceptance or cancellation. Malformed initial pairs stay invalid but recover through initial admission; admitted pairs retain exact, digest-bound, unique/disjoint patches and complete rechecking. Removed the 33-call and repeated-response stops, patch edit-count/size ceilings, source and draft byte stops, request/context facade byte/length stops and the persisted three-attempt clamp. Source token defaults are -1 (omit client ceiling, provider maximum); gateway connect/read and protocol waits default unlimited. Recoverable transport failures return to core with cancellation-aware backoff; permission/compiler/host failures do not become source edits.

A real equipment failure exposed a shared compiler short-circuit result-ownership bug. The logical branch now owns its shared result in both lowerer producer selection and operand completion, preventing selected RHS reads from being hoisted. A fresh JS regression checks first-use and changing-state assignments. The complete DEAL gate passed, including JS backend614/0, shared cross-backend2798/0, Node conformance502/0 and production JVM/Lua/oracle suites (`compiler-all.exit=0`).

Final build7 succeeded and its APKs were installed before the strict cohort. Seven native suites passed: GenerationGateway, Hardening, Toolkit, Guidance, Inspection, Discovery and Protocol. The Protocol fixture deliberately waits121seconds to test removal of the old120second consumer stop. API mirror/boundary/guidance/toolkit and Python gateway/transport/metrics checks passed; Deal UI's complete suite also passed (framework139, runtime429, workspace tests; `ui-final.exit=0`).

# Limits and acceptance still open

**Do not claim globally unlimited execution.** Remaining restrictions include discovery facts1..16/text2048/context8192/catalogue32768/reply8192/brief4096/reference16, Nano and Flash discovery4096output tokens, generic gateway8192output tokens, Jev60second timeout, sandbox evaluation-return sizes and schema depth/field/maximum ceilings. Some are explicit typed provider/domain contracts and must remain validated; others are arbitrary resource policies needing a separate configuration/removal change. Unbounded synchronous sandbox evaluation also needs a dedicated nonterminating-code cancellation test; the existing noncooperative-model cancellation fixture is not equivalent. This work has not established that distinction exhaustively. Observation retention, record-size limits and capped locality computation explicitly report incompleteness and do not license executing invalid source. Provider context/output limits and physical memory remain finite even with client ceiling -1. Unlimited recovery can consume unbounded time and money.

A separate ordinary Calendar run after strict-cohort archival (not overlapping builds/tests) opened Acceptance departure, generated fresh source, read Vehicle, rendered15:16/15:06 and retained that result after force-stop/relaunch through the normal date→event entrance. XML/screens and Calendar storage are retained. The earlier historical “Opening preparation…” failure was not reproduced in this sample, but no root cause or lifecycle fix was established; one successful sample does not close that historical defect. The four measured scenarios ran in Organizer's explicit test host, not four ordinary Activity journeys. Screenshots were captured but not visually reviewed. No visual or exhaustive product acceptance is claimed.
