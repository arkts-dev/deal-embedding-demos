---
kind: research
id: organizer-assistance
created_at: 2026-10-09T15:06:00Z
updated_at: 2026-10-09T15:06:00Z
status: implemented-with-limits
references:
  - .deus/design/organizer-assistance.md
  - .deus/research/unified-logging.md
  - .deus/research/editable-rental-generation.md
  - .deus/research/deal-studio-comparison.md
question: Does requirement-led native assistance replace the developer front door while preserving real execution and engineering visibility?
mode: local
provider: host-agent
sources:
  - android/apps/organizer/src/OrganizerActivity.kt
  - android/apps/organizer/src/AssistanceDesign.kt
  - android/apps/organizer/src/WorkspaceScreen.kt
  - android/apps/organizer/src/DiagnosticsActivity.kt
  - android/apps/organizer/src/OrganizerCapabilities.kt
  - dependencies/deal-embedding/core/catalogue-source.deal
  - dependencies/deal-embedding/core/generation-guidance.md
  - dependencies/deal-embedding/android/src/rendering/DealRenderer.kt
  - android/tests/AssistanceInstrumentation.kt
  - tools/verify-organizer-assistance.py
  - tools/verify-workspace-clarity.py
gaps:
  - Fresh live-model generation has not been rerun; changed guidance and production request instructions are not live-model validated.
  - Saved AI source retains its old wording; it is deliberately not silently rewritten.
  - Native UI hierarchy assertions and captures are not an independent visual-design or comprehensive accessibility review.
  - Logging remains bounded and may omit events or artifacts; no provider-internal telemetry or lossless audit guarantee.
---

# Organizer assistance evidence

## Implemented journey

The overview prioritizes identifiable outstanding requirements. Find equipment enters the supported missing Sound/Backline requirement directly, rather than finding an old workspace. Optional budget/preferences request an editable shortlist; unconstrained comparison stays catalogue-eligible. Users never select the generation mechanism. Production remains the existing real chooser/source/compiler/sandbox path.

The request surface uses Material inputs and a deliberate Find options action. Pending preparation disables submission and conflicting input, displays a spinner and permits cancellation. Failure preserves prior assistance. Returning through the same requirement resumes a mounted instance; cold opening checks retained source without inference or automatic stock reads. Change requirements remains an ordinary task action. Unsupported concerns do not acquire decorative integration buttons.

One Inspect entrance exposes scoped/global activity, actual provenance, exact source, native disclosed request/failure, capture controls and retained workspaces. Historical logger loss remains visible. Correlation includes generation events before workspace mounting through their shared trace. Reads are bounded and missing terminal events are explicitly not success.

Native review now shows the selection associated with its invoking workspace, not every historical preparation. The association is supplied by the serial native invocation context, not generated source or a global latest-record guess. Preparation persistence checks its actual commit result. Review says Not booked and never changes coverage.

## Visual and component changes

AssistanceDesign defines quiet native surfaces, a small spacing/shape rhythm and Material hierarchy. Organizer no longer uses continuously animated backdrop/heroes. Renderer hero text is restrained and action/step targets are at least 48 dp. The demonstrated budget stepper exposed a real gap: a cents-valued integer displayed 2000 rather than euros. IntField now has presentation-only format euro-cents; typed payload, limits, step and DEAL validation are unchanged. Authored fixture displays €20.00. Generation guidance requests the same representation.

Catalogue source has friendly task/error copy and an observable disabled Load options button alongside progress. Changes are checked against 16 explicit revised source-pair expectations derived from the retained pre-migration baseline, not an automatically blessed new output. Guidance serialization hash was deliberately revised for presentation and format contracts.

OpenDesign was inspected at 802708f6c9f294347ef777b1fda49b9cbe26ef72: HomeHero submission/loading behavior, current scenario tests, shared tokens, and curated Material guidance/evidence. Current tests explicitly retire an older scenario rail; it was not copied. Studio f47d86134f824d5082f6f3e960e6821ebc1dbc81 was reinspected for result hierarchy, collapsed details, busy-disabled Compose controls and explicit pack contracts. No web engine, Studio renderer or broad widget pack was imported; native mechanisms remain independently implemented.

## Direct verification

Actual Organizer and test APK builds and installs completed. Catalogue, Workspace, UiLifecycle and Logging instrumentation returned success on the device. Catalogue still checks eight issued presentations and failure ownership. Workspace includes native review-identity isolation and currency/request decisions. Logging still exercises real storage failure and incomplete-history recovery.

Frozen native presentation tests passed separately in light mode and dark/grayscale with enlarged text, restoring captured settings. They assert a single Inspect entrance, absence of technology/provenance badges from ordinary surfaces, concise failure/review language, disabled submission/input during pending work, spinner and cancellation. These are explicitly presentation fixtures, not model calls.

The requirement-led device journey used authored test source checked through the real toolchain/sandbox against discovered contracts and the live requirement's exact period. Actual Compose quantity editing, explicit real Rental read, €16.00 option selection, native review, Inspect source/activity, Back/resume and cold recheck passed. Source and Rental persistence remained unchanged; the replay produced 106 observed events and zero model/choice/source calls. The temporary source was deleted and its prior requirement link restored after testing.

Four authored editable behavior cases passed validation, quantity/budget/search, empty and permission-denial/recovery tests. Unchanged retained full-strategy AI pairs were also replayed separately; they are not rewritten fixtures. Core policy parity (11 fixtures/132 decisions), compiled-core expectations, gateway observation mocks, boundary and build-cache checks passed.

## Failures and limits

Early failures were retained: stale Refining status assertion, a removed navigation callback still referenced, UI-automation checking enabled on a Text child rather than its clickable parent, and a test toolbar overlapping system status bars. These were corrected and the affected passes repeated. A device UI dump was killed; another native replay was disrupted by starting instrumentation concurrently. Neither was counted as a completed journey; the serialized subsequent replay passed.

No fresh inference was spent. Consequently, the revised ordinary request's actual live-model route, source acceptance and resulting copy remain unproven until an explicitly budgeted generation pass. Existing accepted AI artifacts may still show older technical prose. Inspect is a bounded observability facade over the existing unified logger, not provider-internal tracing or a lossless history platform. UI XML and saved captures establish hierarchy/control facts but do not substitute for human assessment of visual polish.
