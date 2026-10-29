---
kind: research
id: framework-refactoring-plan
created_at: 2026-10-28T00:00:00Z
updated_at: 2026-10-29T00:00:00Z
status: implementation-in-progress
references:
  - dependencies/deal-embedding/api/embedding.d.deal
  - dependencies/deal-embedding/android/src/Protocol.kt
  - dependencies/deal-embedding/android/src/discovery/Discovery.kt
  - dependencies/deal-embedding/android/src/compiler/FragmentComposition.kt
  - android/apps/shared-ui/Assistance.kt
question: Which APIs and implementations move where?
mode: local
provider: code-inspection-and-user-review
sources:
  - Current implementation and supplied API inventory
  - Architecture review in this conversation
gaps:
  - Implementation, behavioral preservation and cross-platform execution require verification.
---

# Framework refactoring plan

Paths are relative to `dependencies/deal-embedding/`, except app paths. Implementation and verification remain incomplete. Portable lowering/view binding and the corrected four-task measurement are recorded in [framework-corrected-four.md](framework-corrected-four.md); that evidence does not close ordinary-journey, visual or cross-platform acceptance.

## 1. One capability model

Merge `WireType`/`CapabilityType`, `Parameter`/`CapabilityParameter`, `SharedCapabilityFunction`/`CapabilityFunction` and `SharedCapabilityContract`/`CapabilityContract` into `WireType`, `Parameter`, `CapabilityFunction` and `CapabilityContract` in `android/src/CapabilityApi.kt`.

Use `WireField(name,type,optional)` instead of native field maps plus `optionalFields`. Carry nesting, element types, collection/numeric bounds, ordered parameters, descriptions and versions. Put `NumberType`/`NullType` on `WireType`.

Move `json`/`parse` and schema/value/argument/identifier validation into internal `capabilities/CapabilityCodec.kt`. Delete `native()` and the second model. Expose `nativeCapabilities(contract,handlers)` and `NativeCapabilities`; internalize invocation. Align the model with `api/embedding.d.deal` through generated native records. Each app authors its contract once; derive transport metadata and compiler declarations from it.

## 2. Bootstrap and execution

Replace both `Embedding` constructors and direct `ApplicationProtocol` construction with `Embedding.open(context,config,capabilities,trustProvider)` returning the app-facing protocol.

Internalize `CapabilityRegistry`, `CapabilityHost` and `CapabilitySession`: catalogue access, reads, session creation, identity, receive/drain and teardown. Expose catalogue refresh through `api.refresh()`, permissions through `grant`/`revoke`, and owned-resource disposal through `close`; absorb `shutdown` there. Replace `invokingExperience()` with immutable handler invocation context.

Expose storage name, state retention and generation/discovery settings in unified configuration. Derive contracts from registrations/catalogue. Internalize source/generation asset selection; move `candidateTest` to test configuration. Replace `policyAssets` with explicit app-policy registration.

Expose gateway endpoint/backend/timeouts there. Replace `ModelClient.complete(...,log)` with optional inference-provider requests/completions plus cancellation, without framework logger dependencies. Internalize cancellation ownership; expose token `check`/`onCancel`. Make `GenerationRequest` an opaque issued handle; expose `ProtocolFailure(code,message)`.

## 3. Android capability transport

Internalize `CapabilityDiscovery` construction, discover/close and endpoints behind bootstrap's trust callback. Distinguish deterministic service lookup from LLM discovery; resolve names separately.

Confine the action constant, connections and Binder describe/invoke/cancel/success/failure methods to the adapter. Document exported-service manifest wiring; generated Binder visibility remains an Android constraint.

Expose `CapabilityService` capabilities, authorization, consent and consent-change hooks. Put bind/destroy handling in the adapter; move artificial delays to test fixtures. Internalize `CapabilityBindings.declaration`/`script` behind compilation.

## 4. Portable discovery

Move `Discovery.prompt()` and examples into `core/discovery-guidance.deal`; move response/reference validation and `DISCOVERED_TASK_INTENT` into `core/discovery-policy.deal`, and evidence/catalogue admission into `core/discovery.deal`. Delete Kotlin policy/prompt copies. Platforms supply facts, contracts, hashing and inference mechanisms.

Expose `api.discovery.discover(facts)` returning a future of nullable framework-issued suggestion. Rename internal frozen `DiscoveryInput` to `DiscoveryFacts`; internalize identity/serialization. Rename `factIds` to `supportingFactIds` atomically across prompt, response, handoff and consumers. Expose title, brief, supporting references and capability IDs. Route `request(suggestion,currentFacts)` through portable admission, expose cancellation and dispose discovery through `api.close()`.

## 5. Assistance integration

Move `AssistedTask` to `android/src/rendering/AssistedTask.kt` for lifecycle/content integration. Replace Compose card: `ui/assistance/suggestion.dealui`; Open/Dismiss behavior: `core/assistance.deal`.

Extract `TaskHelp` into `android/src/discovery/AssistanceController.kt`: discovery, generation, cancellation, activation and saved-experience reuse. Replace direct app dependencies with injected settings, grants/trust, saved-link storage, Inspect navigation and composable proposal-review callback. Consolidate overlapping admission checks; carry lifecycle fencing, status, errors, retry and dismissal into the controller/view split.

## 6. DEAL/Deal UI composition

Replace maintained Kotlin-generated bodies in `FragmentComposition.kt`/`BehaviorCandidate.kt` with `ui/fragments/<kind>.deal` and `.dealui` for all eight fragment kinds. Move shared algorithms into checked `ui/fragments/support/*.deal`.

Move formula parsing, configuration validation, dependencies and composition policy into `core/composition/{formula,validation,compose}.deal`. Use compiler-backed structural instantiation/namespacing, not unchecked text substitution. Derive shape adapters from `ui/fragment-contract.json`; remove duplicate schema definitions.

Reduce `ExperienceCompiler.kt` to declaration derivation, staging and compiler invocation. Retire replaced generators; isolate custom sections. Materialize once, then use canonical-source exact edits. Structural composition and native-model generation require tooling verification.

## 7. App responsibilities and optional extension

Split Organizer capabilities into app-owned `OrganizerCapabilityContract.kt`, thin `OrganizerCapabilities.kt`, `ProposalService.kt` and `ProposalStore.kt`. Remove `Synthetic demo` from Organizer fact text.

Optionally add semantic item-order/emphasis proposals with app validation/application, not native-view manipulation.

## 8. Verification

Test public API boundaries, shared discovery and composed runtime behavior, authority, cancellation, freshness, restoration and exact recovery. Verify installed bytes before inference; retain failed measurements. Delete superseded implementations, not their functionality. Commit repositories separately; no push.
