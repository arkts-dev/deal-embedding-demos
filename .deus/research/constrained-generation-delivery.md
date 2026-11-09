---
kind: research
id: constrained-generation-delivery
created_at: 2026-11-01
updated_at: 2026-11-09
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
  - Production is cut over; catalogue provenance and broader backend mechanisms remain unfinished.
  - Agent/build failure-chain and backend coverage remain incomplete.
  - Complete platform-independent coverage and the final correctness/latency target remain unproved.
---

# Production delivery plan

Port studio machinery to DEAL; shared core owns policy, hosts supply mechanisms. Preserve APIs/UX/authority. Target: four correct scenarios within20 seconds combined.

## Mapping and implementation status

`core/` and `ui/` are under `dependencies/deal-embedding/`. Reference: `deal-studio-ohos` revision `7ee06af1941729b00a110493ad9d1d9b5b127d17`, under `entry/src/main/ets/vera/`.

| Mechanism | Port status |
|---|---|
| `NeedsPlan` / `NeedsChoose` | Parsing through retries/filtering/choices and real composition assembly tested together. Production entry replaced; obsolete extraction/admission/recovery removed; broader delivery incomplete. |
| `VeraCompose` | Reader/chooser/contracts and complete part-source emission connect through screen slots to assembly; partial source lowering executes on Android; broader constructs remain unsupported. |
| `AgentHole` / `AgentCustom` | Writer/checks, authority, partial retention, lookup/refusal reuse/splitting and notices tested. Original identities bound widening/final repairs. Integrated catalogue/custom path tested. |
| `ScreenPlan` / `VeraBuildRoute` | State/slots, assembly, surface/whole fallback and budget exhaustion tested. Broad failure-chain/backend coverage remains. |
| Supporting core | Catalogue filling, layouts/alignment, previews/questions, repairs, smoke/play policy, records and structural edits tested. Transitive coverage remains incomplete. |
| Production | `generation.deal` invokes shared buildScreen with frozen disclosure/contracts and pinned catalogue data, then ordinary canonical admission. All four public scenario fixtures and operational-failure checks pass deterministically. |

## Required recovery alignment

Lifecycle: construction → checked provisional composition → canonical materialization. Provisional section checks do not establish canonical identity. After materialization, allow atomic revision/digest-bound exact edits only; stale/overlapping edits leave both files unchanged. Enforce maintained-neighbor preservation across construction and repair.

Issue **UI-section holes** with stable IDs, typed ports, owned state/actions/views/capabilities. Admit declared interfaces/writes/calls only; reject neighbor mutation/missing imports.

**Approved budget:** three model invocations per stable need/request: initial → diagnostic repair → thinking-enabled repair; count dispatches, including failed calls. Group calls charge every participating need; splitting, renaming or re-entry never resets accounting. Record revision/diagnostic fingerprints. Cancellation/refusal/operational failure may exit early; exhaustion retains checked work and reports unresolved needs. This supersedes no-attempt-ceilings for local holes only; other ceilings remain unchanged.

Split refused groups; retry only unresolved needs within remaining budgets. Remove transitive dependents lacking outputs; retain independent working sections with visible gap notices. Report missing behavior as failed semantics, not a delivery blocker; never invent facts. Jev coverage readings are advisory, not proof.

Fallback requires no surviving runnable task after dependency pruning and real provisional assembly/checking, including partial sections. Notices alone do not count. Record subset/revision/check evidence. Cancellation/provider/checker-infrastructure failures never establish absence. Local rejection or exhaustion alone never authorizes whole-screen generation.

## Invariants and acceptance

Portable DEAL owns shared synthesis, composition, recovery and fallback; native/Python adapters supply mechanisms and transport. Reference behavior is authoritative; the independently designed evidence/admission strategy is removed. Preserve intent/context, disclosed contracts, grants, cancellation and capability authority. No scenario/package/provider-name branches, fabricated answers or field-name-derived units. Retain documented checked-JSON/Unicode boundary differences; do not add policy under the guise of portability.

Construct/check sections before canonical materialization; afterward use revision/digest-bound unique, disjoint exact edits only, never regeneration. Measure custom work, fallback, rejected turns and escalation. Before inference, finish deterministic verification without overlapping builds. Before fresh cohorts, archive six apps, classify before generated-only purge and verify retained data; preserve historical receipts. Time to first runnable committed frame; assess semantics/visuals separately. Plumbing checkpoints are not completion.

## Evidence and limitations

Historical Build30 passed15 host/nine device suites/seven installed hashes and dispatch/frame tests; harness continues without retries. Cohort20 archived/purged six apps with retained data verified; all four rendered. Budget25.465s/Food58.235s passed semantics; Departure48.037s lacked calculations; Equipment188.689s completed comparison but budget representation was unsupported by assessor. Extraction consumed14.827/26.976/30.531/149.870s across7/6/8/21 calls respectively. No full-source fallback or exact edits. Full stages/tokens: ignored `benchmark-20/pipeline-stages.json`.

JS/JVM differentials cover needs-to-source, catalogue/custom chains, records/edits and budgets; full compiler coverage remains unclaimed. Public generation reaches shared buildScreen, Android compilation/smoke and canonical publication. Budget tests over/under/equal, decimal comma and both inputs. Departure invokes actual Vehicle and recomputes edited distance/start/buffers. Food invokes Pizza menu/delivery, checks six pizzas/vegan minimum/delivery-inclusive total, zero quantities, budget/deadline edits and late windows. Equipment selects by authoritative attributes/components and invokes two whole-basket Rental quotes; checks hire/deposit separation, budget boundaries, unavailable quotes and edited periods. Both cover failure/retry/revocation, retained reopen, exact reload and unchanged provider preferences. Injected responses/domain logic are test-only. Shared SDK lowering now chains answer-handler calls, shares freshness epochs, suppresses stale downstream dispatch and rejects cycles; ordinary Deal UI owns execution. Host JS/JVM lowering equality plus real sessions verify out-of-order replies, disposal and chaining. Read analysis uses iterative AST traversal; time/number lowering preserves Unicode/edges. Cancellation/provider/unsupported failures never fallback. Nine modules/three harnesses removed. Evidence: ignored `build/portable-commerce/{device-4.log,budget-departure-regression.log,host-4.log}`, earlier `build/portable-departure/`, `build/portable-public/`, `build/portable-integration/`, `build/needs-plan-parity/`. Initial device timeout retained; later passes. No inference/visual acceptance; reference-flow/screen timed out, boundaries need rebuilt packages. Catalogue provenance, broader effects/controls, quote-expiry/pending-edit coverage, search/questions and surface/play remain incomplete. Live correctness/latency remain unproved.

## Execution order and controlling checkpoint

1. Actively wire four scenarios; fix exposed gaps generally: provenance, custom analysis, disclosed capabilities, lowering, search/questions and surface/play. No separate audit or scenario-specific production patches.
2. Keep one production route; delete further obsolete adapters only after checking retained catalogue/source clients. Preserve canonical exact-edit/reload authority and operational errors.
3. Verify four Android scenarios and committed-frame timing; remedy semantics/performance until the target holds. Defer OHOS and premature live benchmarks.

No external blocker.

**Disposition:** Incomplete. Historical320.426s/51.274s miss target; deterministic fixtures are not live delivery.
