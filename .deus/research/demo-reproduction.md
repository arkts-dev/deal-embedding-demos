---
kind: research
id: demo-reproduction
created_at: 2026-10-12T09:30:00Z
updated_at: 2026-10-12T09:40:00Z
status: reproduced-with-limits
references:
  - .deus/design/organizer-assistance.md
  - .deus/research/organizer-device-audit.md
  - README.md
question: Can the delivered demo independently reproduce both fresh model routes through ordinary user controls without an exhausted audit gate or debug replay?
mode: local
provider: host-agent
sources:
  - tools/generation-gateway.py
  - tools/test-generation-gateway.py
  - tools/verify-demo-reproduction.py
  - android/apps/development/GenerationGateway.kt
  - android/apps/organizer/src/WorkspacePresentation.kt
  - android/apps/organizer/src/WorkspaceScreen.kt
  - android/tests/WorkspaceInstrumentation.kt
  - dependencies/deal-embedding/core/generation.deal
gaps:
  - Real inference requires the connected development-machine gateway and provider configuration; this is not offline inference.
  - Two observed fresh routes do not guarantee a future model's acceptance, route, latency or behavioral correctness.
  - Native screenshot visual interpretation and comprehensive accessibility certification are unavailable to this model.
  - Logging remains bounded; omissions, evictions and prior recovered incomplete history remain visible.
---

# Delivered-demo reproduction

## Exact failure and operational fix

Ordinary Riders → Boom stand → retained options → Change request → Continue → Find options reproduced the delivered failure without a debug launcher. Inspect reported IllegalStateException: Generation gateway unavailable (502). Its actual cause was the running gateway's inherited exhausted audit-choice admission gate, not a source compiler failure. The gateway discarded that cause into a generic HTTP error.

The gateway now defaults to demo mode, ignoring inherited audit-budget environment variables. Autonomous audit limits require explicit --audit, counts path and positive choice/source ceilings. Optional count persistence records admissions without imposing a demo ceiling. There is no gateway retry loop; DEAL retains at most three source attempts per request and repeated-response stopping. The health endpoint exposes demo/audit mode and configuration readiness, not invented provider success.

Gateway failures carry closed, safe JSON codes: audit admission, provider HTTP status, provider unreachable, timeout or generic gateway failure. Provider bodies, authentication headers, credential values and endpoint URLs are not reflected. The Android client retains the safe code/HTTP facts as a typed IOException, while ordinary Organizer displays actionable connection/timeout messages. Inspect retains the technical cause. Source rejection copy no longer exposes workspace taxonomy or attempt counts outside Inspect.

A real stopped gateway then reproduced connection refusal. Ordinary UI showed AI connection unavailable and preserved budget 18. A deliberate Retry repeated the real refusal; Inspect exposed ConnectException. Keep previous options returned to the newly accepted editable interface. No model calls occurred during that failure test. The gateway was restored to demo mode afterward.

## Fresh reproduction, not replay

The executable reproducer performs only documented UI taps and one submission per invocation. It never starts a debug replay, edits private preferences, forces a route or retries a failed request. Its parallel observer reads log artifacts only; it does not navigate the phone. No intervention changed generation while either workflow ran. Artifact capture was enabled through the actual confirmation before submission.

| Fresh task | Choices | Source/repair calls | Actual result |
|---|---:|---:|---|
| Blank budget/preferences equipment comparison | 1 | 0 | Chooser selected compare/text/no notice/read0; checked DEAL catalogue source mounted |
| Budget 18 and adapter preference | 1 | 2 | Chooser returned unsupported; model-authored pair rejected once, repaired then checked/mounted |

Total new inference admissions: two choices and two source calls. No exhausted request was restarted. Comparison choice took 54223 ms; editable choice 5419 ms, source calls 319369 and 154780 ms. Preparation can take minutes, not an implied instant result.

The first source response failed UI2050 for a write through borrowed state. Actual check feedback and prior response were supplied to the second model call. The final pair matched retained source except outer whitespace normalization; no handwritten correction was substituted. Both source attempts, actual prompts/replies, feedback and native observations were captured into ignored private build evidence before bounded logger eviction.

During both runs Inspect → Dialogue was opened while work was active, then actual AI reply drilldown and accepted source were inspected. Comparison Source showed Template with model-directed authorship explanation; editable Source showed AI-built with the true attempt count. Both exact source extensions were opened. Source provenance, not eligibility or rendered appearance, established the routes.

## Actual behavior and authority

Both new interfaces started without inventory rows. Load options invoked real Rental equipment with the exact disclosed start/end instants. Comparison selection prepared €8; editable quantity 2 prepared €16 within the €18 ceiling. Native review displayed Not booked. Provider persistence and consent were unchanged. Local preparation did not alter event coverage or reserve equipment.

The delivered editable artifact subsequently passed cold no-read/no-stale-review checks; €8 and €16 selections; input-edit removal of stale review; budget-limited empty results; search no-match and adapter recovery; and live return retaining quantity 2. A verifier initially assumed zero budget must be invalid, but this model's source legitimately permits zero and returns empty results. That failed assumption was retained and corrected after inspecting actual accepted code, not by rewriting it or redefining an observed failure as a pass. Quantity remains 1–8 with validation in DEAL.

Workspace, Catalogue and UiLifecycle instrumentation passed after installation. Workspace assertions cover actionable audit/connection/timeout messages and hiding provider HTTP codes from ordinary explanations. Gateway tests exercise closed error responses, safe health, explicit audit ceilings versus count-only demo admissions, bounded capture and secret exclusion. Core/policy/boundary checks passed. Final installed APKs passed Workspace assertions again and frozen light/dark/grayscale/enlarged-text failure/pending/cancel scenarios with settings restored. A final ordinary journey on the delivered APK reopened the exact fresh source, explicitly read real Rental stock, prepared €8 and reached Not booked review. Capture was then off and the phone was left at the compact fresh request with empty optional fields, ready for deliberate user submission. The gateway health reported demo/configured/no automatic retries and forwarding was present.

## Reproducibility instructions

README contains prerequisites and ordinary steps for both requests, pre-call capture, Inspect during preparation, exact route verification, explicit Load options and Not booked review. Returning to accepted options is explicitly not fresh inference. Each new demonstration uses Change request → Continue → Find options; model acceptance is not promised. Failures preserve prior assistance and require deliberate recovery or Retry.

The ready environment must be checked for demo mode, loopback forwarding and actual Rental sharing before handoff. Audit restrictions must never silently remain as the delivered operational policy. Content capture should be turned off after observing personal/provider data. Private live artifacts and operation state are not included in this tracked report.
