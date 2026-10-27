---
kind: research
id: deterministic-fast-path-audit
created_at: 2026-10-26T00:00:00Z
updated_at: 2026-10-27T00:00:00Z
status: historical-audit-implementation-followup
references:
  - .deus/research/compact-generation-four.md
  - .deus/research/maintained-generation-four.md
question: What concrete additions enable deterministic first-path composition for all four acceptance workflows while retaining an explicit custom-code slow path?
mode: local
provider: local-source-inspection
sources:
  - android/tests/FrameworkE2eInstrumentation.kt
  - dependencies/deal-embedding/android/src/compiler/CompactCandidate.kt
  - dependencies/deal-embedding/android/src/capabilities/CapabilityContract.kt
  - dependencies/deal-embedding/android/src/capabilities/CapabilityBindings.kt
  - dependencies/deal-embedding/core/generation.deal
  - dependencies/deal-embedding/core/source-agent.deal
  - dependencies/deal-embedding/ui/platform.dealui-pack
  - dependencies/deal-embedding/android/src/rendering/DealRenderer.kt
  - android/apps/pizza/src/PizzaCapabilities.kt
  - android/apps/rental/src/RentalCapabilities.kt
  - android/apps/rental/src/RentalModel.kt
  - android/tests/RentalReadInstrumentation.kt
  - dependencies/deal-ui/ui/values.deal
gaps:
  - Proposed operations have not been implemented or compiler/runtime-verified.
  - No new build, device benchmark or inference was performed in this audit.
  - No language or ownership-checker defect was isolated.
---

# Finding

No identified external prerequisite prevents implementation. The current fast path lacks behavioral operators; raw source strings remain its ordinary extension mechanism. All four workflows have identifiable deterministic implementation work. This audit does not establish speed or automatic faithfulness to arbitrary prose.

Audit baseline revisions: root c2865ba, embedding 8be646b, Deal UI 6d445cd, DEAL 890d9dfc. Runtime/product source was not changed during the original audit. Subsequent implementation and verification are tracked separately in `maintained-generation-four.md`; the findings below describe the baseline, not the replacement emitter.

# Four-workflow coverage

| Workflow | Required maintained computation/binding | UI coverage | Authority |
|---|---|---|---|
| Budget | Typed numeric inputs; subtract, absolute magnitude, comparisons and conditional labels; derived dependency inference from expression references; cents/integer validation | Existing IntField, Money, Comparison suffice | Supplied assumptions, no provider |
| Departure | Explicit estimate call; result binding; subtraction chain start-unload-travel, then departure-inspection; distance invalidates estimate, local buffers/start recompute; integral minute validation and clock formatting | Existing IntField and Time/Metric suffice for acceptance; add a dedicated editable minute-of-day TimeField for native usability | Vehicle remains the source of travel duration; do not inline its formula |
| Food | Menu+delivery read workflow under one user action; typed row projection; size eligibility; feasible integer allocation with exact total, minimum dietary quota and delivered-budget constraint; keyed quantity edits, selected-window binding, weighted sums and current validity | Existing Item/IntField/Option/Money/Comparison can express required interactions | Provider prices, dietary fields and meetsRequirement flag remain authoritative |
| Equipment | Catalogue read; typed nested attributes/component projection; predicates, ID joins, component expansion and requirement coverage; construct package and separate alternatives; serialize basket ID/quantity pairs; whole-basket quotes; availability/cost/deposit comparison; local budget recomputation | Existing keyed rows, Option, Money, Comparison and actions suffice | Rental owns stock consumption, quote price, whole-basket deposit and availability |

No missing visual widget explains food/equipment fallback: existing primitives can render them. A TimeField is a useful actual missing input widget, not a substitute for behavioral composition. IntField currently renders plus/minus buttons, not direct numeric typing (DealRenderer.kt:236 onward); large monetary edits are possible but cumbersome. Extend it with direct numeric entry and proper finite/integral/range admission, retaining existing action payload contracts.

