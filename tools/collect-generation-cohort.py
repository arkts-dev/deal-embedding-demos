#!/usr/bin/env python3
"""Collect archived first-frame evidence. Never invokes inference or changes a device.
Usage: collect-generation-cohort.py COHORT_DIRECTORY
Unknown/evicted capture is explicit; request-summary counters survive capture eviction.
"""
import base64
import collections
import hashlib
import json
import pathlib
import sys
import tarfile
import xml.etree.ElementTree as ET

out = pathlib.Path(sys.argv[1]); cohort = out / 'cohort'
events = {}; artifacts = {}; results = []; sources = {}; files = {}
archives = sorted((p for p in cohort.glob('*.tar') if not p.name.endswith(('pre-purge.tar', 'clean.tar', 'post.tar'))), key=lambda p: (p.stat().st_mtime_ns, p.name))
for archive in archives:
    with tarfile.open(archive) as tar:
        for member in tar.getmembers():
            if not member.isfile(): continue
            name = member.name
            if not (name.startswith('files/embedding-log/') or name.startswith('shared_prefs/') or name.startswith('files/framework-e2e-')): continue
            raw = tar.extractfile(member).read()
            if name == 'files/framework-e2e-results.json': results = json.loads(raw)
            elif name == 'files/embedding-log/events.jsonl':
                for line in raw.decode().splitlines():
                    e = json.loads(line)
                    events[(e['sequence'], e.get('trace'), e['stage'], e['outcome'])] = e
            elif name.startswith('files/embedding-log/') and name.endswith('.json'): artifacts[pathlib.PurePosixPath(name).stem] = raw.decode()
            elif name.startswith('shared_prefs/'):
                values = {n.get('name'): n.text for n in ET.fromstring(raw)}
                for key, value in values.items():
                    if key.startswith('workspace.') and key.endswith(('.deal', '.dealui')):
                        sources[key] = base64.b64decode(value) if values.get(key.rsplit('.', 1)[0]+'.sourceEncoding') == 'base64' else value.encode()
            elif name.startswith('files/framework-e2e-'): files[pathlib.PurePosixPath(name).name] = raw
