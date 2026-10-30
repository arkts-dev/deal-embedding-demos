---
kind: research
id: recipe-ui-four
created_at: 2026-10-30
updated_at: 2026-10-30
status: measured-with-quality-gaps
references:
  - framework-corrected-four.md
  - deal-v15-migration.md
question: "What does deterministic UI composition deliver, and what changed after rebasing the Android compiler branch?"
mode: local
provider: local-tools
sources:
  - dependencies/deal-embedding/core/composition/recipes.deal
  - dependencies/deal-embedding/ui/platform.dealui-pack
  - tools/generation-gateway.py
  - build/recipe-ui/benchmark/analysis.json
  - build/recipe-ui/benchmark-rebased/analysis.json
gaps:
  - Independent visual acceptance remains pending; screenshots were captured but not visually reviewed.
  - One fresh sample per scenario and revision is not a controlled compiler-only performance experiment.
  - Discovery provider compute and first-token timing are unavailable.
---

# Deterministic composition and fresh benchmarks

## Scope and implementation

The embedding-owned DEAL recipe derives hierarchy from checked fragment kinds and workflow dependencies, without another model call or new model layout fields. It places one required Hero, outcome metrics, input/action sections, status and content groups. Generic placement rejects missing required slots, unacceptable kinds, overcapacity and repeated placement. Shared controls are assigned once, to the first dependent workflow. Read/alternatives are nonvisual resources; their current implementations return no views.

The authoritative pack adds Screen, InputGroup, MetricGroup, ActionGroup and ContentGroup with declarative child contracts. Existing props/defaults remain. Allocation presents its comparison before controls and its recalculation action after controls. Recursive editable binding preserves enabled-state propagation through grouped descendants. The native renderer handles adaptive widths, headings, typography and ancestor-aware surface framing; it does not choose task hierarchy. Section now renders a consistently inset, always-visible group rather than a locally collapsible group. Existing lower-level renderer styling is not fully tokenized.

`ui/theme.json` owns the new geometry/semantic roles, projected into PackTheme during the library build. The UI recipe reference was pinned to OHOS `7ee06af1941729b00a110493ad9d1d9b5b127d17`, not the older reference. Its constrained slots/theme/framing informed the implementation; no ArkTS task logic was transplanted. OHOS's documented invalid-placement fallback is not evidence of whole-screen rejection there.

The first deterministic device run exposed the embedding's arbitrary 2 MiB UI mount ceiling. It was removed; real sandbox evaluation remains authoritative. Compact subsequently passed, including large lossless bridge transfer. No compiler-output optimization was performed.

## Evidence and preservation

Before **each** cohort all six apps were stopped and archived, then generated workspace preferences/files, saved/dismissed generated selections and generated preparation records were purged. All archives and preflight classification completed before mutation. Retained preferences and retained domain/authority/history files were compared exactly afterward. No package-data reset occurred. `benchmark/cohort/clean-state.json` and its rebased counterpart record zero remaining generated experiences.

Each cohort ran fresh discovery and UI generation for Budget, Departure, Food and Equipment. These are **not cold-cache experiments**. Generation explicitly used Cerebras `qwen-3.8-27b`, low reasoning; discovery independently used `deepseek-flash`, thinking disabled. No silent backend switch, answer injection, manual repair or timed rerun occurred. Stop: first runnable generated frame committed to the swap chain; semantics and screenshots follow untimed.

Both coherent builds passed 13 host checks and seven deterministic device suites; all seven installed APK hashes matched built bytes. Receipts: `host-2/results.json`, `device-2/results.json`, `host-3/results.json`, `device-3/results.json` under `build/recipe-ui/`. Failed earlier runs remain alongside them. The first Deal UI suite invocation used the wrong working directory and failed to locate an example; it is not a semantic failure.

## Fresh results

Seconds; model is UI-generation host elapsed across all tool turns, checking includes Deal UI and DEAL elapsed. First-frame includes discovery and orchestration.

| Scenario | Before frame | After frame | Before discovery | After discovery | Before model | After model | Before checking | After checking | Before/after semantics |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---|
| Budget | 13.097 | 12.065 | 2.297 | 2.302 | 1.879 | 1.370 | 2.257 | 1.507 | pass / pass |
| Departure | 13.854 | 11.033 | 2.376 | 2.462 | 3.126 | 2.573 | 3.251 | 1.767 | pass / fail |
| Food | 18.480 | 17.541 | 2.543 | 2.752 | 4.073 | 4.928 | 4.915 | 2.779 | pass / fail |
| Equipment | 19.549 | 15.782 | 2.896 | 3.023 | 6.365 | 4.288 | 3.497 | 1.930 | pass / pass |
| **Sum** | **64.980** | **56.421** | **10.112** | **10.539** | **15.443** | **13.159** | **13.920** | **7.983** | **4/4 / 2/4** |

Before: compiler branch `313a45b3`. After: `android-current` at `6ae488cb`, rebased onto pulled master `91a6e47c`. Seven retained branch commits replayed. The one conflict was resolved by retaining indexed operand lookup and logical-branch result ownership, rather than replacing upstream indexing with the old implementation. This was rebase compatibility, not compiler performance investigation.

Observed DEAL compile elapsed:

