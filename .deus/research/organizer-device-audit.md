---
kind: research
id: organizer-device-audit
created_at: 2026-10-09T17:25:00Z
updated_at: 2026-10-09T18:40:00Z
status: verified-with-limits
references:
  - .deus/design/organizer-assistance.md
  - .deus/research/organizer-assistance.md
sources:
  - android/apps/organizer/src/OrganizerActivity.kt
  - android/apps/organizer/src/WorkspaceScreen.kt
  - android/apps/organizer/src/DiagnosticsActivity.kt
  - android/apps/organizer/src/OrganizerCapabilities.kt
  - dependencies/deal-embedding/android/src/rendering/DealRenderer.kt
  - dependencies/deal-embedding/android/src/capabilities/CapabilityRegistry.kt
  - dependencies/deal-embedding/core/generation-guidance.md
  - tools/verify-assistance-live.py
  - tools/verify-renderer-defaults.py
  - tools/verify-workspace-clarity.py
question: What actually works and fails when the complete Organizer screen family and fresh catalogue/source journeys are exercised through device controls?
mode: local
provider: host-agent
gaps:
  - This is a bounded screen/control and scenario audit, not proof over every possible input, future model response or hardware failure.
  - The harness cannot visually interpret screenshots; hierarchy/control checks and retained captures do not constitute independent human visual-design certification.
  - Generated applications are not guaranteed to meet arbitrary future tasks; source acceptance needed repairs in both fresh source workflows.
  - Logging is bounded; capture omissions, rotation and prior recovered loss remain explicit. The catalogue exchange artifacts were evicted; retained event metadata/digests and device captures establish observation, not continued availability of its full prompt.
  - Some retained first-generation copy remains verbose. The linked native shell no longer duplicates the generated task heading; unlinked artifacts retain their explicit native title.
---

# Device audit

## Fresh model demonstrations, not historical replay

Three fresh production workflows were initiated through native request controls. The gateway counted three chooser admissions and six source/repair admissions against the authorized eight-source/repair ceiling. No route bypass or authored substitute was used. Each source workflow retained the normal three-attempt bound; the session budget did not silently change retry policy. A subsequent deliberate request and explicit Retry were rejected before inference by the choice ceiling. This exercises genuine transport-failure recovery, not source-attempt exhaustion; the latter has separate deterministic instrumentation/frozen presentation coverage.

1. Unconstrained boom-stand request: real chooser response, validated catalogue selection, DEAL synthesis, UI/DEAL compilation and mounting. One choice, zero source calls. Choice completed in 9043 ms. Actual catalogue rows were €8 stand and €12 package. Selection and local review passed. This is model-directed catalogue construction, not model-authored source.
2. €18 total budget and adapter preference: chooser returned unavailable, then three actual source calls. First rejected for files-array envelope; second for a non-exported State; third checked and mounted. Source durations 268906, 101611 and 101524 ms. Real stock read, selection and native review worked, but source repeated verbose dates/UTC and excessive structure. Useful functionality did not establish presentation acceptance.
3. Same editable task after correcting explicit envelope/export/presentation guidance: chooser returned unavailable, then three source calls. Rejections were borrowed-state helper/mutation UI2051 and UI2050, followed by accepted source. Durations 163642, 91354 and 129511 ms. Final model reply and retained source match except host-normalized outer whitespace. No handwritten edit was substituted. Concise local period, euro budget, quantity/search controls, real read, matching row, €8/€16 review, denial/recovery, budget empty/invalid/re-enable, Back and cold reopening were exercised.

These are two accepted source workflows out of two attempts, both third-pass acceptance, zero first-pass acceptance. They do not establish general reliability. The first remains a presentation counterexample, not silently rewritten successful evidence.

## Demonstration paths

Find equipment means a fresh request when none is retained. View equipment options means returning to accepted assistance. Change request is a visible task action rather than hidden under a technical menu. Continue preserves current options until replacement succeeds. Leave optional constraints empty for the catalogue-eligible case; enter budget/preferences requiring editable behavior for the source case. Inspect establishes the actual route, not those inputs alone.

