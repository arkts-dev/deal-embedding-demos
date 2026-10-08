---
kind: research
id: llm-source-generation
created_at: 2026-10-08T21:31:08Z
updated_at: 2026-10-08T21:31:08Z
status: verified-with-limits
references:
  - dependencies/deal-embedding/core/choice-policy.deal
  - dependencies/deal-embedding/core/generation-guidance.md
  - dependencies/deal-embedding/core/generation-guidance.deal
  - dependencies/deal-embedding/android/src/compiler/ExperienceCompiler.kt
  - android/tests/SourceGenerationInstrumentation.kt
  - android/tests/fixtures/source-generation/quantity-budget.json
  - android/tests/fixtures/source-generation/single-budget.json
question: Can LLM-authored DEAL and Deal UI implement useful Rental demo behavior beyond the catalogue template, and which observed failures should drive the next changes?
mode: local
provider: local-source-and-android-device-tests
sources:
  - android/tests/CatalogueInstrumentation.kt
  - android/tests/SourceGenerationInstrumentation.kt
  - tools/verify-core-migration.js
  - tools/verify-organizer-catalogue.py
  - android/tests/fixtures/source-generation/quantity-budget.json
  - android/tests/fixtures/source-generation/single-budget.json
gaps:
  - Two related demo variants do not establish general source-generation quality or a reliable success rate.
  - Revised guidance still required source repair in both workflows; generation latency remains high.
  - Empty results and staging errors have generated UI branches but were not behaviorally exercised by this experiment.
  - Denial recovery used local grant revocation, not remote provider death or provider-owner consent revocation.
  - Provider confirmation remains unconnected; native review is local preparation, not a reservation.
  - Read/preparation-only prompt guidance does not enforce a capability subset for arbitrary fallback source.
  - Compiler classification is deliberately conservative and cannot prove root cause from a diagnostic alone.
---

# Contract and experiment boundary

The approved sequence was a small failure-contract correction followed by demo-driven real source-generation exploration, with observability and deterministic replay central. Production remains choice-first. Only the test ModelClient returns an unsupported choice to force the ordinary bounded source route. No DEAL or Deal UI implementation changes, new backend, workflow engine or production source-route switch were introduced.

Two related Rental cases require behavior absent from the stock template:

- **Quantity-budget:** two boom stands, at least two available units, total budget 1,800 cents. The stand qualifies at **€16.00** total; the package does not.
- **Single-budget:** one boom stand, total budget 1,000 cents. The stand qualifies at **€8.00**; the €12.00 package does not.

Both require user-triggered loading, stable item selection, a quantity/period/total-price descriptor for native review, honest non-reservation wording and recovery after a denied read. Generation/checking must not invoke application providers. Initial execution uses bounded local fixtures; accepted source is subsequently checked against actual discovered contracts and exercised through Compose and real Rental.

# Observed generation results

| Guidance | Case | Source calls | Checker outcome | Workflow duration |
| --- | --- | ---: | --- | ---: |
| Original | Quantity-budget | 3 | Exhausted on view syntax | 273 s |
| Original | Single-budget | 3 | Exhausted on view syntax | 263 s |
| Revised | Quantity-budget | 3 | Accepted on third source call | 245 s |
| Revised | Single-budget | 2 | Accepted on second source call | 264 s |

These durations include checking and, in the revised runs, interaction until the verifier's retry-control error. They are not isolated inference timings. There were **four real generation workflows and eleven source calls**, with **zero first-pass acceptances**. Initial failures were retained before making an evidence-driven guidance change and starting a separately named experiment round. No failed workflow was automatically repeated. No further inference was used to correct the harness, verify behavior, or perform native replay.

The revised accepted source was not manually repaired. The exact accepted pairs are retained as test-only fixtures, never as production fallback templates. Canonical identity is SHA-256 of UTF-8 `deal + NUL + dealui`:

- Quantity-budget: `598c5f6b6a824b4a7fcd0a86759550d9d099e2013cd4e4e1bee255fca5e14023`.
- Single-budget: `ba6a9c31380a048e0d7668b2722eb27fa1df23841f8dfc3f6ff2451b92cc3a37`.

# Useful outcome verification

Both accepted pairs passed Android sandbox replay with **zero source calls**:

- No application capability invocation during generation/checking/mount initialization.
- Exact disclosed interval forwarded on catalogue reads.
- Only the stand row admitted; total price correctly €16.00 or €8.00.
- Selection reflected in checked UI state.
- Exactly one preparation call with original title, provider, item ID/quantity, both period strings, total price and deadline.
- Visible preparation success and grant-denial error; restoring the grant and invoking the declared retry action recovered the catalogue.
- Two successful reads and one stage call per case; failed authorization did not execute the provider handler.

Saved-source replay through Organizer then used **actual Rental**, native Compose controls and native review. Both cases displayed the expected single row, published selection and populated review with the expected descriptor/price. Rental persistent state, excluding consent configuration, was unchanged. This proves useful preparation beyond the stock template, not provider confirmation or reservation.

# Failure ledger and surgical changes

