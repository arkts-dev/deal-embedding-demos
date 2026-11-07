---
kind: research
id: constrained-generation-delivery
created_at: 2026-11-01
updated_at: 2026-11-07
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

Port studio machinery to readable DEAL; shared core owns policy, hosts supply mechanisms. Delete superseded policy; preserve APIs, UX and authority. Target: four correct scenarios within20 seconds combined; parity does not guarantee timing.

## Mapping and implementation status

`core/` and `ui/` are under `dependencies/deal-embedding/`. Reference: `deal-studio-ohos` revision `7ee06af1941729b00a110493ad9d1d9b5b127d17`, under `entry/src/main/ets/vera/`.

| Mechanism | Port status |
|---|---|
| `NeedsPlan` / `NeedsChoose` | Parsing through retries/filtering/choices and real composition assembly tested together. Production entry replaced; obsolete extraction/admission/recovery removed; broader delivery incomplete. |
| `VeraCompose` | Reader/chooser/contracts and complete part-source emission connect through screen slots to assembly; partial source lowering executes on Android; broader constructs remain unsupported. |
| `AgentHole` / `AgentCustom` | Writer/checks, authority, partial retention, lookup/refusal reuse/splitting and notices tested. Original identities bound widening/final repairs. Integrated catalogue/custom path tested. |
| `ScreenPlan` / `VeraBuildRoute` | State/slots, assembly, surface/whole fallback and budget exhaustion tested. Broad failure-chain/backend coverage remains. |
| Supporting core | Catalogue filling, layouts/alignment, previews/questions, repairs, smoke/play policy, records and structural edits tested. Transitive coverage remains incomplete. |
| Production | `generation.deal` invokes shared buildScreen with frozen disclosure/contracts and pinned catalogue data, then ordinary canonical admission. Public arithmetic/budget/departure and operational-failure fixtures pass. |

## Required recovery alignment

Lifecycle: construction → checked provisional composition → canonical materialization. Provisional section checks do not establish canonical identity. After materialization, allow atomic revision/digest-bound exact edits only; stale/overlapping edits leave both files unchanged. Enforce maintained-neighbor preservation across construction and repair.

Issue **UI-section holes**, not only pure functions: stable IDs, typed imports/exports, owned state/actions/views and capabilities. Admit only declared interfaces/writes/calls; reject neighbor mutation/missing imports. Retain checked sections.

**Approved budget:** three model invocations per stable need/request: initial → diagnostic repair → thinking-enabled repair; count dispatches, including failed calls. Group calls charge every participating need; splitting, renaming or re-entry never resets accounting. Record revision/diagnostic fingerprints. Cancellation/refusal/operational failure may exit early; exhaustion retains checked work and reports unresolved needs. This supersedes no-attempt-ceilings for local holes only; other ceilings remain unchanged.

Split refused groups; retry only unresolved needs within remaining budgets. Remove transitive dependents lacking outputs; retain independent working sections with visible gap notices. Report missing behavior as failed semantics, not a delivery blocker; never invent facts. Jev coverage readings are advisory, not proof.

Fallback requires no surviving runnable task after dependency pruning and real provisional assembly/checking, including partial sections. Notices alone do not count. Record subset/revision/check evidence. Cancellation/provider/checker-infrastructure failures never establish absence. Local rejection or exhaustion alone never authorizes whole-screen generation.

## Invariants and acceptance

Portable DEAL owns shared synthesis, composition, recovery and fallback; native/Python adapters supply mechanisms and transport. Reference behavior is authoritative; the independently designed evidence/admission strategy is removed. Preserve intent/context, disclosed contracts, grants, cancellation and capability authority. No scenario/package/provider-name branches, fabricated answers or field-name-derived units. Retain documented checked-JSON/Unicode boundary differences; do not add policy under the guise of portability.

Construct/check sections before canonical materialization; afterward use revision/digest-bound unique, disjoint exact edits only, never regeneration. Measure custom work, fallback, rejected turns and escalation. Before inference, finish deterministic verification without overlapping builds. Before fresh cohorts, archive six apps, classify before generated-only purge and verify retained data; preserve historical receipts. Time to first runnable committed frame; assess semantics/visuals separately. Plumbing checkpoints are not completion.

## Evidence and limitations

Historical Build30 passed 15 host/nine device suites/seven installed hashes and dispatch/frame tests. Harness records errors and continues without retries. Cohort20 archived/purged six apps with retained data verified; all four rendered. Budget25.465s/Food58.235s passed semantics; Departure48.037s lacked calculations; Equipment188.689s completed comparison but budget representation was unsupported by assessor. Extraction consumed14.827/26.976/30.531/149.870s across7/6/8/21 calls respectively. No full-source fallback or exact edits. Full stages/tokens: ignored `benchmark-20/pipeline-stages.json`.

JS/JVM differentials cover needs-to-source, catalogue/custom chains, records/edits and budgets; full compiler coverage remains unclaimed. Public `ApplicationProtocol.generate` reaches shared buildScreen, Android compilation/smoke and canonical publication. Arithmetic/budget mount, recompute, reopen and reload exact source; budget tests over/under/equal, decimal comma and both inputs. Departure now constructs custom source with issued planning provenance, calls actual Vehicle, recomputes edited distance/start/buffers, handles failure/retry/revocation, retains state and reloads exact source. Read analysis uses compiler AST and iterative DEAL traversal; nested JS/JVM regression catches recursive JVM slot clobbering. SDK lowering uses ordinary Deal UI effects with pre-update arguments and stale-completion fencing. Host tests cover out-of-order replies and disposal. Time/number lowering preserves Unicode/edge semantics; metrics reuse adaptive layout. Cancellation/provider/unsupported failures never fallback. Nine modules/three harnesses removed. Exact-edit/Guidance/gateway/Jev regressions pass. Evidence: ignored `build/portable-departure/device-sdk-10.log`, `host-sdk-7.log`, `provenance/` (505 JS/JVM writer/provenance cases,937 reader cases), prior `build/portable-budget/`, `build/portable-public/`, `build/portable-route/`, `build/portable-integration/`, `build/needs-plan-parity/`. No live inference. Reference-flow/screen timed out; boundaries need rebuilt packages. Catalogue provenance, broader effects/controls, search/questions and surface/play remain unfinished; unsupported constructs reject operationally. Food/equipment and live correctness/latency remain unproved.

## Execution order and controlling checkpoint

1. Actively wire four scenarios; fix exposed gaps generally: provenance, custom analysis, disclosed capabilities, lowering, search/questions and surface/play. No separate audit or scenario-specific production patches.
2. Keep one production route; delete further obsolete adapters only after checking retained catalogue/source clients. Preserve canonical exact-edit/reload authority and operational errors.
3. Verify four Android scenarios and committed-frame timing; remedy semantics/performance until the target holds. Defer OHOS and premature live benchmarks.

No external blocker.

**Disposition:** Incomplete. Historical320.426s/51.274s miss target; deterministic fixtures are not live delivery.
