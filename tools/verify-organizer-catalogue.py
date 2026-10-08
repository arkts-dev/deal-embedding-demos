#!/usr/bin/env python3
"""One real Organizer generation via native controls, then catalogue selection/native review.

Requires Organizer/Rental installed, Rental consent granted and the development gateway.
Keeps receipts and accepted source only in ignored build/. Never retries generation.
"""
import argparse
import hashlib
import json
import os
import pathlib
import re
import subprocess
import time
import xml.etree.ElementTree as ET

ROOT = pathlib.Path(__file__).resolve().parents[1]
ADB = [str(pathlib.Path.home() / 'Android/Sdk/platform-tools/adb')]
if os.environ.get('ANDROID_SERIAL'):
    ADB += ['-s', os.environ['ANDROID_SERIAL']]
PACKAGE = 'dev.deal.apps.organizer'


def shell(*args):
    return subprocess.check_output(ADB + ['shell', *args], text=True, timeout=45)


def ui():
    shell('uiautomator', 'dump', '/sdcard/catalogue-verification.xml')
    return ET.fromstring(shell('cat', '/sdcard/catalogue-verification.xml'))


def find(label, scroll=False):
    for _ in range(7 if scroll else 1):
        for node in ui().iter('node'):
            if label in (node.get('text'), node.get('content-desc')) and node.get('bounds') != '[0,0][0,0]':
                return node
        if scroll:
            shell('input', 'swipe', '540', '1900', '540', '750', '300')
    raise AssertionError('Missing native control: ' + label)


def tap(label, scroll=False):
    node = find(label, scroll)
    left, top, right, bottom = map(int, re.findall(r'\d+', node.get('bounds')))
    shell('input', 'tap', str((left + right) // 2), str((top + bottom) // 2))
    time.sleep(.5)


def private(package, file):
    result = subprocess.run(ADB + ['shell', 'run-as', package, 'cat', file], text=True, capture_output=True, timeout=15)
    if result.returncode:
        return None
    return result.stdout


def provider_state():
    result = {}
    for filename in ('rental.xml', 'proposals.xml'):
        raw = private('dev.deal.apps.rental', 'shared_prefs/' + filename)
        prefs = {n.get('name'): n.text for n in ET.fromstring(raw)} if raw else {}
        result[filename] = {k: v for k, v in prefs.items() if k != 'allowed'}
    return result


def snapshot():
    return json.loads(private(PACKAGE, 'files/workspace-snapshot.json'))


def nodes(tree):
    yield tree
    for child in tree['children']:
        yield from nodes(child)


def prop(node, name):
    return next(p for p in node['props'] if p['name'] == name)


def wait(test, seconds=240):
    deadline = time.monotonic() + seconds
    while time.monotonic() < deadline:
        if test():
            return
        time.sleep(.5)
    raise AssertionError('Timed out; inspect private generation trace and workspace snapshot')


parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--replay', metavar='SAVED_ID', help='Replay an already accepted source; never call inference')
args = parser.parse_args()
before = provider_state()
previous_raw = private(PACKAGE, 'shared_prefs/organizer-experience.xml')
previous_keys = {n.get('name') for n in ET.fromstring(previous_raw)} if previous_raw else set()
launch = ['am', 'start', '-S', '-W', '--user', '0', '-n', PACKAGE + '/.OrganizerActivity']
if args.replay:
    launch += ['--es', 'replayWorkspace', args.replay]
shell(*launch)
time.sleep(2)
if not args.replay:
    tap('Missing: Boom stand', scroll=True)
    tap('Arrange fulfilment', scroll=True)
    # The single authorized inference workflow. Do not retry this click.
    tap('Build workspace')
wait(lambda: any(n.get('text') == 'DEAL WORKSPACE · generated' for n in ui().iter('node')))
prefs = {n.get('name'): n.text for n in ET.fromstring(private(PACKAGE, 'shared_prefs/organizer-experience.xml'))}
if args.replay:
    source_keys = {'workspace.' + args.replay + '.deal', 'workspace.' + args.replay + '.dealui'}
else:
    source_keys = {k for k in prefs if k not in previous_keys and (k.endswith('.deal') or k.endswith('.dealui'))}
assert len(source_keys) == 2, 'Cannot identify the accepted source pair'
accepted = {k: prefs[k] for k in sorted(source_keys)}
if not args.replay:
    (ROOT / 'build/catalogue-real-source.json').write_text(json.dumps(accepted, indent=2))
tap('Load options', scroll=True)
wait(lambda: any(n['component'] == 'ui.Option' for n in nodes(snapshot()['tree'])), 30)
loaded = snapshot()
assert loaded.get('fault', '') == '', loaded.get('fault')
rows = [n for n in nodes(loaded['tree']) if n['component'] == 'ui.Option']
assert {prop(n, 'value')['stringValue'] for n in rows} == {'stand', 'package'}
assert {prop(n, 'price')['stringValue'] for n in rows} == {'€8.00', '€12.00'}
assert before == provider_state(), 'Loading changed provider persistent state'
tap('Boom microphone stand', scroll=True)
picked = snapshot()
stand = next(n for n in nodes(picked['tree']) if n['component'] == 'ui.Option' and prop(n, 'value')['stringValue'] == 'stand')
assert prop(stand, 'selected')['booleanValue']
tap('Prepare for review', scroll=True)
wait(lambda: any(p.get('stringValue') == 'Prepared for native review. No reservation has been made.' for n in nodes(snapshot()['tree']) for p in n['props']), 30)
review = next(n.get('text') for n in ui().iter('node') if n.get('text', '').startswith('Review '))
tap(review)
texts = [n.get('text', '') for n in ui().iter('node')]
assert 'Prepared operations' in texts and 'Boom microphone stand' in texts and '€8.00' in texts
assert any('Item stand:1' in t and 'Period:' in t for t in texts)
assert before == provider_state(), 'Preparation/native review changed provider persistent state'
receipt = {'realGeneration': not bool(args.replay), 'savedSourceReplay': bool(args.replay), 'nativeControls': True, 'relevantRows': len(rows), 'prices': ['€8.00', '€12.00'],
           'selectionPublished': True, 'nativeReviewPopulated': True, 'providerPersistentStateUnchanged': True,
           'sourceHash': hashlib.sha256(json.dumps(accepted, sort_keys=True).encode()).hexdigest()}
filename = 'catalogue-replay-verification.json' if args.replay else 'catalogue-real-verification.json'
(ROOT / 'build' / filename).write_text(json.dumps(receipt, indent=2) + '\n')
print(json.dumps(receipt, indent=2))
