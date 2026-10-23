---
kind: research
id: authority-four
created_at: 2026-10-23T10:00:00Z
updated_at: 2026-10-23T10:00:00Z
status: partial
references:
  - dependencies/deal-embedding/android/src/discovery/Discovery.kt
  - dependencies/deal-embedding/core/generation-guidance.deal
  - dependencies/deal-embedding/core/repair-guidance.deal
  - dependencies/deal-embedding/core/source-repair.deal
  - dependencies/deal-embedding/ui/generation-guidance.md
  - android/tests/DiscoveryInstrumentation.kt
  - tools/verify-generation-guidance.js
question: What happens after domain-neutral authority, serialization and outcome guidance changes and a fresh four-scenario purge and benchmark?
mode: local
provider: compiled policy and native tests plus live device execution
sources:
  - build/authority-four/results.json
  - build/authority-four/numbers.json
  - build/authority-four/events.json
  - build/authority-four/semantic-assessment.json
  - build/authority-four/distributions.json
  - build/authority-four/final.tar
  - build/authority-four/clean-state.json
  - build/authority-four/historical-integrity.json
gaps:
  - One sample per scenario; no causal or population latency claim.
  - Equipment semantic assessment closed while a quote was pending; outcome inconclusive.
  - No alternate-model experiment or change of language Unicode semantics.
  - Supplied assertions are not exhaustive or visual approval.
---

# Changes

Embedding commit 95ac821 changes generic discovery/source guidance, not domain implementations. Discovery now preserves actual required provider invocations even if catalogue descriptions expose formulas or examples; selecting a capability reference alone is not enough. A generic sensor illustration demonstrates the authority boundary without routing by scenario. Generation distinguishes requested feasible initial choices from editable empty controls and preserves explicit manual-selection requests. No allocation algorithm, task-name branch, provider formula or expected answer was added to the framework or renderer.

Initial source instructions explicitly separate independently serialized files and one JSON-escape layer; workflow reminders repeat decoded-file separation. Rejected exact searches recommend smaller verbatim unique regions, explain escape levels and preserve atomic no-relocation behavior. Source-local E1052 guidance recommends literal Unicode characters while retaining the current language grammar. Contradictory digit-loop guidance now points to existing ui/values.decimal. Exact digest, unique/disjoint matching, compiler admission, grants and explicit provider actions remain unchanged. No heuristic source normalization or semantic test feedback enters the timed path.

These are prompt/diagnostic improvements, not deterministic semantic guarantees. No LLM was silently switched: Cerebras qwen-3.8-27b with low reasoning and client source output ceiling omitted remains the source backend, DeepSeek Flash discovery remains unchanged, and no model superiority is established. No compiler optimization or new Unicode language feature was attempted without separate evidence/design.

# Verification and preservation

Gateway/transport, metrics, API, compiled generation-guidance and compiled source-repair checks passed. Discovery prompt-policy assertions check the authority guidance; they do not prove model adherence. The native Discovery suite also exercises canonical admission, cancellation, catalogue freshness and zero premature reads; native GenerationGateway covers the real compiled generation path. Both returned instrumentation code0. Full library/app/test build and seven APK installations completed before inference. No build/native test overlapped the measured cohort.

With explicit user authorization, all six app states were archived before purging generated files, source/drafts/links and test receipts. The purge compared retained preferences and verified zero generated state, preserving domain, permissions, sharing and disclosure/capture choices. Fresh means fresh generation, not cold compiler/provider caches. Both prior first-render evidence archive checksums remain unchanged; historical 932-file integrity verification found zero mismatches. Gateway process/configuration remained unchanged; cumulative gateway counts are not cohort usage. Per-call receipts establish counts and tokens.

# Method

Unchanged first-render harness: start before foreground Organizer/protocol setup; discover once; explicitly admit; generate/check/repair until runnable; stop at the first fault-free generated frame commit through ExperienceSurface and the real renderer. Frame commit means swap-chain submission, not physical scanout. Freeze timings before screenshot, provider actions and semantic assertions. No candidateTest callback, semantic feedback or automatic null-discovery retry. All four ran the explicit test host/grants, not ordinary app navigation or consent journeys. The generated first results remain retained; no regenerated semantic replacement was substituted.

