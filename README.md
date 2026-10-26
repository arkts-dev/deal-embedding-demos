# Native DEAL embedding demo

Build with `DEAL_DISCOVERY_BACKEND=nano` (default) or `DEAL_DISCOVERY_BACKEND=deepseek-flash`, independently of `DEAL_GENERATION_BACKEND=cerebras`. For cloud discovery, run the existing gateway with `--discovery-backend deepseek-flash`; it reads `DEXTER_DEEPSEEK_API_KEY` from the environment. No credential enters an APK. The same strict discovery/admission path applies, without local fallback or automatic retries.

Current executable source targets **DEAL v1.4** only. Deal UI, generated code, embedding declarations and native transports use the same number-only ABI and explicit required-field construction. Old generated source is rechecked and rejected when obsolete; there is no legacy compiler or translation lane. Historical benchmark evidence remains unchanged.

Six native apps — Organizer, Todo, Calendar, Rental, Pizza and Vehicle — publish capabilities and host checked DEAL + Deal UI assistance through the same protocol. Build-selected Nano (default) or DeepSeek Flash discovers a tip; accepting **Open** requests Cerebras source generation and on-device checking/execution. No WebView or on-device APK generation is involved.

See [Delivery status](delivery.md) for the four read-only scenarios, verified plumbing, known semantic failures, build/test commands and replay entry points. This is not blanket product acceptance.

All six apps share **Settings** for assistance disclosure, sharing and tool access, and one **Inspect** UI (Activity / Dialogue / Source) with native themes and app/request/experience scope. **Settings → Delete all generated experiences** asks for confirmation before deleting generated tools, retained edits, links and dismissals; domain records, permissions, access choices and retained diagnostic evidence stay unchanged. Native proposal review remains separate. This is behaviorally tested, not visual approval; see `delivery.md`.

## Protocol and ownership

`dependencies/deal-embedding/api/embedding.d.deal` is the public Source → Experience → Session protocol. Its app-linked catalogue contract is `catalogue-policy.d.deal`; behavioral rules are documented alongside it. Native bootstrap supplies compiler, executor, inference, storage, authenticated transport, renderer and observation mechanisms. `ExperienceRuntime` is internal, not a compatibility API.

Embedding's compiled `core/generation.deal` owns orchestration, fallback and cancellation-aware unlimited recovery through targeted digest-bound exact repair. Organizer's compiled `android/apps/organizer/policy/app-policy.deal` implements offer/lower; equipment eligibility, described choices and source synthesis live in its policy directory, not the library. `tools/build-organizer-policy.py` links that policy against the exact public declarations. The library build has no consuming-app inputs. Generated UI cannot import privileged embedding APIs.

`ProtocolController` runs an ordinary compiled DEAL consumer with local synchronous context/request/handle state and nonblocking async native mechanism lanes; no individual answers are pre-injected. Organizer and Calendar place the framework's `ExperienceSurface` and explicitly activate/close its owner; they do not wire run/next/dispatch/published or widget forwarding. The surface uses a production compiled DEAL consumer through `ApplicationProtocol`; app shells remain native. Hiding/review detaches reception and rendering without silently closing execution or forgetting source. Failed retained owners explicitly cold-recover on activation. Preparation acceptance uses framework cleanup guards; physical deletion failures retain reclamation ownership. Shared framework helpers handle bounded observation paging, exchange correlation and display-only tree traversal. Apps author local capabilities in the shared WireType/Parameter vocabulary. Embedding starts automatic session progress at run, independently of observation. Only next delivers snapshots; dispatch returns null. Grant/revoke synchronously update the authoritative permission epoch, queried by native admission and reply checks; no deferred native grants or app polling remain. Contexts validate/copy schema and disclosed JSON; handles are instance-issued. Generate/load check without mounting or saving. First run saves and starts one session; live run resumes, close retains, cold run rechecks without inference, forget deletes. Returned metadata cannot mint authority. Optional closed-record fields may be absent. Full wire/lifecycle/observation bounds are in the embedding API README.

## Portable UI toolkit

The embedding library's [`ui/`](dependencies/deal-embedding/ui/README.md) owns the pack, composition guidance and checked source examples. Apps retain the `./platform.dealui-pack` import and use the same ExperienceSurface; no consumer-specific Kotlin widgets are needed. Adaptive Row, semantic headings/captions/actions and labeled Money/Metric/Comparison extend the legacy vocabulary. DEAL owns arithmetic, validation and explicit comparison status; the native backend formats values and lays them out.

Host conformance: `python3 tools/verify-toolkit.py`. Android sandbox: `ToolkitInstrumentation`. Internal native fixture interaction/force-stop retention: `ANDROID_SERIAL=<device> python3 tools/verify-toolkit-device.py`. Optional `ToolkitLiveInstrumentation -e live true` performs repeated fresh budget generation with independent initial/edit/reopen checks, not Nano discovery or cross-app semantic acceptance. Visual review remains separate; a compilation or fixture pass is not visual approval or trustworthy fresh assistance.

## Build and development services

