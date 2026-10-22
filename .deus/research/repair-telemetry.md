# Repair telemetry contract

Generation observations use the existing trusted, bounded DEAL log. `repair-metrics` and `request-summary` carry machine-readable JSON in `Event.target`; they contain no source or reasoning text and remain available with content capture disabled. Logger health and retention bounds still apply: absent events are not successful or complete requests.

The core permits one initial generation followed by at most 32 targeted exact-patch repairs. Invalid initial envelopes, repeated replies, cancellation and operational failures still terminate early. Patch admission remains in compiled DEAL; native telemetry only observes its boundaries.

## Records and baselines

Each repair call has a request/attempt ID, numbered repair, target check ID, revision-bound base, result revision, diagnostic inventory/progression, admission/recheck outcomes, phase durations, and immediate/cumulative change metrics. Revision hashes use the exact patch protocol's scalar-length-prefixed source strings, not the separate persisted-state digest.

Changes use line LCS after trimming equal prefixes/suffixes, capped at four million cells per file. Added/removed line counts are exact for that algorithm; `changedBytes` counts UTF-8 bytes of added and removed lines including their original newline delimiters. Oversized comparisons report incomplete/null rather than approximate counts. Immediate changes compare the last valid base; cumulative changes compare the initial valid pair. Rejected edits leave both sources unchanged. Neither locality nor diagnostic disappearance proves semantic minimality or preservation.

Diagnostic codes are linked to successive actual checks. Persistent means present at consecutive checks; recurred means seen earlier, absent from the preceding check, then returned; new means not seen in any earlier check. No-check repairs retain null remaining diagnostics. Runtime faults produce linked summary revisions after successful source checks.

## Milestones and completeness

The generation-end summary is emitted for success, failure or cancellation. Later mount and first native-publication acknowledgements emit revisions with the same request ID, rather than changing the source-generation terminal time. Publication is not pixel-present latency. Source checked, runtime mounted, snapshot published and explicitly tested behavior are independent. Runtime/preservation evaluation defaults to `not_tested`.

Initial and repair call counts, model durations, patch application, checking, mount and publication are separate. Missing usage is null, never zero; reasoning is included in completion. Missing provider metadata, internal telemetry failure, bounded record overflow and prospective logger loss must remain visible. Oversized records are replaced by a small incomplete record, never silently truncated JSON. Alternative injected models without provider metadata are valid execution mechanisms but incomplete usage samples.

`tools/generation-metrics.py` automatically deduplicates request-summary revisions and repair IDs and aggregates distributions, admission/recheck rates, failures, cache counters and initial/repair usage. Inputs are exported unified events, optional build revisions, and explicitly supplied evaluation records. An evaluation record uses `requestId`, `outcome`, optional `timeToTestedUsefulMs`, `preservationOutcome` and test evidence; it must come from actual tests. The aggregator never derives an evaluation from mount/publication. Small heterogeneous cohorts have descriptive nearest-rank p90s, not robust population estimates. Missing/evicted requests have no knowable denominator without external cohort receipts.

## Stream definitions

Canonical fields are `requestToFirstMeaningfulOutputMs`, `firstReasoningReceiptMs`, `lastReasoningReceiptMs`, `firstContentReceiptMs`, `lastContentReceiptMs`, and `streamCompleteMs`. These are monotonic receipt offsets from provider request start. First meaningful output excludes empty/role/usage-only events; it is not the first network byte. Reasoning/content spans are receipt spans, not model compute or prefill. Optional provider-reported timing is retained separately without asserting equivalence. Historical field aliases remain for existing readers. Hidden reasoning text is discarded.
