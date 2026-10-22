#!/usr/bin/env python3
"""Aggregate unified generation events. JSON inputs are event lists/JSONL; no artifacts/reasoning read.
Evaluation JSON is explicit test evidence, never inferred from publication. One independent request
per sample; latest linked summary wins. p90 uses nearest rank and is descriptive even for small n.
"""
import argparse
import collections
import json
import math
import pathlib
import statistics


def distribution(values):
    known = sorted(v for v in values if isinstance(v, (int, float)) and not isinstance(v, bool))
    return dict(samples=len(values), known=len(known), unknown=len(values)-len(known),
                median=statistics.median(known) if known else None,
                p90=known[math.ceil(.9*len(known))-1] if known else None,
                min=min(known) if known else None, max=max(known) if known else None)


def aggregate(events, evaluations=None, revisions=None):
    evaluations = evaluations or []
    latest = {}
    repairs = {}
    malformed = 0
    oversized = 0
    for event in events:
        if event.get('stage') not in ('request-summary', 'repair-metrics'):
            continue
        try:
            record = json.loads(event['target'])
        except (ValueError, KeyError, TypeError):
            malformed += 1
            continue
        if record.get('outcome') == 'telemetry_oversized':
            oversized += 1
            continue  # logger record remains explicit evidence of loss; never invent its counters
        if event['stage'] == 'repair-metrics':
            repairs[record['attemptId']] = record
        else:
            key = record['requestId']
            if record['summaryRevision'] > latest.get(key, {}).get('summaryRevision', 0):
                latest[key] = record
    samples = list(latest.values())
    repair = list(repairs.values())
    if any(x.get('outcome') == 'passed' and not x.get('evidence') for x in evaluations):
        raise ValueError('Passed evaluations require explicit test evidence')
    tests = {x['requestId']: x for x in evaluations}
    for s in samples:
        t = tests.get(s['requestId'])
        s['evaluation'] = t if t else {'outcome': 'not_tested', 'preservationOutcome': 'not_tested'}
    def rate(n, d):
        return dict(numerator=n, denominator=d, fraction=n/d if d else None)
    admitted = [r for r in repair if r['patchOutcome'] == 'applied']
    checked = [r for r in repair if r['recheckOutcome'] in ('passed', 'rejected')]
    totals = {}
    cache_calls = []
    for operation in ('initial', 'repair'):
        totals[operation] = {}
        relevant = [s for s in samples if s[operation+'Calls'] > 0]
        for key in ('in', 'cached', 'out', 'reasoning'):
            values = [s['tokens'][operation].get(key) for s in relevant]
            totals[operation][key] = dict(knownRequests=sum(v is not None for v in values), unknownRequests=sum(v is None for v in values),
                                         knownSubtotal=sum(v for v in values if isinstance(v, (int, float))))
    # Calls with provider metadata: initial call is represented by summary tokens, repairs by records.
    for s in samples:
        if s['initialCalls']:
            cache_calls.append(s['tokens']['initial'])
    cache_calls += [r.get('providerMetadata', {}) for r in repair]
    known_cache = [c for c in cache_calls if isinstance(c.get('cached'), (int, float))]
    shares = [c['cached']/c['in'] for c in cache_calls if isinstance(c.get('cached'), (int, float)) and isinstance(c.get('in'), (int, float)) and c['in'] > 0]
    settings = {json.dumps(s, sort_keys=True) for request in samples for s in request.get('settings', [])}
    return dict(schema=1, malformedMetricEvents=malformed, oversizedMetricEvents=oversized, requestCount=len(samples), repairCount=len(repair), revisions=revisions or {}, settings=[json.loads(x) for x in sorted(settings)],
        limitations=['Observational heterogeneous requests; no causal attribution.',
                     'Small-cohort p90 is descriptive, not a robust population estimate.',
                     'Missing/evicted summaries are not observable denominators; inspect logger health.',
                     'Line locality and diagnostic disappearance do not prove semantic preservation.',
                     'Only explicit evaluation evidence establishes tested usefulness; publication is not pixel-present latency.'],
        distributions=dict(repairMs=distribution([r.get('totalRepairMs') for r in repair]),
            timeToCheckedSourceMs=distribution([s.get('timeToCheckedSourceMs') for s in samples]),
            timeToTestedUsefulMs=distribution([tests.get(s['requestId'], {}).get('timeToTestedUsefulMs') if tests.get(s['requestId'], {}).get('outcome') == 'passed' else None for s in samples]),
            immediateChangedBytes=distribution([r.get('immediate', {}).get('changedBytes') if isinstance(r.get('immediate'), dict) else None for r in repair]),
            cumulativeChangedBytes=distribution([s.get('cumulative', {}).get('changedBytes') if isinstance(s.get('cumulative'), dict) else None for s in samples]),
            cachedInputSharePerCall=distribution(shares)),
        patchAdmission=rate(len(admitted), len(repair)), recheckPass=rate(sum(r['recheckOutcome']=='passed' for r in checked), len(checked)),
        cacheHitFraction=rate(sum(c['cached'] > 0 for c in known_cache), len(known_cache)), cacheUsageUnknownCalls=len(cache_calls)-len(known_cache),
        cachedInputShare=dict(knownCalls=len(shares), unknownCalls=len(cache_calls)-len(shares),
            fraction=(sum(c['cached'] for c in cache_calls if isinstance(c.get('cached'), (int,float)) and isinstance(c.get('in'), (int,float)) and c['in']>0) /
                      sum(c['in'] for c in cache_calls if isinstance(c.get('cached'), (int,float)) and isinstance(c.get('in'), (int,float)) and c['in']>0)) if shares else None),
        tokens=totals, failureStages=dict(collections.Counter(s['failureStage'] for s in samples if s.get('failureCode'))),
        failureCodes=dict(collections.Counter(s['failureCode'] for s in samples if s.get('failureCode'))),
        patchRejections=dict(collections.Counter(r['patchRejectionCode'] for r in repair if r.get('patchRejectionCode'))), requests=samples, repairs=repair)


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('events', nargs='+', type=pathlib.Path)
    p.add_argument('--evaluations', type=pathlib.Path)
    p.add_argument('--revisions', type=pathlib.Path)
    p.add_argument('--output', required=True, type=pathlib.Path)
    a = p.parse_args()
    events=[]
    for file in a.events:
        raw=file.read_text()
        try:
            data=json.loads(raw)
            events.extend(data if isinstance(data,list) else [data])
        except ValueError:
            events.extend(json.loads(line) for line in raw.splitlines() if line.strip())
    result=aggregate(events,json.loads(a.evaluations.read_text()) if a.evaluations else None,json.loads(a.revisions.read_text()) if a.revisions else None)
    a.output.write_text(json.dumps(result,indent=2)+'\n')


if __name__ == '__main__':
    main()
