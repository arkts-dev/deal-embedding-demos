#!/usr/bin/env python3
"""Fresh ordinary-tap demo reproduction. Real inference; no route override or retries.

Run --task comparison, then --task editable. Each invocation submits exactly once.
DEAL permits one choice plus at most three source attempts. Never substitute replay.
Private evidence goes to ignored build/demo-reproduction, not tracked source.
"""
import argparse, hashlib, json, pathlib, runpy, threading, time, xml.etree.ElementTree as ET
p=argparse.ArgumentParser(description=__doc__)
p.add_argument('--task', required=True, choices=['comparison','editable'])
a=p.parse_args()
m=runpy.run_path(str(pathlib.Path(__file__).with_name('assistance-device.py')))
out=pathlib.Path('build/demo-reproduction')/a.task; out.mkdir(parents=True,exist_ok=True)
m['capture'].__globals__['OUT']=out
shell,tap,scrolltap,back=(m[k] for k in ('shell','tap','scrolltap','back'))
def private(name):return shell('run-as','dev.deal.apps.organizer','cat',name)
def snapshot():return json.loads(private('files/workspace-snapshot.json'))
def nodes(n):
    yield n
    for c in n['children']:yield from nodes(c)
def props(n):return {v['name']:v for v in n['props']}
def provider():
    r=ET.fromstring(shell('run-as','dev.deal.apps.rental','cat','shared_prefs/rental.xml'))
    return {n.get('name'):(n.tag,n.text,n.get('value')) for n in r}
stop=threading.Event();events={};observer_errors=[]
def observe():
    while not stop.is_set():
        try:
            for page in ['previous.jsonl','events.jsonl']:
                text=private('files/embedding-log/'+page)
                for l in text.splitlines():
                    try:e=json.loads(l)
                    except ValueError:continue
                    if int(e['time'])<started:continue
                    key=(e['trace'],e['sequence'],e['span']);events[key]=e
                    artifact=e.get('artifact')
                    if artifact and not (out/(artifact+'.json')).exists():
                        try:(out/(artifact+'.json')).write_text(private('files/embedding-log/'+artifact+'.json'))
                        except Exception:observer_errors.append('Artifact unavailable: '+artifact)
            (out/'events.jsonl').write_text(''.join(json.dumps(e)+'\n' for e in sorted(events.values(),key=lambda e:int(e['time']))))
        except Exception as e:observer_errors.append(type(e).__name__)
        stop.wait(1)
def wait_rows():
    end=time.monotonic()+40
    while time.monotonic()<end:
        s=snapshot();assert not s.get('fault'),s.get('fault')
        r=[n for n in nodes(s['tree']) if n['component']=='ui.Option']
        if r:return r
        time.sleep(.3)
    m['capture']('read-failure');raise AssertionError('No actual Rental option after explicit Load options')
before=provider()
m['start']();tap('Riders');scrolltap('Boom stand')
labels=m['labels'](m['ui']());tap('View equipment options' if 'View equipment options' in labels else 'Find equipment')
if 'Change request' in m['labels'](m['ui']()):tap('Change request');tap('Continue')
if a.task=='editable':
    tap('Total budget (€) · optional');shell('input','text','18');back();tap('Preferences');scrolltap('Anything else? (optional)');shell('input','text','adapter');back()
m['capture']('request-before-submit');tap('Inspect');tap('Dialogue')
if 'Capture content' in m['labels'](m['ui']()):tap('Capture content');tap('Enable')
back();started=int(time.time()*1000);thread=threading.Thread(target=observe,daemon=True);thread.start()
try:
    tap('Find options');tap('Inspect');tap('Dialogue');m['capture']('dialogue-during-call')
    # Stay in Inspect while real exchanges/checks arrive. No competing navigation.
    deadline=time.monotonic()+1400
    while time.monotonic()<deadline:
        es=list(events.values())
        calls=[e for e in es if e['stage'] in ('choice','source') and e['outcome']=='started']
        assert sum(e['stage']=='choice' for e in calls)<=1,'Unexpected extra choice admission'
        assert sum(e['stage']=='source' for e in calls)<=3,'Source attempt bound exceeded'
        if any(e['stage']=='request' and e['outcome']=='failed' for e in es):
            m['capture']('failed-dialogue');back();m['capture']('failed-user-state');tap('Inspect');tap('Source');m['capture']('failed-engineering-cause');raise AssertionError('Fresh generation failed; no automatic retry')
        if any(e['stage']=='mount' and e['outcome']=='completed' for e in es):break
        time.sleep(1)
    else:raise AssertionError('Fresh generation deadline exceeded; no restart')
    m['capture']('dialogue-complete')
    role='AI reply · Write or repair the interface' if a.task=='editable' else 'AI reply · Choose a suitable interface'
    scrolltap(role);m['capture']('actual-model-reply');tap('Close');tap('Source');m['capture']('source-after-acceptance');back()
    # Re-enter Inspect through accepted assistance to establish trusted provenance.
    tap('Inspect');tap('Source');r=m['capture']('trusted-source-provenance')
    expected='AI-built' if a.task=='editable' else 'Template'
    assert any(expected in t for t in m['labels'](r)),'Observed route differs from requested demonstration'
    tap('View .deal');m['capture']('accepted-deal');tap('Close');tap('View .dealui');m['capture']('accepted-dealui');tap('Close');back()
    s=snapshot();assert not any(n['component']=='ui.Option' for n in nodes(s['tree'])),'Mount unexpectedly read stock'
    if a.task=='editable':
        for _ in range(3):m['swipe']('up')
        n=next(n for n in nodes(s['tree']) if n['component']=='ui.IntField' and props(n).get('label',{}).get('stringValue')=='Quantity')
        tap(props(n)['accessibilityLabel']['stringValue']+' increase')
    scrolltap('Load options');rows=wait_rows();m['capture']('real-rental-results')
    row=next(n for n in rows if props(n)['value']['stringValue']=='stand')
    expected_price='16.00' if a.task=='editable' else '8.00'
    assert expected_price in props(row)['price']['stringValue']
    scrolltap(props(row)['title']['stringValue'])
    n=next(n for n in nodes(snapshot()['tree']) if n['component']=='ui.Button' and props(n).get('text',{}).get('stringValue','').startswith('Prepare'))
    scrolltap(props(n)['text']['stringValue']);scrolltap('Review selection');r=m['capture']('native-not-booked-review')
    assert 'Not booked' in m['labels'](r) and any(expected_price in t for t in m['labels'](r))
    assert provider()==before,'Rental persistence or consent changed'
    calls=[e for e in events.values() if e['stage'] in ('choice','source') and e['outcome']=='started']
    receipt={'freshInference':True,'ordinaryTapsOnly':True,'task':a.task,'choiceCalls':sum(e['stage']=='choice' for e in calls),'sourceCalls':sum(e['stage']=='source' for e in calls),'actualRoute':expected,'realRentalRead':True,'nativeNotBookedReview':True,'providerPersistenceUnchanged':True,'observerWarnings':list(set(observer_errors))}
    (out/'receipt.json').write_text(json.dumps(receipt,indent=2)+'\n');print(json.dumps(receipt,indent=2))
finally:
    stop.set();thread.join(timeout=10)
