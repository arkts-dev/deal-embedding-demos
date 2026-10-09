#!/usr/bin/env python3
"""Test-only frozen native UI states; never evidence of real model/provider execution."""
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
OUT = pathlib.Path('build/assistance/clarity')
OUT.mkdir(parents=True, exist_ok=True)


def shell(*args):
    return subprocess.check_output(ADB + ['shell', *args], text=True, timeout=30).strip()


def ui():
    shell('uiautomator', 'dump', '/sdcard/workspace-clarity.xml')
    return ET.fromstring(shell('cat', '/sdcard/workspace-clarity.xml'))


def find(text):
    for node in ui().iter('node'):
        if node.get('text') == text or node.get('content-desc') == text:
            return node
    raise AssertionError('Missing visible native label: ' + text)


def control_enabled(text):
    root = ui()
    for n in reversed(list(root.iter('node'))):
        if n.get('clickable') == 'true' and any(text in (c.get('text'), c.get('content-desc')) for c in n.iter('node')):
            return n.get('enabled') == 'true'
    return False


def tap(text):
    node = find(text)
    x1, y1, x2, y2 = map(int, re.findall(r'\d+', node.get('bounds')))
    shell('input', 'tap', str((x1 + x2) // 2), str((y1 + y2) // 2))
    time.sleep(.5)


def launch(screen):
    shell('am', 'start', '-S', '-W', '--user', '0', '-n', 'dev.deal.connectors.tests/.WorkspaceClarityActivity', '--es', 'screen', screen)
    time.sleep(1)


def capture(name):
    (OUT / (name + '.xml')).write_text(ET.tostring(ui(), encoding='unicode'))
    (OUT / (name + '.png')).write_bytes(subprocess.check_output(ADB + ['exec-out', 'screencap', '-p'], timeout=30))


def ordinary():
    root = ui()
    labels = [n.get('text', '') for n in root.iter('node')]
    assert sum(t == 'Inspect' for t in labels) == 1
    assert not any(t in ('AI-built', 'Template', 'Workspace', 'deal', 'deal ui', 'Details', 'Execution', 'Native') for t in labels)
    assert not any('private compiler' in t or 'source attempts' in t for t in labels)
    return root


settings = [('system', 'font_scale'), ('secure', 'accessibility_display_daltonizer_enabled'), ('secure', 'accessibility_display_daltonizer')]
previous = {(scope, key): shell('settings', 'get', scope, key) for scope, key in settings}
night_mode = re.search(r'(auto|no|yes|custom)', shell('cmd', 'uimode', 'night')).group(0)
results = []
try:
    for mode in os.environ.get('CLARITY_MODES', 'normal,bedtime').split(','):
        shell('cmd', 'uimode', 'night', 'yes' if mode == 'bedtime' else 'no')
        shell('settings', 'put', 'system', 'font_scale', '1.3' if mode == 'bedtime' else '1.0')
        shell('settings', 'put', 'secure', 'accessibility_display_daltonizer', '0')
        shell('settings', 'put', 'secure', 'accessibility_display_daltonizer_enabled', '1' if mode == 'bedtime' else '0')
        launch('saved')
        ordinary()
        find('Equipment options')
        tap('Review selection')
        ordinary()
        find('Not booked')
        capture(mode + '-review')
        launch('failure')
        ordinary()
        find('Couldn’t prepare options')
        find('Edit request')
        find('Keep previous options')
        capture(mode + '-failure')
        tap('Edit request')
        ordinary()
        assert control_enabled('Find options')
        tap('Find options')
        ordinary()
        assert not control_enabled('Find options')
        assert not control_enabled('Total budget (€) · optional')
        find('Preparing equipment options')
        find('Checking…')
        capture(mode + '-pending')
        tap('Cancel')
        find('Equipment options')
        launch('request')
        find('Find options')
        tap('Preferences')
        find('Anything else? (optional)')
        capture(mode + '-request')
        tap('Inspect')
        find('Test-only engineering surface · frozen presentation facts')
        results.append({'mode': mode, 'taskLanguage': True, 'oneInspect': True, 'reviewBoundary': True, 'failure': True, 'disabledSpinner': True, 'cancel': True})
finally:
    shell('am', 'force-stop', 'dev.deal.connectors.tests')
    for (scope, key), value in previous.items():
        shell('settings', 'delete', scope, key) if value == 'null' else shell('settings', 'put', scope, key, value)
    shell('cmd', 'uimode', 'night', night_mode)
print(json.dumps({'noInference': True, 'presentationFixturesOnly': True, 'settingsRestored': True, 'results': results}, indent=2))
