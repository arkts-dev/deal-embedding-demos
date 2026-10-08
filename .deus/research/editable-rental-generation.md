---
kind: research
id: editable-rental-generation
created_at: 2026-10-08T23:15:00Z
updated_at: 2026-10-08T23:15:00Z
status: verified-with-limits
references:
  - android/tests/EditableRentalInstrumentation.kt
  - android/tests/fixtures/editable-generated
  - android/tests/fixtures/editable-rental
  - dependencies/deal-embedding/core/source-repair.deal
  - dependencies/deal-embedding/core/generation.deal
  - dependencies/deal-embedding/android/src/rendering/DealRenderer.kt
question: Can richer editable Rental inputs and bounded delta repair improve useful LLM generation without changing runtime or authority boundaries?
mode: local
provider: compiled-core-and-android-device-tests
sources:
  - tools/generation-gateway.py
  - tools/verify-core-migration.js
  - tools/verify-organizer-catalogue.py
  - android/tests/EditableRentalInstrumentation.kt
  - android/tests/CatalogueInstrumentation.kt
gaps:
  - Four related focus variants are not independent task families or a reliability benchmark.
  - Full strategy ran before delta; network/cache/order effects prevent causal latency claims.
  - Zero first-pass acceptances; three workflows exhausted the checker budget.
  - Production chooser guidance excludes unsupported editable templates, but real chooser selection was not tested in this experiment.
  - Remote provider death, provider confirmation and production function-level authority enforcement remain separate.
  - Current delta repair resends full base source and duplicates it in conversation context; input-token cost increased.
---

# Useful result

The editable Rental demo is implemented: quantity, total budget and search inputs, field validation, honest loading/empty/error/retry states, selection and populated local native review. Existing IntField/TextField/Button gained backward-compatible enabled/supporting/error/step props; SearchField adds an explicit string-input component. Compose integer arithmetic uses wide intermediates, bounded steps and distinct accessible increase/decrease controls. Application decisions remain in DEAL.

A deterministic authored fixture verified the component/application contract before inference. Five unchanged LLM-authored accepted source pairs were then retained as test-only assets. All five passed replay of initial rows, quantity 2/budget 1800 filtering to the stand at €16.00, preparation descriptor, selection preservation, search adapter filtering, stale-result invalidation, quantity-zero validation, empty budget and grant-denial recovery. No provider calls occur during generation or initialization.

One retained LLM pair was replayed through actual Organizer Compose controls and remote Rental: native quantity-zero validation disabled loading, quantity and budget were edited to 2/1800, Search received adapter, one stand row loaded, selection published and native review displayed the exact quantity/period/total price. Provider persistent state was unchanged. No new inference was used during downstream verification.

Organizer now exposes an explicit Editable rental goal. Production remains choice-first, with issued template guidance stating that editable forms and spending-ceiling enforcement require source fallback. The fixed catalogue template remains unchanged. The accepted pair can be demonstrated with saved-source replay independently of stochastic generation.

# Bounded comparison

Model: configured `cortex`, temperature 0.2, max output 8192 tokens, remote development gateway. Four frozen requests shared a complete editable Rental contract and varied emphasis: quantity-budget, search-selection, validation-empty and denial-recovery. Each accepted result was tested against the complete contract. Full ran first, then delta; this is not an alternating-order benchmark. Calls were persisted before invocation; each attempted workflow refuses automatic repetition.

| Strategy / focus | Source calls | Checker | Final behavioral replay | Original workflow duration |
| --- | ---: | --- | --- | ---: |
| Full / quantity-budget | 2 | Accepted | Passed | 183 s |
| Full / search-selection | 3 | Accepted | Passed | 258 s |
| Full / validation-empty | 3 | Exhausted | No accepted app | 286 s |
| Full / denial-recovery | 3 | Accepted | Passed | 285 s |
| Delta / quantity-budget | 2 | Accepted | Passed | 137 s |
| Delta / search-selection | 2 | Accepted | Passed | 161 s |
| Delta / validation-empty | 3 | Exhausted | No accepted app | 188 s |
| Delta / denial-recovery | 3 | Exhausted | No accepted app | 180 s |

**21 source calls of the approved 24; no ceiling extension. Zero first-pass acceptances.** Full accepted 3/4; delta accepted 2/4. All five accepted pairs ultimately passed behavior; three exhausted workflows remain failures. Failed workflow duration includes checking and any interaction before the original verifier stopped. It is not time-to-working-app evidence for exhausted runs. With only four related requests, p95 would be the maximum observation and is not a useful reliability estimate.

