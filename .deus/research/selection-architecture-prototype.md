---
kind: research
id: selection-architecture-prototype
created_at: 2026-10-27T00:00:00Z
updated_at: 2026-10-27T00:00:00Z
status: deterministic-slice-pass-authoring-gate-failed
references:
  - .deus/research/maintained-fragments-delivery.md
question: Can one maintained resource-selection-evaluation implementation replace task-shaped solvers without model-authored execution graphs?
mode: local
provider: local-compiler-runtime-and-configured-cerebras
sources:
  - dependencies/deal-embedding/android/src/compiler/SelectionComposition.kt
  - dependencies/deal-embedding/android/src/compiler/BehaviorCandidate.kt
  - dependencies/deal-embedding/ui/selection-prototype.md
  - tools/SelectionHarness.kt
  - tools/selection-fixtures.py
  - tools/test-selection-session.js
  - tools/verify-selection.py
  - tools/probe-selection-authoring.py
gaps:
  - All three live authoring cases failed acceptance; no corrected architecture is established.
  - This slice does not implement complete food/equipment requirements, quantity row editing, deposits or ordinary device journeys.
  - Exhaustive enumeration has no demonstrated large-instance operational viability.
  - Additive category coverage is not exclusive role assignment; nested component expansion is unsupported.
  - Global refresh invalidation is conservative, not independently cached composable resources.
  - The existing shipping task-shaped routes remain; the experiment is explicitly gated off.
---

## Result

One experimental semantic composer handles food allocation, two-role equipment baskets and an unrelated three-role laboratory fixture through configuration. It infers record declarations/projections from disclosed capability contracts. Maintained code owns state, handlers, enumeration, stock checking, basket transport, authoritative evaluation and per-reply freshness. No fixture-specific branch or custom source is used.

This is a useful deterministic slice, **not proof that the architecture can yet be rectified**. Configured-model authoring failed all three cases. Do not promote the probe or claim delivery.

## Direct deterministic evidence

`tools/verify-selection.py` compiles the actual emitter and generated DEAL/Deal UI, then executes the production loader/session with only providers and time mocked. It preserves separate run directories and checks default shipping admission rejects experimental operations.

The fixtures vary structure, not just identifiers:

- Food: no-argument menu read, deadline-string delivery read, exact total and overlapping category quotas, local cost evaluation. An independent exhaustive oracle compares feasible quantity vectors.
- Equipment: two period arguments, nested attributes/components, arbitrary requirement count, authoritative comma-separated whole-basket quotes and minute-based TTL.
- Laboratory: two differently typed read arguments, a different nested schema without attributes, three roles, typed record-array basket and three evaluation arguments, millisecond TTL.

Runtime assertions cover infeasibility, late eligibility, failed refresh after success, stale numeric/text input completion, unavailable first quote followed by another candidate, sequential replies spanning expiry, backward clock, restoration invalidation, unknown component IDs, zero component stock and duplicate item IDs. Extended assertions cover 33 allocated units and three roles at quantity two, including mixed component/separate baskets consuming shared stock. Existing maintained/graph compiler-runtime regressions also pass; they establish preservation, not generality. These are supplied configurations, not model interpretation or visual acceptance. Edits during disabled numeric controls are programmatic transition tests.

## Live authoring falsification

After deterministic verification, three isolated requests used Cerebras qwen-3.8-27b, low reasoning, provider completion maximum, no automatic retries and no selector/verifier. Credentials stayed in a loopback gateway. The model received the semantic contract, disclosed capability declarations and task requirements, not exemplar plans, source graphs or expected provider answers. Each submission was preserved exactly; no JSON correction, manual repair or retry was applied. The unrelated case was dispatched separately after the first script stopped on malformed equipment data.

| Case | Provider reply | Acceptance result |
|---|---|---|
| Food | Valid encoded composition; real compilation passed | Runtime edit failed: changed deadline still sent the literal original time to the provider |
| Equipment | Valid outer native arguments, malformed inner composition JSON | Rejected without correction |
| Laboratory | Valid outer native arguments, malformed inner composition JSON and invented reference syntax | Rejected without correction |

Known usage across all three: **9,256 input / 15,107 completion / 13,546 reasoning / 2,048 cached tokens**. Reasoning is a subset of completion; cached a subset of input. Provider receipt durations were approximately 5.722, 2.771 and 3.005 seconds. These are isolated authoring costs, not first-frame latency or comparative delivery measurements. Hidden reasoning text was discarded, not retained.

## Interpretation

Removing fixed workflow labels and cardinality is mechanically feasible for this slice. It does not yet establish usable semantic compression: model output was unreliable despite generic maintained execution. Transport fragility and task-binding errors are distinct failures; neither is repaired by the passing supplied fixtures.

The next uncertainty is whether a simpler, provider-native structured contract can reliably express semantic bindings without returning to task templates or low-level graph programming. That is unproven. Full option optimization, editable allocations, authority dimensions such as deposits, independent resource lifetimes and complete task acceptance remain outside this prototype.

No APK was built/installed, no domain state was changed, and no new delivery cohort was run. Historical measurement evidence is retained. The prior report now explicitly withdraws architectural acceptance.
