#!/usr/bin/env python3
"""No-inference native presentation checks, including restored dark/grayscale/font settings."""
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
OUT = pathlib.Path('build/workspace-clarity')
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


settings = [('system', 'font_scale'), ('secure', 'accessibility_display_daltonizer_enabled'), ('secure', 'accessibility_display_daltonizer')]
previous = {(scope, key): shell('settings', 'get', scope, key) for scope, key in settings}
night = shell('cmd', 'uimode', 'night')
night_mode = re.search(r'(auto|no|yes|custom)', night).group(0)
results = []
try:
    for mode in ('normal', 'bedtime'):
        shell('cmd', 'uimode', 'night', 'yes' if mode == 'bedtime' else 'no')
        shell('settings', 'put', 'system', 'font_scale', '1.3' if mode == 'bedtime' else '1.0')
        shell('settings', 'put', 'secure', 'accessibility_display_daltonizer', '0')
        shell('settings', 'put', 'secure', 'accessibility_display_daltonizer_enabled', '1' if mode == 'bedtime' else '0')
        for screen, label in [('ai', 'AI-built workspace'), ('catalogue', 'Catalogue workspace'), ('saved', 'Saved-source workspace')]:
            launch(screen)
            find(label)
            find('Source details')
            find('Organizer review · 1')
            capture(mode + '-' + screen)
            tap('Source details')
            texts = [n.get('text', '') for n in ui().iter('node')]
            assert any(('Source attempts: 2' if screen == 'ai' else 'model selected' if screen == 'catalogue' else 'not recorded') in t for t in texts)
            tap('Close details')
            tap('Organizer review · 1')
            find('Organizer review · native')
            assert any('not reservations' in n.get('text', '') for n in ui().iter('node'))
            capture(mode + '-' + screen + '-review')
        launch('failure')
        find('Workspace couldn’t be built')
        find('Change request')
        find('Keep existing workspace')
        find('Build again · new run')
        texts = [n.get('text', '') for n in ui().iter('node')]
        assert any('3 source attempts' in t and 'unchanged' in t for t in texts)
        assert not any('private compiler diagnostics' in t for t in texts)
        capture(mode + '-exhausted')
        tap('Change request')
        find('Build workspace')
        tap('Build workspace')
        find('Building your workspace')
        find('Checking AI-written code · attempt 2 of 3')
        find('Cancel generation')
        capture(mode + '-progress')
        tap('Cancel generation')
        find('Saved-source workspace')
        results.append({'mode': mode, 'originLabels': True, 'details': True, 'nativeReviewBoundary': True, 'exhaustion': True, 'progress': True, 'cancelControl': True})
finally:
    shell('am', 'force-stop', 'dev.deal.connectors.tests')
    for (scope, key), value in previous.items():
        if value == 'null':
            shell('settings', 'delete', scope, key)
        else:
            shell('settings', 'put', scope, key, value)
    shell('cmd', 'uimode', 'night', night_mode)
print(json.dumps({'noInference': True, 'settingsRestored': True, 'results': results}, indent=2))
