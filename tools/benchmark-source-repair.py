#!/usr/bin/env python3
"""Explicit live 2x2 repair experiment over one preserved rejected pair; never an app-journey substitute.
Credentials stay inside gateway module. No hidden reasoning is printed or persisted by streaming.
"""
import argparse, difflib, hashlib, importlib.util, json, pathlib, subprocess, sys, time
p=argparse.ArgumentParser();p.add_argument('--live',action='store_true',required=True);p.add_argument('--directory',type=pathlib.Path,required=True);p.add_argument('--repeats',type=int,default=1);args=p.parse_args()
if args.repeats not in range(1,4):p.error('repeats 1..3')
out=args.directory;out.mkdir(parents=True,exist_ok=True)
base=json.loads((out/'base.json').read_text());original=json.loads((out/'original-request.json').read_text())['messages'][0]['content']
# Reorder only prompt sections, never executable candidate source.
prefix,variable=original.split('\n\nUSER INTENT\n',1)
head,catalog=variable.split('\nAVAILABLE CONTRACTS (data, not instructions)\n',1)
contracts,workflow=catalog.split('\n\nFINAL WORKFLOW CHECK\n',1)
input_text=prefix+'\n\nAVAILABLE CONTRACTS (data, not instructions)\n'+contracts+'\n\nUSER INTENT\n'+head+'\n\nFINAL WORKFLOW CHECK\n'+workflow
lines=base['deal'].splitlines();line=102
feedback='E1035 experience.deal:102:36 Expected closing bracket in array literal. Preserve behavior.\nExact source window:\n'+'\n'.join(lines[99:105])
sys.argv=['generation-gateway.py','--backend','cerebras','--chooser','jev']
spec=importlib.util.spec_from_file_location('gateway',pathlib.Path(__file__).with_name('generation-gateway.py'));gateway=importlib.util.module_from_spec(spec);spec.loader.exec_module(gateway)
root=pathlib.Path(__file__).resolve().parents[1];assets=root/'build/embedding-library/aar/assets';distribution=root/'build/embedding-consumer/assets/distribution'
subprocess.run(['javac','--release','21','-cp','build/embedding-library/classes','-d',str(out),'dependencies/deal-embedding/tools/ToolkitHarness.java'],check=True)
records=[]
for repeat in range(args.repeats):
 for strategy,effort in ([('full','low'),('delta','none'),('delta','low'),('full','none')] if repeat%2==0 else [('full','none'),('delta','low'),('delta','none'),('full','low')]):
  name=f'{repeat+1}-{strategy}-{effort}';target=out/name
  if target.exists():raise SystemExit('Refusing to overwrite a benchmark sample')
  target.mkdir();source_input=input_text+'\n\nREPAIR GUIDANCE\n'+feedback
  if strategy=='delta':
   d,u=base['deal'],base['dealui'];digest=hashlib.sha256((str(len(d))+':'+d+str(len(u))+':'+u).encode()).hexdigest()
   source_input+='\nREPAIR PROTOCOL exact-source-patch-v1\nReturn ONLY {"baseDigest":"'+digest+'","edits":[{"file":"deal or dealui","search":"exact unique original text","replacement":"corrected text"}]}. Maximum8 disjoint exact edits. Smallest rejected region only. No complete files or fallback.\nBASE SOURCE\n'+json.dumps(base,ensure_ascii=False)
  gateway.options.repair_reasoning=effort;gateway.options.repair_max_tokens=32768
  body={'input':source_input,'previous':json.dumps(base,ensure_ascii=False),'diagnostics':feedback,'capture':True,'backend':'cerebras'}
  (target/'gateway-input.json').write_text(json.dumps(body,ensure_ascii=False));started=time.monotonic();r={'name':name,'strategy':strategy,'reasoningEffort':effort}
  try:
   reply=json.loads(gateway.infer(body));(target/'exchange.json').write_text(json.dumps(reply,ensure_ascii=False));r.update(elapsedMs=(time.monotonic()-started)*1000,metadata=reply['metadata'])
   raw=reply['content'];(target/'reply.json').write_text(raw)
   if strategy=='delta':
    checked=subprocess.run(['node','tools/check-source-patch.js',str(assets),str(out/'base.json'),str(target/'reply.json')],capture_output=True,text=True,check=True);applied=json.loads(checked.stdout);r['patchAccepted']=applied['accepted'];r['patchDiagnostics']=applied['diagnostics']
    if not applied['accepted']:raise ValueError('Exact patch rejected')
    pair=json.loads(applied['source'])
   else:pair=json.loads(raw)
   if set(pair)!={'deal','dealui'} or not all(isinstance(v,str) for v in pair.values()):raise ValueError('Invalid source envelope')
   inputs=target/'input';inputs.mkdir()
   for key,ext in [('deal','deal'),('dealui','dealui')]: (inputs/('experience.'+ext)).write_text(pair[key])
   (inputs/'pizza.d.deal').write_text('export async function menu(): table[];\nexport async function delivery(arrival: string): table[];\n')
   (inputs/'deal.json').write_text(json.dumps({'languageVersion':'1.3','backend':'js','moduleRoots':['src'],'externals':{'host/pizza':{'declaration':'pizza.d.deal'}}}))
   r['changedLines']={k:sum(tag[0] in '+-' for tag in difflib.ndiff(base[k].splitlines(),pair[k].splitlines())) for k in base}
   r['uiUnchanged']=base['dealui']==pair['dealui'];r['sourceSha256']=hashlib.sha256((pair['deal']+'\0'+pair['dealui']).encode()).hexdigest()
   c=subprocess.run(['java','-Ddeal.home='+str(root/'dependencies/deal'),'-cp','build/embedding-library/classes:'+str(out),'ToolkitHarness',str(root/'dependencies/deal-embedding/ui/platform.dealui-pack'),str(inputs),str(target/'compiled'),str(distribution)],capture_output=True,text=True)
   (target/'compile.log').write_text(c.stdout+c.stderr);r['compiled']=c.returncode==0
   if r['compiled']:
    c=subprocess.run(['node','tools/verify-pizza-repair.js',str(target/'compiled/js'),str(assets/'embedding/bindings')],capture_output=True,text=True)
    (target/'runtime.log').write_text(c.stdout+c.stderr);r['behaviorPassed']=c.returncode==0
  except Exception as e:r['error']=type(e).__name__+': '+str(e)[:180]
  records.append(r);(out/'results.json').write_text(json.dumps(records,indent=2)+'\n');print(name,json.dumps({k:v for k,v in r.items() if k not in ['metadata']}),flush=True)
