#!/usr/bin/env python3
"""Ordinary six-app shell UI check. Opens/cancels reset; never deletes existing experiences.
Actual deletion/owner failure is tested with isolated AssistanceShellInstrumentation.
Screenshots are evidence for separate visual review, not automatic visual approval.
"""
import importlib.util
import json
import pathlib
import time

ROOT = pathlib.Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location('device', ROOT / 'tools/assistance-device.py')
d = importlib.util.module_from_spec(spec)
spec.loader.exec_module(d)
OUT = ROOT / 'build/coherent-shell'
OUT.mkdir(parents=True, exist_ok=True)
def expect_labels(*expected):
    # Observation-only wait: no repeated tap, inference request or recovery action.
    deadline = time.monotonic() + 15
    while True:
        labels = d.labels(d.ui())
        if all(label in labels for label in expected):
            return labels
        if time.monotonic() >= deadline:
            raise AssertionError({'expected': expected, 'visible': labels})
        time.sleep(.5)


results = []
for app, activity in [('calendar', 'AgendaActivity'), ('organizer', 'OrganizerActivity'), ('todo', 'TodoActivity'),
                      ('rental', 'RentalActivity'), ('pizza', 'PizzaActivity'), ('vehicle', 'VehicleActivity')]:
    d.shell('am', 'start', '-S', '-W', '--user', '0', '-n', f'dev.deal.apps.{app}/.{activity}')
    d.tap('Settings')
    expect_labels('Settings', 'On-device suggestions', 'Tailored tools', 'Share with connected apps')
    assert 'dev.deal.shell.AssistanceSettingsActivity' in d.shell('dumpsys', 'activity', 'activities')
    d.capture(f'shell-{app}-settings-final')
    d.tap('Inspect')
    expect_labels('Activity', 'Dialogue', 'Source', 'All assistance')
    assert 'dev.deal.shell.AssistanceInspectActivity' in d.shell('dumpsys', 'activity', 'activities')
    assert 'Workspaces' not in d.labels(d.ui())
    d.tap('Dialogue'); d.tap('Source'); d.capture(f'shell-{app}-inspect-final')
    d.tap('Back')
    d.scrolltap('Delete all generated experiences')
    expect_labels('Delete all generated experiences?', 'Cancel', 'Delete')
    d.capture(f'shell-{app}-reset-confirm-final')
    d.tap('Cancel')
    assert 'Delete all generated experiences?' not in d.labels(d.ui())
    d.tap('Back')
    results.append({'app': app, 'settings': 'shared', 'inspect': 'shared', 'reset': 'confirmation cancelled; data preserved'})
    (OUT / 'device-ui.json').write_text(json.dumps({'checks': results, 'visualAcceptance': False}, indent=2) + '\n')
    print('PASS', app, 'common Settings/Inspect and cancel-safe reset', flush=True)
print('No visual approval or destructive device reset asserted.')
