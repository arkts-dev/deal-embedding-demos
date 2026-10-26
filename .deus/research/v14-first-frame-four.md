---
kind: research
id: v14-first-frame-four
created_at: 2026-10-26T12:58:00Z
updated_at: 2026-10-26T12:58:00Z
status: reviewed
references:
  - android/tests/FrameworkE2eInstrumentation.kt
  - dependencies/deal-embedding/ui/generation-guidance.md
  - tools/generation-gateway.py
question: How does a fresh generated-state four-scenario cohort perform after the DEAL v1.4 migration?
mode: local
provider: Cerebras qwen-3.8-27b low reasoning; DeepSeek Flash discovery
sources:
  - build/v14-first-frame-four/revisions.json
  - build/v14-first-frame-four/apk-hashes.json
  - build/v14-first-frame-four/cohort/clean-state.json
  - build/v14-first-frame-four/cohort/results.json
  - build/v14-first-frame-four/cohort/numbers.json
  - build/v14-first-frame-four/cohort/native-tool-observations.json
  - build/v14-first-frame-four/verification.json
  - build/v14-first-frame-four/preservation.json
  - build/v14-first-frame-four/historical-integrity.json
  - build/bottleneck-optimizations-four/cohort/numbers.json
gaps:
  - One sample per scenario; no causal attribution or population reliability estimate.
  - Additional late-delivery assessment fails for food despite passing the historical supplied checks.
  - Fresh generated state does not establish cold compiler, process, filesystem or provider caches.
  - Discovery token usage is unknown; source usage is reported separately.
  - Screenshots and semantic assertions do not establish visual or exhaustive acceptance.
---

# Procedure

User explicitly authorized generated-state purge and live rerun. All six app states were archived before deletion. Purge receipts verify zero generated files, drafts, saved keys and links, with remaining preference records unchanged. On-device observation logs were retained, not purged. Domain records, grants and disclosure preferences were preserved. Post-run comparison confirms retained domain/authority records unchanged; only Organizer's capture preference changed through benchmark instrumentation.

Executed the existing four-scenario FrameworkE2eInstrumentation once, with no runner retries, candidateTest or semantic feedback. Independent Flash discovery, Cerebras qwen-3.8-27b, low initial/repair reasoning, and current native tool protocol. Exact installed Organizer/tests APK hashes matched built artifacts. No source changes or builds during measurement. Code revisions: demo 79a9976, embedding dfc0418, Deal UI 6d445cd, DEAL 34087771.

Timing freezes at the first fault-free generated frame committed to the swap chain, not physical scanout. Semantic actions and additional tests occur afterward, without source repairs or retiming. All four completed. Newly generated programs remain on device.

# Results

Seconds. Discovery is inference-only; total additionally includes foreground setup, handoff and rendering. Checking is a subset, not an additive independent phase.

| Scenario | Discovery | Generation → frame | Total → frame | Checking | Model turns | Supplied checks |
|---|---:|---:|---:|---:|---:|---|
| Budget | 1.940 | 5.093 | 11.443 | 1.821 | 1 | Pass |
| Departure | 2.325 | 10.001 | 13.382 | 2.571 | 1 | Pass |
| Food | 2.312 | 22.482 | 25.833 | 2.956 | 3 | Pass |
| Equipment | 2.866 | 14.747 | 18.864 | 2.014 | 2 | Pass |
| Sum | 9.443 | 52.323 | 69.522 | 9.362 | 7 | 4/4 |

Previous pre-migration aggregate total was 78.221 seconds: new sample is 8.699 seconds / 11.1% lower. Budget is 2.1% slower; departure 9.8% slower; food 19.9% faster; equipment 16.4% faster. This is not proof that v1.4 makes generation faster: compiler, runtime, guidance and sampled programs changed, with one run per task and mixed caches.

**Food fails the separate late-delivery assessment.** Selecting an actual late delivery window leaves status "Ready" with otherwise valid quantities and budget. The previous cohort passed that additional assessment. Therefore this run is not 4/4 general semantic success, despite passing all four historical supplied assertion sets. No fix or rerun was made.

# Recovery and usage

Seven protocol-valid native replies: four initial paired writes, one focused read of food DEAL lines 130–170, and two exact edit batches. Eight file writes, two admitted edit batches containing five edits total, six complete-pair checks; zero observed protocol rejections. Food's initial rejection contains six E3003 contextual table-read errors; read then two-edit batch passes. Equipment's initial rejection is E1007 (reserved identifier), followed by a three-edit batch. Read-only followups are not edit attempts.

Source operation wall time totals 39.028 seconds versus 48.658 previously. Initial provider calls total 35.759 seconds versus 44.404; subsequent calls 2.981 versus 3.991. Check time totals 9.362 versus 9.293. These are observed wall receipts, not isolated model compute.

| Scenario | Input tokens | Completion | Reasoning subset | Cached input subset |
|---|---:|---:|---:|---:|
| Budget | 18,168 | 2,206 | 1,475 | 0 |
| Departure | 18,197 | 11,289 | 9,286 | 17,408 |
| Food | 66,516 | 26,345 | 21,817 | 58,368 |
| Equipment | 40,620 | 17,256 | 13,208 | 17,408 |
| Total | 143,501 | 57,096 | 45,786 | 93,184 |

Previous source totals: 126,010 input / 72,090 completion / 59,756 reasoning / 100,352 cached. Discovery usage is unknown, not zero. No hidden reasoning text is retained or reported.

# Evidence and limits

280 trace-filtered unified events, four request summaries, all referenced artifacts retained. Source pairs, failed checks, trees, screenshots, clean-state receipts and private archives are under build/v14-first-frame-four. All 143 fingerprinted files in the two preceding comparison directories remain unchanged; formerly open gateway logs/counters were excluded and separately frozen before replacing the gateway. The new gateway writes only the new cohort's counters.

These results establish fresh source generation, checking, first-frame latency and the stated behavioral assertions on this device. They do not establish held-out semantic correctness, cold-cache performance, visual quality or reliable speed improvement.