Inspect → Dialogue shows actual choice/source requests and replies as they arrive. Opening an exchange decodes argument-array wrapping for readable content; exact artifacts and metadata remain retained separately. It is not hidden reasoning. Full content requires capture before a call, with explicit private-content confirmation. The caller can use Inspect during generation and see disabled destructive workspace actions. Failures show a Retry button; manual retry is a new deliberate call, never an automatic exhausted-workflow restart.

## Bugs reproduced and fixed

- Omitted enabled tree props were treated as false, making a retained Load options control inert. Renderer now honors enabled=true defaults for Button, Option and inputs, preserving explicit false. Sparse portable-tree Compose tests verify actual clicks/string/integer payloads for omitted/true/false.
- Plan edits mutated the same ShowState object. Checks and agreement/filter displays could remain stale even though persistence changed. Publish a fresh plan identity; actual Record check/Undo, edit Save/Cancel and agreement/undo then visibly update.
- Outstanding covered-but-unchecked requirements vanished between Needs attention and Ready. All not-ready requirements remain actionable.
- Missing filter included conflicts and empty filter results had only blank act headings. Missing/conflict filters are distinct; unmatched filters show an explicit empty message.
- Checklist By person/By type were effectively inert modes. They now sort by act/department and show relevant grouping copy; deadline remains deadline-ordered.
- Record agreement marked unrelated Backline requirements covered while overlapping windows remained. It now records only the resource's allocations and explicitly retains the unresolved overlap, with Undo agreement.
- A missing negotiable Hospitality group displayed Complete through vacuous mandatory-only matching. Missing negotiable needs now keep the group incomplete.
- Requirement quantity editing accepted zero/negative or silently refused deletion. Quantity drafts validate 1–999 and disable Save; cancellation restores the prior value. Edits reset verification appropriately.
- Bottom navigation left the request covering the newly selected tab. Leaving a nonpending request now clears it.
- Department Open ignored its department and could retain a different act filter. It now opens scoped Riders with an explicit Show all departments action.
- Cold reopening exposed a durable earlier review despite initializing a new UI with no selection. Saved preparation is now withheld until a new staging result; same-process live return may resume only its still-current review state. Cold-source verifier explicitly checks that no Review selection entrance appears before a new read/selection/staging.
- Native review retained stale selections after edits/load failure. A new application action hides that review entrance until a new native staging result; repeated identical staging is distinguished by its unique native reference.
- JSON-form local descriptors were dumped into ordinary review. Recognized quantity/period/location/act are now readable native fields; original descriptor is unchanged.
- Permission Retry retained a dead Binder after the provider process restarted. Only a new explicit request rediscovers dead endpoints, requiring compatible active contracts and preserving grants; no failed operation is automatically replayed. Real denial → provider restart/consent restoration → Try again succeeded without Organizer restart.
- Deleting the authored fixture through Inspect removed its temporary requirement link before test cleanup, so cleanup failed to restore the prior assistance. Cleanup now restores the saved previous link when absent as well as when still pointing to the fixture, without overriding a replacement; explicit audit recovery rechecked the already-known source identity without inference.
- Global retained-workspace iteration was unstable and made the current assistance difficult to locate. Inspect places its current workspace first, with remaining titles ordered; actual authored-fixture deletion through confirmation was verified and its prior requirement link restored.
- Inspect limited unrelated events before filtering dialogue, allowing provider/action traffic to hide still-retained model exchanges. Dialogue now filters model events first and explicitly caps the latest 200 model records, still honestly reporting unavailable artifacts.
- Inspect event metadata buried model content. Dialogue drilldown now puts readable actual exchange content before exact technical fields. It retains privacy/omission labels.

## Tapped surfaces

