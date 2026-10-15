#!/usr/bin/env python3
"""Capture the current preparation screen and check its presentation boundary.

Run for each actual UI state after normal navigation. This is a structural
regression, NOT visual acceptance: screenshots still require an image-capable
reviewer against the active design guide. Does not grant access or run models.
"""
import argparse
import json
import re
from pathlib import Path
import subprocess
import xml.etree.ElementTree as ET

p = argparse.ArgumentParser(description=__doc__)
p.add_argument('--state', required=True, choices=['suggestion', 'pending', 'populated', 'empty', 'failure', 'inspect', 'access'])
p.add_argument('--visible', action='append', default=[], help='Exact task label/value required above the fold; repeat for controls and result')
a = p.parse_args()
out = Path('build/preparation-presentation')
out.mkdir(parents=True, exist_ok=True)

def adb(*args):
    return subprocess.check_output(['adb', *args], timeout=30)

activity = adb('shell', 'dumpsys', 'activity', 'activities').decode()
expected = '.PreparationInspectActivity' if a.state == 'inspect' else '.PreparationActivity'
assert any(expected in line for line in activity.splitlines() if 'topResumedActivity=' in line), 'Open the expected Calendar screen first'
adb('shell', 'uiautomator', 'dump', '/sdcard/preparation-presentation.xml')
xml = adb('shell', 'cat', '/sdcard/preparation-presentation.xml')
(out / f'{a.state}.xml').write_bytes(xml)
(out / f'{a.state}.png').write_bytes(adb('exec-out', 'screencap', '-p'))
nodes = list(ET.fromstring(xml).iter('node'))
text = [n.get('text', '') for n in nodes if n.get('text')]
width, height = map(int, re.findall(r'\d+', ET.fromstring(xml).find('node').get('bounds'))[2:])

def fully_visible(node):
    left, top, right, bottom = map(int, re.findall(r'\d+', node.get('bounds')))
    return 0 <= left < right <= width and 0 <= top < bottom <= height

for label in a.visible:
    matches = [n for n in nodes if n.get('text') == label]
    assert matches, f'Missing task content: {label}'
    assert any(fully_visible(n) for n in matches), f'Task content clipped/offscreen: {label}'
descriptions = [n.get('content-desc', '') for n in nodes if n.get('content-desc')]
if a.state not in ['inspect', 'access']:
    assert 'Inspect' in text and 'Back' in descriptions
    assert not any(n.get('class') == 'android.widget.Switch' for n in nodes), 'Inline grant control leaked into task UI'
    for forbidden in ['On-device discovery uses', 'Allow Calendar capabilities', 'Generation failed:', 'Discovery failed:', 'java.lang.', 'UI2004', 'ATTEMPT_LIMIT', 'Create this tool?', 'Send this event', 'Cortex', 'Cerebras']:
        assert not any(forbidden in s for s in text), f'Diagnostic copy leaked: {forbidden}'
    if a.state == 'suggestion':
        assert text.count('Open') == 1 and 'Dismiss' in descriptions
    elif a.state == 'pending':
        assert sum(s in ['Preparing…', 'Looking ahead…'] for s in text) == 1
    elif a.state == 'populated':
        assert 'Open' not in text and 'Preparing…' not in text and 'Looking ahead…' not in text
    elif a.state == 'failure':
        assert any(s in ['Couldn’t find help', 'Preparation unavailable', 'Couldn’t create this tool', 'Tool unavailable', 'Couldn’t save reminder'] for s in text)
receipt = {'state': a.state, 'structuralChecks': 'passed', 'visualReview': 'required', 'requiredVisible': a.visible, 'texts': text, 'descriptions': descriptions}
(out / f'{a.state}.json').write_text(json.dumps(receipt, ensure_ascii=False, indent=2))
print(f'Structural checks passed: {a.state}. Screenshot: {out / (a.state + ".png")}. Visual review still required.')
