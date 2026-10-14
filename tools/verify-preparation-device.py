#!/usr/bin/env python3
"""Opt-in device check of the ordinary launcher path after separate development setup.
Uses existing grants; never silently grants access, resets admission or retries inference.
"""
import argparse, pathlib, re, subprocess, time, xml.etree.ElementTree as ET
p=argparse.ArgumentParser(description=__doc__)
p.add_argument('--live',action='store_true',required=True)
p.add_argument('--generated',action='store_true',help='Verify an already checked generated viewing or budget plan, no forced model result')
p.add_argument('--prepared',action='store_true',help='Use existing fixtures without recreating IDs or consuming extra admission')
p.add_argument('--scenario',choices=['Viewing','Budget review','Held-out','Ambiguous','Unrelated'],default='Viewing')
a=p.parse_args()
out=pathlib.Path('build/preparation-e2e');out.mkdir(parents=True,exist_ok=True)
def adb(*args):return subprocess.check_output(['adb',*args],text=True,timeout=20)
def nodes():
    adb('shell','uiautomator','dump','/sdcard/preparation.xml')
    return list(ET.fromstring(adb('shell','cat','/sdcard/preparation.xml')).iter('node'))
def texts():return [n.get('text') for n in nodes() if n.get('text')]
def tap(label,description=False):
    for _ in range(8):
        match=next((n for n in nodes() if n.get('content-desc' if description else 'text')==label),None)
        if match is not None:
            x,y,w,h=map(int,re.findall(r'\d+',match.get('bounds')))
            adb('shell','input','tap',str((x+w)//2),str((y+h)//2));time.sleep(.4);return
        adb('shell','input','swipe','550','1950','550','900','400')
    raise AssertionError('Missing '+label)
def record(name):
    nodes();adb('pull','/sdcard/preparation.xml',str(out/(name+'.xml')))
    with (out/(name+'.png')).open('wb') as f:subprocess.run(['adb','exec-out','screencap','-p'],stdout=f,check=True,timeout=20)
def prefs(app,file):return adb('shell','run-as','dev.deal.apps.'+app,'cat','shared_prefs/'+file+'.xml')
def launch(app):
    adb('shell','am','force-stop','dev.deal.apps.'+app)
    adb('shell','monkey','-p','dev.deal.apps.'+app,'-c','android.intent.category.LAUNCHER','1');time.sleep(1)
def settled():
    deadline=time.monotonic()+75
    while time.monotonic()<deadline:
        t=texts()
        if 'Looking ahead…' not in t:return t
        time.sleep(1)
    raise AssertionError('Discovery did not settle')
task,event,expected={'Viewing':('Viewing','Apartment viewing','Download last three payslips'),
    'Budget review':('Budget review','Project budget review','Check revised vendor quotation'),
    'Held-out':('Held-out','Client onboarding','Fetch company incorporation certificate'),
    'Ambiguous':('Ambiguous','Review meeting','Download the report'),
    'Unrelated':('Budget review','Swimming lesson',None)}[a.scenario]
assert any(n.attrib.get('name')=='allowed' and n.attrib.get('value')=='true' for n in ET.fromstring(prefs('todo','provider'))),'Enable Todo → Extensions first'
assert any(n.attrib.get('name')=='disclose' and n.attrib.get('value')=='true' for n in ET.fromstring(prefs('calendar','preparation'))),'Enable Calendar → Extensions first'
# Setup is explicitly outside the acceptance path.
if not a.prepared:
    subprocess.run(['python3','tools/setup-preparation-demo.py',{'Viewing':'viewing','Budget review':'budget','Held-out':'held-out','Ambiguous':'ambiguous','Unrelated':'unrelated'}[a.scenario]],check=True)
# Actual acceptance starts at the launcher, viewing today, not tomorrow or Development.
launch('calendar');t=settled();record(a.scenario+'-ordinary')
assert 'Sample tasks added' not in t and 'Development' not in t and 'Find preparation help' not in t,t
links=[v for v in t if 'might help you prepare' in v or v.startswith('Could “')]
if expected is None:
    assert not links,t
else:
    assert any(expected in v for v in links),t
    assert ('Could “' in links[0]) == (a.scenario=='Ambiguous'),t
    before=prefs('todo','provider');tap(links[0])
    deadline=time.monotonic()+20
    while time.monotonic()<deadline:
        t=texts()
        if 'Opening plan…' not in t:break
        time.sleep(1)
    assert 'Load preparation tasks' not in t,t
    if a.generated:
        assert a.scenario in ('Viewing','Budget review'), 'Generated interaction check supports these two fixtures'
        if a.scenario=='Viewing':
            assert 'Payslip checklist' in t,t
            tap('Print the three payslips',True)
            tap('Preparation estimate in minutes increase',True)
        else:
            assert 'Budget comparison' in t,t
            tap('Enter the approved budget in euros increase',True)
            tap('Enter the revised vendor quotation in euros increase',True)
        record(a.scenario+'-generated')
    else:
        assert expected in t,t
        tap('Edit time for '+expected,True);tap('Minutes: '+expected+' increase',True)
        tap('Update times');t=texts()
        if '14:14' not in t:
            adb('shell','input','swipe','550','900','550','1950','400');t=texts()
        assert '14:14' in t and '14:30' in t,t
    assert before==prefs('todo','provider'),'Plan changed tasks'
    record(a.scenario+'-populated-plan');tap('Back');settled();tap('Dismiss suggestion',True)
    before=prefs('calendar','preparation');launch('calendar');t=settled()
    assert not any('might help you prepare' in v or v.startswith('Could “') for v in t),t
    assert before==prefs('calendar','preparation'),'Reopening retried unchanged facts'
    record(a.scenario+'-dismissed')
print('PASS ordinary launcher → quiet discovery → populated plan → adjust/dismiss:',a.scenario)
