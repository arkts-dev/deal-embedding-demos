---
kind: research
id: workspace-clarity
created_at: 2026-10-09T00:06:00Z
updated_at: 2026-10-09T00:06:00Z
status: verified-with-limits
references:
  - android/apps/organizer/src/WorkspacePresentation.kt
  - android/apps/organizer/src/WorkspaceScreen.kt
  - android/apps/organizer/src/OrganizerActivity.kt
  - dependencies/deal-embedding/android/src/bindings/ExperienceGenerator.kt
  - tools/verify-workspace-clarity.py
question: Can a phone user distinguish AI-authored source, catalogue templates, native review and generation exhaustion without relying on color?
mode: local
provider: android-native-surfaces-and-compiled-core-tests
sources:
  - android/tests/CatalogueInstrumentation.kt
  - android/tests/WorkspaceClarityActivity.kt
  - android/tests/EditableRentalInstrumentation.kt
  - dependencies/deal-embedding/android/src/storage/ExperienceStorage.kt
  - tools/verify-organizer-catalogue.py
gaps:
  - Bed mode was interpreted as bedtime-style dark/grayscale use; Android Bedtime scheduling, DND and Extra dim were not exercised.
  - Enlarged font was tested at 1.3, not every accessibility scale or screen size.
  - Screenshots were captured; no independent human visual review or TalkBack listening was performed.
  - No new live inference was used; failure and progress surfaces use deterministic test-only facts, with actual generation rejection independently tested through compiled core.
  - Previously saved source without provenance is labelled unknown rather than retroactively asserted to be AI-authored.
---

# Implementation

Native workspace headers now say AI-built workspace, Catalogue workspace or Saved-source workspace. Each has an explanation and Source details. Host-owned origin comes directly from the trusted generation result's route, not app source, model descriptions or title matching. CheckedCandidate/LiveWorkspace carry origin and attempt count; workspace storage retains them. Debug replay validates stored enum metadata, rechecks the source and defaults missing provenance to unknown. Retained LLM fixture replay explicitly records its known AI origin; authored fixtures are not relabelled AI-built.

The native review says Organizer review · native, with an explicit boundary and not-reservations wording. Prepared-count text has a polite live region. A solid high-contrast bordered header distinguishes the checked application surface below without relying on violet/green accents.

The request sheet keeps progress and exhaustion above its bottom actions. AI source writing/checking includes attempt numbers. Exhaustion, repeated rejected responses, template defects and operational interruption have separate typed presentation. No raw compiler diagnostic excerpt appears in the failure panel. User actions are Change request, Keep existing workspace / Back to plan and Build again · new run. New runs are explicit; generation is not automatically repeated. Cancellation now actually signals the running cancellation token instead of only hiding the sheet. Activity destruction cancels outstanding generation before releasing its host.

Money inputs are labelled Rental total budget (€) for rental goals, Total spending budget (€) otherwise. The spending constraint is distinct from generation attempts; generated accepted source was not edited to pretend a new generation result. Existing generated no-options messages remain intact.

# Direct evidence

Catalogue instrumentation passed actual trusted-core generation of template and AI-source candidates, verifying origin and persisted template metadata. Distinct invalid responses exhausted three source calls and produced GenerationRejected.ATTEMPT_LIMIT; identical responses stopped early with REPEATED_RESPONSE. Both preserved a healthy mounted workspace. Template failure remained a no-repair TEMPLATE_CHECK rejection. Native presentation assertions verified that failure text excludes diagnostic content and identifies the attempt count. Source checking without retained authorship remains unknown.

Test-only WorkspaceClarityActivity renders the same production RequestSheet, WorkspaceView and ReviewSheet. The native verifier passed labels, explanations/details, review separation, persistent exhaustion, Change request, explicit new-run action, progress and cancel control in normal mode and in dark + grayscale mode at 1.3 font scale. System settings were saved and restored, including in the failure path. Screenshots and accessibility XML were captured under ignored build storage. These are control/text visibility checks, not a full accessibility certification.

Five retained unchanged LLM source pairs passed Android replay after metadata changes. Real Organizer replay then showed AI-built workspace, used actual quantity/budget/search controls, read remote Rental, selected the €16.00 two-stand option and populated Organizer review · native. Rental persistent state remained unchanged. No inference or provider reservation/write was performed in this slice.

Catalogue, workspace-isolation, UI lifecycle and prior source replay instrumentation passed; compiled-core/source parity, policy, boundary and cache regressions passed. The consumer cache now explicitly fingerprints source-repair.deal, which the previous root list omitted.

# Verification failures retained

The new larger native header reduced the scroll viewport. The existing verifier's long swipes skipped quantity controls, then aimed above the content viewport. Reduced swipe distance and increased bounded search steps; no generated source changes. A subsequent attempt retained IME focus and typed a stray character when tapping Load options; replaced the prior Escape key dismissal with Android Back to close the keyboard, then unchanged-source native replay passed.

The first native details assertion expected predefined wording in the catalogue details dialog; the dialog correctly explains model-selected choices instead. Corrected the assertion to that semantic explanation without changing implementation.

# Limits

Provenance describes authoring route, not trustworthiness, correctness or write authority. Unknown legacy metadata stays unknown. Generation failure preservation applies to generation/check failures; runtime/host death remains a separate lifecycle case. The native header counts all local prepared history, not only preparation from the current workspace. Function-level authority enforcement and provider confirmation remain deferred. No extra inference budget was consumed.
