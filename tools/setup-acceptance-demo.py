#!/usr/bin/env python3
"""Add labelled acceptance facts only; preserve records, saved tools and authorities."""
import datetime as dt
import json
import os
import subprocess
import xml.etree.ElementTree as ET
from zoneinfo import ZoneInfo

ADB = ['adb'] + (['-s', os.environ['ANDROID_SERIAL']] if os.environ.get('ANDROID_SERIAL') else [])
DAY = dt.date(2026, 10, 23)


def shell(*args):
    return subprocess.check_output(ADB + ['shell', *args], text=True).strip()


def at(hour, minute=0):
    return int(dt.datetime.combine(DAY, dt.time(hour, minute), ZoneInfo('Europe/Berlin')).timestamp() * 1000)


def update(app, name, key, transform):
    package = 'dev.deal.apps.' + app
    shell('am', 'force-stop', package)
    path = 'shared_prefs/' + name + '.xml'
    raw = subprocess.check_output(ADB + ['exec-out', 'run-as', package, 'cat', path], text=True)
    root = ET.fromstring(raw)
    node = next(n for n in root if n.get('name') == key)
    node.text = json.dumps(transform(json.loads(node.text)))
    subprocess.run(ADB + ['shell', 'run-as', package, 'sh', '-c', '"cat > ' + path + '"'],
                   input=ET.tostring(root, encoding='unicode'), text=True, check=True)


def equipment(show):
    if not any(r['id'] == 'acceptance-equipment' for r in show['requirements']):
        show['requirements'].insert(0, dict(id='acceptance-equipment', act='Static Bloom', department='Sound',
            group='Acceptance demo', name='Vocal kit comparison', quantity=1,
            specification='One adjustable boom stand with clip adapter AND one balanced XLR female to male cable, minimum 10 m.',
            location='The Foundry, stage centre', **{'from': at(16, 15)}, until=at(22, 30),
            mandatory=True, substitutions='Exact specification or better; no shorter cable', dependencies=[],
            notes='Synthetic acceptance demo. Unfinished hire comparison. Compare package against separate items using actual Rental catalogue and read-only quotes. Hire budget EUR 20.00, editable; refundable deposit separate. Period 23 October 2026 16:15 to 22:30 Europe/Berlin. No booking authorized.',
            riderRef='Acceptance demo', supply='', coverage='Missing', commitment='None', verified=False))
    return show


def food(tasks):
    if not any(t['id'] == 'acceptance-food' for t in tasks):
        tasks.insert(0, dict(id='acceptance-food', title='Acceptance food planning', assignee='Morgan', due=at(18, 30),
            location='The Foundry, stage entrance', show='Acceptance demo',
            instructions='Synthetic acceptance demo. Unfinished food plan: six 30 cm pizzas, at least two vegan, budget EUR 80.00 including delivery, before 18:30. Read actual Pizza menu and delivery windows on explicit Load. Editable quantities and budget; recompute total and feasibility. Read-only planning, no order or proposal authorized.',
            checklist=[], complete=False, minutes=15, links=[]))
    return tasks


if __name__ == '__main__':
    update('organizer', 'organizer', 'show', equipment)
    update('todo', 'provider', 'tasks', food)
    # Additive Calendar fixture setup via the test APK, not production scenario branches.
    receipt = shell('am', 'instrument', '--user', '0', '-w',
                    'dev.deal.connectors.tests/.AcceptanceFactsInstrumentation')
    if 'INSTRUMENTATION_CODE: 0' not in receipt or 'INSTRUMENTATION_RESULT: failure' in receipt:
        raise RuntimeError('Calendar acceptance fixture setup failed: ' + receipt)
    print('Acceptance facts added. Existing records, saved tools, grants and sharing choices preserved.')
