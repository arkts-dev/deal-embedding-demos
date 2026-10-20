# Four-scenario delivery

## Result

**Build → live inference/network → checked execution works for all four scenarios. Full product acceptance does not.** Six app APKs plus tests were built with explicit Cerebras selection and installed together. Genuine fresh discovery/generation was exercised; the final installed builds were then exercised by reopening those saved tools, making real provider reads and retaining edits after force-stop. Saved replay is not fresh inference.

| Scenario | Demonstrated | Remaining failures / limits |
| --- | --- | --- |
| Organizer → Rental | Nano tip → Open → Cerebras → checked UI; actual equipment and quote calls; separate stand/cable €13, deposit €50; budget edit/recalculation and cold retention | Required package-versus-separate comparison omitted; unlabeled result amounts. Not accepted. |
| Todo → Pizza | Nano tip → Open → Cerebras → checked UI; actual menu and delivery calls; quantity/budget controls and retained edits | Claims zero vegan availability despite actual menu; €5 displayed as total. First generation failed keyword member syntax; an explicit second attempt compiled. Not accepted. |
| Calendar → Vehicle | Fresh acceptance-event tip/generation; actual simulated estimate: 12 km → 29 min; event at 16:00 → departure 15:16, inspection start 15:06 with 15 min unloading/10 min inspection | Edited start 16:05 gives departure 15:21 and inspection 15:11 after explicit Calculate; retained cold. Recalculation requires that action. Visual review rejected header crowding/result hierarchy. No general semantic reliability claim. |
| Calendar budget | Fresh Nano/Cerebras generation and checked local computation from €180/€197.50; correct €17.50 over result demonstrated | A later generated candidate omitted suitable numeric bounds; its edits collapsed to zero. Older retained tool computes correctly but says only “Difference”. Not accepted as a consistently correct/clear fresh result. |

No booking/order/proposal writes were exercised for acceptance. Known failures are retained, not patched out of accepted generated source. No automatic live-inference retry or silent backend fallback was introduced. Semantic admission beyond compilation remains missing.

## Fixed mechanisms

- DEAL emits valid else-if chains without a final else.
- DEAL UI offers settled checkpoints; pending effects preserve the prior checkpoint instead of causing false action failure.
- Embedding persists checked source as lossless Base64 UTF-8 in preferences. Previously XML serialization could change trailing whitespace after a source-ending newline; the exact-source digest then correctly rejected the draft on process reload. Legacy source remains readable; no trimming or digest bypass.
- Organizer explicitly opts into framework draft retention, without app-owned generated state copies.
- Native IntField rendering now honors pack defaults 0/999/1 when min/max/step are omitted. Generation guidance separately requires explicit bounds suitable for supplied values; default 999 is not a valid euro-cent budget ceiling.
- Rental rejects empty/malformed/unknown/duplicate baskets, invalid instants and component-stock overcommit; invalid input is not a free available quote.
- Nano output budget was raised based on observed truncation. Failures and capture-bound Surface evidence remain inspectable.

Orchestration, state transitions and policy remain behind the compiled DEAL/DEAL UI protocol. Kotlin supplies mechanisms, native domain records/authority and rendering, not hard-coded generated workflows.

## Reproduce

```sh
JAVA_OPTS=-Xmx768m DEAL_GENERATION_BACKEND=cerebras \
  tools/build.sh calendar organizer todo rental pizza vehicle tests
# Install each completed build/*.apk, then start the existing gateway:
python3 tools/generation-gateway.py --backend cerebras --chooser jev
adb reverse tcp:8787 tcp:8787
python3 tools/setup-acceptance-demo.py
```

Setup adds labelled synthetic Organizer/Todo/Calendar facts, preserving existing records, saved tools, grants and provider sharing. It requires initialized domain stores, installed test APK and ordinary Calendar permission. It never enables authority. Enable only desired consumer grants/provider sharing and generation disclosure explicitly through Settings; enable capture in Inspect before inference if source/replies are required.

Entry points on the prepared device:

- Organizer → Overview → Review requirement for **Vocal kit comparison** → tip/Open or retained tool → **Get quote**.
- Todo → **Acceptance food planning** → tip/Open or retained tool → **Load Menu & Delivery**.
- Calendar → **23 Oct 2026 → Acceptance departure** → tip/Open or retained tool → **Calculate departure**.
- Calendar → **23 Oct 2026 → Acceptance budget** → tip/Open or retained tool. The existing zero-valued failed candidate is preserved, not reset to fabricate a pass.
- The distinct older **Budget review reverify** event retains working arithmetic with an ambiguous “Difference” label; it is not a replacement for fresh acceptance.

Changing facts or deliberately invoking explicit retry is a new attempt, never evidence that the previous failure passed. Reopening unchanged saved assistance rechecks source without inference. Provider reads happen only on generated actions, not to help discovery/generation/checking/mounting.

## Verification and evidence

Final build and installation completed for all seven APKs. Passed: canonical API/boundary checks, generation guidance, build-cache/gateway tests, JS session tests, device Guidance, RentalRead, Discovery, Inspection, Cleanliness, GenerationGateway and RemoteCapability. Compiler baseline: 449 passed, none failed. RemoteCapability executes 1,005 real Binder calls; that is infrastructure coverage, not semantic acceptance.

`ColdDraftInstrumentation` runs separate write/read invocations with force-stop between, checks exact newline-ending source and retained value 18100. Renderer-default device checks verify actual enabled states and +/- payloads with omitted bounds, explicit enabled and explicit disabled. Four final saved-tool replays retained edited values/results; these do not erase the failed budget candidate.

Private evidence stays in ignored `build/delivery/`: `build-release.log`, `install-release-*.txt`, `apk-sha256.txt`, `release-*.txt`, `release-live-replays.json`, provider/source/diagnostic records and event exports. Actual screen captures are in ignored `build/assistance/audit/delivery-*`. Earlier failed attempts remain available there.

Actual screenshot review was performed through the configured image-review tool for all four final saved screens. **No visual acceptance:** result labels/hierarchy and crowded Calendar header remain defects. Review suggestions about forcing every app to one palette are not accepted: preserve each native app's theme. External status-overlay badges are not generated task UI. Pixel critique does not establish execution or semantic correctness.

This delivery establishes the requested working end-to-end infrastructure and committed fixes, with semantic/presentation failures explicitly outstanding; it does not establish four accepted product scenarios.