| Aggregate reported provider usage | Full | Delta |
| --- | ---: | ---: |
| Input tokens | 76,867 | 94,508 |
| Output tokens | 52,543 | 26,927 |
| Cached input tokens (subset of input) | 0 | 8,448 |
| Repair output tokens | 29,330 | 2,952 |

Delta sharply reduced repair output, but sent more input and had fewer checker acceptances. Faster observed durations do not prove causality because prompts, generated source, cache and order differed. Keep full as production default; delta is an explicit opt-in experiment. The gateway now records shape-only model/settings/usage/finish-reason telemetry, not prompts or outputs.

# Repair contract

`source-repair.deal` owns textual patch decisions, not a Kotlin editor. It accepts a closed baseDigest/edits envelope with at most eight unique, disjoint exact replacements, per-edit bounds and closed file identities. Match all edits against original source, not earlier replacement output. Reject stale bases, missing or ambiguous matches, overlapping occurrences/edits and malformed patches. Recheck the complete source pair through the ordinary compiler. Identical repeated model responses terminate early; operational errors still stop rather than trigger repair.

SHA-256 is a native mechanism only. Final implementation uses a length-prefixed two-source encoding to avoid delimiter ambiguity. The live comparison used a fixed delimiter encoding; this mechanical identity hardening was verified deterministically afterward without spending inference. A supplemental regression rejects overlapping occurrences such as aa within aaa. The final patch contract was exercised end-to-end on Android using a synthetic unknown-component rejection followed by a digest-bound UI edit.

This is not semantic compiler-scoped repair or a new executable artifact format. Full source remains canonical. Compiler-origin metadata and dependency-scoped repair would require a separately justified compiler integration.

# Failure ledger

| Observation | Disposition |
| --- | --- |
| Most initial source candidates used nullable selection/property comparisons; E3006/E3003 repeatedly rejected | Real repairs succeeded for five workflows; three exhausted. No upstream defect was established from these diagnostics, and no DEAL edit was made. Added compact guidance favoring non-null selection IDs/explicit locals rather than assuming property narrowing. |
| UI2050 borrowed-state writes in full search candidate | Real model repaired the immutable update; accepted source replay passed. |
| UI2023 view collection length | Delta repaired the UI to use an explicit DEAL presence scalar. |
| Two full validation candidates ended with E1006; third also lacked closure | Retained rejected source and diagnostics. Provider finish reason was stop, not token-limit truncation; root cause not established. |
| Verifier rejected correct €16.00 total and € 16.00 total formatting | Normalize harmless spaces and total suffix when asserting numerical price; do not require one decorative spelling. No source edit or new inference. |
| Verifier required capitalized Quantity in validation error | Case-insensitive semantic assertion; generated error and disabled loading were already correct. |
| Verifier expected retry as a Button; generated Failure exposed onRetry | Invoke compiler-issued retry slot. Replayed pair passed without new inference. |
| Authored fixture runs overwrote a missing accepted-source experiment path | Corrected evidence storage so only real acceptance writes experiment accepted files. The full validation task's prior fixture-shaped saved artifact is excluded; only the actual five exported accepted pairs enter generated fixtures. |
| Native ADB typing adapter produced only p | Reproduced lost IME edits while the serialized DEAL worker lagged behind input. Renderer now buffers draft edits until authoritative acknowledgements; app policy stays in DEAL. Actual multi-character input passed afterward. |
| Older frozen policy test lacked new sha256 export | Updated sandbox metadata for the declared mechanism. |
| Exact prompt hash changed after component facts and guidance revisions | Updated explicit serialization regression; sixteen catalogue source-pair parity cases still pass. |
| Exhaustion tests expected three repeated source calls | Repeated-response stop deliberately spends fewer calls; assertions updated, healthy workspace preservation unchanged. |

# Evidence and boundaries

Final authored full/delta fixtures, five generated fixtures, catalogue, source-generation replay, lifecycle and workspace isolation instrumentation passed on Android. Compiled-core exact source-pair parity, invalid envelopes, fatal host propagation and patch negative cases passed. Production JS lifecycle/UI checks, artifact boundaries and cache regressions passed. Native editable replay passed after installing the final app. No Deal UI or DEAL implementation changes were needed, and no new backend, interpreter, domain-specific component or provider write was introduced.

The next reliability step should not be more catalogue breadth or blind inference repeats. Address the repeated nullable-selection construction and shorten repair input context through measured compiler-facing guidance/construction. Add genuinely different tasks before asserting generality. Delta's output savings justify further investigation, not promotion. Routine diagnostics remain bounded and shape-only; explicit experiment responses and private device exports remain under ignored build storage.