```sh
JAVA_OPTS='-Xmx768m' DEAL_GENERATION_BACKEND=cerebras tools/build.sh organizer rental calendar todo pizza vehicle tests
adb install --user 0 -r build/organizer.apk
adb install --user 0 -r build/rental.apk
python3 tools/generation-gateway.py --chooser jev --counts build/demo-model-counts.json
adb reverse tcp:8787 tcp:8787
```

### Calendar preparation demo

After setup, open Calendar, choose a date and tap a Gig Demo event. Embedding's build-selected discovery examines the captured selected-event facts and canonical capability contracts, returning one suggestion or none. Tap **Open** to accept the exact suggestion and generate a checked DEAL tool using the build-selected remote backend. Compilation, rendering, interaction and source-bound drafts run locally. Unchanged reopening reuses saved checked source; changed facts invalidate pending results and saved assistance admission.

Calendar → **Settings** controls suggestions and remote generation disclosure separately. Cloud discovery has its own opt-in and never inherits consent for local inference. **Delete all generated experiences** removes saved source/drafts, generated links and dismissals after confirmation, preserving permissions and Calendar records. **Inspect** exposes captured facts, exact accepted briefs, model routing, traces, elapsed times, failures and checked source. Event details are available through the event menu. This flow does not read Todo or personal calendars. Explicit capability grants and native Calendar write confirmation remain separate from accepting a suggestion.

Discovery mechanisms, shared instructions, reply validation, cancellation and single-use admission belong to embedding, not Calendar policy. Calendar owns authorized facts, foreground triggers, placement, dismissal and native authority. One discovery request and one suggestion slot are active at a time. Dismissed suggestions stay suppressed for unchanged input; operational failures are not abstention. Semantic usefulness and reliable selectivity remain unproven; successful generation does not establish visual acceptance.

Checks: `DiscoveryInstrumentation` covers deterministic discovery boundaries and admission; `DiscoveryLiveInstrumentation` requires explicit `-e live true` and retains live failures; `DiscoveryCalendarInstrumentation` tests the controlled native reminder adapter. `tools/check-preparation-presentation.py` captures structural device evidence without claiming visual acceptance. `tools/review-demo-screen.py` sends one explicitly selected screenshot and design reference to DeepSeek Flash using `DEXTER_DEEPSEEK_API_KEY` for visual-only critique.

Requires JDK 25, Kotlin/Compose plugin, Android SDK API 37/build tools 36; apps target API 36. Runtime dependencies are pinned. `EMBEDDING_AAR` can select a prebuilt library. Each independent dependency checkout is committed separately.

Calendar and Organizer supply endpoint/backend configuration to embedding's gateway client; connection handling, cancellation and provider observations are library-owned behind the existing compiled generation path. `GenerationGatewayInstrumentation` tests this with local HTTP fixtures, not live inference. The external gateway binds loopback and reads credentials in memory, never packages them. Jev choice uses `~/jev.key`, `/v1/systemone` and `jev-latest`; `--jev-key-file`/`--jev-model` override these. Source/repair backend is selected in each host APK at build time: `DEAL_GENERATION_BACKEND=cerebras tools/build.sh calendar` selects Cerebras `qwen-3.8-27b` (default); `DEAL_GENERATION_BACKEND=cortex tools/build.sh calendar` explicitly selects Cortex. Start `python3 tools/generation-gateway.py --backend cerebras --enable-cortex --chooser jev` to serve both; an APK's selection overrides the tooling default, and an unavailable backend fails rather than switching. Cerebras reads `~/cerebras.key` gateway-side and uses low reasoning effort, temperature 1, top-p 0.95, strict native source tools and the provider's completion maximum by default. The app's disclosure and Inspect identify the selected backend; existing saved plans retain their original provenance. Choice probabilities/confidence are metadata—not correctness, grants or domain authority. Recoverable provider failures return to compiled core recovery, without switching providers.

Generation mechanics: `--initial-reasoning` / `--repair-reasoning` independently accept Cerebras Qwen `low|medium|high` (low defaults; source reasoning cannot be disabled); `--initial-max-tokens` / `--repair-max-tokens` independently bound total completion, not an exact thinking budget (-1 defaults: omit the client ceiling and use the provider maximum). Streaming is enabled for source telemetry; `--no-stream-source` disables it. Initial generation uses native strict submit_compact/source_fallback tools. Embedding validates the typed compact plan and deterministically emits canonical DEAL/Deal UI state, controls, dependency recalculation and async plumbing; task-specific helpers/views remain generated and compiler-checked. There is no extra selector/verifier call. Malformed plans leave staging unchanged; rejected compilation returns exact materialized bytes. After materialization only read_file/edit_sources/check_sources operate on the pair, with revisions and admission owned by portable core. Explicit source_fallback enables paired initial write_file calls; all preceding work remains charged and multi-call permission ends once either file exists. read_file supports inclusive 1-based line ranges (numbered literal lines) or full reads with both bounds null. Raw source/patch framing is removed without a fallback. Only complete tool calls execute; full source pairs reach checking; reasoning text is discarded by the streaming adapter. Metadata distinguishes first output/reasoning/content receipt times, content span, total wall time, provider timing, token/cache counts and finish reason. Receipt spans are not provider CPU or thinking-time attribution. Keep raw private artifacts private.

