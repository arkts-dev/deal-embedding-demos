#!/usr/bin/env python3
"""Second bounded repair for existing experiment samples; preserves every failed first repair."""
import argparse,json,pathlib,sys,importlib.util,subprocess,time,hashlib,difflib

def rejected_source_feedback(project, status, original):
 """Only structured diagnostics attributed to the application source admit another call."""
 report=json.loads((project/'diagnostics.json').read_text())
 errors=[d for d in report.get('diagnostics',[]) if d.get('severity')=='error']
 app=project/'src/experience.deal';view=project/'src/experience.dealui'
 if status not in (1,2) or report.get('version')!=1 or not errors:raise ValueError('Not a structured source rejection')
 for d in errors:
  file=pathlib.Path(d.get('range',{}).get('file',''));code=d.get('code','')
  if file not in (app,view) or not (code.startswith('UI') or (code.startswith('E') and len(code)==5 and code[1] in '12345') or code=='E6006') or code=='E2010':
   raise ValueError('Operational/framework diagnostic; source repair prohibited')
 windows=[]
 for d in errors:
  file=pathlib.Path(d['range']['file']);position=d['range'];line=position['startLine'];lines=original['dealui' if file==view else 'deal'].splitlines()
  if line not in range(1,len(lines)+1):raise ValueError('Diagnostic outside original source')
  windows.append(f"{d['code']} {file.name}:{line}:{position['startColumn']} {d['message']}\nExact source window:\n"+'\n'.join(lines[max(0,line-3):line+2]))
 return '\n'.join(windows)
p=argparse.ArgumentParser();p.add_argument('--live',action='store_true',required=True);p.add_argument('directory',type=pathlib.Path);a=p.parse_args();out=a.directory
sys.argv=['generation-gateway.py','--backend','cerebras','--chooser','jev']
s=importlib.util.spec_from_file_location('gateway',pathlib.Path(__file__).with_name('generation-gateway.py'));g=importlib.util.module_from_spec(s);s.loader.exec_module(g)
root=pathlib.Path(__file__).resolve().parents[1];assets=root/'build/embedding-library/aar/assets'
subprocess.run(['javac','--release','21','-cp','build/embedding-library/classes','-d',str(out),'dependencies/deal-embedding/tools/ToolkitHarness.java'],check=True)
results=json.loads((out/'results.json').read_text());all_results=[]
for sample in results:
 t=out/sample['name'];dest=t/'second';dest.mkdir()
 record={'name':sample['name'],'strategy':sample['strategy'],'effort':sample['reasoningEffort']}
 try:
  base={k:(t/'input'/('experience.'+k)).read_text() for k in ['deal','dealui']};(dest/'base.json').write_text(json.dumps(base))
  # Check the actual rejected pair again to obtain current diagnostic coordinates.
  c=subprocess.run(['java','-Ddeal.home='+str(root/'dependencies/deal'),'-cp','build/embedding-library/classes:'+str(out),'ToolkitHarness',str(root/'dependencies/deal-embedding/ui/platform.dealui-pack'),str(t/'input'),str(dest/'recheck'),str(root/'build/embedding-consumer/assets/distribution')],capture_output=True,text=True)
  (dest/'recheck.log').write_text(c.stdout+c.stderr)
  diagnostic=rejected_source_feedback(dest/'recheck',c.returncode,base)
  (dest/'feedback.txt').write_text(diagnostic)
  b=json.loads((t/'gateway-input.json').read_text());text=b['input'].split('\n\nREPAIR GUIDANCE\n')[0]
  text+='\n\nREPAIR GUIDANCE\nFix only the compiler-rejected cause and same syntax elsewhere; preserve behavior.\n'+diagnostic
  if sample['strategy']=='delta':
   d,u=base['deal'],base['dealui'];digest=hashlib.sha256((str(len(d))+':'+d+str(len(u))+':'+u).encode()).hexdigest()
   text+='\nREPAIR PROTOCOL exact-source-patch-v1\nReturn ONLY {"baseDigest":"'+digest+'","edits":[{"file":"deal or dealui","search":"exact original text","replacement":"corrected text"}]}. Maximum8 unique/disjoint edits. No full-source fallback.\nBASE SOURCE\n'+json.dumps(base,ensure_ascii=False)
  b.update(input=text,previous=json.dumps(base,ensure_ascii=False),diagnostics=diagnostic);g.options.repair_reasoning=sample['reasoningEffort'];g.options.repair_max_tokens=32768
  (dest/'gateway-input.json').write_text(json.dumps(b,ensure_ascii=False))
  start=time.monotonic();reply=json.loads(g.infer(b));record.update(elapsedMs=(time.monotonic()-start)*1000,metadata=reply['metadata']);(dest/'exchange.json').write_text(json.dumps(reply,ensure_ascii=False));(dest/'reply.json').write_text(reply['content'])
  if sample['strategy']=='delta':
   c=subprocess.run(['node','tools/check-source-patch.js',str(assets),str(dest/'base.json'),str(dest/'reply.json')],capture_output=True,text=True,check=True);patch=json.loads(c.stdout);record['patchAccepted']=patch['accepted']
   if not patch['accepted']:raise ValueError(patch['diagnostics'])
   pair=json.loads(patch['source'])
  else:pair=json.loads(reply['content'])
  if set(pair)!={'deal','dealui'} or not all(isinstance(v,str) for v in pair.values()):raise ValueError('Invalid source envelope')
  inputs=dest/'input';inputs.mkdir()
  for k in ['deal','dealui']:(inputs/('experience.'+k)).write_text(pair[k])
  for n in ['pizza.d.deal','deal.json']:(inputs/n).write_bytes((t/'input'/n).read_bytes())
  original=json.loads((out/'base.json').read_text());record['changedLines']={k:sum(tag[0] in '+-' for tag in difflib.ndiff(original[k].splitlines(),pair[k].splitlines())) for k in original};record['uiUnchanged']=pair['dealui']==original['dealui']
  c=subprocess.run(['java','-Ddeal.home='+str(root/'dependencies/deal'),'-cp','build/embedding-library/classes:'+str(out),'ToolkitHarness',str(root/'dependencies/deal-embedding/ui/platform.dealui-pack'),str(inputs),str(dest/'compiled'),str(root/'build/embedding-consumer/assets/distribution')],capture_output=True,text=True);(dest/'compile.log').write_text(c.stdout+c.stderr);record['compiled']=c.returncode==0
  if record['compiled']:
   c=subprocess.run(['node','tools/verify-pizza-repair.js',str(dest/'compiled/js'),str(assets/'embedding/bindings')],capture_output=True,text=True);(dest/'runtime.log').write_text(c.stdout+c.stderr);record['behaviorPassed']=c.returncode==0
 except Exception as e:record['error']=type(e).__name__+': '+str(e)[:160]
 all_results.append(record);(out/'second-results.json').write_text(json.dumps(all_results,indent=2));print(json.dumps({k:v for k,v in record.items() if k!='metadata'}),flush=True)