| Scenario | Before | After | Change |
|---|---:|---:|---:|
| Budget | 2.162 | 1.400 | −35.2% |
| Departure | 3.145 | 1.686 | −46.4% |
| Food | 4.737 | 2.603 | −45.0% |
| Equipment | 3.344 | 1.766 | −47.2% |
| **Sum** | **13.388** | **7.455** | **−44.3%** |

First-frame sum improved **13.2%**; checking improved **42.7%**. These observations support a material compilation-time improvement, but are not a controlled equal-source compiler benchmark: fresh plans differ, Equipment lost a tool turn and provider cache usage changed. Compiler phases overlap and were not summed or investigated internally.

Against historical corrected four-scenario total 58.127 s, before was 11.8% slower and after 2.9% faster. That is historical context, not equivalent-quality acceptance: the after cohort failed two semantics checks.

### Failures retained, not explained away

Before Equipment: first native submission rejected `Unknown port item.itemId`; a second native-tool turn succeeded. There were five UI-generation provider calls across four scenarios, not four, and **zero exact-source repair calls**. After: four provider calls, no admission rejection, zero repair calls. Both cohorts used maintained-only composition, no chooser, custom sections or full-source fallback.

After Departure displayed correct departure 15:16 but inspection completion 17:10 instead of required 15:06. Food's initial allocation stayed 0 of 6/Not feasible rather than producing six pizzas with at least two vegan. These failures are in the original results and accepted source; neither was fed back to the model or silently corrected. Deterministic suites passed on the new compiler, but they do not establish live authoring correctness. No causal attribution of these generated-plan failures to the compiler is established.

## Why the LLM phases take time

### UI generation: reasoning output, not a gateway backlog

Before: 93,895 input, 15,875 completion, **12,912 reasoning** tokens (81.3% of completion), 53,248 cached input. After: 74,422 input, 13,554 completion, **11,485 reasoning** (84.7%), 72,704 cached. Reasoning is a subset of completion, not additional billable-token arithmetic. Maintained plans are compact, but the model still emits thousands of reasoning tokens for nontrivial scenarios.

Source gateway elapsed fell **15.108 → 12.927 s**. Provider-reported total was **12.374 → 10.564 s**, comprising approximately:

| Provider-reported source time | Before | After |
|---|---:|---:|
| Queue | 0.113 s | 0.975 s |
| Prompt | 1.893 s | 1.052 s |
| Completion | 10.345 s | 8.488 s |

Provider total minus these sums has minor provider-accounting differences. Completion dominates. In the first cohort, cached Departure prompt processing was 0.087 s versus uncached Food 0.811 s; Food also spent 2.647 s in completion. Cache hits help prefill, but cannot remove reasoning/output time. After Food remained the slowest source-model call despite warm input caching.

Local gateway queue was only **0.106–3.106 ms** before and **0.086–1.961 ms** after. The single-worker pool was not a measured bottleneck in these serial runs; parallel-load behavior remains untested. Source response headers arrived after **0.524–0.620 s** before, **0.526–0.591 s** after. First reasoning and tool-content receipt offsets are retained separately; the final tool arguments can arrive near stream end. Streaming receipt does not mean runnable partial UI.

Gateway minus provider totals was **2.733 → 2.363 s** across all source calls. This is transport/server/accounting residual, **not isolated network RTT**. The rejected Equipment tool turn added 1.460 s gateway time for the successful follow-up, in addition to the first rejected call and admission work; it must not be mislabeled an exact-source repair.

### Discovery: small output, mostly cached input, substantial unisolated request latency

Each cohort used exactly **37,858 input** tokens, **36,992 cached** and **866 reported uncached** (97.7% cached). Output was **725 → 710 tokens**. Discovery gateway elapsed was **8.406 → 8.591 s** and full discovery stages **10.112 → 10.539 s**. Warm prompt caching did not make requests instantaneous.

Local gateway queue stayed **0.097–3.787 ms** across both cohorts. Response headers took **0.588–0.699 s**; full discovery responses took roughly **1.697–2.513 s** per call. Discovery is nonstreamed here, so first-token, provider queue/prefill/compute and network components cannot be separated. Thinking was explicitly disabled; missing reasoning usage remains unknown rather than asserted zero. The evidence rules out local pool backlog as the explanation for these samples, not all provider/network latency.

### There is significant non-LLM latency too

Generation outside source-model host time and checking was **16.093 → 15.608 s**. Event receipt gaps from the final source completion to composition assembled were approximately **0.922/2.094/4.507/3.993 s** before, increasing with composition complexity. These gaps include composition/admission and surrounding orchestration/log delivery; they are not pure measured recipe CPU. Pre-generation outside discovery added **9.412 → 9.132 s**, including the cold first-scenario setup. Mount events were about 0.1–0.2 s apiece, not the dominant cost.

Thus the next measured targets are excessive generation reasoning and composition/orchestration overhead, plus semantic authoring reliability. Faster compiler elapsed helps but cannot solve these. Do not suppress reasoning, weaken validation, inject task answers or claim unmeasured RTT/CPU savings from these observations.

## Acceptance boundary

Portable recipes, authoritative build projection, deterministic runtime and fresh benchmark collection are verified. All source, rejections, event/artifact history, original timings and screenshots remain under ignored `build/recipe-ui/`; collectors report no missing captured artifacts. Independent visual review is **pending** because screenshots could not be inspected with the current model. No claim of visual acceptance or successful four-scenario post-rebase delivery is made. No push was requested.
