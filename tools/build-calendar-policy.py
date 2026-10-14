#!/usr/bin/env python3
"""Compile Calendar-owned policy; embed the same authored template used by checked load."""
import json, pathlib, shutil, subprocess
root=pathlib.Path.cwd();stage=root/'build/calendar-policy';src=stage/'src'
shutil.rmtree(stage,ignore_errors=True);src.mkdir(parents=True)
shutil.copy(root/'android/apps/calendar/policy/app-policy.deal',src/'app-policy.deal')
shutil.copytree(root/'dependencies/deal-ui/ui',src/'ui')
# Encode source literals with DEAL escaping, not JSON's potentially unsupported escapes.
def quote(text):
    return '"'+text.replace('\\','\\\\').replace('"','\\"').replace('\n','\\n').replace('\r','\\r').replace('\t','\\t')+'"'
base=root/'android/apps/calendar/experiences/departure'
(src/'departure-source.deal').write_text('import * as embedding from "embedding";\nexport function departure(): embedding.Source {return {deal:'+quote((base/'departure.deal').read_text())+',dealui:'+quote((base/'departure.dealui').read_text())+'};}\n')
for name in ('embedding','catalogue-policy'):
    shutil.copy(root/f'dependencies/deal-embedding/api/{name}.d.deal',src/f'{name}.d.deal')
(stage/'deal.json').write_text(json.dumps({'languageVersion':'1.2','backend':'js','moduleRoots':['src'],'externals':{name:{'declaration':f'src/{name}.d.deal'} for name in ('embedding','catalogue-policy')}}))
subprocess.run(['java','-Ddeal.home='+str(root/'dependencies/deal'),'-cp',str(root/'build/embedding-consumer/classes.jar'),'deal.Main','compile',str(src/'app-policy.deal'),'--backend','js','--output',str(stage/'js')],check=True)
for app in ('calendar','tests'):
    shutil.copytree(stage/'js',root/f'build/assets/{app}/calendar-policy',dirs_exist_ok=True)
# Foreground discovery policy runs in a separate trusted isolate, not a generated UI.
shutil.copy(root/'android/apps/calendar/policy/preparation.deal',src/'preparation.deal')
for name in ('policy-data','protocol','chooser'):
    shutil.copy(root/f'dependencies/deal-embedding/core/host/{name}.d.deal',src/f'{name}.d.deal')
(src/'facts.d.deal').write_text('export async function current(): string;\nexport async function remember(state: string): null;\n')
config=json.loads((stage/'deal.json').read_text())
config['externals'].update({f'embedding/{name}':{'declaration':f'src/{name}.d.deal'} for name in ('policy-data','protocol','chooser')})
config['externals']['host/preparation-facts']={'declaration':'src/facts.d.deal'}
(stage/'deal.json').write_text(json.dumps(config))
subprocess.run(['java','-Ddeal.home='+str(root/'dependencies/deal'),'-cp',str(root/'build/embedding-consumer/classes.jar'),'deal.Main','compile',str(src/'preparation.deal'),'--backend','js','--output',str(stage/'discovery')],check=True)
for app in ('calendar','tests'):
    shutil.copytree(stage/'discovery',root/f'build/assets/{app}/calendar-discovery',dirs_exist_ok=True)
