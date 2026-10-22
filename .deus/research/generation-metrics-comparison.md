---
kind: research
id: generation-metrics-comparison
created_at: 2026-10-21
updated_at: 2026-10-21
status: reviewed
references:
  - https://github.com/egavrin/deal-studio-ohos/tree/7dae6db35a45932a750689afc8e28139b4e8e620
question: What generation and repair metrics do deal-embed and pinned deal-studio-ohos actually track?
mode: local
provider: local-source-inspection
sources:
  - tools/generation-gateway.py
  - tools/gateway_transport.py
  - dependencies/deal-embedding/android/src/bindings/EmbeddingLog.kt
  - dependencies/deal-embedding/android/src/bindings/ExperienceGenerator.kt
  - dependencies/deal-embedding/android/src/compiler/ExperienceCompiler.kt
  - dependencies/deal-embedding/android/src/execution/UiSession.kt
  - upstream entry/src/main/ets/vera/VeraTiming.ets
  - upstream entry/src/main/ets/vera/DeepSeekEngine.ets
  - upstream entry/src/main/ets/pages/GeneratePage.ets
  - upstream tools/vera-bench/report.py
gaps:
  - No new live model experiment was conducted for this comparison.
  - Publication and screenshot capture do not establish pixel-present latency or visual acceptance.
  - Source edit locality is evaluated in retained experiments, not a standard production aggregate.
  - Provider receipt spans do not isolate queue, prefill or reasoning compute.
---

# Generation and repair metrics

## Scope and product objective

Compare actual code, not similarly named dashboard labels. Upstream is pinned to `7dae6db35a45932a750689afc8e28139b4e8e620`. No upstream code was copied. Repairs are normal for complex cases: optimize time to tested useful behavior, targeted edits and preservation, not zero-repair output as an acceptance gate.

## Metrics matrix

| Category | deal-embed | Pinned deal-studio-ohos |
|---|---|---|
| Model call | Model/backend, initial/repair/discovery operation, reasoning effort, completion limit, temperature, finish reason | Request source/round, thinking selection, generation settings, finish reason; attempts grouped into runs/refinements |
| Stream timing | First/last reasoning and content receipts, first output, receipt spans, total, chunk count; optional provider timing | Sent/end, first meaningful delta (called first byte), first/last reasoning and content, first tool delta; request body characters |
| Tokens/cache | Provider input/completion/reasoning/cached counts; missing counts remain unknown; invariant-prefix fingerprint | Input/completion/cache/reasoning counts; cache percentage/fresh-input count; missing fields retain initialized zero |
| Pipeline stages | Discovery, choice/lowering, source calls, UI check/lower, DEAL compile, mount, snapshot production/publication, capability/action outcomes | UI selection, model/tool rounds, compilation; separate template selection/search/content durations; generation start/end |
| Repairs/failures | Attempt counts, repeated response, exact patch admission diagnostics, application versus operational rejection; complete checked pair/source retention | Compile attempts, request/empty/edit/parser/type failure layer/code, compile result, cut-off finish, edit-to-rewrite fallback |
| Runtime evidence | Actual capability request/reply traces, interaction snapshots, source-bound cold retention; scenario tests distinguish semantics from compilation | Compile trace; smoke init/view/decode result and node count, with a bare environment; preview/device/harness checks are separate |
| Edit locality | Exact digest/unique/disjoint admission; line changes and UI-byte preservation measured in retained experiments, not continuously aggregated | Refinement SourceDelta: added/removed lines and kept percentage; not necessarily compiler repair locality |
| Aggregates | Current reports aggregate per-call/stage totals and scenario outcomes; no general latency percentile dashboard | Run/program cumulative model/compile time and tokens; benchmark per-prompt median/min–max and first-attempt compile rate; grouped errors |
| Rates/cost | Not a standard measured billing or throughput KPI | Approximate USD using configured rates; output tokens divided by generation time minus initial wait |

## Definitions that matter

Our `firstOutputMs` is the first nonempty reasoning/content delta, not TCP first byte or first arbitrary SSE event (`gateway_transport.py:24–110`). Raw provider queue/prompt/completion timings are retained only when provided (`generation-gateway.py:119–128`). Our publication acknowledgement is not a frame-present timestamp (`UiSession.kt:73–82`).

Upstream `TurnTiming.saw()` marks `firstByteAt` when reasoning/content/tool output is observed, not necessarily the first network byte (`VeraTiming.ets:45–74`). Its Generate page “wait” uses time to first meaningful output; “think” uses first content minus first meaningful output (`DeepSeekEngine.ets:516–522`). “Write” is generation minus wait minus think (`GeneratePage.ets:1193–1206`). These are useful observed phase proxies, not isolated server compute. Its trace reasoning span is separately last minus first reasoning receipt (`VeraTiming.ets:122–143`), so even its two “thinking” durations are not identical.

Upstream token fields initialize to zero and update when usage exists (`VeraTiming.ets:55–89`). A zero there does not distinguish missing metadata from reported zero. Our gateway uses null for absent/invalid counts. Neither token share is a timing share. Upstream approximate cost is a rate calculation, not an invoice (`GeneratePage.ets:1048–1053`).

`tools/vera-bench/report.py:99–185` counts compilation attempts, model requests, model/reasoning/content/tool durations, outcomes, first compile success and per-prompt medians. Tool-result “used in program” is source-name matching (`report.py:63–67,187–215`), not evidence that the capability executed. Its run-end success is not full semantic acceptance. The harness sleeps before screenshots; it does not measure pixel presentation. The first-render comment in `VeraTiming.ets` alone is not proof of an instrumented frame-present metric.

## Important differences

Upstream tracks tool-assisted generation rounds, retrieval timing and offered-versus-source-used results. Our native capability reads belong only to execution, so adding generation-time provider calls merely to match those counters would violate our boundary. Discovery, compiler check, mount and actual runtime read times must remain distinct.

Upstream retains reasoning text in its trace and offers a reasoning export (`VeraTiming.ets:122–143`, `report.py:237–250`). Our streaming adapter discards it; only counts and receipt timing belong in reports.

Upstream falls back from failed edits to full rewriting (`GeneratePage.ets:838–858`). Our supported repair path is exclusively digest-bound exact patches, with no rewrite fallback. Historical full/none experiments remain immutable evidence, not executable product options.

## Metrics to prioritize next

1. **Time to verified useful behavior**, including all permitted repairs and checking; report failures and separate operational, syntax, runtime and semantic outcomes.
2. **Per-repair latency and locality**: changed bytes/lines, edited regions, untouched-file preservation, patch admission result and recheck duration. Locality measurements do not prove semantic preservation.
3. **Behavior preservation** across edits and cold retention; no unwanted capability reads or authority changes.
4. **Per-operation tokens/cache and phase timing**; report distributions only with enough independent samples and keep unknown metadata unknown.

Repair count and first-pass compilation remain descriptive diagnostics, not the optimization target. Neither fast invalid patches nor compiled-but-wrong tools count as success.
