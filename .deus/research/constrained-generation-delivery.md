---
kind: research
id: constrained-generation-delivery
created_at: 2026-11-01
updated_at: 2026-11-06
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
  - Production orchestration is not connected; divergent policy remains.
  - Agent/build failure-chain coverage and backend integration remain unfinished.
  - Complete platform-independent coverage and the final correctness/latency target remain unproved.
---

# Production delivery plan

Port all platform-independent studio core to readable DEAL, including supporting dependencies beyond the seven initially named files. One shared core owns orchestration and policy; Android and later OHOS supply backend mechanisms. Replace production and delete superseded policy. Preserve public APIs, promptless UX and authority. No independent policy additions. Final outcome: four semantically correct Android scenarios within 20 seconds combined; parity alone does not guarantee this timing.

## Mapping and implementation status

Paths under `core/` and `ui/` belong to `dependencies/deal-embedding/`. OHOS reference: `deal-studio-ohos` revision `7ee06af1941729b00a110493ad9d1d9b5b127d17`, under `entry/src/main/ets/vera/`.

| Mechanism | Port status |
|---|---|
| `NeedsPlan` / `NeedsChoose` | Parsing through retries/filtering/choices and real composition assembly tested together. Production replacement/deletion pending; step incomplete. |
| `VeraCompose` | Reader/chooser/contracts and complete part-source emission connect through screen slots to assembly; partial source lowering executes on Android; broader constructs remain unsupported. |
| `AgentHole` / `AgentCustom` | Writer/checks, authority, partial retention, lookup/refusal reuse/splitting and notices tested. Original identities bound widening/final repairs. Integrated catalogue/custom path tested. |
| `ScreenPlan` / `VeraBuildRoute` | State/slots, assembly, surface/whole fallback and budget exhaustion tested. Broad failure-chain/backend coverage remains. |
| Supporting core | Catalogue filling, layouts/alignment, previews/questions, repairs, smoke/play policy, records and structural edits tested. Transitive coverage remains incomplete. |
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

JS/JVM differentials cover needs-to-source, filling/layouts, alignment, repairs, records/edits and catalogue/custom agent chains, including search, lists/function-hole repairs. Operational failures propagate; grouped/split/re-entered budgets share identities. Catalogue remains data; broader failures remain. JVM storage/origin assembly fixes constant-pool exhaustion; affected suites pass. Full compiler suite stops at an unchanged rejected-syntax JS fixture, not a complete pass. Production unchanged. Partial shared lowering passes actual Android compilation, sandbox input recomputation/restoration; unsupported constructs reject. Evidence: ignored `build/portable-integration/` and `build/needs-plan-parity/`; historical failures: `build/constrained-generation/`.

## Execution order and controlling checkpoint

1. Integration first: connect orchestration to the real backend. Close transitive/failure/coverage gaps exposed by compilation and end-to-end execution; no standalone expansion unless connection requires it.
2. Switch the single production route to the shared core and delete superseded extraction/admission/recovery policy. Needs replacement depends on backend integration; do not mark complete before replacement/deletion.
3. After cutover, verify all four Android scenarios and measure committed-frame timing; remedy semantics/performance until the target holds. Defer OHOS and premature live benchmarks.

No external blocker. Integration remains unfinished; timing feasibility is unproved. Updates state changes, tests and remaining integration dependencies, explicitly identifying whether production changed. No speculative progress claims.

**Disposition:** Incomplete; no supported progress percentage. Historical combined first-frame time is 320.426s versus 51.274s; neither establishes the target. Backend tests do not establish production delivery.