Repairs are exclusively exact unique/disjoint/digest-bound patches with diagnostic source windows. There is no strategy selector or full-rewrite repair path. Failed patches retain the last valid base and continue with rejection feedback. Invalid initial envelopes recover initial admission with the unadmitted response retained as feedback; they never activate invalid source. Repairs are expected for complex cases: judge time to useful behavior, edit locality and preserved behavior, not zero-repair output alone. Invariant guidance and canonical contracts precede variable context in a stable system prefix; delta requests include the base only once. No saved UI replay is counted as prefix-cache acceleration. Checked `ui/values` helpers replace repeated decimal/time/bounds code while generated DEAL retains domain arithmetic and policy.

Default gateway mode is demo, without autonomous audit ceilings. Explicit verification mode requires `--audit --counts <new-file> --choice-limit N --source-limit N`. Never leave an exhausted audit process serving the delivered app. DEAL generation recovers until checked acceptance or explicit cancellation, with no attempt/repeated-response ceiling. Invalid initial envelopes trigger initial-admission recovery; valid source pairs use exact targeted patches. Classified model/provider transport failures retry, never masquerade as application-source errors. Source token limits and native gateway timeouts default to -1 (provider maximum/unlimited). Cancellation stops acceptance/waiting, not necessarily remote cost. Health reports configuration/mode, not successful inference.

## Ordinary Organizer assistance

Keep the gateway and USB forwarding running. Consumer grants and Rental's provider sharing are independent explicit choices in **Suggestions, tools & sharing**; merely seeing a capability is not authorization.

Open **Overview → Review requirement**, or select a requirement in **Riders**. The requirement has one assistance entrance: discovery → smart tip → **Open** → generated checked UI. Use its explicit read action to fetch provider data. Generation/checking/mounting do not fetch stock. Edit generated controls and cold reopen the requirement to verify source-bound retention without inference. **Inspect** exposes actual source and exchanges, including failures; returning from Inspect does not retry inference.

Outputs vary and compiler admission does not prove suitability. `FrameworkE2eInstrumentation` is an explicit live opt-in framework journey: trusted runtime assertions feed failed observations into targeted repair, with real provider reads and state restoration. Passing its assertions does not establish ordinary Activity lifecycle correctness or visual acceptance. The present four-scenario scope is read-only planning, with no bookings, orders or proposal confirmation.

## Inspect

One **Inspect** entrance provides Activity, Dialogue, Source and retained experiences. Activity is newest event first; Dialogue is newest exchange first with request → reply → observed check feedback internally. Inspect reads source/events/artifact/health protocol operations rather than private storage internals.

Enable **Inspect → Capture future content → Enable** before generation to inspect full exchanges. Capture is explicit/private/nonretroactive, bounded and independently evicted; no hidden reasoning or lossless-audit claim. Turn it off afterward. Terminal debugging: `ANDROID_SERIAL=<device> python3 tools/embedding-log.py --follow` (also `--json`, `--trace`, `--artifact`; capture is controlled only in common Inspect). This debug reader does not invoke models.

## Verification

```sh
python3 tools/verify-protocol-api.py <authoritative-protocol-document>
python3 tools/test-jev-chooser.py
python3 tools/test-generation-gateway.py
node tools/verify-generation-guidance.js
python3 tools/verify-boundaries.py
python3 tools/test-build-cache.py
adb install --user 0 -r build/tests.apk
adb shell am instrument --user 0 -w dev.deal.connectors.tests/.ProtocolInstrumentation
adb shell am instrument --user 0 -w dev.deal.connectors.tests/.ProtocolPolicyInstrumentation
adb shell am instrument --user 0 -w dev.deal.connectors.tests/.LoggingInstrumentation
```

Protocol instrumentation executes an ordinary compiler-emitted DEAL consumer through the reusable executor: every public export, typed Deal UI trees, copies/forgeries, generation, lifecycle/cold reopening across bootstrap, terminated-session recovery, explicit capabilities and cancellation even while noncooperative inference continues. Inference is deterministic fixture inference, with no network—not zero model calls. Protocol-policy instrumentation runs actual Organizer offer/lower and core with catalogue/fallback/provider failure/invalid catalogue-source cases. Policy regressions exercise compiled app policy, exact source parity, invalid envelopes, host error propagation and guidance. Renderer/default and clarity scripts test authored native states, not fresh generation. Full Deal UI session regressions remain in the Deal UI dependency.

`tools/verify-demo-reproduction.py --task comparison|editable` submits once via ordinary controls, with actual inference and no forced route/replay/retry; it records private evidence under ignored `build/`. Stop on failure. Saved replay, compiled fixtures and instrumentation are not fresh inference or independent user acceptance.

Acceptance is the delivered app opened and used independently. Android/JS is the supported demo boundary. Offline inference, other backends, general portability, compiler resource isolation, comprehensive visual/accessibility certification and comparative AI product value are not established. Unrelated supplied PDFs remain untouched.
