---
kind: research
id: embedding-surface
created_at: 2026-10-13
updated_at: 2026-10-13
status: verified-selected-android-paths
references:
  - dependencies/deal-embedding/android/src/rendering/ExperienceSurface.kt
  - android/apps/organizer/src/OrganizerActivity.kt
  - android/apps/calendar/src/CalendarActivity.kt
question: Do standard demo hosts use a reusable framework-owned session/render surface rather than wiring the low-level protocol?
mode: local
provider: local-source-build-and-device
sources:
  - android/tests/SurfaceInstrumentation.kt
  - android/tests/ProtocolInstrumentation.kt
  - android/tests/ProtocolPolicyInstrumentation.kt
  - tools/verify-boundaries.py
gaps:
  - No fresh live inference or independent ordinary-user acceptance.
  - No exhaustive lifecycle-race or renderer-failure coverage.
---

# Integration

ExperienceSurface is framework code, separate from bootstrap. Its explicitly owned native adapter activates the accepted Experience and receives snapshots. Its Compose surface renders the tree, forwards compiler-issued widget events and records renderer acceptance after successful composition. This acknowledges renderer acceptance, not pixels displayed. No DEAL API changed; the low-level protocol remains available for custom adapters and trusted consumers.

Organizer and Calendar supply accepted experiences, placement, errors and lifecycle decisions. Their run/initial-next/observe/dispatch/published wiring and direct DealRenderer calls are removed. Optional surface hooks retain application date presentation, review invalidation and private diagnostic snapshots, not protocol scheduling. Organizer retains owners across navigation/review and closes them on deletion/destruction; Calendar explicitly closes the previous owner after successful replacement. Detaching composition does not close or delete source.

# Observed verification

Organizer, Calendar and tests built. API parity and boundary checks passed; the boundary test rejects manual protocol/render wiring in both demos. Android surface instrumentation passed actual Compose rendering and accessibility widget clicks, automatic publication, hide/show state retention, failed activation preserving an existing surface, explicit closure retaining saved source, and cold activation resetting without inference. Protocol and Organizer policy suites also passed, including synchronous permissions and explicit Load/select/prepare to Not booked review with fixture inference.

The initial test build collided with Android Activity.setVisible; renaming the test's Compose visibility state fixed compilation. That failed build is not counted as evidence. No fresh model calls were spent. These tests establish selected integration paths, not complete ordinary-user or production acceptance.
