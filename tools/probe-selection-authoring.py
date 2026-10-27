#!/usr/bin/env python3
"""Opt-in isolated model-authoring probe. Credentials remain in a loopback gateway.
No retries, selectors, verifiers, device writes or changes to shipping settings.
"""
import argparse,hashlib,http.server,json,pathlib,sys,threading,time,urllib.request
from gateway_transport import read_stream
ROOT=pathlib.Path(__file__).resolve().parents[1]
parser=argparse.ArgumentParser();parser.add_argument('--live',action='store_true');parser.add_argument('--task',choices=['food','equipment','laboratory']);args=parser.parse_args()
if not args.live:parser.error('--live is required')
OUT=ROOT/'build/selection-authoring'/str(time.time_ns());OUT.mkdir(parents=True)
guide=(ROOT/'dependencies/deal-embedding/ui/selection-prototype.md').read_text()
# This local handler is the only component with access to the external credential.
class Gateway(http.server.BaseHTTPRequestHandler):
 def log_message(self,*args):pass
 def do_POST(self):
  try:
   body=json.loads(self.rfile.read(int(self.headers['Content-Length'])))
   payload=dict(model='qwen-3.8-27b',messages=body['messages'],temperature=1,top_p=.95,reasoning_effort='low',clear_thinking=True,stream=True,stream_options={'include_usage':True},tools=[dict(type='function',function=dict(name='submit_selection_probe',description='Submit semantic composition data; no executable source.',strict=True,parameters=dict(type='object',additionalProperties=False,required=['composition'],properties=dict(composition=dict(type='string',description='JSON-encoded composition conforming to the supplied semantic contract.')))))],tool_choice='required',parallel_tool_calls=False)
   key=(pathlib.Path.home()/'cerebras.key').read_text().strip()
   request=urllib.request.Request('https://api.cerebras.ai/v1/chat/completions',data=json.dumps(payload).encode(),headers={'Authorization':'Bearer '+key,'Content-Type':'application/json','User-Agent':'deal-embed-selection-probe/1.0'})
   start=time.monotonic()
   with urllib.request.urlopen(request,timeout=None) as response:data,phases=read_stream(response,start)
   result=dict(response=data,phases=phases,settings=dict(model=payload['model'],reasoning_effort='low',completionLimit='provider maximum',automaticRetries=False))
   encoded=json.dumps(result).encode();self.send_response(200);self.end_headers();self.wfile.write(encoded)
  except Exception as e:
   encoded=json.dumps(dict(error=type(e).__name__)).encode();self.send_response(502);self.end_headers();self.wfile.write(encoded)
server=http.server.HTTPServer(('127.0.0.1',0),Gateway);thread=threading.Thread(target=server.serve_forever,daemon=True);thread.start()
# Task facts/bindings only. No exemplar plan, source, graph, selected answers or provider records.
specs={
'food':('host/pizza', '''Build a crew meal allocation. Inputs use IDs/labels total/Required quantity=6 (0..100), budget/Budget=8000 cents (0..100000), deadline/Required by=1110 minute of day (0..1439). Read menu and delivery for the editable deadline formatted HH:mm. Only diameterMm 300 items qualify. Exactly total items, at least two vegan, quantities no greater than total. Delivery eligibility must hold, and total meal cost plus delivery charge must fit budget. For this probe use the first eligible delivery record; guard its presence. Cost must come from the selected delivery record, never a literal. Local evaluation suffices; TTL 60000ms.'''),
'equipment':('host/rental', '''Compare every stock-qualified basket covering one adjustable stand with clipAdapter and one balanced cable of lengthMm at least 10000. Matching kind values are stand and cable. Components provide their referenced items. Include package and separate alternatives. Editable text inputs IDs/labels start/Start=2026-10-23T14:15:00Z, end/End=2026-10-23T20:30:00Z. Read equipment for that period. Provider quote is authoritative: do not prune using catalogue cents. Quote each candidate using the documented id:quantity comma-separated transport and the same period. Result cents, available and expiresInMinutes govern current results; convert TTL to milliseconds. Each role quantity is exactly one.'''),
'laboratory':('host/laboratory', '''Find alternatives for lab stations. Input IDs/labels zone/Zone text north; headcount/Stations integer 1 (1..10). Read inventory(zone,true). For each station require exactly one sensor, one power and one logger, matching spec.role; capacity is stock. Component contents refer to skuRef and units. Include bundles and separate items; shared stock must be respected. Provider assess(zone, typed basket array [{sku,units}], revision=7) is authoritative; no catalogue price pruning. Use returned charge, ok, validForMs. All three role totals equal headcount.''')}
# Contracts contain no records or expected outputs.
import importlib.util
spec=importlib.util.spec_from_file_location('sf',ROOT/'tools/selection-fixtures.py');sf=importlib.util.module_from_spec(spec);spec.loader.exec_module(sf)
summary=[]
try:
 for name,(module,intent) in specs.items():
  if args.task and args.task!=name:continue
  contract=next(c for c in sf.contracts() if c['module']==module)
  messages=[dict(role='system',content='Author one semantic composition using only this experimental contract. Do not emit source, execution graphs or explanation. Submit through the tool.\n\n'+guide),dict(role='user',content='DISCLOSED CAPABILITY\n'+json.dumps(contract)+'\nTASK\n'+intent)]
  (OUT/(name+'-request.json')).write_text(json.dumps(dict(messages=messages),indent=2))
  start=time.monotonic()
  request=urllib.request.Request(f'http://127.0.0.1:{server.server_port}/probe',data=json.dumps(dict(messages=messages)).encode(),headers={'Content-Type':'application/json'})
  with urllib.request.urlopen(request,timeout=None) as response:result=json.loads(response.read())
  (OUT/(name+'-response.json')).write_text(json.dumps(result,indent=2))
  calls=result['response']['choices'][0]['message'].get('tool_calls',[])
  if len(calls)!=1 or calls[0]['function']['name']!='submit_selection_probe':raise ValueError('Expected one native submission')
  arguments=json.loads(calls[0]['function']['arguments']);plan=json.loads(arguments['composition'])
  (OUT/(name+'.json')).write_text(json.dumps(plan,indent=2))
  summary.append(dict(task=name,elapsedMs=(time.monotonic()-start)*1000,usage=result['response'].get('usage'),calls=len(calls)))
  (OUT/'summary.json').write_text(json.dumps(summary,indent=2));print(name+' authored',flush=True)
finally:server.shutdown();server.server_close()
print(str(OUT),flush=True)
