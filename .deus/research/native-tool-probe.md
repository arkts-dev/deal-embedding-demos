---
kind: research
id: native-tool-probe
created_at: 2026-10-23T15:30:00Z
updated_at: 2026-10-23T15:30:00Z
status: reviewed
references:
  - tools/probe-native-tools.py
  - tools/probe-model-protocol.js
  - tools/probe-native-continuation.py
  - tools/test-native-tools-probe.py
  - dependencies/deal-embedding/core/source-repair.deal
question: Does the current Cerebras model's native tool interface reduce protocol failures on retained failing repair cases without changing the live path?
mode: local
provider: Cerebras qwen-3.8-27b live API with low reasoning
sources:
  - build/native-tools-probe/plan.json
  - build/native-tools-probe/summary.json
  - build/native-tools-probe/continuations.json
  - build/native-tools-preflight/tests-final.log
  - build/native-tools-preflight/gateway-tests.log
  - build/native-tools-preflight/transport-tests.log
  - build/native-tools-preflight/repair-tests.log
  - https://inference-docs.cerebras.ai/capabilities/tool-use
gaps:
  - Four deliberately selected difficult historical bases with three repetitions, not representative production sampling.
  - Repair interface only; initial file writes, autonomous reads/check requests, streaming, cancellation and production orchestration are not evaluated.
  - No runtime, device, provider-read, semantic, visual or first-render assessment.
  - Follow-up tool results tested once per failed first response, without a paired raw follow-up arm.
  - Strict function tools still JSON-encode executable source strings and do not guarantee exact searches or valid programs.
---

# Outcome

**Yes: native strict function calls showed fewer protocol failures in this bounded paired repair probe. This does not establish production readiness. The live raw-framing path remains unchanged.**

Official Cerebras documentation explicitly lists qwen-3.8-27b support for named/required tools, strict mode, parallel calls and multi-turn calling. Actual API responses confirmed the model accepted strict native function definitions and returned tool_calls. No model or provider switch occurred. Raw custom tools/grammar support was not established.

# Method

Four frozen first-repair bases were recovered directly from retained model-request captures: first-render food/equipment with escaped UI contamination inside DEAL, settled food with unsupported Unicode escape, and raw-framing food with UI2023 array-length access. Captures, original diagnostics and admitted capability catalogues are retained in ignored experiment output. The originals were not modified.

Three repetitions per case and interface, 24 first-response calls total, in a predetermined shuffled order. Both arms received identical frozen source/diagnostics and current compiled focused repair guidance. This is an interface-focused probe, not a replay of complete generation history: neither arm received the full production language guide, discovery brief or former repair history.

Native arm: a named, strict edit_sources function with an atomic array of file/search/replacement objects, additionalProperties false, fixed file enum and parallel_tool_calls false. Host supplies the current base digest and collision-free framing internally, then invokes the **existing packaged core patch checker**, not a Python implementation. Model never copies revision digests or boundary tokens. No filesystem path, shell, provider or credential tool exists.

Raw arm: the current compiled core repair instruction, including digest and literal file-labelled base, parsed/applied by the same packaged checker. No tolerant extraction, fuzzy matching, file relocation, unescaping or rewrite fallback.

Both arms use Cerebras qwen-3.8-27b, low reasoning, temperature 1, top_p 0.95, clear_thinking true, nonstreaming responses and no output-token maximum. Calls are sequential; no automatic request retries. All 24 returned HTTP success; no length terminations. Hidden reasoning content was discarded; reported token counters were retained. The experiment contacted the explicitly selected source provider, not public web research with private code.

Accepted pairs were fully compiled using the existing ToolkitHarness/UiSourceGenerator/DEAL path and declarations derived from the same admitted contracts. An exact retained accepted food pair compiled in preflight before live calls. Model-free protocol/application tests were added during the probe and passed afterward; they did not all precede the first live call. No build, device installation, generated-state purge or activation occurred.

# First-response results

| Measure | Strict native edit tool | Current raw framing |
|---|---:|---:|
| Responses | 12 | 12 |
| Protocol-valid | **12/12** | **0/12** |
| Exact atomic application | **9/12** | 0/12 |
| Full compiler pass | **2/12** | 0/12 |
| Protocol failures | **0/12** | **12/12** |
| Other application rejection | 3/12 | 0/12 |
| Applied but compiler-rejected | 7/12 | 0/12 |
| Summed provider request time | 42.500 s | 32.218 s |

All raw failures were PATCH_FRAME: digest/token confusion, omitted header or incorrect boundary syntax, consistent with retained failures. Native arguments were correctly structured but three searches had zero exact matches. They were rejected atomically, not repaired by guessing. Some native attempts removed contaminated source correctly but revealed later DEAL errors; others introduced unrelated class fields or still-invalid view expressions. Protocol success is not program correctness.

| Frozen case | Native protocol-valid | Native applied | Native first-check pass |
|---|---:|---:|---:|
| JSON-contaminated food | 3/3 | 2/3 | 0/3 |
| JSON-contaminated equipment | 3/3 | 1/3 | 0/3 |
| Unicode food | 3/3 | 3/3 | 0/3 |
| Raw-framing food | 3/3 | 3/3 | 2/3 |

These are selected-case descriptive rates. Repetitions within a base are not independent population samples. No statistical generalization, causal production speedup or first-render latency claim follows. The native arm used more completion tokens and provider time despite better admission.

# Multi-turn support check

After freezing all first-response results, each of the ten non-compiling native trials received one native role:tool result carrying actual admission/compiler diagnostics, focused guidance and the current exact files. Accepted edits stayed applied; rejected edits left the original base intact. The next call again used the strict edit tool. This audit's one-follow-up sample boundary is not a proposed production recovery ceiling.

All **10/10** follow-ups returned structurally valid native tool calls; **8/10** applied exactly and **2/10** compiled. Thus **4/12** native trials reached a checked pair within the sampled first response plus one follow-up; **8/12** remained unresolved. No semantic completion claim. Remaining diagnostics included UI1009, E1052 unsupported Unicode escapes, E1007 and E1034; two equipment searches still did not match. Follow-up provider time was 46.725 s. No paired raw continuation was run, so this is a native support/feedback observation, not a comparative recovery-rate measurement.

# Usage

Provider-reported; reasoning is a subset of completion and cached input a subset of input, not additional tokens. All usage known.

| Calls | Input | Completion | Reasoning subset | Cached input |
|---|---:|---:|---:|---:|
| Native first responses (12) | 70557 | 67933 | 60556 | 36864 |
| Raw first responses (12) | 69693 | 49993 | 39231 | 31744 |
| Native follow-ups (10) | 119148 | 61722 | 57850 | 44032 |

# Verification and decision

Four model-free adapter tests pass: strict shape/exterior-text rejection, exact quotes/backslashes/CRLF/Unicode, host boundary collision avoidance, wrong-file/ambiguous/empty/atomic rejection. Existing gateway, gateway transport and compiled source-repair regressions pass. Syntax checks pass. This does not resolve the separately retained native ProtocolPolicy regression or certify the complete suite.

**The requested protocol-improvement gate passed for these retained repair cases.** The strongest supported next step is a separately authorized sandboxed agent prototype, not replacing the live path yet. It must evaluate initial one-file writes, explicit reads/check feedback, complete source-generation context, exact revision binding across turns, streaming/cancellation and held-out cases, then checked/runtime/semantic results. Native tool calls still encode source strings in JSON and leave exact-search and programming failures intact. Current production generation/core/gateway code and device state were not changed by this experiment.