# Missing framework capabilities

1. Replace raw initializers/expressions/actions/effects/view strings in the maintained route with typed nodes: literal/reference, arithmetic/comparison/boolean/select, format, record/array construction, projection/filter/reduction, keyed update, ID join and component expansion. Derive dependencies from references; current depends lists are model claims independent of raw expressions (CompactCandidate.kt:73–84).
2. Add an actual finite-domain allocation operation over quantities with linear sum/quota/cost constraints. It accepts a candidate set and explicit constraints, not pizza names, counts, prices or a fixture answer. Choose a stable feasible result unless a task specifies an optimization objective. Do not substitute a cheapest-item heuristic for constraint satisfaction. Define/test numerical validity, infeasibility, cancellation and computational cost; interrupted/exhausted search is not proven infeasibility. This is the largest new algorithmic item, not already supplied by sum/filter operators.
3. Bind call nodes to full CapabilityContract, not the current module-name set (CompactCandidate.kt:28, ExperienceGenerator.kt:106). Validate functions, positional arguments, result paths/types, optional absence and references. CapabilityBindings.kt:7 deliberately lowers records to table; retain that ABI and emit contextual typed reads/projected records using the full schema. Native invocation validation remains unchanged.
4. Compile immutable nested updates and read-only helper operations with the real Deal UI checker. Emit complete construction only where new records are constructed. No blanket deep-copy requirement, mutable-alias escape or ownership-rule relaxation.
5. Use typed view nodes checked against the authoritative pack AST, including children, conditional/list keys, row-plus-payload action bindings and derived presentation values. Generate scalar view bindings rather than unsupported view-time function calls. Any widget addition requires pack, renderer, contracts/tests and matched packaging, not an undeclared component name.
6. Model an explicit user action as a dependency-ordered workflow: read, project, compute, bind, quote. Preserve transactional results or mark partial results honestly; retry the failed workflow/stage, not arbitrarily the first effect (current CompactCandidate.kt:136). Capture inputs/revisions and invalidate dependent results. Existing async functions can implement basic sequencing; effect policies/failure mappers are explicitly unsupported by ExperienceCompiler.kt:27, so do not assume those features work.

# Concrete provider-contract defects/gaps

**Pizza deadline handling:** PizzaCapabilities.kt:16–20 describes only a string requiredBy. The handler at :32–35 parses LocalTime but silently substitutes 18:30 for any parse failure. A previously generated ISO timestamp therefore appears to work by accidental fallback. Specify the accepted local-time format/zone at the app boundary, reject malformed input, test another deadline and preserve provider-owned meetsRequirement. The schema's generic string alone cannot reveal these semantics.

**Pizza size:** menu returns only prose size ("30 cm"). Add an app-owned numeric diameter/unit field so deterministic matching does not parse arbitrary names or rely on prefix matching ("30" also matches other sizes). Preserve existing provider records and dietary/pricing authority.

**Rental:** authoritative nested attributes and component IDs already exist; no reason to recreate them or match display prose. Current quote accepts a domain-specific comma-separated ID:quantity string; bind a validated serialization step explicitly. Existing tests establish whole-basket prices/deposits and shared stock. Missing calendar conversion is not a blocker for this acceptance case: its facts already supply UTC instants; preserve them rather than reconstructing from a friendly label.

**Quote freshness:** quote returns relative expiresInMinutes, but current app-session bindings expose no generic clock/tick to application computation. No inspected path establishes elapsed-expiry invalidation. Add session-owned time/expiry invalidation through the supported runtime, including suspend/reopen; no autonomous provider refresh. Keep expired results stale until explicit refresh, with provider TTL authoritative. Do not call this solved by merely displaying “15 minutes.” For restoration without trustworthy age evidence, fail closed to stale rather than resetting the TTL.

