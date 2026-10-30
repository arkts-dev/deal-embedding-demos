---
kind: research
id: deal-v15-migration
created_at: 2026-10-30T15:10:00Z
updated_at: 2026-10-30T15:19:00Z
status: verified-with-limits
references:
  - dependencies/deal/docs/spec-v1.5.md
  - dependencies/deal-ui/docs/deal-ui-framework-design.md
  - .deus/research/framework-refactoring-plan.md
question: Does the Android embedding stack compile and execute using DEAL v1.5 only?
mode: local
provider: local-files-and-deterministic-tests
sources:
  - dependencies/deal-embedding/core/decoded.deal
  - dependencies/deal-embedding/core/composition/view-binding.deal
  - dependencies/deal-embedding/core/composition/lowering-codec.deal
  - dependencies/deal-embedding/tools/FragmentSyntax.java
  - dependencies/deal-ui/src/main/java/deal/ui/UiBorrowedValueChecker.java
  - dependencies/deal-ui/src/main/java/deal/ui/UiDealGenerator.java
  - android/tests/ProtocolInstrumentation.kt
  - tools/verify-generation-guidance.js
gaps:
  - No fresh inference or new timing cohort.
  - No visual acceptance or UI recipe enrichment.
  - Non-Android execution and the full compiler suite are outside this acceptance.
---

# Result

The maintained Android stack now targets DEAL v1.5: a TypeScript syntax subset with additional checked semantics, not unrestricted TypeScript. Production builders emit v1.5 manifests, checked Record extraction, inferred DEAL for-of bindings, Promise results and unknown-catch extraction. Optional absence remains undefined, distinct from null. Provider properties retain their wire names. No historical compiler/translation lane was added.

Dependency direction remains apps → embedding → Deal UI → DEAL. Portable composition, expressions, allocation, workflows, initialization and view policy remain in DEAL/Deal UI; native code supplies syntax, transport, staging, loading and rendering mechanisms.

# Changes and observed failures

- Decoded arrays preserve already-typed identity; unknown-descriptor arrays are element-checked into fresh typed arrays, never retagged. View-binding explicitly writes copied views/props back. A focused JVM probe verified typed identity, decoded copy/writeback, shared record-element identity, primitive arrays and mixed-element rejection. Builder arrays originate from typed create(), not decoded plans; no speculative writeback patch was added there.
- Provider codec lowering normalizes arrays before decoding leaves. Generated code uses inferred loop bindings and checked primitive/Record assertions. Formula wire key arguments remains unchanged despite renamed local bindings.
- Deal UI borrowed-value analysis follows assertions and inferred iteration elements. Regressions reject alias/loop mutation and permit read-only helpers. Declarative view has(...) remains supported and lowers to Object.hasOwn.
- Repair guidance no longer bans contextual from/property keywords or recommends contextual table-read typing. Parser-filtered lexer facts and structured offending context guide advisory feedback; compiler checking remains authoritative.
- Device Protocol testing exposed one missed async fixture result; its Promise<Done> correction passed in the final rebuild. Toolkit host testing also required its external declaration to use Promise<number> without async.
- An earlier catch/helper JVM failure was not reproduced by focused probes. Restructuring avoids the observed failure; no compiler defect or fix is claimed.

# Verification

Final coherent build produced Organizer, tests, Calendar, Vehicle, Todo, Rental and Pizza APKs. All seven installed APK hashes matched their build artifacts. No package-data reset or inference was used.

Final host checks passed: maintained behavior/formulas, selection, dependency boundaries, framework assistance, source agent, exact repair, protocol transport, generation guidance, exact public declaration comparison and toolkit. These exercise real production compilers and sessions with supplied plans or mocked external boundaries, not fresh model authoring.

Deal UI passed 144 checks, 429 runtime invariants and workspace tests. Six additional Python suites passed: generation gateway, transport, generation metrics, cohort collector, build cache and chooser.

Final device regressions passed: Discovery, Compact, Protocol, AssistanceShell, RentalRead, DiscoveryCalendar and Guidance. Scope includes injected discovery, maintained compilation/runtime, lifecycle/cancellation, isolated reset, provider validation, controlled Calendar confirmation, packaged async example and cold source-bound retention. Compact intentionally reports Android RESULT_OK (-1); other suites report 0. Assessment uses each suite's actual receipt, not one assumed result key.

The first separate six-app Settings/Inspect navigation check timed out after Calendar and Organizer passed; that attempt remains inconclusive for the remaining apps. The independent rerun passed all six apps: common Settings/Inspect, confirmed reset dialog and cancellation with data preserved. Screenshots support separate review, not visual approval.

Private build/test/hash/count receipts remain under ignored build/v15/. Earlier failures and historical timing cohorts were retained, not relabeled as current success.

# Prose scope

Whitespace-word counts for maintained noncompiler Markdown guidance: **20,787 → 12,472**, a **40.001% reduction**. The inventory includes unchanged API/grammar material, not only shortened files. Compiler documents, historical delivery/cross-app/selection-prototype reports and research are excluded. Historical evidence is unchanged. Editing constraints remain scoped; completion inventories were not inserted into AGENTS policy.

# Revisions and limits

- DEAL rebased android-current onto master; superseded shared-frame patch skipped. Final commit ec29e304 contains only two Java 21 Android compatibility files. Compiler documentation/tests were restored; extra compiler testing stopped.
- Deal UI: cba2e95.
- Embedding: e7c9491.
- Root migration commit contains this report and consuming app/tool changes.

Build/host execution, installed-byte identity, deterministic device semantics, fresh inference and visual approval are separate claims. No new inference/timing cohort, complete ordinary task-journey acceptance, visual redesign or other-backend certification is established. UI recipe enrichment remains deferred.
