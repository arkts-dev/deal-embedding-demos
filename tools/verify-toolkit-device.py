#!/usr/bin/env python3
"""Real compiled gallery: native actions, retention and adaptive bounds. Visual review separate."""
import json
import os
import pathlib
import re
import subprocess
import time
import xml.etree.ElementTree as ET

ROOT = pathlib.Path(__file__).resolve().parents[1]
OUT = ROOT / 'build/toolkit/device'
OUT.mkdir(parents=True, exist_ok=True)
ADB = ['adb'] + (['-s', os.environ['ANDROID_SERIAL']] if os.environ.get('ANDROID_SERIAL') else [])


def shell(*args):
    return subprocess.check_output(ADB + ['shell', *args], text=True, timeout=30).strip()


def ui():
    shell('uiautomator', 'dump', '/sdcard/toolkit-gallery.xml')
    return ET.fromstring(shell('cat', '/sdcard/toolkit-gallery.xml'))


def bounds(n):
    return tuple(map(int, re.findall(r'\d+', n.get('bounds'))))


def label(text, root=None):
    matches = [n for n in (root if root is not None else ui()).iter('node') if text in (n.get('text'), n.get('content-desc'))]
    assert matches, 'Missing: ' + text
    return matches[0]


def tap(text):
    root = ui()
    matches = [n for n in root.iter('node') if n.get('clickable') == 'true' and any(text in (c.get('text'), c.get('content-desc')) for c in n.iter('node'))]
    assert matches, 'Missing action: ' + text
    x1, y1, x2, y2 = bounds(matches[-1])
    shell('input', 'tap', str((x1+x2)//2), str((y1+y2)//2))
    time.sleep(.4)


def launch(example='comparison', retain=True, fail=False):
    shell('am', 'start', '-S', '-W', '-n', 'dev.deal.connectors.tests/.ToolkitActivity', '--es', 'example', example, '--ez', 'retain', str(retain).lower(), '--ez', 'fail', str(fail).lower())
    end = time.monotonic()+25
    expected = 'Approved budget (€)' if example == 'comparison' else 'Refundable deposit' if example == 'values' else 'Read value'
    while time.monotonic() < end:
        r = ui()
        if any(expected in (n.get('text'), n.get('content-desc')) for n in r.iter('node')):
            return r
        assert not any(n.get('text') == 'Unable to open' for n in r.iter('node')), 'Gallery failed to open'
    raise AssertionError('Gallery opening timeout')


def capture(name):
    root = ui()
    ET.ElementTree(root).write(OUT / (name+'.xml'))
    (OUT / (name+'.png')).write_bytes(subprocess.check_output(ADB + ['exec-out', 'screencap', '-p'], timeout=30))
    return root


font = shell('settings', 'get', 'system', 'font_scale')
size = shell('wm', 'size')
night = shell('cmd', 'uimode', 'night').split(':')[-1].strip()
results = []
try:
    shell('settings', 'put', 'system', 'font_scale', '1.0')
    shell('wm', 'size', 'reset')
    shell('cmd', 'uimode', 'night', 'no')
    launch(); tap('Reset amounts')
    r = capture('budget-light')
    label('Over budget', r); label('€17.50', r)
    tap('Approved budget (€) increase')
    label('€16.50')
    tap('Inspect'); label('Back'); tap('Back'); label('€16.50')
    launch(); label('€16.50')
    tap('Reset amounts'); label('€17.50')
    results.append('Native step payload changes comparison; Inspect roundtrip and actual force-stop retain edited result without inference')
    launch('values', retain=False)
    r = capture('values-light')
    for text in ['Package', 'Separate items', 'Refundable deposit', '€12.00', '€13.00', '€50.00', 'Travel', 'Latest departure']:
        label(text, r)
    assert bounds(label('Separate items', r))[1] > bounds(label('Package', r))[1], 'Phone comparison should use aligned label/value rows'
    assert bounds(label('€12.00', r))[2] == bounds(label('€13.00', r))[2], 'Comparison amount column must align'
    shell('wm', 'size', '840x1885')
    shell('settings', 'put', 'system', 'font_scale', '1.6')
    launch('values', retain=False)
    r = capture('values-narrow-large')
    assert bounds(label('Separate items', r))[1] > bounds(label('Package', r))[1], 'Large text should wrap comparison'
    for n in r.iter('node'):
        if n.get('text') in ['Package', 'Separate items', '€12.00', '€13.00', 'Refundable deposit', '€50.00']:
            x1,y1,x2,y2=bounds(n)
            assert 0 <= x1 < x2 <= 840, (n.get('text'), n.get('bounds'))
    results.append('Phone comparison amounts align; narrow width / 1.6 font scale stacks values with visible essential bounds on screen')
    shell('wm', 'size', 'reset'); shell('settings', 'put', 'system', 'font_scale', '1.0')
    shell('cmd', 'uimode', 'night', 'yes')
    launch(); capture('budget-dark')
    launch('values', retain=False); capture('values-dark')
    shell('cmd', 'uimode', 'night', 'no')
    shell('run-as','dev.deal.connectors.tests','rm','-f','files/toolkit-pending.json')
    launch('generation-example', retain=False, fail=True)
    capture('reading-empty'); tap('Read value')
    # Capture pixels immediately: UIAutomator waits for idle and can miss short progress states.
    (OUT / 'reading-pending.png').write_bytes(subprocess.check_output(ADB + ['exec-out', 'screencap', '-p'], timeout=30))
    time.sleep(3); capture('reading-failure'); label('Check access and try again')
    pending=json.loads(shell('run-as','dev.deal.connectors.tests','cat','files/toolkit-pending.json'))
    def checked_nodes(node):
        return [node]+[n for child in node['children'] for n in checked_nodes(child)]
    action=next(n for n in checked_nodes(pending) if n['component'].endswith('.PrimaryAction'))
    assert next(p['booleanValue'] for p in action['props'] if p['name']=='enabled') is False
    (OUT / 'reading-pending.json').write_text(json.dumps(pending,indent=2)+'\n')
    tap('Try again')
    time.sleep(3); label('Check access and try again')
    launch('generation-example', retain=False); tap('Read value'); time.sleep(3); label('42'); capture('reading-success')
    results.append('Native primary/retry controls execute explicit async failure and success through ExperienceSurface')
    print('\n'.join('PASS '+s for s in results))
    print('Screenshots captured; native visual approval is not asserted by this script.')
finally:
    shell('am', 'force-stop', 'dev.deal.connectors.tests')
    shell('settings', 'put', 'system', 'font_scale', font)
    override = re.search(r'Override size: (\d+x\d+)', size)
    shell('wm', 'size', override.group(1) if override else 'reset')
    shell('cmd', 'uimode', 'night', night)
    (OUT / 'receipt.json').write_text(json.dumps({'checks': results, 'visualReview': 'separate', 'liveInference': False}, indent=2)+'\n')
