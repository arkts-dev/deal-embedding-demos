---
kind: research
id: raw-framing-four
created_at: 2026-10-23T13:00:00Z
updated_at: 2026-10-23T13:00:00Z
status: reviewed
references:
  - dependencies/deal-embedding/core/source-framing.deal
  - dependencies/deal-embedding/core/source-repair.deal
  - tools/generation-gateway.py
  - tools/test-source-repair.js
question: Does removing model-authored JSON source and patch envelopes improve a fresh four-scenario first-render cohort?
mode: local
provider: retained live model receipts and native instrumentation
sources:
  - build/raw-framing-four/cohort/results.json
  - build/raw-framing-four/cohort/numbers.json
  - build/raw-framing-four/cohort/distributions.json
  - build/raw-framing-four/cohort/final.tar
  - build/raw-framing-four/cohort/clean-state.json
  - build/raw-framing-four/cohort/artifact-retention.json
  - build/raw-framing-four/interrupted-attempts.json
  - build/raw-framing-four/historical-integrity.json
  - build/raw-framing-four/native-GenerationGateway-verified.log
  - build/raw-framing-four/patch-final-tests.log
gaps:
  - One fresh sample per scenario, not controlled causal comparison or population latency.
  - Supplied semantic assertions are not exhaustive correctness or visual review.
  - Deterministic catalogue policy regression fails before source inference; unresolved.
  - Exterior whitespace and empty-input defects required interrupted attempts and rebuilds, retained separately.
---

# Replacement

Model-authored JSON source/patch envelopes and provider source/patch JSON schemas are removed, with no compatibility fallback. Core accepts only DEAL-SOURCE/1 and DEAL-PATCH/1 raw framed replies. API/provider transport and internal host-serialized source pairs remain JSON; persisted sources and application APIs are unchanged. Source payload quotes, backslashes, Unicode and whitespace are literal, never heuristically unescaped or extracted. A chosen boundary token avoids payload marker collisions. Exterior whitespace is framing; payload whitespace is preserved. Empty/malformed/duplicate/unknown/colliding frames reject and recover. Patches remain digest-bound, unique, disjoint, atomic and fully rechecked; no fuzzy matching, file relocation or full rewrite repair.

Compiled regressions cover old JSON rejection, empty/whitespace/malformed responses, duplicate/unknown/colliding boundaries, exact newline/CR/backslash/Unicode preservation, stale digest, zero/multiple/overlapping matches and atomic application. Gateway/transport, guidance, metrics/cache, public API/boundaries, toolkit and native generation gateway checks passed. Native hardening and protocol checks passed. A compound-failure observer termination assertion failed once and passed on separate rerun; no root-cause/fix claim. ProtocolPolicy failed twice with E8001 expected table after choice validation, before source inference/checking, in the deterministic catalogue path. This is retained as an unresolved regression, not concealed by the live benchmark.

The first live attempt rejected leading blank lines until explicitly allowing exterior framing whitespace. A second attempt rendered budget/departure, then a provider returned empty food content with stop and reasoning-only completion; the new parser incorrectly indexed an empty split array. Empty-input recovery is now covered. The new module was also initially missing from the consuming build fingerprint; tools/build.sh now includes it. Original attempts, logs, sources, purges and stale build receipt remain. The final cohort ran only after the rebuilt exact packaged parser and native gateway/waiting regressions passed. No builds/tests overlapped measured inference.

# Method

All six apps were archived before each authorized generated-state purge. Checks verified zero generated files/keys while preserving domain, authority and retained preferences. Historical cohorts remain unchanged. Fresh generation does not imply cold provider/compiler/library caches. Same Cerebras qwen-3.8-27b low reasoning, source output default unlimited and DeepSeek Flash discovery. No model switch or expected-answer injection.

Clock unchanged: before foreground host/protocol setup and discovery through first fault-free generated frame commit (swap-chain submission, not physical scanout). Recovery/checking is included. Actual provider reads, semantic actions/screenshots and cleanup are afterward, without candidateTest feedback, regeneration or retiming. Explicit Organizer test host/grants are used, not ordinary navigation/consent journeys.

# Final fresh cohort

Seconds. Checking includes all candidate checks and is nested inside generation/total.

| Scenario | Discovery | Generation to frame | Total to frame | Checking | Repair calls | Supplied semantics |
|---|---:|---:|---:|---:|---:|---|
| Budget | 1.740 | 4.629 | 10.950 | 1.915 | 0 | Passed |
| Departure | 2.052 | 10.516 | 13.677 | 2.399 | 0 | Passed |
| Food | 2.318 | 86.122 | 89.470 | 2.664 | 15 | Failed initial allocation |
| Equipment | 2.826 | 19.051 | 22.904 | 3.060 | 0 | Passed |

Total **137.001s**, versus prior settled-four **140.036s**. Equipment improved in this sample; food worsened substantially. This is not evidence of a reliable overall speedup or causal raw-framing benefit. Three passed supplied semantics versus three in the prior cohort, but different scenarios: food now fails and equipment passes. Screenshots are capture, not visual approval.

Nineteen source calls: four initial plus fifteen repairs, all for food. Four complete latest summaries; zero malformed/oversized metrics; all referenced captures retained across progress/final archives. Food initial UI2023 collection-length access required repair. Fourteen patches were rejected as PATCH_FRAME; only the fifteenth applied and passed checking. Repeated errors included missing boundary-token header field, inconsistent digest/token markers, and invented deal-replacement/dealui-replacement markers instead of replacement. No accepted patch needed fuzzy targeting; no JSON envelope contamination was observed in these admitted raw initial replies. Raw framing exchanged escaping failures for substantial framing compliance trouble; it did not solve model protocol reliability.

Food model time **80.150s** versus **2.664s** checking. Its allocation loops assign at most one vegan unit per vegan row, then visit each catalogue row only once to fill remaining demand. With three menu rows it produces four pizzas rather than six, and reports infeasible despite a feasible solution. This is generated domain logic, not serialization or an evaluator wait failure. Budget and departure passed arithmetic/edit and actual Vehicle-read checks; equipment passed supplied actual catalogue/quote comparison checks. No semantic feedback was sent to generation.

# Source usage

Provider-reported; completion includes reasoning, input includes cached input. Subsets must not be added again.

| Scenario | Input | Completion | Reasoning subset | Cached input |
|---|---:|---:|---:|---:|
| Budget | 13909 | 2441 | 1773 | 13312 |
| Departure | 13990 | 11988 | 10065 | 12288 |
| Food | 306266 | 122253 | 96489 | 278528 |
| Equipment | 14188 | 24823 | 20143 | 12288 |
| Total | 348353 | 161505 | 128470 | 316416 |

Discovery separately: 28845 input, 691 completion, 28160 cached input; reasoning unknown. Gateway admission counts include interrupted attempts and are not final-cohort counts. Receipt timings are not isolated compute, queue time or billing.

# Conclusion

The requested replacement is complete and exercised end to end, without restoring JSON output or weakening source/patch admission. The fresh benchmark does **not** establish that raw framing is more reliable: difficult repair output still repeatedly violates its protocol. Model generation/recovery remains the dominant hard-case cost, and semantic correctness remains separate from first render. The catalogue-only native regression remains unresolved and must not be described as a fully green test suite.
