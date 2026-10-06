#!/usr/bin/env python3
"""Device checks against real checked trees, Compose actions and Calendar Provider."""
import hashlib, json, os, pathlib, shlex, subprocess, time, xml.etree.ElementTree as ET
root = pathlib.Path(__file__).resolve().parents[1]
os.chdir(root)
adb = pathlib.Path.home() / 'Android/Sdk/platform-tools/adb'
package = 'dev.deal.apps.calendar'
def run(*args): return subprocess.check_output([str(adb), *args], text=True, timeout=45).strip()
def shell(*args):
    args=list(args)
    if args[:2]==['am','start']: args[2:2]=['--user','0']
    if args[:2]==['am','force-stop']: args[2:2]=['--user','0']
    if args[:2]==['content','query']: args[2:2]=['--user','0']
    if args[:2]==['dumpsys','package']: pass
    return run('shell', ' '.join(shlex.quote(str(x)) for x in args))
def snapshot():
    for _ in range(10):
        try: return json.loads(shell('run-as',package,'head','-c','262144','files/ui-snapshot.json'))
        except json.JSONDecodeError: time.sleep(.1)
    raise AssertionError('Snapshot was not readable')
def nodes(tree):
    yield tree
    for child in tree['children']: yield from nodes(child)
def prop(node,name): return next((p for p in node['props'] if p['name']==name),{})
def integer(component):
    return [prop(n,'value')['intValue'] for n in nodes(snapshot()['tree']) if n['component']=='ui.'+component]
def texts(): return [prop(n,'value').get('stringValue','') for n in nodes(snapshot()['tree'])]
def wait(predicate):
    for _ in range(100):
        if predicate(): return
        time.sleep(.15)
    raise AssertionError('Timed out waiting for checked UI state')
def xml():
    shell('uiautomator','dump','/sdcard/experience.xml')
    return ET.fromstring(shell('head','-c','65536','/sdcard/experience.xml'))
