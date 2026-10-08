---
kind: research
id: deal-studio-comparison
created_at: 2026-10-08T21:42:00Z
updated_at: 2026-10-08T21:42:00Z
status: inspected-with-limits
references:
  - https://github.com/egavrin/deal-studio/tree/f47d86134f824d5082f6f3e960e6821ebc1dbc81
  - .deus/research/llm-source-generation.md
  - dependencies/deal-embedding/core/generation.deal
question: What additional DEAL Studio ideas should inform deal-embed, and how do they change the architectural and product priorities?
mode: local
provider: gh-api-source-inspection
sources:
  - https://github.com/egavrin/deal-studio/blob/f47d86134f824d5082f6f3e960e6821ebc1dbc81/docs/research/2026-09-04-deal-generation-latency-study.md
  - https://github.com/egavrin/deal-studio/blob/f47d86134f824d5082f6f3e960e6821ebc1dbc81/docs/ui-first-v20-systemic-acceptance-2026-09-24.md
  - https://github.com/egavrin/deal-studio/blob/f47d86134f824d5082f6f3e960e6821ebc1dbc81/app/src/main/java/com/offlineassistant/app/generatedapp/UiFirstGeneratedAppCompiler.kt
  - https://github.com/egavrin/deal-studio/blob/f47d86134f824d5082f6f3e960e6821ebc1dbc81/app/src/main/java/com/offlineassistant/app/generatedapp/DirectGeneratedAppCompiler.kt
  - https://github.com/egavrin/deal-studio/blob/f47d86134f824d5082f6f3e960e6821ebc1dbc81/tooling/deal-ui-pack/deal-studio-v20.semantics.json
  - https://github.com/egavrin/deal-studio/blob/f47d86134f824d5082f6f3e960e6821ebc1dbc81/tooling/deal-android-bridge/toolchain.lock
  - https://github.com/egavrin/deal-studio/blob/f47d86134f824d5082f6f3e960e6821ebc1dbc81/app/src/main/java/com/offlineassistant/app/generatedapp/CanonicalGeneratedAppRefiner.kt
  - https://github.com/egavrin/deal-studio/blob/f47d86134f824d5082f6f3e960e6821ebc1dbc81/app/src/main/java/com/offlineassistant/app/generatedapp/GenerationCapabilityContracts.kt
gaps:
  - Source and tracked evidence inspected; Studio was not built or run during this research.
  - The toolchain-locked streaming-compiler revision was unavailable through the public repository API.
  - Studio's held-out functional and latency promotion gates remain open in tracked evidence.
  - Model, protocol, runtime and task differences prevent a causal latency comparison with our Rental experiments.
---

# Finding

The important additional idea is **compiler-owned reusable construction and typed bindings, with model-authored residual behavior**, not another native-to-DEAL relocation. Our current split is a trusted catalogue template versus complete source-pair generation. Studio investigates an intermediate route: checked UI contracts and ordinary control behavior supplied mechanically, with the model deciding presentation and genuinely task-specific logic. Canonical DEAL/Deal UI source remains the executable artifact, not a new workflow format.

This strengthens the case for reducing the model's required work and repair scope. It does not establish that UI-first or source-free constructor tools should replace our demonstrated production route.

# What was observed

Used `gh repo view`, commit/tree/contents APIs and commit history to inspect Studio main at `f47d86134f824d5082f6f3e960e6821ebc1dbc81`, including executable coordinators, bridge, pack semantics, restoration/refinement, tests and recorded experiments.

The README describes older sequential construction and contains stale pack/product statements. Current `GeneratedAppStudioViewModel.kt` invokes `DirectGeneratedAppCompiler` for the direct route; that adapter runs a compiler-hosted raw generation transaction and rejects declared capabilities. `UiFirstGeneratedAppCompiler` is a separate route. Coherent v20 is build-option-controlled and false by default. Its admission verifies source, presentation and AppInterface identities, checks capability subsets, recompiles the pair and probes initialization. The preview is explicitly provisional; final source need not preserve its ABI.

