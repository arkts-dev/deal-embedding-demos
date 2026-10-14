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

### Calendar preparation demo

After setup, open Calendar normally on today. A compact tomorrow preview offers a quiet preparation link; tapping opens the supporting event note and an already-populated, locally executable plan. Discovery uses a small “Looking ahead…” spinner, then a suggestion or nothing. Uncertain links ask a question; the user can inspect the plan without authorizing any write.

Setup is separate: Todo → **Extensions** controls task sharing; Calendar → **Extensions** separately controls Jev discovery and Cortex tailoring. **Clear saved assistance** removes plans/drafts and discovery history, preserving grants and Calendar/Todo records. **Inspect** is separate from the ordinary flow and exposes decisions, elapsed times, failures, source origin and exact source. Create labelled fixtures using `python3 tools/setup-preparation-demo.py viewing` (or `budget`, `ambiguous`, `unrelated`, `held-out`); there is no Development link. Debug fixture extras do not change grants or unrelated records.

Jev classifies each disclosed event/task pair as related, uncertain or unrelated. Compiled trusted `policy/preparation.deal` issues the questions, validates answers/references, checks current facts, bounds admissions and remembers dismissal. Generic trusted execution and capability transport live in embedding. The accepted `.deal/.dealui` plan runs locally: choose tasks, edit durations/travel/buffer, calculate preparation and departure. Accepted immutable facts are safely encoded into checked initial state; the planner has no capabilities, task completion, appointment writes or model access. The initial fallback is authored. While its screen is foreground, Cortex may generate specialized DEAL/Deal UI through the existing trusted embedding generation/check path. Checked success offers **View tailored plan**, never a silent replacement. Leaving cancels unfinished generation; failure keeps the fallback. Saved source and opt-in compiler-owned drafts are bound to exact source/facts and revalidated against current grants; unchanged reopening does not rerun inference.

Only changed facts may trigger another discovery call: no automatic retry or daily quota, one in-flight check, 60-second discovery deadline, four events/eight tasks. Generation has a foreground cancellation deadline; live timings and rejected repairs remain in Inspect. Provider sharing and Calendar’s remote disclosure opt-in are independent; Calendar → **Extensions → AI discovery** disables disclosure. This is foreground demo assistance, not a background agent or live route estimator.

Evidence scope: development initially exposed false semantic associations and compiler limitations; these attempts are not reliability successes. The conservative pair classifier was then evaluated separately on the frozen eight-case set. A passing set is bounded demo evidence, not a general accuracy guarantee. Authored fallback success, fresh generated success, interrupted attempts and visual-only review are separate evidence; do not treat fallback as a generation success.

Small checks: `node tools/verify-preparation.js` verifies the compiled lifecycle policy without network. `node tools/verify-preparation.js --live` makes eight live chooser calls including held-out wording and negative cases. `python3 tools/verify-preparation-device.py --live --scenario Viewing` separates fixture setup from ordinary launcher/link/populated-plan/dismissal acceptance and and retains private device receipts in ignored `build/preparation-e2e/`. The device check requires the gateway/ADB reverse above and explicit Todo sharing. `PreparationInstrumentation` checks real compiled initial state and planner actions with no capabilities. `tools/review-demo-screen.py` sends one explicitly selected screenshot and design reference to DeepSeek Flash for visual-only critique using `DEXTER_DEEPSEEK_API_KEY`; it neither changes the main agent model nor certifies behavior.

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
