---
kind: research
id: constrained-generation-delivery
created_at: 2026-11-01
updated_at: 2026-11-03
status: incomplete
references:
  - framework-speed-retrace.md
question: "Does production generation implement the approved constrained architecture end to end?"
mode: local
provider: local-tools
sources:
  - dependencies/deal-embedding/core/generation.deal
  - dependencies/deal-embedding/ui/fragment-contract.json
  - tools/generation-gateway.py
gaps:
  - Live extraction remains unreliable despite typed recovery and alternatives.
  - Backend comparison, repeatability and visual assessment remain incomplete.
  - Accepted latency regression: 320.426s versus 51.274s; OHOS mechanism parity unproven.
---

# Production delivery plan

Implement one production route, preserving app API/promptless UX. Not acceptance.

## Mapping and implementation status

Paths under `core/` and `ui/` belong to `dependencies/deal-embedding/`. OHOS reference: `deal-studio-ohos` revision `7ee06af1941729b00a110493ad9d1d9b5b127d17`, under `entry/src/main/ets/vera/`.

| Requirement | OHOS reference | Our code and status |
|---|---|---|
| Typed needs; nonthinking first | `NeedsChoose.drawPlan` | `core/needs.deal`, `core/constrained-generation.deal`, `core/generation-guidance.deal`: decoded evidence matching and classified rejection implemented; extraction unreliable live. |
| Typed alternatives, prerequisites, dimensions, coverage | `buildChooseRequest`, `VeraCompose` | `core/need-candidates.deal`, `core/need-expressions.deal`: disclosed-field/type checks and relation lowering implemented; comparison has two issued presentations. |
| Exact issued Jev choices | `composeFromSketch` | `core/need-selection.deal`, existing chooser/protocol: answer/dependency validation, 32-question batches; bounded aliases and explicit oversized-description gaps. |
| Canonical assembly/checking | `assembleProgram` | `core/generation.deal`, constrained orchestration, existing composer/checker, `core/source-agent.deal`: single route/exact repair implemented; full-flow tests pass. |
| Local uncovered behavior | `agent/AgentHole.ets:130–132,180–215`, `agent/AgentCustom.ets:340–400` | `core/need-holes.deal`, source-agent, `core/composition/fragments.deal`, `ui/fragments/support/instances.deal`, `ui/fragment-contract.json`: typed owned section subtrees and scalar holes preserve neighbors; recovery integration implemented. |
| Whole-screen gate | `agent/VeraBuildRoute.ets:162–168` | Dependency-closed subset probes implemented; exact repairs protect maintained bytes and body containment. |
| Explicit routing/escalation | `DeepSeekEngine` | `tools/generation-gateway.py`: nonthinking/thinking needs and separately configured escalation backend; Cerebras source/repair. Backend comparison incomplete. |
| Deterministic/live proof | staged checks/timings | `tools/test-constrained-generation.js`, `tools/test-constrained-flow.js`, existing suites: build30 passes 15 host/nine device suites/seven installed hashes. Dispatch receipt/device regression passes; cohort20 renders all four. |

## Required recovery alignment

Lifecycle: construction → checked provisional composition → canonical materialization. Provisional section checks do not establish canonical identity. After materialization, allow atomic revision/digest-bound exact edits only; stale/overlapping edits leave both files unchanged. Enforce maintained-neighbor preservation across construction and repair.

Issue **UI-section holes**, not only pure functions: stable IDs, typed imports/exports, owned state/actions/views and capabilities. Admit only declared interfaces/writes/calls; reject neighbor mutation/missing imports. Retain checked sections.

**Approved budget:** three model invocations per stable need/request: initial → diagnostic repair → thinking-enabled repair; count dispatches, including failed calls. Group calls charge every participating need; splitting, renaming or re-entry never resets accounting. Record revision/diagnostic fingerprints. Cancellation/refusal/operational failure may exit early; exhaustion retains checked work and reports unresolved needs. This supersedes no-attempt-ceilings for local holes only; other ceilings remain unchanged.

Split refused groups; retry only unresolved needs within remaining budgets. Remove transitive dependents lacking outputs; retain independent working sections with visible gap notices. Report missing behavior as failed semantics, not a delivery blocker; never invent facts. Jev coverage readings are advisory, not proof.