Overview outstanding rows, checked/Ready behavior, timeline and department entries; Riders All/Missing/Conflicts/Awaiting approval, both acts/each act, group collapse/expand and all eight requirement detail families; requirement edit fields, mandatory switch, invalid quantity, Save/Cancel, check/undo; Resources Sound/Backline/Hospitality, agreement/undo; Fulfilment type/person/deadline and detail entry; compact request budget/preferences/keyboard, change confirmation/keep/continue, disabled pending states, cancellation, operational failure/Edit request/Retry; catalogue and model-authored options, selection, preparation, local review/back, live reuse and cold reopen; Inspect Activity/Dialogue/Source/Workspaces, scope toggle, raw source/exchange drilldowns, capture confirmation/cancel/on/off, diagnostic clear confirmation/keep and deletion confirmation/keep.

Retained old source is deliberately not rewritten. Source-specific verifier controls use the actual compiler-issued labels rather than assuming older/newer Prepare labels are interchangeable. Early automation failures from offscreen controls, duplicate content descriptions and overlapping instrumented activity launches were not counted as product passes; affected journeys were serialized and repeated.

## Acceptance matrix

| Frozen contract | Observation | Verdict |
|---|---|---|
| Requirement → options → selection → native review → return | Both fresh routes; serial accepted-source regression; actual prices and selection | Pass for demonstrated equipment tasks |
| Real chooser/source/check/compiler/mount/provider boundaries | Fresh traces, actual model artifacts, compiled checked pairs and real Rental reads | Pass for owned demonstrated boundaries |
| Task language; no ordinary source badges/codes | Native hierarchy checks and revised accepted-source UI | Pass for checked states; first generated source retains verbose counterexample |
| No duplicate native/generated headings | Linked native shell supplies Back/Change request without repeating the generated title; revised source result Section occurs once | Pass in final ordinary revised-source journey; exact source untouched |
| Pending/disabled/cancel/error/retry | Frozen native states, actual generation progress, provider denial/recovery and gateway failure | Demonstrated states pass; no proof for arbitrary generated effects |
| Inspect/dialogue/source/privacy | Actual live request/reply drilldowns; captured source attempts retained privately | Pass with bounded eviction/loss; catalogue exact exchange no longer available in the logger |
| Explicit reads, Not booked, no coverage/provider writes | Cold/no-read checks, stage references, plan/provider/source equality | Pass for demonstrated journeys |
| Native visual contract/light/dark/grayscale/large text/keyboard | Native frozen scenarios and actual captures/control interaction | Behavioral evidence; independent visual certification unavailable |

The repeated-title shortfall was corrected in the native shell, not by stripping arbitrary generated text or rewriting retained source. A final ordinary Riders → Boom stand → View equipment options journey confirmed one result title, €8 review, live resume and no stale cold-review entrance. Fixing guidance has not retrospectively corrected first-source verbosity. No universal or fully exhaustive acceptance claim is made.

## Regression evidence

Actual APK builds/installs and Catalogue, Workspace, UiLifecycle, Logging device instrumentation passed. Explicitly authored fixture journey remains a separate no-model test and produced 132 events with source/provider equality, Resume and cold recheck. Sparse renderer controls passed three modes. Native frozen states passed light and dark/grayscale/enlarged-text, restoring captured settings. Core source-pair/policy checks, boundary, gateway/call-budget and build-cache checks passed. The final editable model pair passed the reproducible live-source verifier: €8/€16 preparation, stale review removal, budget empty/invalid recovery, actual search edits, repeated identical preparation and unchanged source/provider state. Earlier verifier swipes crossed text/keyboard regions and missed offscreen Load options; content-gutter scrolls fixed the harness and a complete pass followed.

Pack Boolean defaults were also found classified as other in build-extracted guidance facts. Extraction now retains Boolean type and core composition honors it; an explicit compiled-guidance regression checks enabled=true. This final guidance-fact correction was compiled/tested without spending further model calls.

Final authored ordinary-navigation rerun passed with 132 observed events and no inference; cleanup restored the original fresh-source requirement link. Full content capture was turned off after the audit. Exact live captures, UI XML, model artifacts, call counts and failures remain in ignored build evidence, not this tracked document. No credentials or raw private runtime data are included here. Screenshots cannot be visually interpreted by this configured model, so visual polish is supported by hierarchy/control evidence, not a claimed human-equivalent visual review.
