#!/usr/bin/env python3
"""Compile app-owned DEAL policy against embedding's public protocol declarations."""
import json,pathlib,shutil,subprocess
root=pathlib.Path.cwd();stage=root/'build/organizer-policy';src=stage/'src';shutil.rmtree(stage,ignore_errors=True);src.mkdir(parents=True,exist_ok=True)
for p in (root/'android/apps/organizer/policy').glob('*.deal'):shutil.copy(p,src/p.name)
shutil.copytree(root/'dependencies/deal-ui/ui',src/'ui',dirs_exist_ok=True)
shutil.copy(root/'dependencies/deal-embedding/core/source-literals.deal',src/'source-literals.deal')
for p in (root/'dependencies/deal-embedding/core/host').glob('*.d.deal'):shutil.copy(p,src/p.name)
externals={'embedding':{'declaration':'src/embedding.d.deal'},'catalogue-policy':{'declaration':'src/catalogue-policy.d.deal'},'embedding/policy-data':{'declaration':'src/policy-data.d.deal'},'embedding/observer':{'declaration':'src/observer.d.deal'}}
(stage/'deal.json').write_text(json.dumps({'languageVersion':'1.3','backend':'js','moduleRoots':['src'],'externals':externals}))
# Source-literals is ordinary DEAL; alias resolution remains project-owned.
p=src/'equipment-source.deal';p.write_text(p.read_text().replace('"embedding/source-literals"','"./source-literals"'))
subprocess.run(['java','-Ddeal.home='+str(root/'dependencies/deal'),'-cp',str(root/'build/embedding-consumer/classes.jar'),'deal.Main','compile',str(src/'app-policy.deal'),'--backend','js','--output',str(stage/'js')],check=True)
output=stage/'js'
for app in ('organizer','tests'):
    dest=root/'build/assets'/app/'organizer-policy';shutil.copytree(output,dest,dirs_exist_ok=True)
