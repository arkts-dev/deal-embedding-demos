# Native DEAL embedding demo

Organizer, Todo, Calendar, Rental, Pizza and Vehicle publish capabilities and host checked DEAL/Deal UI through one protocol, without WebView/generated APKs. **DEAL v1.5 only**: TypeScript syntax subset, distinct checked semantics. Records/catches yield unknown; assertions check; arrays are invariant; absence is undefined; async results use Promise. Recheck obsolete source, never translate/use old compilers.

[delivery.md](delivery.md)/research preserve history, not v1.5 acceptance. Separate build, host/device semantics, fresh inference and visual approval.

## Ownership and protocol

Dependency direction is apps → embedding → Deal UI → DEAL. Public signatures are [embedding.d.deal](dependencies/deal-embedding/api/embedding.d.deal) and app-linked catalogue-policy; [API rules](dependencies/deal-embedding/api/README.md) define lifecycle/wire behavior. Native bootstrap supplies compiler, sandbox, storage, authenticated transport, renderer and observation mechanisms. Compiled core/generation owns orchestration, fallback and unlimited cancellation-aware exact repair. App-linked DEAL policy owns domain offers/lowering; Organizer equipment policy stays outside library inputs.

ApplicationProtocol runs compiled DEAL through ProtocolController. Apps activate/close ExperienceSurface owners, never wire sessions, pump updates or duplicate state. Detach preserves execution; close retains source; forget deletes. Failed owners cold-recover on explicit activation. Preparation guards retain rejected-handle cleanup ownership, including failures.

Contexts copy/freeze validated schema/data. Issued handles, not copied metadata, confer identity. Generate/load check without mounting/saving; first run saves and starts one session, repeated run resumes it, cold run rechecks without inference. Embedding progresses autonomously; next alone delivers snapshots, dispatch returns null and published means renderer acceptance, not pixels. Synchronous grant/revoke epochs fence native admission and late replies. Provider consent/security remain independent.

## UI and ordinary journeys

Library [ui/](dependencies/deal-embedding/ui/README.md) owns pack/guidance/examples/fragments. Preserve ./platform.dealui-pack contracts. DEAL owns behavior/conclusions; native code formats/layouts/accessibility. Shared rows, hierarchy, actions and Money/Metric/Comparison serve every app.

All apps share Settings and Inspect while retaining native themes. Settings owns suggestions, disclosure, sharing and grants. Confirmed Delete all generated experiences stops owners and deletes generated tools/drafts/links/dismissals, preserving records, permissions, access/capture choices and diagnostic evidence; cancel deletes nothing. Native proposal confirmation is separate. Reset is not automatic or transactional across experiences.

Organizer: Overview → Review requirement, or Riders requirement → tip → Open → checked tool. Calendar: date → event → Preparation → tip → Open; event details/settings stay in menus. Todo and the other hosts use the same assistance facade. Read providers only through explicit generated actions; generation/checking/mounting must not fetch stock. Edit controls and cold-reopen to test source-bound retention. Inspect remains available before generation/after failure; returning does not retry inference. Current scenarios are read-only planning, not booking/order approval.

Calendar supplies selected-event facts/authority/storage; AssistedTask owns discovery/generation/activation. Facts/contracts stay input-bound and foreground-fenced. One request/suggestion; dismissals persist for unchanged input. Preserve Provider consent/revision/write confirmation and isolated demo records. No implied Todo/personal-calendar reads. Selectivity/visual quality need separate evidence.

## Build and gateway

Requires JDK 25, Java-21-compatible Android sources, Kotlin/Compose plugin, SDK API 37/build tools 36; target API 36. Dependencies pinned; EMBEDDING_AAR selects prebuilts. Commit repositories separately; no implicit push.

```sh
JAVA_OPTS=-Xmx768m DEAL_GENERATION_BACKEND=cerebras \
  tools/build.sh organizer tests calendar vehicle todo rental pizza
adb install --user 0 -r build/organizer.apk
adb install --user 0 -r build/tests.apk
python3 tools/generation-gateway.py --backend cerebras --chooser jev
adb reverse tcp:8787 tcp:8787
```

DEAL_DISCOVERY_BACKEND=nano is default; deepseek-flash selects remote discovery independently of source backend. Start gateway with --discovery-backend deepseek-flash; only it reads DEXTER_DEEPSEEK_API_KEY. Cloud disclosure is separate and OFF by default. Discovery has no automatic retry/provider fallback. Nano requires foreground AICore availability.