CapabilityType supports structural kinds and numeric ranges, not integer/unit/time/read-vs-write semantics (CapabilityContract.kt:25–42). Do not infer those properties from number/string types. Use explicit validated operator refinements and existing domain contracts/handlers. All four benchmark flows need only read functions; exclude proposal/write functions from their workflow nodes. A generic numeric schema must continue to permit legitimate fractions.

# Routing, slow path and metrics

Current usedFallback means catalogue-to-source, NOT deterministic-to-custom: source-agent.deal:141 returns true even for a wholly assembled compact plan; generation.deal:46 does likewise. Therefore it cannot measure the requested fallback rate.

Add separate internal route/coverage telemetry: maintained-only, custom-section, full-source; requested/filled operation IDs; custom source volume; unsupported reasons; composition attempts; exact-edit recovery; model/check/frame timing and usage. Any model-written source repair makes the delivery no longer wholly deterministic even if its initial plan used maintained operations; record that transition. Retain workspace origin semantics separately.

Keep one generation planning response, deterministic validation/binding, no separate inference selector or verifier. Existing generation.deal:22–27 invokes app-policy choice BEFORE source composition when an offer is available. Reconcile this route explicitly: composition-first for this generation workflow, preserve app-owned policies without silently adding their chooser before the fast-path attempt. Do not delete unrelated catalogue product behavior under the label of cleanup.

Custom sections are explicit missing-operation implementations with typed ports; full-source is a recorded last resort. Detect custom need before final source materialization. Assemble once; subsequent repairs edit the canonical pair only. Unsupported behavior must not be silently omitted or a partial notice counted as task completion. Having a coverage graph proves supplied-plan coverage, not completeness against arbitrary prose.

# Operational repair and verification additions

source-agent.deal:37–45 accumulates the complete history and returns expanded source on compact rejection (:144). ExperienceGenerator.kt:50 sets a 512 KiB isolate return limit and :86 retrieves a complete request batch. Implement lossless paged/chunked transport plus bounded active-context selection with preserved current source/revision/tool identities; do not truncate canonical bytes or merely raise a limit. Separate semantic history from transport paging. Test the actual oversized-operation path; the exact payload responsible for the observed exception remains unisolated.

Remove contradictory compact/full-program instructions (generation-guidance.md:8–14 versus :23–26), not add another warning. Compile and execute maintained plans for all four before live inference; test altered IDs/prose/order/prices/units/deadlines, late delivery, no feasible allocation, malformed inputs, shared component stock, quote failure/expiry, edits, restoration and subsequent exact patches. Expected answers stay in tests, not framework code or synthesis facts.

Preserve historical supplied assertions and separately add provider-read-count, freshness, deadline and coverage tests. Existing evaluator uses labels/control shapes (FrameworkE2eInstrumentation.kt:87–181); a new TimeField needs deterministic evaluator support and separate comparability disclosure, not automatic semantic failure for an unfamiliar control.

Capture metrics continuously from the start so retention does not erase early trials. Build/install/hash-check final bytes before a fresh archived cohort, unchanged providers/reasoning, no candidate semantic feedback/runner retries or retiming. Report fallback frequency and all elapsed work. No new purge/inference was performed for this audit.

# Implementation order

1. Typed operation and per-workflow coverage contract, full capability binding, route/metric distinction, minimal compiler regressions.
2. Maintained emitters, immutable collection operations, allocation feasibility and typed view/workflow emission; fix Pizza boundary/data gaps at its owner.
3. Expiry/time support and UI input improvements with ownership/runtime tests.
4. Explicit typed custom/full-source slow path and lossless recovery transport.
5. Deterministic four-workflow and variation tests; phase-specific guidance; build/install/final-byte verification; independent commits.
6. One fully captured fresh cohort and candid comparison. Any remaining unsupported slot is a measured slow-path case, not an excuse to skip implementing identified fast-path coverage.
