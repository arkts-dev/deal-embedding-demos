#!/usr/bin/env python3
"""Replay an explicitly named accepted source through real Compose/Rental/review.

No inference. A replay receipt never substitutes for the fresh captured model dialogue.
Supports the current task-facing catalogue and editable source contracts without rewriting them.
"""
import argparse, hashlib, json, pathlib, runpy, time, xml.etree.ElementTree as ET
m = runpy.run_path(str(pathlib.Path(__file__).with_name('assistance-device.py')))
shell, tap, scrolltap, capture, back = (m[k] for k in ('shell', 'tap', 'scrolltap', 'capture', 'back'))
p = argparse.ArgumentParser(description=__doc__)
p.add_argument('--workspace', required=True)
p.add_argument('--editable', action='store_true')
a = p.parse_args()
PKG='dev.deal.apps.organizer'
def private(package, name):return shell('run-as', package, 'cat', name)
def source():
    prefs={n.get('name'):n.text for n in ET.fromstring(private(PKG,'shared_prefs/organizer-experience.xml'))}
    return {k:prefs['workspace.'+a.workspace+'.'+k] for k in ('deal','dealui')}
def provider():
    # RentalStore persists quotes/reservations in rental.xml. Read failures must
    # fail the test, never masquerade as unchanged empty provider state.
    r=ET.fromstring(private('dev.deal.apps.rental','shared_prefs/rental.xml'))
    return {n.get('name'):(n.tag,n.text,n.get('value')) for n in r if n.get('name')!='allowed'}
def snapshot():return json.loads(private(PKG,'files/workspace-snapshot.json'))
def nodes(n):
    yield n
    for c in n['children']:yield from nodes(c)
def props(n):return {p['name']:p for p in n['props']}
def wait(predicate):
    end=time.monotonic()+30
    while time.monotonic()<end:
        s=snapshot();assert not s.get('fault'),s.get('fault')
        if predicate(s):return s
        time.sleep(.2)
    raise AssertionError('Operation did not complete')
def rows(s):return [n for n in nodes(s['tree']) if n['component']=='ui.Option']
def control(kind,prop,value):
    return next(n for n in nodes(snapshot()['tree']) if n['component']=='ui.'+kind and props(n).get(prop,{}).get('stringValue')==value)
def field(label):return control('IntField','label',label)
def field_tap(label,direction):
    n=field(label);label=props(n)['accessibilityLabel']['stringValue']
    try:tap(label+' '+direction)
    except AssertionError:
        for _ in range(4):m['swipe']('up')
        tap(label+' '+direction)
def load():
    for _ in range(4):m['swipe']('up')
    scrolltap('Load options');return wait(lambda s:len(rows(s))>0)
def prepare(total):
    row=next(n for n in rows(snapshot()) if props(n)['value']['stringValue']=='stand')
    assert '€'+total in props(row)['price']['stringValue']
    scrolltap(props(row)['title']['stringValue'])
    button=next(n for n in nodes(snapshot()['tree']) if n['component']=='ui.Button' and props(n).get('text',{}).get('stringValue','').startswith('Prepare'))
    scrolltap(props(button)['text']['stringValue']);scrolltap('Review selection')
    r=capture('live-'+('source' if a.editable else 'catalogue')+'-review-'+total)
    assert 'Not booked' in m['labels'](r) and '€'+total in m['labels'](r)
    back()
before=provider();original=source()
shell('am','start','-S','-W','--user','0','-n',PKG+'/.OrganizerActivity','--es','replayWorkspace',a.workspace)
time.sleep(1)
assert not rows(snapshot()),'Cold opening must not fetch inventory'
assert 'Review selection' not in m['labels'](m['ui']()),'Cold opening must not expose a stale prepared selection'
load();prepare('8.00')
if a.editable:
    field_tap('Quantity','increase');wait(lambda s:not rows(s))
    assert 'Review selection' not in m['labels'](m['ui']()),'Stale native review entrance'
    load();prepare('16.00')
    field_tap('Total budget (€)','decrease');field_tap('Total budget (€)','decrease');field_tap('Total budget (€)','decrease')
    scrolltap('Load options');wait(lambda s:any('No matching equipment' in props(n).get('value',{}).get('stringValue','') for n in nodes(s['tree'])))
    assert not rows(snapshot())
    for _ in range(15):field_tap('Total budget (€)','decrease')
    n=field('Total budget (€)');assert props(n)['value']['intValue']==0 and props(n)['error']['stringValue']
    button=control('Button','text','Load options');assert not props(button)['enabled']['booleanValue']
    field_tap('Total budget (€)','increase');assert props(control('Button','text','Load options'))['enabled']['booleanValue']
    for _ in range(17):field_tap('Total budget (€)','increase')
    for _ in range(5):m['swipe']('up')
    scrolltap('Search equipment options');shell('input','keycombination','113','29');shell('input','text','zzzz');back()
    scrolltap('Load options');wait(lambda s:any('No matching equipment' in props(n).get('value',{}).get('stringValue','') for n in nodes(s['tree'])))
    assert not rows(snapshot())
    for _ in range(5):m['swipe']('up')
    scrolltap('Search equipment options');shell('input','keycombination','113','29');shell('input','text','adapter');back();load()
    prepare('16.00')
    # Identical staging twice must still expose the newly prepared review.
    load();prepare('16.00')
assert source()==original,'Saved source was altered'
assert provider()==before,'Provider persistence changed'
receipt={'savedSourceReplay':True,'newModelCalls':0,'editable':a.editable,'realRentalAndNativeReview':True,'explicitReadsOnly':True,'unchangedSource':True,'unchangedProviderPersistence':True,'sourcePairSha256':hashlib.sha256((original['deal']+'\0'+original['dealui']).encode()).hexdigest()}
path=pathlib.Path('build/assistance/audit')/('live-source-receipt.json' if a.editable else 'live-catalogue-receipt.json');path.write_text(json.dumps(receipt,indent=2)+'\n');print(json.dumps(receipt,indent=2))
