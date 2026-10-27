#!/usr/bin/env python3
"""Opt-in one-shot authoring through the production loopback gateway; no retries or correction.
Run only after deterministic verification and all builds have stopped. Credentials stay in gateway.
"""
import argparse,hashlib,importlib.util,json,pathlib,subprocess,time,urllib.request,urllib.error
ROOT=pathlib.Path(__file__).resolve().parents[1]
p=argparse.ArgumentParser();p.add_argument('--live',action='store_true');p.add_argument('--task',choices=['budget','departure','food','equipment','laboratory']);args=p.parse_args()
if not args.live:p.error('--live is required')
OUT=ROOT/'build/fragment-authoring'/str(time.time_ns());OUT.mkdir(parents=True)
spec=importlib.util.spec_from_file_location('fixtures',ROOT/'tools/behavior-fixtures.py');fixtures=importlib.util.module_from_spec(spec);spec.loader.exec_module(fixtures)
contracts=fixtures.contracts();(OUT/'contracts.json').write_text(json.dumps(contracts,indent=2))
guide=(ROOT/'dependencies/deal-embedding/ui/behavior-guidance.md').read_text()
tasks={
 'budget':([], 'Build an editable approved-versus-committed budget comparison. Approved is 18000 EUR cents, committed is 19750 EUR cents. Show difference and over/under/equal; both values stay editable.'),
 'departure':(['host/trip'],'Prepare departure for an event starting at 16:00. Distance is 12 km. Read actual travel minutes from the disclosed provider; allow start, distance, 15-minute unloading buffer and 10-minute pre-departure inspection buffer edits. Show latest departure and earlier inspection time; local arithmetic edits recompute and changed provider input invalidates its result.'),
 'food':(['host/pizza'],'Prepare crew dinner: six pizzas, at least two vegan, diameter 300 mm, budget 8000 EUR cents including actual delivery charge, required by 18:30. One action reads the real menu and delivery options and proposes a feasible whole-quantity allocation. Keep quantities, quantity requirement, minimum vegan, budget, deadline and delivery-window selection editable. Invalid/late window or infeasible quantities must not show Ready. Deadline edits must bind into provider reads. No source or task-answer literals.'),
 'equipment':(['host/rental'],'Compare available equipment hire for 2026-10-23T14:15:00Z until 2026-10-23T20:30:00Z. Require one adjustable stand with clip adapter and one balanced XLR female-to-XLR male cable at least 10000 mm. Match actual attributes and components, not names. Include package and separate alternatives under current shared stock. Read authoritative whole-basket quotes using id:quantity comma-separated items and the same period. Show hire and refundable deposits separately, budget 2000 EUR cents editable. Quotes expire according to each expiresInMinutes; refresh explicit. No catalogue-price answers or guessed identities.'),
 'laboratory':(['host/laboratory'],'Prepare three distinct physical units for a laboratory: one for each of three roles. Every role requires features.compatible=true. A single compatible unit cannot satisfy multiple roles. Read catalogue("cold",true,7). Resolve components from parts[].code/count and authoritative stock. Show the number of stock-qualified baskets as an integer result. There is no quote provider in this task. Do not add one or assume a price.')
}
summary=[]
try:
 for name,(modules,intent) in tasks.items():
  if args.task and name!=args.task:continue
  disclosed=[c for c in contracts if c['module'] in modules]
  text='MAINTAINED COMPOSITION\n'+guide+'\nDISCLOSED CAPABILITIES\n'+json.dumps(disclosed)+'\nAUTHORIZED TASK\n'+intent+'\nSOURCE OPERATION\ninitial\nNATIVE TOOL TURN\n'+json.dumps(dict(messages=[],initialWrites=True,compact=True))
  body=dict(input=text,previous='',diagnostics='',capture=True)
  (OUT/(name+'-request.json')).write_text(json.dumps(body,indent=2))
  startedFiles=['dependencies/deal-embedding/ui/fragment-contract.json','dependencies/deal-embedding/ui/behavior-guidance.md','dependencies/deal-embedding/android/src/compiler/FragmentComposition.kt','dependencies/deal-embedding/android/src/compiler/BehaviorCandidate.kt']
  (OUT/(name+'-inputs-sha256.json')).write_text(json.dumps({f:hashlib.sha256((ROOT/f).read_bytes()).hexdigest() for f in startedFiles},indent=2))
  receipt=dict(task=name,settings=dict(model='qwen-3.8-27b',reasoning='low',completionLimit='provider maximum',automaticRetries=False))
  try:
   start=time.monotonic();request=urllib.request.Request('http://127.0.0.1:8787/generate',data=json.dumps(body).encode(),headers={'Content-Type':'application/json','X-Deal-Generation-Backend':'cerebras'})
   with urllib.request.urlopen(request,timeout=None) as r:raw=r.read()
   result=json.loads(raw);(OUT/(name+'-response.json')).write_bytes(raw);receipt['elapsedMs']=(time.monotonic()-start)*1000;receipt['metadata']=result.get('metadata')
   calls=json.loads(result['content'])['tool_calls'];receipt['calls']=len(calls)
   if len(calls)!=1 or calls[0]['function']['name']!='submit_compact':raise ValueError('Expected one maintained submission')
   plan=json.loads(calls[0]['function']['arguments'])
   import jsonschema
   schema=json.loads((ROOT/'dependencies/deal-embedding/ui/fragment-contract.json').read_text());jsonschema.validate(plan,schema)
   target=OUT/(name+'.json');target.write_text(json.dumps(plan,indent=2))
   cp='build/behavior-verification/host:build/embedding-library/aar/classes.jar:build/behavior-four/json.jar:/snap/kotlin/current/lib/kotlin-stdlib.jar'
   for label,command in [
    ('assemble',['java','-cp',cp,'dev.deal.embedding.BehaviorHarnessKt',str(target),str(OUT/name/'input'),str(OUT/'contracts.json')]),
    ('compile',['java','-Ddeal.home=dependencies/deal','-cp','build/embedding-library/classes:build/behavior-verification/host','ToolkitHarness','dependencies/deal-embedding/ui/platform.dealui-pack',str(OUT/name/'input'),str(OUT/name/'check'),'build/embedding-consumer/assets/distribution'])]:
    r=subprocess.run(command,cwd=ROOT,stdout=subprocess.PIPE,stderr=subprocess.STDOUT);(OUT/(name+'-'+label+'.log')).write_bytes(r.stdout);receipt[label]=r.returncode
    if r.returncode:break
   if receipt.get('compile')==0:
    r=subprocess.run(['/home/igelhaus/.nvm/versions/node/v26.11.1/bin/node','tools/test-fragment-authoring.js',str(OUT),name],cwd=ROOT,stdout=subprocess.PIPE,stderr=subprocess.STDOUT);(OUT/(name+'-runtime.log')).write_bytes(r.stdout);receipt['runtime']=r.returncode
  except urllib.error.HTTPError as error:
   receipt['failure']='HTTPError: '+str(error.code);(OUT/(name+'-http-error.json')).write_bytes(error.read())
  except Exception as error:
   receipt['failure']=type(error).__name__+': '+str(error)
  summary.append(receipt);(OUT/'summary.json').write_text(json.dumps(summary,indent=2));print(name+': '+json.dumps({k:v for k,v in receipt.items() if k not in ('metadata','settings')}),flush=True)
finally:
 files=[x for x in OUT.rglob('*') if x.is_file()]
 (OUT/'sha256.json').write_text(json.dumps({str(f.relative_to(OUT)):hashlib.sha256(f.read_bytes()).hexdigest() for f in files},indent=2))
 print(str(OUT),flush=True)
