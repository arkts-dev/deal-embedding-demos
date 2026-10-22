---
kind: research
id: first-render-four
created_at: 2026-10-22T17:05:00Z
updated_at: 2026-10-22T17:05:00Z
status: reviewed
references:
  - android/tests/FrameworkE2eInstrumentation.kt
  - tools/generation-gateway.py
question: What does fresh generation cost through the first runnable rendered experience, excluding semantic assessment?
mode: local
provider: device execution and retained artifacts
sources:
  - build/first-render-four/results.json
  - build/first-render-four/numbers.json
  - build/first-render-four/distributions.json
  - build/first-render-four/events.json
  - build/first-render-four/semantic-assessment.json
  - build/first-render-four/final.tar
  - build/first-render-four/progress-log.tar
gaps:
  - One fresh sample per scenario, not population latency or a causal comparison.
  - Frame commit is swap-chain submission, not physical display scanout or visual acceptance.
  - Supplied assertions are not exhaustive semantics.
  - Actual latency measurements do not include ordinary application navigation or manual consent time.
---

# Method

The harness now uses no candidateTest callback. Each scenario starts before launching the foreground Organizer host, constructing the protocol and discovering one opportunity. Null discovery is not retried. The issued suggestion alone supplies the generation request. Compiled-core source/patch recovery continues until the source pair checks; semantic expectations never enter inference or recovery.

The checked experience mounts through the real ExperienceSurface and DealRenderer in the foreground Activity. Android's registerFrameCommitCallback records the stop timestamp when a generated, fault-free snapshot's frame has been committed to the swap chain. The timestamp is fixed before screenshots, control dispatch, provider reads, semantic assertions and closure. It is not merely a fabricated publication acknowledgement. The host and grants are explicit test authority, not ordinary app consent behavior.

Semantic evaluation subsequently exercises that same experience with no model calls or feedback. No cold-reopen work is included. The instrumentation status code only indicates completion of the benchmark; semantic outcomes are separate. The original signed-budget check was too narrow: it required positive150 rather than accepting signed-150 with Under budget. Retained source/tree establish this case is correct under the signed convention. The oracle was corrected without regenerating source or changing any timed result; original results remain intact.

Before the cohort: gateway/transport tests passed; the updated test APK built and installed; each app was force-stopped and archived; only generated assistance state was purged, with domain/authority/disclosure/capture preservation assertions and zero generated state verified. No build or test overlapped measured scenarios. Existing compiler/library caches and provider prefix caches were not purged: fresh means fresh discovery/generation and no saved-source reuse, not a cold machine. Gateway ran current strict source-pair/patch schemas, Cerebras qwen-3.8-27b low reasoning, token limit-1; discovery DeepSeek Flash. APK hashes and build/installation/purge receipts are retained.

# Timings

Seconds. Discovery-only starts just before Discovery construction/inference; start-to-suggestion additionally includes foreground host/protocol/catalogue setup. Total run starts before that setup. Generate-to-render starts after explicit opportunity admission, before generate. Compiler/check times sum all attempted candidates including failed ones; their nested compiler stages must not be added again.

| Scenario | Discovery only | Start to suggestion | Generate to render | Total to first frame | All compiler/check operations | Main DEAL compiler | UI stage | Patch calls |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| Budget | 1.829 | 5.973 | 4.494 | 10.664 | 1.834 | 1.766 | 0.023 | 0 |
| Departure | 2.195 | 3.109 | 10.093 | 13.383 | 2.101 | 2.032 | 0.029 | 0 |
| Food | 2.548 | 3.460 | 225.146 | 228.861 | 3.608 | 3.097 | 0.196 | 20 |
| Equipment | 2.692 | 3.561 | 52.857 | 56.588 | 2.471 | 2.314 | 0.049 | 5 |

