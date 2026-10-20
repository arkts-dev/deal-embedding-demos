#!/usr/bin/env python3
"""Add isolated realistic review records, never replace saved tools or change authority.
Fixture provenance is recorded by this tool, not printed on the task canvas.
"""
import importlib.util
import pathlib

spec = importlib.util.spec_from_file_location('acceptance', pathlib.Path(__file__).with_name('setup-acceptance-demo.py'))
a = importlib.util.module_from_spec(spec)
spec.loader.exec_module(a)


def equipment(show):
    if not any(r['id'] == 'enriched-vocal-kit' for r in show['requirements']):
        show['requirements'].insert(0, dict(id='enriched-vocal-kit', act='Static Bloom', department='Sound',
            group='Sound', name='Vocal kit hire', quantity=1,
            specification='One adjustable boom stand with clip adapter and one balanced XLR female to male cable, minimum 10 m.',
            location='The Foundry, stage centre', **{'from': a.at(16, 15)}, until=a.at(22, 30),
            mandatory=True, substitutions='Exact specification or better; no shorter cable', dependencies=[],
            notes='Compare package hire with separate stand, adapter and cable using Rental catalogue and quotes. Hire budget EUR 20.00, editable; refundable deposit separate. Collection period 23 October 2026 16:15–22:30 Europe/Berlin. Read-only planning; no booking.',
            riderRef='', supply='', coverage='Missing', commitment='None', verified=False))
    return show


def food(tasks):
    if not any(t['id'] == 'enriched-food' for t in tasks):
        tasks.insert(0, dict(id='enriched-food', title='Crew dinner', assignee='Morgan', due=a.at(18, 30),
            location='The Foundry, stage entrance', show='Static Bloom',
            instructions='Plan six 30 cm pizzas, at least two vegan. Budget EUR 80.00 including delivery, arrival before 18:30. Load Pizza menu and delivery availability. Keep quantities and budget editable; recalculate total and feasibility. Read-only planning; no order.',
            checklist=[], complete=False, minutes=15, links=[]))
    return tasks


if __name__ == '__main__':
    a.update('organizer', 'organizer', 'show', equipment)
    a.update('todo', 'provider', 'tasks', food)
    receipt = a.shell('am', 'instrument', '--user', '0', '-w', '-e', 'enriched', 'true',
                      'dev.deal.connectors.tests/.AcceptanceFactsInstrumentation')
    if 'INSTRUMENTATION_CODE: 0' not in receipt or 'INSTRUMENTATION_RESULT: failure' in receipt:
        raise RuntimeError(receipt)
    print('Added absent-only review records: Vocal kit hire, Crew dinner, Budget reconciliation, Equipment collection.')
    print('Existing records, saved experiences, failed attempts and authority choices preserved.')