# Timings

Seconds. All compiler/check attempts are summed and already included in generation/total. DEAL/UI stages are nested, not additional.

| Scenario | Discovery only | Start to suggestion | Generation to render | Total to render | All checking | DEAL stage | UI stage | Repairs |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| Budget | 2.003 | 5.514 | 4.996 | 10.664 | 1.882 | 1.808 | 0.023 | 0 |
| Departure | 2.440 | 3.276 | 8.168 | 11.654 | 2.259 | 2.189 | 0.028 | 0 |
| Food | 2.675 | 3.573 | 22.961 | 26.696 | 2.256 | 2.107 | 0.073 | 1 |
| Equipment | 2.551 | 3.453 | 38.739 | 42.491 | 3.300 | 3.167 | 0.063 | 2 |

Seven source calls: four initial, three exact patches. Two applied; one rejected PATCH_NO_MATCH. Food repaired E1053 missing semicolon; equipment still generated escaped view contamination inside DEAL, then one unmatched search before a successful removal. No unsupported Unicode-escape failure occurred in this sample. Total91.505s versus87.752s previously: food faster, equipment slower, no overall speedup claim.

# Tokens

Source generation plus every repair. Completion includes reasoning; cached input is included in input, not additional.

| Scenario | Input | Completion | Reasoning subset | Cached input |
|---|---:|---:|---:|---:|
| Budget | 13880 | 1653 | 1041 | 2048 |
| Departure | 13968 | 8440 | 6584 | 13312 |
| Food | 32143 | 30557 | 25701 | 25600 |
| Equipment | 56535 | 62327 | 54354 | 47104 |
| Total | 116526 | 102977 | 87680 | 88064 |

Discovery separately28845input/701completion, cached2432; reasoning usage unknown. Receipt spans are not isolated compute or billing. All referenced artifacts retained across progress/final archives. Aggregator records four requests and three repairs; missing/oversized counters and summary completeness retained in distributions.json.

# Separate semantic assessment

Budget: passed supplied arithmetic, editable amounts, equality and signed-under-budget checks.

Departure: passed supplied checks with an actual completed host/trip.estimate read (29minutes), departure15:16/inspection15:06, and edited start15:41/15:31. The discovery brief now explicitly calls the provider and no longer supplies its formula. This demonstrates adherence in one sample, not universal enforcement.

Food: incorrect. Source contains an initial allocation function, so the prior 'only editable zeros' diagnosis no longer describes its intended algorithm. But it filters returned size using exact string `"30"`, while provider records say `"30 cm"`; all menu rows are discarded. After actual menu/delivery reads the rendered result has no pizza options and zero quantities. No later edit checks were reached. Unit/string interpretation belongs to generated domain logic or typed provider contracts, never a framework pizza heuristic.

Equipment: inconclusive, not established incorrect. Catalogue correctly identifies package, stand and cable. The evaluator dispatched a quote action then completed its assertions/closed the session while host/rental.quote was still pending (cancelled, not failed). Its settle routine only waits for short unchanged snapshots/no progress; asynchronous dispatch can be queued before the loading frame becomes visible. Further separate assessment needs an action-delivery acknowledgement or state-transition-aware waiting, and must not repeat unnecessary quote reads. No semantic repair or retiming was performed. Original raw evaluator failure is preserved in results.json; semantic-assessment.json records the corrected classification.

# Remaining bottlenecks

Model times: budget2.502s, departure5.296s, food19.681s, equipment34.308s. Checking respectively1.882/2.259/2.256/3.300s. Equipment serialization and exact-search reliability remain unresolved despite clearer guidance. Food still mishandles provider units despite generating allocation logic. Compilation/renderer correctness defects were not demonstrated. Separately, the semantic evaluator needs robust asynchronous action completion before asserting or closing; this does not invalidate already frozen render latency but limits equipment usefulness evidence.
