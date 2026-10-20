#!/usr/bin/env python3
"""Test APK Compose regression: sparse props honor pack defaults; false stays false."""
import os
import pathlib
import re
import subprocess
import time
import xml.etree.ElementTree as ET

ADB = [str(pathlib.Path.home() / 'Android/Sdk/platform-tools/adb')]
if os.environ.get('ANDROID_SERIAL'):
    ADB += ['-s', os.environ['ANDROID_SERIAL']]


def shell(*args):
    return subprocess.check_output(ADB + ['shell', *args], text=True, timeout=30).strip()


def ui():
    shell('uiautomator', 'dump', '/sdcard/renderer-defaults.xml')
    return ET.fromstring(shell('cat', '/sdcard/renderer-defaults.xml'))


def control(label):
    root = ui()
    nodes = list(root.iter('node'))
    for n in reversed(nodes):
        if n.get('clickable') == 'true' and any(label in (c.get('text'), c.get('content-desc')) for c in n.iter('node')):
            return n
    raise AssertionError('Missing control: ' + label)


def tap(label):
    n = control(label)
    x1, y1, x2, y2 = map(int, re.findall(r'\d+', n.get('bounds')))
    shell('input', 'tap', str((x1 + x2) // 2), str((y1 + y2) // 2))
    time.sleep(.3)


def received(value):
    assert any(n.get('text') == value for n in ui().iter('node')), value


try:
    for screen in ('omitted', 'enabled', 'disabled'):
        shell('am', 'start', '-S', '-W', '--user', '0', '-n', 'dev.deal.connectors.tests/.WorkspaceClarityActivity', '--es', 'screen', 'renderer-' + screen)
        time.sleep(1)
        enabled = screen != 'disabled'
        for label in ('Fixture button', 'Fixture option', 'Fixture quantity decrease', 'Fixture quantity increase', 'Default bounds increase', 'Default bounds decrease', 'Fixture text', 'Fixture search'):
            assert (control(label).get('enabled') == 'true') == enabled, (screen, label)
        tap('Fixture button')
        received('Received 1: click' if enabled else 'No action received')
        if enabled:
            tap('Fixture option'); received('Received 2: fixture')
            tap('Fixture quantity increase'); received('Received 3: 3')
            tap('Fixture quantity decrease'); received('Received 3: 1')
            tap('Default bounds increase'); received('Received 6: 3')
            tap('Default bounds decrease'); received('Received 6: 1')
            tap('Fixture text'); shell('input', 'text', 'x'); received('Received 4: x')
            shell('input', 'keyevent', '4')
            tap('Fixture search'); shell('input', 'text', 'y'); received('Received 5: y')
        print(screen + ': native Compose enabled state and action payloads passed')
finally:
    shell('am', 'force-stop', 'dev.deal.connectors.tests')
print('No model or provider calls; test-only sparse portable trees')
