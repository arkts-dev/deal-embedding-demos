#!/usr/bin/env python3
"""Native requirement-led journey using clearly authored fixture source and real Rental reads.

Run AssistanceInstrumentation first. This never invokes a model. Leaves test source/link
for inspection; an explicit fixture deletion through Inspect removes its link.
"""
import hashlib
import json
import os
import pathlib
import re
import subprocess
import time
import xml.etree.ElementTree as ET

ADB = [str(pathlib.Path.home() / 'Android/Sdk/platform-tools/adb')]
if os.environ.get('ANDROID_SERIAL'):
    ADB += ['-s', os.environ['ANDROID_SERIAL']]
PACKAGE = 'dev.deal.apps.organizer'
OUT = pathlib.Path('build/assistance/device')
OUT.mkdir(parents=True, exist_ok=True)


def shell(*args):
    return subprocess.check_output(ADB + ['shell', *args], text=True, timeout=45)


def private(package, path):
    return shell('run-as', package, 'cat', path)


def ui():
    shell('uiautomator', 'dump', '/sdcard/organizer-assistance.xml')
    return ET.fromstring(shell('cat', '/sdcard/organizer-assistance.xml'))


def find(label, scroll=False):
    for _ in range(12 if scroll else 1):
        for n in ui().iter('node'):
            if label in (n.get('text'), n.get('content-desc')) and n.get('bounds') != '[0,0][0,0]':
                return n
        if scroll:
            shell('input', 'swipe', '540', '1850', '540', '1050', '300')
    raise AssertionError('Missing: ' + label)


def tap(label, scroll=False):
    n = find(label, scroll)
    assert n.get('enabled') == 'true', label + ' is disabled'
    x1, y1, x2, y2 = map(int, re.findall(r'\d+', n.get('bounds')))
    shell('input', 'tap', str((x1+x2)//2), str((y1+y2)//2))
    time.sleep(.6)


def snapshot():
    return json.loads(private(PACKAGE, 'files/workspace-snapshot.json'))


def nodes(n):
    yield n
    for child in n['children']:
        yield from nodes(child)


def prop(n, key):
    return next(p for p in n['props'] if p['name'] == key)


def wait(test, seconds=60):
    end = time.monotonic() + seconds
    while time.monotonic() < end:
        if test():
            return
        time.sleep(.5)
    raise AssertionError('Timed out')


def screenshot(name):
    (OUT / (name+'.xml')).write_text(ET.tostring(ui(), encoding='unicode'))
    (OUT / (name+'.png')).write_bytes(subprocess.check_output(ADB+['exec-out','screencap','-p'], timeout=30))


def ordinary():
    labels = [n.get('text', '') for n in ui().iter('node')]
    assert labels.count('Inspect') == 1
    assert not any(t in ('Workspace', 'Template', 'AI-built', 'deal', 'deal ui', 'Details', 'Execution', 'Native') or t.startswith('Workspaces ·') for t in labels)


def provider_state():
    result = {}
    for name in ('rental.xml', 'proposals.xml'):
        p = subprocess.run(ADB + ['shell','run-as','dev.deal.apps.rental','cat','shared_prefs/'+name], text=True, capture_output=True)
        result[name] = {n.get('name'): n.text for n in ET.fromstring(p.stdout) if n.get('name') != 'allowed'} if p.returncode == 0 else {}
    return result


before = provider_state()
prefs = private(PACKAGE, 'shared_prefs/organizer-experience.xml')
source_hash = hashlib.sha256(prefs.encode()).hexdigest()
start = int(time.time()*1000)
shell('am', 'start', '-S', '-W', '--user', '0', '-n', PACKAGE+'/.OrganizerActivity')
time.sleep(2)
ordinary()
find('Needs attention')
screenshot('overview')
tap('Find equipment')
wait(lambda: any(n.get('text') == 'Equipment options' for n in ui().iter('node')))
ordinary()
screenshot('opened')
assert not any(n['component'] == 'ui.Option' for n in nodes(snapshot()['tree']))
tap('Quantity increase', True)
tap('Load options', True)
wait(lambda: any(n['component'] == 'ui.Option' for n in nodes(snapshot()['tree'])), 30)
ordinary()
loaded = list(nodes(snapshot()['tree']))
assert snapshot().get('fault', '') == ''
row = next(n for n in loaded if n['component'] == 'ui.Option' and prop(n,'value')['stringValue'] == 'stand')
assert prop(row,'price')['stringValue'].replace(' ','') == '€16.00'
tap('Boom microphone stand', True)
tap('Prepare for review', True)
wait(lambda: any(n.get('text') == 'Review selection' for n in ui().iter('node')), 30)
tap('Review selection')
ordinary()
find('Not booked')
find('Boom microphone stand', True)
screenshot('review')
assert before == provider_state()
tap('Inspect')
find('Observed activity · not AI reasoning')
tap('Source')
tap('View .deal')
find('.deal · untrusted source')
screenshot('source')
tap('Close')
tap('Activity')
screenshot('activity')
tap('Back')
shell('input','keyevent','4')
time.sleep(.5)
tap('Back to requirement')
find('Boom stand')
tap('Find equipment', True)
wait(lambda: any(n.get('text') == 'Equipment options' for n in ui().iter('node')))
assert prop(next(n for n in nodes(snapshot()['tree']) if n['component']=='ui.IntField' and prop(n,'accessibilityLabel')['stringValue']=='Quantity'),'value')['intValue'] == 2
shell('am','start','-S','-W','--user','0','-n',PACKAGE+'/.OrganizerActivity')
time.sleep(2)
tap('Find equipment')
wait(lambda: any(n.get('text') == 'Equipment options' for n in ui().iter('node')))
assert prop(next(n for n in nodes(snapshot()['tree']) if n['component']=='ui.IntField' and prop(n,'accessibilityLabel')['stringValue']=='Quantity'),'value')['intValue'] == 1
assert not any(n['component']=='ui.Option' for n in nodes(snapshot()['tree']))
assert hashlib.sha256(private(PACKAGE,'shared_prefs/organizer-experience.xml').encode()).hexdigest() == source_hash
assert before == provider_state()
events = []
for page in ('previous.jsonl','events.jsonl'):
    p = subprocess.run(ADB+['shell','run-as',PACKAGE,'cat','files/embedding-log/'+page],capture_output=True,text=True)
    events += [json.loads(line) for line in p.stdout.splitlines() if line.startswith('{')]
fresh = [e for e in events if int(e['time']) >= start]
assert not any(e['stage'] in ('choice','source','model-provider') for e in fresh)
assert {'check','compiler-ui','compiler-deal','mount','action','capability','transport','effect-delivery','snapshot'} <= {e['stage'] for e in fresh}
receipt = {'fixtureSource': True, 'realToolchainSandboxAndRental': True, 'modelCalls': 0, 'requirementEntry': True, 'nativeSelectionReview': True, 'resume': True, 'coldRecheck': True, 'sourceAndProviderStateUnchanged': True, 'inspectSourceAndActivity': True, 'events': len(fresh)}
(OUT/'receipt.json').write_text(json.dumps(receipt,indent=2)+'\n')
print(json.dumps(receipt,indent=2))
