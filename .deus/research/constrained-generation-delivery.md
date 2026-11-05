---
kind: research
id: constrained-generation-delivery
created_at: 2026-11-01
updated_at: 2026-11-05
status: incomplete
references:
  - framework-speed-retrace.md
question: "Has the platform-independent OHOS studio core replaced production generation in portable DEAL, serving four correct Android scenarios within 20 seconds combined?"
mode: local
provider: local-tools
sources:
  - dependencies/deal-embedding/core/generation.deal
  - dependencies/deal-embedding/ui/fragment-contract.json
  - tools/generation-gateway.py
gaps:
  - Tested port components are not integrated; divergent production policy remains.
  - Full composition, reference recovery and assembly remain unfinished.
  - Complete platform-independent dependency coverage and the final correctness/latency target remain unproved.
---

# Production delivery plan

Port all platform-independent studio core to readable DEAL, including supporting dependencies beyond the seven initially named files. One shared core owns orchestration and policy; Android and later OHOS supply backend mechanisms. Replace production and delete superseded policy. Preserve public APIs, promptless UX and authority. No independent policy additions. Final outcome: four semantically correct Android scenarios within 20 seconds combined; parity alone does not guarantee this timing.

## Mapping and implementation status

Paths under `core/` and `ui/` belong to `dependencies/deal-embedding/`. OHOS reference: `deal-studio-ohos` revision `7ee06af1941729b00a110493ad9d1d9b5b127d17`, under `entry/src/main/ets/vera/`.

| Mechanism | Port status |
|---|---|
| `NeedsPlan` / `NeedsChoose` | Parsing, sketch conversion, filtering, streaming, retry/review and preliminary choices tested. Production replacement/deletion pending composition dependencies; step incomplete. |
| `VeraCompose` | Section/part eligibility, expression menus/questions and answer-confidence decoding tested. Written-formula parsing/evaluation/validation tested; example environments, remaining questions and full bindings unfinished. |
| `AgentHole` / `AgentCustom` | Reference port not started. Existing local recovery is not parity evidence. |
| `ScreenPlan` / `VeraBuildRoute` | Reference port not started. Existing assembly/fallback is not parity evidence. |
| Supporting platform-independent core | Include catalogue/contracts, layout/emission and transitive dependencies. Complete dependency coverage remains unverified. |
| Production | `core/generation.deal` still invokes the divergent constrained route. Tested port modules have not improved installed Android behavior. |

## Required recovery alignment

Lifecycle: construction → checked provisional composition → canonical materialization. Provisional section checks do not establish canonical identity. After materialization, allow atomic revision/digest-bound exact edits only; stale/overlapping edits leave both files unchanged. Enforce maintained-neighbor preservation across construction and repair.

Issue **UI-section holes**, not only pure functions: stable IDs, typed imports/exports, owned state/actions/views and capabilities. Admit only declared interfaces/writes/calls; reject neighbor mutation/missing imports. Retain checked sections.

**Approved budget:** three model invocations per stable need/request: initial → diagnostic repair → thinking-enabled repair; count dispatches, including failed calls. Group calls charge every participating need; splitting, renaming or re-entry never resets accounting. Record revision/diagnostic fingerprints. Cancellation/refusal/operational failure may exit early; exhaustion retains checked work and reports unresolved needs. This supersedes no-attempt-ceilings for local holes only; other ceilings remain unchanged.

Split refused groups; retry only unresolved needs within remaining budgets. Remove transitive dependents lacking outputs; retain independent working sections with visible gap notices. Report missing behavior as failed semantics, not a delivery blocker; never invent facts. Jev coverage readings are advisory, not proof.

Fallback requires no surviving runnable task component after dependency pruning and real provisional assembly/checking, including retained partial sections/notices. Notices alone do not count. Record subset/revision/check evidence. Cancellation/provider/checker-infrastructure failures never establish absence. Local rejection or exhaustion alone never authorizes whole-screen generation.

## Invariants and acceptance

Portable DEAL owns shared synthesis, composition, recovery and fallback; native/Python adapters supply mechanisms and transport. Reference behavior is authoritative, not the independently designed evidence/admission strategy currently in production. Preserve intent/context, disclosed contracts, grants, cancellation and capability authority. No scenario/package/provider-name branches, fabricated answers or field-name-derived units. Retain documented checked-JSON/Unicode boundary differences; do not add policy under the guise of portability.

Construct/check sections before canonical materialization; afterward use revision/digest-bound unique, disjoint exact edits only, never regeneration. Measure custom work, fallback, rejected turns and escalation. Before inference, finish deterministic verification without overlapping builds. Before each fresh cohort, archive all six apps, classify before generated-only purge and verify retained data exactly; preserve every historical receipt. Stop timing at first runnable committed frame before untimed semantics. Report semantic and visual outcomes separately. Semantically incorrect plumbing may be checkpointed, but does not satisfy the final objective.

## Evidence and limitations

Build30 passes 15 host/nine device suites/seven installed hashes; dispatch-bound acknowledgement and frame selftests pass. Harness records per-scenario operational errors and continues without retries. Cohort20 archived/purged six apps with retained data verified; all four rendered. Budget25.465s/Food58.235s passed semantics; Departure48.037s lacked calculations; Equipment188.689s completed comparison but budget representation was unsupported by assessor. Extraction consumed14.827/26.976/30.531/149.870s across7/6/8/21 calls respectively. No full-source fallback or exact edits. Full stages/tokens: ignored `benchmark-20/pipeline-stages.json`.

Parity includes `compose-formula`: parser, evaluator, reference emission, time units and written-validation decisions. JS/JVM pass 500 formula and 96 validation cases alongside prior suites. Successful bindings, example mismatches, missing numbers, confidence rejection and conversion checked. Power/remainder are injected numeric primitives; Unicode boundary differences explicit. Production unchanged; earlier-binding example environments and complete composition remain. Receipts: ignored `build/needs-plan-parity/`; historical failures: `build/constrained-generation/`.

## Execution order and controlling checkpoint

1. Finish reference example environments and composition decisions, then recovery and assembly with their platform-independent dependencies. Differential-test each mechanism against the pin; isolated tests are not production delivery.
2. Switch the single production route to the shared core and delete superseded extraction/admission/recovery policy. Needs replacement depends on the unfinished composer; do not mark it complete before integration/deletion.
3. After internal parity, benchmark all four Android scenarios, report semantics separately, and remedy correctness/performance until the final target is met. Defer OHOS backend integration and fresh live benchmarks until portable internal parity.

No known external or proven DEAL/toolchain blocker prevents implementation. Remaining dependencies are unfinished work; timing feasibility is unproved. Updates state changes, tests and remaining integration dependencies, explicitly identifying whether production changed. No unrelated cleanup or speculative progress claims.

**Disposition:** 10% SF estimated, held until integrated replacement is demonstrated. Historical combined first-frame time is 320.426s versus 51.274s; neither establishes the final target. Checkpoints do not imply acceptance or full parity.
