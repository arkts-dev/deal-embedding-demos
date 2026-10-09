---
kind: research
id: workspace-lifecycle
created_at: 2026-10-09T01:00:00Z
updated_at: 2026-10-09T01:00:00Z
status: verified-with-limits
references:
  - android/apps/organizer/src/WorkspaceShelf.kt
  - android/apps/organizer/src/OrganizerActivity.kt
  - android/apps/organizer/src/WorkspaceScreen.kt
  - dependencies/deal-embedding/android/src/ExperienceRuntime.kt
question: Does the phone demo expose a practical build, use, leave, reopen and delete lifecycle rather than fixed presentation fixtures?
mode: local
provider: native-controls-and-android-instrumentation
sources:
  - android/tests/WorkspaceInstrumentation.kt
  - android/tests/CatalogueInstrumentation.kt
  - tools/verify-organizer-catalogue.py
  - tools/verify-workspace-clarity.py
gaps:
  - No new inference was used; previously accepted LLM source was exercised with real providers.
  - Local inputs and selection persist only while their isolate is alive, not across process restart.
  - Compact status currently maps native adapter progress strings, not a versioned typed progress API.
  - No general stale-context system or provider write confirmation was added.
---

# Corrections

The recurring 2/3 was a frozen presentation test value, not a universal model progress result. Production counts came from live source calls, but exposing compiler repair counts made the screen look like an internal benchmark. Main UI now shows Connecting, Choosing template, Building, Refining or Checking with an indeterminate spinner and Cancel. Exact historical attempts and repair reasons stay in Details. Tags are AI-built, Template or neutral Workspace, plus deal/deal ui; explanations are not repeated in the main header. Native review has Native and Not reserved tags.

The previous demo was wired to real generation and remote Rental, but its workspace lifecycle was incomplete: leaving a workspace destroyed saved source, app shutdown also forgot it, and no ordinary source shelf existed. Storage now separates releasing runtime resources from deleting accepted source. Explicit forget deletes; close and shutdown release isolates/dex only. Reopen uses stable identity and ordinary checker/current capability contracts without inference. Failed checking/mounting does not replace saved code. Successful mount persists before admission to the live registry.

Organizer exposes Workspaces with Resume for mounted state and Open for saved code. Back hides the workspace, preserving its live inputs and selection. Navigation returns to the native app instead of leaving an open surface covering every tab. Native back cancels generation or returns from review/workspace. Process restart lists accepted source, rechecks on Open and starts fresh application state; provider data is read only by explicit generated actions. Delete is confirmed and does not erase prepared review history. New build is explicit under Details for workspaces linked to current requirements, keeping the old workspace. Selecting the same unchanged requirement reuses its linked accepted workspace; changed quantity, specification or exact interval changes the native link key rather than silently reusing old assumptions.

# Evidence

Workspace instrumentation passed close/reopen with stable identity, full runtime shutdown followed by recheck/reopen, no initialization provider calls, explicit subsequent read, and deletion. Catalogue instrumentation passed actual typed exhaustion and provenance, and the presentation mapping tests. UI lifecycle instrumentation passed. Five unchanged LLM pairs had already passed behavior replay; three full-strategy pairs passed again during this slice.

The native editable verifier passed real Compose inputs, remote Rental, selected two stands at €16.00 and populated native review with provider persistence unchanged. It then navigated Back to workspaces and Resume, checking that quantity 2 was retained. After cold process restart it used the ordinary Workspaces/Open controls, checking original accepted source remained unchanged, quantity reset to initial 1, and no rows were silently fetched. No debug-only source selection is needed for those subsequent openings.

Normal and dark/grayscale 1.3-font native surface checks passed compact tags, Details, failure controls, spinner, Checking/Refining and cancellation control. These are deterministic rendering checks, not live model trials. Earlier aggregate commands timed out during repeated accessibility dumps, not model calls; tests were rerun in bounded individual modes. Device settings restoration is in a finally block, but external hard termination can prevent it; the test should not be killed while holding altered settings.

One shelf verification initially selected an older duplicate title and reopened the wrong saved pair. Distinguishing Resume (the live identity) from Open (saved identities) made the user lifecycle and test selection unambiguous. Final native lifecycle replay passed unchanged source.

# Limits

This is not a compiled-artifact cache, persistent generated-state migration or automatic background regeneration. Accepted source is the durable artifact. Live input state remains in Deal UI; restarting intentionally initializes it again. Archived debug experiment sources can appear in the shelf on a development phone. Development inference still requires the configured gateway/device reverse connection. Provider confirmation and function-level authority enforcement remain separate; the UI does not confer authority.