| Observation | Disposition and ownership |
| --- | --- |
| Original view syntax used `if`/`If`, `$value`, then invalid event arguments; UI1013/UI1007/UI1009 exhausted repair | Added a literal pack-import/`When`/`Else`/`ForEach`/`payload` example to core-owned guidance. This is missing guidance, not an inherent DEAL limitation. |
| Original sources imported `ui` and used reserved `from` state fields | Inspection exposed further issues hidden behind the first parser errors. Guidance now states the literal pack import and safe application period names. Do not claim these were all independently reached checker diagnostics. |
| Revised quantity source rejected with UI2050 borrowed-immutable writes | Real model repaired helper-returned state aliasing; guidance explicitly calls for a fresh complete state. |
| Revised sources rejected nullable field accesses with E3003 | Diagnostic-driven model repair produced accepted source. No compiler semantics changed. |
| Revised single source used string-key table reads, rejected with E3007 | The existing separately reproduced checker defect resurfaced. Model repair used compatible typed reads; no upstream fix or bridge removal was attempted. |
| Generated component examples advertised empty actions for payload events | Core now derives a payload-bearing example from pack event facts. Payload-free buttons retain empty-action examples. |
| Both accepted workflows failed verification when retry was a `ui.Failure` action rather than a Button | Harness originally assumed a control implementation. It now invokes the compiler-issued `onRetry` slot. Deterministic replay passed; this was not a generated runtime failure. |
| Native quantity verifier saw older prepared operations above the new operation | Bounded review scrolling locates the exact new quantity/period descriptor. Native replay then passed without inference. |
| Retained-fixture replay initially could not find test assets | Fixture assets belong to the instrumentation context, not target Organizer assets. Corrected harness context; both fixture replays passed. |
| First fatal-host regression passed a JS object where DEAL expected a table | Constructed inventory through actual compiled policy before injection. Test setup error corrected. |
| Old exact-prompt migration hash failed after intentional guidance revision | Updated current guidance serialization regression and added literal-syntax/payload assertions; template source parity remains unchanged. |

# Failure classification

`choice-policy.deal` treats explicit `INVALID_JSON` as rejected input. Other parse mechanism failures propagate. Awaited UTF-16 length calls are outside parameter-shape catches, preventing operational failure from silently removing catalogue eligibility. Compiled-core tests inject parse failures into context and answer parsing, and UTF-16 failure into catalogue parameter checking.

Native compilation no longer equates every nonzero status with candidate rejection. Repair requires status 1, a version-1 structured report, error diagnostics attributed to the generated application, and candidate-source diagnostic families excluding project configuration. Missing/malformed reports, other statuses, generated entry/declaration diagnostics, project configuration and backend/publication failures stop with a typed operational error containing bounded codes, not local paths. Device tests cover both classification branches and redaction; ordinary real model E3003/E3007 diagnostics were repaired through this boundary.

The classifier is intentionally conservative: E6005 and other generic lowering errors stop, even when source might contribute. Errors located in application augmentation cannot be distinguished fully from authored application source without source-origin metadata. File/code classification is stronger than exit status, not proof that a compiler is bug-free. Compiler I/O failure was verified in implementation and modeled by no-source-diagnostic cases, not induced on the device filesystem.

# Regression evidence and scope review

Final Android catalogue, retained-source, UI lifecycle and workspace isolation instrumentation passed. Compiled-core checks passed sixteen exact template-source pairs, seven invalid envelopes, JSON/UTF-16/checker failure propagation and current guidance regression. Frozen choice-policy parity passed eleven fixtures and 132 decisions. Production-generated JS lifecycle and existing UI-session checks passed; artifact boundaries and build-cache checks passed. Full upstream compiler suites were not rerun.

Read-only scope review found no reason for another broad ownership migration. Test fixtures remain consumer-owned, library inputs remain independent of them, production routing remains unchanged, and native authority remains native. The central result is **useful repaired LLM-authored source, but poor first-pass reliability and costly latency**. More closely related successful replays would not establish generality. Future experiments should be separately bounded to a meaningfully different demonstrated workflow and should distinguish intent fidelity, checking, repair and runtime/native usefulness. Authority enforcement and upstream data-access fixes remain separate decisions.

# Observability and reproduction

Routine library receipts remain bounded and shape-only. The explicitly selected experiment stores rejected model responses and incoming repair diagnostics in app-private storage, separate from routine logs. Each next attempt retains the preceding diagnostics; exhaustion retains its final failure separately. Experiment summaries distinguish mode, source/choice counts, phase outcomes, durations and accepted pair identity. Private exports stay under ignored `build/`; this report contains findings, not raw live logs or private execution state.

`SourceGenerationInstrumentation` defaults to no-inference retained fixtures. `mode=real` explicitly authorizes the two bounded remote workflows; `mode=saved` replays accepted pairs from a named round. After successful replay, the unchanged pair is available to Organizer's existing debug saved-workspace path. The native verifier's `--source-experiment` requires `--replay` and cannot silently invoke generation. See `README.md` for invocation details.
