#!/usr/bin/env python3
"""Opt-in real-device demo acceptance. Uses ordinary controls; live calls only on changed facts.
Creates/replaces labelled demo records, never forces AI answers. Stores private UI receipts in build/.
"""
import argparse, pathlib, re, subprocess, time, xml.etree.ElementTree as ET
p=argparse.ArgumentParser(description=__doc__);p.add_argument('--live',action='store_true',required=True);args=p.parse_args()
out=pathlib.Path('build/preparation-e2e');out.mkdir(parents=True,exist_ok=True)
def adb(*args):return subprocess.check_output(['adb',*args],text=True,timeout=20)
def nodes():
    adb('shell','uiautomator','dump','/sdcard/preparation.xml')
    return list(ET.fromstring(adb('shell','cat','/sdcard/preparation.xml')).iter('node'))
def texts():return [n.get('text') for n in nodes() if n.get('text')]
def tap(label,description=False):
    for _ in range(12):
        ns=nodes();match=next((n for n in ns if n.get('content-desc' if description else 'text')==label),None)
        if match is not None:
            x,y,a,b=map(int,re.findall(r'\d+',match.get('bounds')));adb('shell','input','tap',str((x+a)//2),str((y+b)//2));time.sleep(.5);return
        adb('shell','input','swipe','550','1950','550','900','400')
    raise AssertionError('Missing '+label)
def record(name):
    nodes();adb('pull','/sdcard/preparation.xml',str(out/(name+'.xml')))
    with (out/(name+'.png')).open('wb') as f:subprocess.run(['adb','exec-out','screencap','-p'],stdout=f,check=True)
def wait():
    deadline=time.monotonic()+75
    while time.monotonic()<deadline:
        t=texts()
        if 'You may need to prepare for tomorrow' in t or any('No preparation suggestion' in v for v in t):return t
        time.sleep(1)
    raise AssertionError('Preparation never settled: '+str(texts()))
def setup(task,event):
    adb('shell','am','start','-n','dev.deal.apps.todo/.TodoActivity');time.sleep(3);tap('Demo');tap(task)
    # Existing provider sharing is not silently enabled by fixture setup.
    assert any(n.attrib.get('name')=='allowed' and n.attrib.get('value')=='true' for n in ET.fromstring(adb('shell','run-as','dev.deal.apps.todo','cat','shared_prefs/provider.xml'))), 'Enable Todo sharing first'
    adb('shell','am','start','-n','dev.deal.apps.calendar/.AgendaActivity');time.sleep(3);tap('Demo');tap(event)
    if 'Find preparation help' in texts():tap('Find preparation help');tap('Allow')
    return wait()
for task,event,expected,uncertain in [('Viewing','Apartment viewing','Download last three payslips',False),('Budget review','Project budget review','Check revised vendor quotation',False),('Held-out','Client onboarding','Fetch company incorporation certificate',False),('Ambiguous','Review meeting','Download the report',True)]:
    t=setup(task,event);assert any(expected in v for v in t),t
    assert any(('Could “' if uncertain else 'may help') in v for v in t),t
    record(task)
    before=adb('shell','run-as','dev.deal.apps.todo','cat','shared_prefs/provider.xml')
    tap('Yes, add to plan' if uncertain else 'Add to plan');time.sleep(3);tap('Load preparation tasks');time.sleep(2)
    tap('Edit time for '+expected,True);tap('Minutes: '+expected+' increase',True)
    tap('Calculate preparation and departure');time.sleep(.5);adb('shell','input','swipe','550','1950','550','800','400')
    t=texts();assert '14:14' in t and '14:30' in t,t;record(task+'-plan')
    assert before==adb('shell','run-as','dev.deal.apps.todo','cat','shared_prefs/provider.xml'), 'Planner changed Todo'
    tap('Back')
t=setup('Budget review','Swimming lesson');assert 'You may need to prepare for tomorrow' not in t,t;record('unrelated')
t=setup('Viewing','Apartment viewing');tap('Dismiss');tap('Today')
# Reopen the same facts: dismissal is persisted; no model retry.
adb('shell','am','force-stop','dev.deal.apps.calendar');adb('shell','am','start','-n','dev.deal.apps.calendar/.AgendaActivity');time.sleep(1)
# Tomorrow's accessible date label is locale-dependent; use the demo event's existing day.
from datetime import date,timedelta
tomorrow=date.today()+timedelta(days=1);tap(tomorrow.strftime('%-d %b'),True);time.sleep(2)
assert 'You may need to prepare for tomorrow' not in texts();record('dismissed-reopen')
print('PASS real-device live Calendar discovery → acceptance → checked planner; viewing/budget/held-out/ambiguity/unrelated/dismissal')