There were 29 source calls: four initial calls and25patch calls. Main DEAL stage sums include a rejected food compilation; successful compilations in this cohort took1.766–2.314seconds. These differ from earlier cohorts; no controlled attribution of the difference is established.

# Tokens

Source generation plus every repair, provider-reported counts. Completion includes reasoning: do not add reasoning to completion. Input includes cached tokens.

| Scenario | Input | Completion | Reasoning subset | Cached input |
|---|---:|---:|---:|---:|
| Budget | 13505 | 1886 | 1280 | 0 |
| Departure | 13569 | 12496 | 10364 | 12288 |
| Food | 412737 | 309653 | 286491 | 331776 |
| Equipment | 107658 | 80344 | 70151 | 80896 |
| Total | 547469 | 404379 | 368286 | 424960 |

Discovery is separate: input/output budget6964/88, departure6987/123, food6979/212, equipment7011/289; total27941/712, cached27136. Discovery reasoning usage is unknown, not zero. All provider phase timing/usage/settings receipts remain in numbers.json/events.json. Request summaries and25repair records were present, telemetryComplete=true, with no malformed/oversized metric records and no health-failure file. Some large payload artifacts rotated during the long food run; the union of progress-log.tar and final.tar retains every artifact referenced by the cohort events (artifact-retention.json). No hidden reasoning text is retained.

# Separate semantic assessment

- Budget: passed supplied arithmetic/edit/equality/under-budget checks. The retained final frame has approved20000, committed19850, difference-150, Under budget. The original positive-only assertion was an oracle issue, not generated-source failure.
- Departure: passed supplied checks. Explicit actual Vehicle estimate12→29; rendered15:16/15:06; editing start960→985 gave15:41/15:31.
- Food: incorrect default allocation. Actual menu and delivery loaded, but quantities remained zero, failing six pizzas/at least two vegan. Later edit assertions were not reached; their correctness is unknown.
- Equipment: incorrect/incomplete comparison. Actual catalogue and quote read, but package alternative was absent and the1200/1300/5000 comparison was not produced. Later budget-edit assertions were not reached.

These are functional assertions plus retained-source/tree review, not visual acceptance or exhaustive correctness. Screenshots captured after the timed stop are retained.

# Observed bottlenecks

1. Food: source-model calls consumed215.950seconds versus3.608seconds of compilation/checking.20patch calls yielded8applied patches,11PATCH_AMBIGUOUS rejections and1PATCH_JSON. The initial schema-valid pair still contained an escaped UI envelope inside the DEAL string; repeatedly targeted wrong-file/mismatched searches did not remove it promptly. E1001 backslash failures recurred, then a later compile exposed E3017 read-only array.length assignments and E3003 untyped table reads. One repair ended with provider finish_reason=length; malformed output was rejected, not executed.
2. Equipment: source-model calls consumed48.646seconds versus2.471seconds of compilation/checking. Five patch calls: threePATCH_AMBIGUOUS, twoapplied. Initial embedded/escaped UI text caused E1001; after its removal, reserved field from caused E1007, then the final patch compiled.
3. Departure: one model call consumed7.347seconds, compilation/checking2.101seconds. No repairs. Budget: model1.920seconds and compiler/check1.834seconds; initial setup before discovery was comparatively substantial. Provider receipt reasoning/content phases are not isolated compute time.

Strict schema enforced two outer string fields in all four initial replies, but did NOT prevent bad source contents or invalid patch searches. The earlier claim that schema alone would eliminate the underlying escaping problem was too strong. Schema does not guarantee language or semantic validity. This cohort also directly contradicts extrapolating the earlier9/9patch-admission observation into a general reliability claim: here only10/25patches applied. Semantic errors did not cause any of these timed repairs.

All provider operations during semantic assessment occurred after the render stop. They therefore contribute zero to reported run latency. No recommendation to optimize provider calls follows from their counts in this benchmark. Earlier mixed acceptance timings remain historical evidence and must not be substituted for these first-frame numbers.