traces = {r['trace'] for r in results if 'trace' in r}
es = sorted((e for e in events.values() if e.get('trace') in traces), key=lambda e: e['sequence'])
# Retain discovery failure events separately even when no generation trace was issued.
all_events = sorted(events.values(), key=lambda e: e['sequence'])
(cohort / 'events.json').write_text(json.dumps(es, indent=2)+'\n')
(cohort / 'all-events.json').write_text(json.dumps(all_events, indent=2)+'\n')
(cohort / 'results.json').write_text(json.dumps(results, indent=2)+'\n')
for name, raw in files.items(): (cohort / name).write_bytes(raw)
numbers = []; operations = []; summaries = {}
for r in results:
    if 'trace' not in r:
        numbers.append(dict(r, status='not_rendered', measurement='unavailable_before_generation')); continue
    sub = [e for e in es if e['trace'] == r['trace']]
    summary_events = [e for e in sub if e['stage'] == 'request-summary']
    summary = json.loads(summary_events[-1]['target']) if summary_events else None
    summaries[r['trace']] = summary
    provider_events = [e for e in sub if e['stage'] == 'model-provider' and e['outcome'] == 'completed']
    providers = [json.loads(e['target'].split(' ', 1)[1]) for e in provider_events]
    def aggregate(key):
        if summary:
            values = [summary['tokens'][phase].get(key) for phase in ['initial', 'repair']]
        else: values = [p.get(key) for p in providers]
        return sum(values) if values and all(v is not None for v in values) else None
    n = dict(r, tokens={k: aggregate(k) for k in ['in', 'out', 'reasoning', 'cached']}, providers=providers,
             retainedProviderEvents=len(providers), summary=summary,
             modelMs=(summary['initialModelMs']+summary['repairModelMs']) if summary else None,
             checkingMs=summary['checkingMs'] if summary else None,
             compilerMs={s: sum(e['durationMs'] for e in sub if e['stage'] == s and e['outcome'] in ['completed', 'failed']) for s in ['compiler-ui', 'compiler-deal']},
             composition=collections.Counter(e['outcome'] for e in sub if e['stage'] == 'composition'))
    composition_events=[e for e in sub if e['stage']=='composition']
    route_events=[e for e in composition_events if e['outcome']=='checked']
    route_detail=None
    if route_events:
        e=route_events[-1]
        raw=artifacts.get(e.get('artifact')) or e.get('target','')
        try:route_detail=json.loads(raw)
        except (ValueError,TypeError):pass
    assembled_detail=None
    assembled_events=[e for e in composition_events if e['outcome']=='assembled']
    if assembled_events:
        e=assembled_events[-1]
        raw=artifacts.get(e.get('artifact')) or e.get('target','')
        try:assembled_detail=json.loads(raw)
        except (ValueError,TypeError):pass
    if route_detail and assembled_detail:
        route_detail.update({key:assembled_detail[key] for key in ['operations','customCharacters','reason'] if key in assembled_detail})
    n['behaviorRoute']=route_detail
    n['fallbackReasons']=[artifacts.get(e.get('artifact')) or e.get('target') for e in composition_events if e['outcome']=='fallback']
    n['behaviorCaptureComplete']=bool(route_detail) if composition_events else None
    numbers.append(n)
    for ext in ['deal', 'dealui']:
        if 'experience' not in r: continue
        key = 'workspace.'+r['experience']+'.'+ext
        if key in sources: (cohort / (r['scenario']+'-accepted.'+ext)).write_bytes(sources[key])
        else:
            checks = [e for e in sub if e['stage'] == 'check' and e['outcome'] == 'started' and e.get('artifact') in artifacts]
            if checks:
                pair = json.loads(artifacts[checks[-1]['artifact']]); (cohort / (r['scenario']+'-accepted.'+ext)).write_text(pair[ext])
    responses = []
    for e in sub:
        if e['stage'] != 'source' or e['outcome'] != 'response': continue
        if e.get('artifact') not in artifacts: responses.append({'capture':'evicted_or_missing'}); continue
        message = json.loads(artifacts[e['artifact']]); calls = []
        for call in message.get('tool_calls', []):
            f = call['function']; args = json.loads(f['arguments'])
            calls.append({'name': f['name'], 'argumentChars':len(f['arguments']), 'file':args.get('file'), 'edits':len(args.get('edits', [])), 'start_line':args.get('start_line'), 'end_line':args.get('end_line'), 'reason':args.get('reason'), 'customCharacters':sum(len(s.get('body','')) for s in args.get('custom',[])), 'operations':dict(collections.Counter(n.get('op') for n in args.get('nodes',[])))})
        responses.append({'calls':calls})
    operations.append({'scenario':r['scenario'],'responses':responses})
(cohort / 'numbers.json').write_text(json.dumps(numbers, indent=2)+'\n')
(cohort / 'native-tool-observations.json').write_text(json.dumps(operations, indent=2)+'\n')
missing = sorted({e['artifact'] for e in es if e.get('artifact') and e['artifact'] not in artifacts})
verification = {'events':len(es),'retainedArtifacts':len(artifacts),'missingArtifacts':missing,
                'summaries':summaries,'semanticFeedbackToGeneration':any(e['stage']=='candidate-test' for e in es),
                'nativeResponsesRetained':sum(e['stage']=='source' and e['outcome']=='response' for e in es),
                'completePairChecksRetained':sum(e['stage']=='check' and e['outcome']=='started' for e in es),
                'sourceDigests':{p.name:hashlib.sha256(p.read_bytes()).hexdigest() for p in cohort.glob('*-accepted.*')}}
(out / 'verification.json').write_text(json.dumps(verification, indent=2)+'\n')
manifest = out / 'previous-evidence-manifest.json'
if manifest.exists():
    old = json.loads(manifest.read_text()); changed = [p for p,h in old.items() if not pathlib.Path(p).exists() or hashlib.sha256(pathlib.Path(p).read_bytes()).hexdigest()!=h]
    (out / 'historical-integrity.json').write_text(json.dumps({'checked':len(old),'changed':changed},indent=2)+'\n')
for n in numbers: print(n['scenario'],n.get('runMs'),n.get('generationToRenderMs'),n.get('modelMs'),n.get('checkingMs'),n.get('tokens'),n.get('semantic'),n.get('composition'))
print('Missing capture artifacts:',len(missing),'(not missing metrics; consult latest summaries)')