Hosts provide endpoint/backend values to embedding's gateway client, never HTTP implementations. Credentials/provider parameters remain gateway-side, outside APKs/logs. Standard gateway binds loopback. Source backend cerebras selects qwen-3.8-27b; cortex is explicit, enabled gateway-side with --enable-cortex. APK selection overrides gateway default; unavailable providers fail rather than switch. Cerebras reads ~/cerebras.key, defaults to low reasoning, temperature 1/top-p .95 and provider completion maximum. Jev reads ~/jev.key, uses /v1/systemone and jev-latest; --jev-key-file/--jev-model override it. Choice confidence is metadata, not correctness or authority.

--initial-reasoning/--repair-reasoning independently accept low/medium/high; source reasoning stays enabled. --initial-max-tokens/--repair-max-tokens limit total completion, not exact thinking (-1 omits client ceiling). Source telemetry streams unless --no-stream-source; hidden reasoning is discarded, partial tools never execute. Receipt spans are not compute attribution. Token/cache/provider timing must remain distinct; missing counters are unknown.

## Generation and repair

Generation extracts evidenced typed needs, enumerates checked portable candidates, asks Jev to select issued alternatives, and deterministically assembles maintained fragments. Custom tools fill issued typed, namespaced section/pure interfaces while preserving selected parts. Three charged local calls precede honest partial notices; only actual checked absence of runnable task subsets enables source_fallback writes. Compiler checks and exact-source repairs remain downstream. Charge all extraction/selection/rejection/custom/fallback work.

Materialize once atomically. Malformed plans leave staging unchanged; compile rejection returns exact expanded bytes. Thereafter only read_file/edit_sources/check_sources operate on canonical source. Host-bound revisions require unique disjoint exact edits; no rewrites, JSON correction, hidden regeneration or strategy fallback. Failed patches preserve the valid base. Paired independent initial writes may share a response only while both files are absent. read_file uses inclusive numbered lines, both bounds null for full reads.

Recover until acceptance/cancellation without attempt/repetition ceilings. Preserve transport classifications/failures across requests. Trusted tests feed observed repair. Stable prefixes avoid duplication; cache replay is not fresh inference. ui/values formats, never owns domain policy.

Gateway demo mode has no audit ceiling. Explicit --audit requires --counts <new-file>, --choice-limit and --source-limit; never leave an exhausted audit server serving the app. Timeouts/token limits default to -1 where supported. Cancellation stops waiting/acceptance, not necessarily remote compute/cost; health describes configuration, not successful inference.

## Inspect and verification

Inspect exposes Activity/Dialogue/Source/saved tools through public observation. Newest exchanges first; request → reply → observed feedback inside each. Explicit Capture future content is private/bounded/nonretroactive, excludes thoughts and is not lossless audit; disable afterward. ANDROID_SERIAL=<device> python3 tools/embedding-log.py --follow reads without inference.

```sh
python3 tools/verify-portable-ui.py <completed-parity-directory>
python3 tools/verify-boundaries.py
adb shell am instrument -w dev.deal.connectors.tests/.PortableBackendInstrumentation
adb shell am instrument -w dev.deal.connectors.tests/.PortableCommerceInstrumentation
adb shell am instrument -w dev.deal.connectors.tests/.BridgeTransferInstrumentation
node tools/test-source-repair.js
node tools/test-protocol.js
node tools/test-framework-assistance.js
node tools/verify-generation-guidance.js
python3 tools/verify-protocol-api.py <authoritative-protocol-document>
python3 tools/test-generation-gateway.py
python3 tools/test-build-cache.py
adb shell am instrument --user 0 -w dev.deal.connectors.tests/.ProtocolInstrumentation
adb shell am instrument --user 0 -w dev.deal.connectors.tests/.ProtocolPolicyInstrumentation
adb shell am instrument --user 0 -w dev.deal.connectors.tests/.LoggingInstrumentation
```

Run deterministic checks before inference; never overlap library rebuilds with verifiers or inference. Toolkit tests cover real compiler/session/renderer behavior, not fresh intent selection. Device Guidance/Discovery/Inspection/GenerationGateway/ColdDraft/RemoteCapability isolate boundaries; live variants require explicit opt-in and retain every failure. FrameworkE2e trusted feedback differs from immutable first-frame measurement. verify-demo-reproduction.py submits once through ordinary controls, stops on failure and records private build/ evidence—no replay, forced route or retry counted as fresh delivery.

Acceptance requires independent app use. Android/JS only: other backends, offline inference, compiler isolation, comprehensive accessibility/visual certification and comparative AI value remain unestablished. Trees/builds imply no visual approval. Preserve history/unrelated documents.
