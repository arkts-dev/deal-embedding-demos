#!/usr/bin/env python3
"""Real model inference through native Calendar controls on the attached device."""
import hashlib,json,pathlib,re,shlex,subprocess,time,xml.etree.ElementTree as ET
root=pathlib.Path(__file__).resolve().parents[1]
adb=pathlib.Path.home()/'Android/Sdk/platform-tools/adb'
package='dev.deal.apps.calendar'
def shell(*args): return subprocess.check_output([str(adb),'shell',' '.join(shlex.quote(str(a)) for a in args)],text=True,timeout=45).strip()
def xml():
    shell('uiautomator','dump','/sdcard/generation-ui.xml')
    return ET.fromstring(shell('head','-c','65536','/sdcard/generation-ui.xml'))
def tap_node(n):
    a,b,c,d=map(int,re.findall(r'\d+',n.get('bounds')))
    shell('input','tap',(a+c)//2,(b+d)//2)
def find(predicate):
    for direction in (0,-1,1):
        for _ in range(8 if direction else 1):
            found=next((n for n in xml().iter('node') if predicate(n) and n.get('bounds')!='[0,0][0,0]'),None)
            if found is not None:return found
            if direction:shell('input','swipe',540,1900 if direction==1 else 700,540,650 if direction==1 else 2050,300)
    raise AssertionError('Native control missing')
def tap(text):tap_node(find(lambda n:n.get('text')==text or n.get('content-desc')==text));time.sleep(.5)
def snapshot():return json.loads(shell('run-as',package,'head','-c','262144','files/ui-snapshot.json'))
def nodes(t):
    yield t
    for c in t['children']:yield from nodes(c)
def ints():return [p['intValue'] for n in nodes(snapshot()['tree']) if n['component']=='ui.IntText' for p in n['props'] if p['name']=='value']
def status():return json.loads(shell('run-as',package,'head','-c','4096','files/generation-status.json'))
def wait(test, seconds=240):
    deadline=time.monotonic()+seconds
    while time.monotonic()<deadline:
        if test():return
        time.sleep(.3)
    raise AssertionError('Generation/behavior timed out')
def source():
    prefs=ET.fromstring(shell('run-as',package,'head','-c','262144','shared_prefs/experience.xml'))
    return {n.get('name'):n.text for n in prefs}
def start_generation(intent):
    shell('am','start','--user','0','-n',package+'/.CalendarActivity')
    time.sleep(1)
    field=find(lambda n:n.get('class')=='android.widget.EditText' and int(re.findall(r'\d+',n.get('bounds'))[1]) > 250)
    tap_node(field)
    # Select existing text through keyboard, not a source injection intent.
    shell('input','keyevent','KEYCODE_MOVE_END')
    shell('input','keycombination',113,29)
    shell('input','text',intent.replace(' ','%s'))
    shell('input','keyevent',4)
    shell('am','start','--user','0','-n',package+'/.CalendarActivity')
    time.sleep(1)
    tap('Generate experience')
    wait(lambda:status()['status']=='running',10)
def generate(intent):
    start_generation(intent)
    wait(lambda:status()['status']!='running')
    assert status()['status']=='activated',status()
    return source(),status()['attempts']
shell('am','start','--user','0','-n',package+'/.CalendarActivity')
time.sleep(3)
pid=shell('pidof',package)
install=next(s for s in shell('dumpsys','package',package).splitlines() if 'lastUpdateTime=' in s)
first,attempt1=generate('Create a counter headed COUNTER LAB. Count starts at 0. Button Add one increments by 1. No capability calls.')
assert ints()==[0]
tap('Add one');wait(lambda:ints()==[1],20)
second,attempt2=generate('TRAVEL LAB: distance 12 and travel 0 as IntText. Add distance adds 2. Estimate trip awaits the discovered travel estimate. Show errors.')
assert first!=second
# Host grant remains native; grant the discovered catalog using the existing native switch.
tap('Your connected apps')
switch=find(lambda n:n.get('checkable')=='true' and n.get('class')!='android.widget.CheckBox')
if switch.get('checked')!='true':tap_node(switch)
tap('Add distance');tap('Estimate trip');wait(lambda:14 in ints() and 33 in ints(),30)
stable=source();stable_tree=snapshot()
start_generation('Create a new multi-step planning interface using available capabilities. Include a heading and several controls.')
tap('Cancel generation')
wait(lambda:status()['status']!='running',30)
assert status()['status']=='cancelled'
assert source()==stable and snapshot()==stable_tree
assert shell('pidof',package)==pid and install in shell('dumpsys','package',package)
receipt={'nativeIntentControls':True,'realModelDifferentWorkflows':True,'counterActionExecuted':True,'discoveredTravelExecuted':True,
         'nativeCancellationRetainedExperience':True,'samePid':True,'sameInstall':True,'attempts':[attempt1,attempt2],
         'sourceHashes':[hashlib.sha256(json.dumps(s,sort_keys=True).encode()).hexdigest() for s in (first,second)]}
(root/'build/generation-ui-device.json').write_text(json.dumps(receipt,indent=2)+'\n')
print(json.dumps(receipt,indent=2))
