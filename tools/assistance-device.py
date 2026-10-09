#!/usr/bin/env python3
"""Manual device-audit helper; captures exact visible hierarchy/screens, never calls inference."""
import os, pathlib, subprocess, time, re, json, xml.etree.ElementTree as ET, sys
ADB=[str(pathlib.Path.home()/'Android/Sdk/platform-tools/adb')]
if os.environ.get('ANDROID_SERIAL'): ADB+=['-s',os.environ['ANDROID_SERIAL']]
OUT=pathlib.Path('build/assistance/audit'); OUT.mkdir(parents=True,exist_ok=True)
def shell(*args): return subprocess.check_output(ADB+['shell',*map(str,args)],text=True,timeout=35).strip()
def ui():
    shell('uiautomator','dump','/sdcard/assistance-audit.xml')
    return ET.fromstring(shell('cat','/sdcard/assistance-audit.xml'))
def labels(root): return [value for n in root.iter('node') for value in (n.get('text'), n.get('content-desc')) if value]
def controls(root):
    return [{'labels':labels(n),'enabled':n.get('enabled'),'bounds':n.get('bounds'),'selected':n.get('selected')} for n in root.iter('node') if n.get('clickable')=='true']
def capture(name):
    r=ui(); (OUT/(name+'.xml')).write_text(ET.tostring(r,encoding='unicode')); (OUT/(name+'.png')).write_bytes(subprocess.check_output(ADB+['exec-out','screencap','-p'],timeout=35)); print(json.dumps(controls(r),indent=2)); return r
def find(label,root=None,index=0):
    r=root if root is not None else ui()
    found=[n for n in r.iter('node') if label in (n.get('text'),n.get('content-desc')) and n.get('bounds')!='[0,0][0,0]']
    if len(found)<=index:raise AssertionError('Missing '+label+' in '+repr(labels(r)))
    return found[index]
def tap(label,index=0):
    r=ui();n=find(label,r,index)
    parents={c:p for p in r.iter() for c in p}
    control=n
    while control.get('clickable')!='true' and control in parents:control=parents[control]
    if control.get('clickable')=='true':assert control.get('enabled')=='true',label+' disabled'
    x1,y1,x2,y2=map(int,re.findall(r'\d+',n.get('bounds')))
    shell('input','tap',(x1+x2)//2,(y1+y2)//2);time.sleep(.5)
def swipe(direction='down'):
    # Swipe the content gutter rather than selecting text in an editable field.
    shell('input','swipe',100,1800 if direction=='down' else 1150,100,1150 if direction=='down' else 1800,350);time.sleep(.4)
def scrolltap(label,index=0):
    for i in range(12):
        try:return tap(label,index)
        except AssertionError as e:
            if not str(e).startswith('Missing'):raise
        swipe()
    raise AssertionError('Missing after scrolling '+label)
def enter(label,text):
    tap(label);shell('input','keyevent','KEYCODE_MOVE_END');shell('input','keyevent','--longpress','KEYCODE_DEL');shell('input','text',text);time.sleep(.4)
def back():shell('input','keyevent','4');time.sleep(.5)
def start():shell('am','start','-S','-W','--user','0','-n','dev.deal.apps.organizer/.OrganizerActivity');time.sleep(1)
if __name__=='__main__':
    cmd=sys.argv[1]
    if cmd=='capture':capture(sys.argv[2])
    elif cmd=='tap':tap(sys.argv[2],int(sys.argv[3]) if len(sys.argv)>3 else 0);capture('last')
    elif cmd=='scrolltap':scrolltap(sys.argv[2]);capture('last')
    elif cmd=='swipe':swipe(sys.argv[2] if len(sys.argv)>2 else 'down');capture('last')
    elif cmd=='back':back();capture('last')
    elif cmd=='start':start();capture('start')