The UI-first v20 semantics descriptor maps scalar `onChange` payloads to matching value properties. Tracked systemic experiments explain why compiler-owned controls and omission of unused optional events reduced business obligations and residual failures. Final familiar host-free samples became runnable in roughly 8–10 seconds, but failures remained in earlier samples and the frozen 40-request comparison was not completed. The checkpoint's workout success was final-APK replay of an earlier accepted pair, not fresh generation on that APK.

The September latency study reports six paired behavior-only runs: compiler-guided generation had 100% compiler validity versus 83.3% for direct-plus-patch, but was slower at the median (12.468 versus 11.256 seconds). Coarse compiler transactions later improved two exploratory samples. Whole-file regeneration performed poorly in a separate one-run comparison. These observations support an experiment, not a proven general speedup.

The Studio bridge uses a bounded synchronous AST interpreter, unlike our production DEAL-to-JS sandbox and asynchronous cross-app effects. Inference is cloud-hosted; compilation/runtime orchestration occurs on the phone. This is not evidence of deployed offline on-device LLM inference.

The toolchain lock pins streaming compiler `c4fb2452d467719c35abb4fea703cd2e65ca8d64`; public GitHub API lookup of that revision failed. The accessible streaming README corroborates scoped construction/repair concepts but was not used as proof of that unavailable revision's implementation.

# Absorb, in priority order

1. **Repair deltas, not entire source pairs.** Preserve accepted work, send compact diagnostics and a bounded target, stop repeated identical candidates. Start with an exact base-digest-bound patch experiment through our unchanged checker, rather than importing a constructor engine. Semantic repair permissions and dependency cones ultimately belong to compiler APIs, not ad hoc Kotlin editing.
2. **Compiler/Deal UI-owned ordinary bindings.** Avoid asking a model to invent payload plumbing, basic editable-value state and unused event handlers when checked pack semantics already determine them. Extend only in response to a demonstrated form/selection task; do not claim current scalar-control machinery solves async catalogue business logic.
3. **Measure the whole denominator.** Separate source-generation validity, repair cost, runtime initialization, meaningful checked preview, first rendered preview and request-relevant interaction. Record model/protocol/token usage and p50/p95 including failures. Our correlated receipts are useful but current opaque response transport lacks this model-cost attribution.
4. **Bound artifact identity to versions.** Compiler/UI/pack/renderer tuple, source-pair digest and state revision are reusable ideas for trustworthy replay and future refinement. Existing cache hashes and candidate catalog revisions cover different concerns.
5. **Keep previews inert and provisional.** A checked structure can offer visible progress, but never fabricate catalogue availability, trigger capabilities or count as useful completion. It should follow latency reduction, not conceal minutes of business generation.

# Do not absorb indiscriminately

Do not copy Studio's synchronous interpreter, much broader component pack, hardcoded platform Host* controls, domain-shaped generation advice, HTML mode, model escalation policy or general syntax wish list. Our differentiator is discovering independent providers and completing a useful, authority-controlled cross-app flow. Studio's familiar host-free control samples do not validate that boundary.

Do not enforce early frozen presentation/ABI as an architectural invariant merely because the older ADR did. Current coherent revision tests explicitly allow ABI changes while requiring final identities and checking. Preview binding failures are evidence that presentation-first commitment can overconstrain business generation.

# Changed big picture

The core/UI/host boundary is still appropriate. The larger bottleneck is now the **generation surface**: how much application boilerplate, coordination and repair context the model must emit. Our repaired Rental sources establish possibility; Studio reinforces that possibility alone does not establish reliability, latency or intent fidelity.

The next useful direction is a bounded A/B against our retained Rental contract: compact source repair versus full-pair regeneration, then one genuinely different demo using reusable typed bindings. Keep choice-first and source fallback; do not promote an unmeasured UI-first engine. Native function-level authority remains a separate issue—Studio's explicit legal capability subset supports that direction but does not automatically fit our module-wide discovery/grants.