Fallback requires no surviving runnable task component after dependency pruning and real provisional assembly/checking, including retained partial sections/notices. Notices alone do not count. Record subset/revision/check evidence. Cancellation/provider/checker-infrastructure failures never establish absence. Local rejection or exhaustion alone never authorizes whole-screen generation.

## Invariants and acceptance

Portable DEAL owns synthesis, admission and composition; native/Python adapters transport checked data. Freeze intent/context/disclosed contracts. No scenario/package/provider-name branches, fabricated answers or inferred field-name units. Validate request-local identifiers, dependencies, dimensions and coverage; unknown units stay unknown. Evidence matching does not prove assigned physical dimensions; extracted-needs coverage does not prove original-requirement completeness. Enforce existing chooser name/description/option limits. Preserve grants, cancellation and disclosure/catalogue authority.

Construct/check sections before canonical materialization; afterward use revision/digest-bound unique, disjoint exact edits only, never regeneration. Measure custom work, fallback, rejected turns and escalation. Before inference, finish deterministic verification without overlapping builds. Before each fresh cohort, archive all six apps, classify before generated-only purge and verify retained data exactly; preserve every historical receipt. Stop timing at first runnable committed frame before untimed semantics. Require artifact-backed code mapping and measured four-scenario plumbing; report semantic and visual outcomes separately. Incorrect semantics does not block delivery.

## Evidence and limitations

Build30 passes 15 host/nine device suites/seven installed hashes; dispatch-bound acknowledgement and frame selftests pass. Cohort19 stopped on provider HTTP failure; preserved. Harness now records per-scenario operational errors and continues without retries. Cohort20 archived/purged six apps with retained data verified; all four rendered. Budget25.465s/Food58.235s passed semantics; Departure48.037s lacked calculations; Equipment188.689s completed comparison but budget representation was unsupported by assessor. Extraction consumed14.827/26.976/30.531/149.870s across7/6/8/21 calls respectively. No full-source fallback or exact edits. One transient preference capture broke collection; read-only recovery records the race, rejects final corruption, and passes three tests. Original failure retained. Full stages/tokens: ignored `benchmark-20/pipeline-stages.json`.

Parity: `needs-plan`, `sketch`, `needs-filtering`, `needs-drawing`, `needs-start` port normalization/filtering/retry/review. Verifier:175 needs,360 sketch,175 integrated,37 filtering,142 streaming,16 retry/review,80 requested-start cases per JS/JVM. Unicode mechanisms injected; surrogate-splitting notices reject. Production unchanged; composition remains. Receipts: ignored `build/needs-plan-parity/`; historical failures: `build/constrained-generation/`.

Cohort2: Budget54.968s passed; Departure429.252s then self-rescheduling refresh stalled, proven by replay. Cohort4 mounted without a committed frame: inconclusive; lifecycle selftest added, cause unproven. Cohorts5/6 aborted on capability/transport failures. Cohorts7–9 explicitly escalated thinking extraction to Cerebras; initial extraction remained DeepSeek. Collection records truncated arguments. Archive/purge verification precedes inference. Receipts: `build/constrained-generation/benchmark*/`. Backend comparison/visual assessment remain absent; mocked boundaries never establish live behavior.

## Execution order

1. Port reference NeedsPlan/NeedsChoose machinery into DEAL; replace independently designed extraction/admission recovery.
2. Align VeraCompose candidate/questions/answer handling, AgentHole/AgentCustom recovery and ScreenPlan/VeraBuildRoute assembly/fallback. Prove each mechanism through differential tests; stage-name similarity is insufficient.
3. Defer OHOS backend integration and fresh live benchmarks until internal mechanism parity. Preserve authority and exact-source boundaries.

**Disposition:** user accepts current regression for checkpointing, not reference parity. Combined first-frame320.426s versus historical51.274s; needs extraction42 calls/222.204s,38 rejected documents. SF estimated22%: foundations verified, composition/recovery/integration/performance remain. Not stage arithmetic;100% requires four correct scenarios within20s. Embedding commit `23fdfc3`; consuming tooling/tests committed separately. No push.
