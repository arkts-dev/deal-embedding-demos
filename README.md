# Native DEAL embedding demo

Organizer's ordinary Kotlin/Compose screens, plan storage and native review host checked DEAL + Deal UI assistance. Calendar uses the same protocol for its departure experience. Todo, Vehicle and Rental are independently owned native providers. No WebView or on-device APK generation is involved.

## Protocol and ownership

`dependencies/deal-embedding/api/embedding.d.deal` is the public Source → Experience → Session protocol. Its app-linked catalogue contract is `catalogue-policy.d.deal`; behavioral rules are documented alongside it. Native bootstrap supplies compiler, executor, inference, storage, authenticated transport, renderer and observation mechanisms. `ExperienceRuntime` is internal, not a compatibility API.

Embedding's compiled `core/generation.deal` owns orchestration, fallback, bounded full-source repair and repeated-response stop. Organizer's compiled `android/apps/organizer/policy/app-policy.deal` implements offer/lower; equipment eligibility, described choices and source synthesis live in its policy directory, not the library. `tools/build-organizer-policy.py` links that policy against the exact public declarations. The library build has no consuming-app inputs. Generated UI cannot import privileged embedding APIs.

`ProtocolController` runs an ordinary compiled DEAL consumer with local synchronous context/request/handle state and nonblocking async native mechanism lanes; no individual answers are pre-injected. Organizer and Calendar place the framework's `ExperienceSurface` and explicitly activate/close its owner; they do not wire run/next/dispatch/published or widget forwarding. The surface uses a production compiled DEAL consumer through `ApplicationProtocol`; app shells remain native. Hiding/review detaches reception and rendering without silently closing execution or forgetting source. Failed retained owners explicitly cold-recover on activation. Preparation acceptance uses framework cleanup guards; physical deletion failures retain reclamation ownership. Shared framework helpers handle bounded observation paging, exchange correlation and display-only tree traversal. Apps author local capabilities in the shared WireType/Parameter vocabulary. Embedding starts automatic session progress at run, independently of observation. Only next delivers snapshots; dispatch returns null. Grant/revoke synchronously update the authoritative permission epoch, queried by native admission and reply checks; no deferred native grants or app polling remain. Contexts validate/copy schema and disclosed JSON; handles are instance-issued. Generate/load check without mounting or saving. First run saves and starts one session; live run resumes, close retains, cold run rechecks without inference, forget deletes. Returned metadata cannot mint authority. Optional closed-record fields may be absent. Full wire/lifecycle/observation bounds are in the embedding API README.

## Build and development services

```sh
JAVA_OPTS='-Xmx768m' tools/build.sh organizer rental calendar todo vehicle tests
adb install --user 0 -r build/organizer.apk
adb install --user 0 -r build/rental.apk
python3 tools/generation-gateway.py --chooser jev --counts build/demo-model-counts.json
adb reverse tcp:8787 tcp:8787
```

Requires JDK 25, Kotlin/Compose plugin, Android SDK API 37/build tools 36; apps target API 36. Runtime dependencies are pinned. `EMBEDDING_AAR` can select a prebuilt library. Each independent dependency checkout is committed separately.

The gateway binds loopback and reads credentials in memory, never packages them. Jev choice uses `~/jev.key`, `/v1/systemone` and `jev-latest`; `--jev-key-file`/`--jev-model` override these. Cortex remains the source/repair provider. Choice probabilities/confidence are metadata—not correctness, grants or domain authority. Provider failures stop without retries or switching providers.

Default gateway mode is demo, without autonomous audit ceilings. Explicit verification mode requires `--audit --counts <new-file> --choice-limit N --source-limit N`. Never leave an exhausted audit process serving the delivered app. DEAL permits at most three source attempts per request. Cancellation stops acceptance/waiting, not necessarily remote cost. Health reports configuration/mode, not successful inference.

## Ordinary Organizer demonstration

Enable **Organizer access** in Rental. Keep the gateway and USB forwarding running.

1. Open Organizer → **Riders → Boom stand**.
2. Tap **Find equipment**, or **View equipment options → Change request → Continue** for a retained request.
3. Leave budget/preferences empty for comparison; for editable assistance, enter budget **18** and preferences requesting editable quantity, budget and search. Tap **Find options** once.
4. Wait for the actual choice/check/source workflow. Several minutes may be required. Cancel and Inspect remain available. Failure offers deliberate retry; no automatic restart.
5. Tap **Load options**. Generation/mounting does not read stock.
6. Select **Boom microphone stand**, tap the source's preparation button, then **Review selection**. Native review says **Not booked**. Quantity 2 within €18 gives €16 when this source/stock supports it.
7. Return to the requirement; options stay retained. Cold reopening checks accepted source without fresh inference or automatic inventory refresh.

These are inputs, not forced routes or canned results. Source and actual exchanges in Inspect establish which route occurred. Name/specification matching is a shortlist, not suitability proof. Review does not reserve, write Rental data, establish agreement or mark coverage. Calendar's native confirmation remains its separate provider-write authority.

## Inspect

One **Inspect** entrance provides Activity, Dialogue, Source and retained experiences. Activity is newest event first; Dialogue is newest exchange first with request → reply → observed check feedback internally. Inspect reads source/events/artifact/health protocol operations rather than private storage internals.

Enable **Dialogue → Capture content → Enable** before generation to inspect full exchanges. Capture is explicit/private/nonretroactive, bounded and independently evicted; no hidden reasoning or lossless-audit claim. Turn it off afterward. Clear deletes diagnostics, not saved source. Terminal debugging: `ANDROID_SERIAL=<device> python3 tools/embedding-log.py --follow` (also `--json`, `--trace`, `--artifact`, `--capture`). This debug reader does not invoke models.

## Verification

```sh
python3 tools/verify-protocol-api.py <authoritative-protocol-document>
python3 tools/test-jev-chooser.py
python3 tools/test-generation-gateway.py
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