def tap_node(node):
    import re
    x1,y1,x2,y2 = map(int,re.findall(r'\d+',node.get('bounds')))
    shell('input','tap',(x1+x2)//2,(y1+y2)//2)
def tap(text):
    for direction in (0,1,-1):
        for _ in range(8 if direction else 1):
            tree=xml()
            found=next((n for n in tree.iter('node') if n.get('text')==text or n.get('content-desc')==text),None)
            if found is not None and found.get('bounds') not in ('[0,0][0,0]',): tap_node(found); time.sleep(.7); return
            if direction: shell('input','swipe',540,1900 if direction==1 else 700,540,650 if direction==1 else 2050,300)
    raise AssertionError('Visible control missing: '+text)
def switch(enabled):
    for _ in range(8):
        tree=xml()
        found=next((n for n in tree.iter('node') if n.get('checkable')=='true' and n.get('class')!='android.widget.CheckBox'),None)
        if found is not None:
            if (found.get('checked')=='true') != enabled: tap_node(found)
            return
        shell('input','swipe',540,2100,540,600,450)
        time.sleep(.5)
    raise AssertionError('Consent switch not visible')
def activate(deal, ui):
    shell('am','start','-n',package+'/.CalendarActivity','--es','deal',deal,'--es','dealui',ui)
source=(root/'android/apps/calendar/experiences/departure/departure.deal').read_text()
view=(root/'android/apps/calendar/experiences/departure/departure.dealui').read_text()
import sys
if '--late-provider' in sys.argv:
    # A previously unknown provider package is installed after Calendar has already started.
    fixture='dev.deal.connectors.tests/.LateProviderService'
    vehicle='dev.deal.apps.vehicle/.VehicleService'
    shell('am','force-stop',package)
    shell('run-as','dev.deal.apps.vehicle','pm','disable',vehicle)
    subprocess.run([str(adb),'uninstall','--user','0','dev.deal.connectors.tests'],check=True)
    try:
        shell('am','start','-n',package+'/.CalendarActivity')
        time.sleep(8)
        old_pid=shell('pidof',package)
        old_install=next(line for line in shell('dumpsys','package',package).splitlines() if 'lastUpdateTime=' in line)
        subprocess.run([str(adb),'install','--user','0','-r','build/tests.apk'],check=True)
        shell('run-as','dev.deal.connectors.tests','pm','enable',fixture)
        marker='LATE DISCOVERY '+str(time.time_ns())
        activate(source,view.replace('YOUR DEPARTURE PLAN',marker))
        wait(lambda: marker in texts())
        contracts=json.loads(shell('run-as',package,'head','-c','65536','files/capability-contracts.json'))
        assert next(c for c in contracts if c['module']=='host/trip')['description']=='Late-installed test travel provider'
        tap('Your connected apps'); switch(True)
        tap('Calculate departure'); wait(lambda: len(integer('Time'))==1)
        assert shell('pidof',package)==old_pid
        assert old_install in shell('dumpsys','package',package)
        receipt={'lateInstalledUnknownProvider':True,'genericDiscoveryAndInvocation':True,'samePid':True,'sameInstall':True}
        (root/'build/late-provider-device.json').write_text(json.dumps(receipt,indent=2)+'\n')
        print(json.dumps(receipt,indent=2))
    finally:
        shell('run-as','dev.deal.connectors.tests','pm','disable',fixture)
        shell('run-as','dev.deal.apps.vehicle','pm','enable',vehicle)
        shell('am','force-stop',package)
        shell('am','start','-n',package+'/.CalendarActivity')
    sys.exit(0)
metadata=shell('dumpsys','package',package)
install=next(line for line in metadata.splitlines() if 'lastUpdateTime=' in line)
pid=shell('pidof',package)
marker = 'CHECKED DEPARTURE ' + str(time.time_ns())
activate(source,view.replace('YOUR DEPARTURE PLAN', marker))
wait(lambda: marker in texts())
if os.environ.get('DEAL_TEST_EVENT'):
    tap(os.environ['DEAL_TEST_EVENT'])
contracts=json.loads(shell('run-as',package,'head','-c','65536','files/capability-contracts.json'))
(root/'build/discovered-contracts.json').write_text(json.dumps(contracts))
declarations=root/'build/discovered-declarations'
declarations.mkdir(exist_ok=True)
for contract in contracts:
    name=contract['module'].split('/')[1]+'.d.deal'
    (declarations/name).write_text(shell('run-as',package,'head','-c','16384','files/capability-declarations/'+name))
initial_buffer = integer('IntText')[0]
# Native consent in both provider apps remains independent.
for provider, activity in [('todo','TodoActivity'),('vehicle','VehicleActivity')]:
    shell('am','start','-n',f'dev.deal.apps.{provider}/.{activity}')
    time.sleep(2)
    shell('input','swipe',540,2100,540,600,350)
    switch(True)
shell('am','start','-n',package+'/.CalendarActivity')
time.sleep(.5)
tap('Your connected apps')
switch(True)
tap('Calculate departure')
wait(lambda: len(integer('Time'))==1)
first=integer('Time')[0]
tap('Add 5 minutes')
wait(lambda: integer('IntText')==[initial_buffer+5,12])
tap('Calculate departure')
wait(lambda: integer('Time')==[first-5])
# Invalid source must not disturb live state.
activate('export function main(): null { return 7; }',view)
time.sleep(3)
assert integer('IntText')==[initial_buffer+5,12]
assert integer('Time')==[first-5]
# A fresh checked view arrives after installation; state survives.
updated=view.replace('YOUR DEPARTURE PLAN','YOUR UPDATED DEPARTURE PLAN')
activate(source,updated)
wait(lambda: 'YOUR UPDATED DEPARTURE PLAN' in texts())
assert integer('IntText')==[initial_buffer+5,12]
assert integer('Time')==[first-5]
assert shell('pidof',package)==pid
assert install in shell('dumpsys','package',package)
# Revoke native host consent; the generated effect exposes denial, then recovers.
tap('Your connected apps'); switch(False)
tap('Calculate departure')
wait(lambda: any('not granted' in text for text in texts()))
tap('Your connected apps'); switch(True)
tap('Calculate departure')
wait(lambda: integer('Time')==[first-5])
# Provider consent is independent of host grants and recovers without a new session.
shell('am','start','-n','dev.deal.apps.todo/.TodoActivity')
time.sleep(2); shell('input','swipe',540,2100,540,600,350); switch(False)
shell('am','start','-n',package+'/.CalendarActivity'); time.sleep(.5)
tap('Calculate departure')
wait(lambda: any('Provider owner' in text for text in texts()))
shell('am','start','-n','dev.deal.apps.todo/.TodoActivity')
time.sleep(2); switch(True)
shell('am','start','-n',package+'/.CalendarActivity'); time.sleep(.5)
tap('Calculate departure'); wait(lambda: integer('Time')==[first-5])
# A keyed generated task selection changes the plan and survives replacement.
selected_task = next(n for n in nodes(snapshot()['tree']) if n['component']=='ui.Toggle')
selected_title = prop(selected_task,'text')['stringValue']
tap(selected_title)
# Text is presentational; click the associated checked component's checkbox.
for _ in range(4):
    current=xml()
    target=next((n for n in current.iter('node') if n.get('class')=='android.widget.CheckBox' and n.get('content-desc')==selected_title),None)
    if target is not None: tap_node(target); break
    shell('input','swipe',540,1900,540,800,300)
time.sleep(.3)
tap('Calculate departure')
wait(lambda: integer('Time')==[first+10])
activate(source,view)
wait(lambda: 'YOUR DEPARTURE PLAN' in texts())
assert integer('Time')==[first+10]
tap('Calculate departure'); wait(lambda: integer('Time')==[first+10])
time.sleep(1)
before=shell('content','query','--uri','content://com.android.calendar/reminders','--projection','event_id:minutes:method')
tap('Review Calendar change'); tap('Keep unchanged')
assert before==shell('content','query','--uri','content://com.android.calendar/reminders','--projection','event_id:minutes:method')
tap('Review Calendar change'); tap('Confirm write')
time.sleep(.5)
after=shell('content','query','--uri','content://com.android.calendar/reminders','--projection','event_id:minutes:method')
assert after!=before
receipt={'sourceSha256':hashlib.sha256(source.encode()).hexdigest(),'uiSha256':hashlib.sha256(updated.encode()).hexdigest(),'samePid':True,'sameInstall':True,'initialDepartureMinute':first,'adjustedDepartureMinute':first-5,'invalidBundleRetainedState':True,'replacementRetainedState':True,'nativeRevocationRecovered':True,'providerConsentRecovered':True,'keyedTaskSelectionRetained':True,'calendarConfirmationVerified':True}
(root/'build/experience-device.json').write_text(json.dumps(receipt,indent=2)+'\n')
print(json.dumps(receipt,indent=2))
