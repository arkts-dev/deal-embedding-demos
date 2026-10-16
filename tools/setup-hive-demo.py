#!/usr/bin/env python3
"""Explicit synthetic demo setup. Preserve existing records; never enable sharing/grants."""
import argparse
import datetime as dt
import json
import os
import subprocess
import xml.etree.ElementTree as ET
from zoneinfo import ZoneInfo

parser = argparse.ArgumentParser()
parser.add_argument('--date', default='2026-10-23')
args = parser.parse_args()
day = dt.date.fromisoformat(args.date)
adb = ['adb'] + (['-s', os.environ['ANDROID_SERIAL']] if os.environ.get('ANDROID_SERIAL') else [])


def shell(*parts):
    return subprocess.check_output(adb + ['shell', *parts], text=True).strip()


def prefs(app, name, changes):
    package = 'dev.deal.apps.' + app
    shell('am', 'force-stop', package)
    path = 'shared_prefs/' + name + '.xml'
    result = subprocess.run(adb + ['shell', 'run-as', package, 'cat', path], capture_output=True, text=True)
    root = ET.fromstring(result.stdout) if result.returncode == 0 else ET.Element('map')
    values = {child.get('name'): child for child in root}
    for key, transform in changes.items():
        old = values.get(key)
        value = transform(old.text if old is not None else None)
        if old is not None:
            root.remove(old)
        ET.SubElement(root, 'string', name=key).text = value
    subprocess.run(adb + ['shell', 'run-as', package, 'mkdir', '-p', 'shared_prefs'], check=True)
    subprocess.run(adb + ['shell', 'run-as', package, 'sh', '-c', f'"cat > {path}"'], input=ET.tostring(root, encoding='unicode'), text=True, check=True)


def at(hour, minute=0):
    return int(dt.datetime.combine(day, dt.time(hour, minute), ZoneInfo('Europe/Berlin')).timestamp() * 1000)


def tasks(old):
    retained = [record for record in json.loads(old or '[]') if record.get('show') != 'Mini-hive demo']
    rows = [
        ('collect', 'Collect the vocal kit', 'Alex', 15, 0, 'Rental desk', 'Static Bloom needs one adjustable boom stand with clip adapter and one balanced XLR female → XLR male cable, minimum 10 m, from 16:15 to 22:30. Compare the package against separate items; rental ceiling €20, refundable deposit separate.', ['Check connectors and cable length', 'Inspect the boom and clip adapter'], 25),
        ('food', 'Arrange dressing-room food', 'Morgan', 18, 30, 'The Foundry', 'Six 30 cm pizzas, at least two vegan, at most €80 including delivery. Deliver before 18:30 to Static Bloom dressing room, stage entrance. Shared show record is authoritative; do not book without review.', ['Verify vegan labels and allergens', 'Compare delivery windows'], 15),
        ('trip', 'Plan the equipment collection', 'Alex', 15, 30, 'Rental desk → The Foundry', 'Demo distance 12 km; vehicle estimate is a simulation, not live traffic. Allow 10 minutes to inspect equipment and 15 minutes to unload. Check shared event deadlines; do not invent a route.', ['Collect exact kit', 'Arrive before soundcheck'], 10),
        ('budget', 'Review the show budget', 'Sam', 14, 0, 'Production desk', 'Approved €180.00, current commitments €197.50. Compare the gap and allow adjusting both amounts. No bank account or payment is connected.', ['Separate refundable deposits', 'Check proposed quantities'], 20),
        ('return', 'Return the rental equipment', 'Alex', 11, 0, 'Rental desk', 'Return each checked-out boom stand, clip adapter and 10 m cable on the following day. Inspect for damage; do not claim a deposit was refunded.', ['Count all included pieces', 'Record inspection'], 20),
        ('personal', 'Water balcony plants', 'Me', 19, 0, 'Home', 'Two pots, 250 ml each. No connection to show equipment or catering.', ['Water each pot'], 5),
    ]
    for key, title, owner, hour, minute, location, instructions, checks, minutes in rows:
        retained.append(dict(id='hive-' + key, title=title, assignee=owner, due=at(hour, minute) + (86400000 if key == 'return' else 0), location=location, instructions=instructions, show='Mini-hive demo', checklist=[dict(text=c, done=False) for c in checks], complete=False, minutes=minutes, links=[]))
    return json.dumps(retained)


prefs('todo', 'provider', {'tasks': tasks})
# This ordinary entrance creates only a labelled fixture in the dedicated demo calendar.
command = ('am start -S -W --user 0 -n dev.deal.apps.calendar/.AgendaActivity '
           '--es demoEvent "Show preparation" --es demoDate ' + day.isoformat() +
           ' --es demoNote "Static Bloom at The Foundry. One boom stand and a balanced XLR female to male cable of at least 10 m needed 16:15–22:30. Six pizzas, at least two vegan, delivered by 18:30; food ceiling 8000 EUR cents including delivery. Demo collection distance 12 km, allow 10 minutes inspection and 15 minutes unloading. Read shared records before proposing changes; no booking has happened."')
shell(command)
print('Labelled synthetic tasks and dedicated-calendar fixture installed. Sharing, tool access and remote-generation disclosure are unchanged.')
