#!/usr/bin/env python3
"""One audit-only native tool-feedback turn after each failed first-response probe.
This is not a production recovery policy. Original first-response results stay immutable.
"""
import argparse,importlib.util,json,pathlib
spec=importlib.util.spec_from_file_location('probe',pathlib.Path(__file__).with_name('probe-native-tools.py'));p=importlib.util.module_from_spec(spec);spec.loader.exec_module(p)
def main():
 ap=argparse.ArgumentParser(description=__doc__);ap.add_argument('--out',type=pathlib.Path,required=True);ap.add_argument('--live',action='store_true');a=ap.parse_args()
 if not a.live:raise ValueError('Explicit --live required')
 out=a.out.resolve();summary=json.loads((out/'summary.json').read_text());key=(pathlib.Path.home()/'cerebras.key').read_text().strip();results=[]
 for run in sorted((out/'runs').iterdir()):
  if not (run/'result.json').exists():continue
  first=json.loads((run/'result.json').read_text())
  if first['arm']!='native' or first['outcome']=='compile_pass':continue
  c=json.loads((out/'cases'/(first['case']+'.json')).read_text());step=run/'followup'
  if step.exists():raise ValueError('Refuse overwriting follow-up')
  step.mkdir();request=json.loads((run/'request.json').read_text());message=json.loads((run/'response.json').read_text())['choices'][0]['message'];applied=json.loads((run/'application.json').read_text());base=json.loads(applied['source']) if applied['accepted'] else c['base']
  diagnostics=json.dumps(first['compile']['diagnostics']) if applied['accepted'] else first.get('rejection','Rejected patch')
  context=p.adapter({'mode':'context','base':base,'diagnostics':diagnostics})
  feedback={'accepted':applied['accepted'],'diagnostics':first.get('compile',{}).get('diagnostics') if applied['accepted'] else first.get('rejection'),'guidance':context['guidance'],'currentFiles':base,'instruction':'Correct the reported failure through edit_sources. Host binds current revision; searches apply to currentFiles, not the original source. Do not emit digests, markers or explanations.'}
  request['messages'] += [{'role':'assistant','content':message.get('content'),'tool_calls':message['tool_calls']},{'role':'tool','tool_call_id':message['tool_calls'][0]['id'],'content':json.dumps(feedback,ensure_ascii=False)}]
  response=p.provider(request,key,step);result={'case':first['case'],'repeat':first['repeat'],'providerMs':response['ms'],'protocolValid':False,'applied':False}
  if response['http']!=200:result.update(outcome='provider_error',http=response['http'])
  else:
   data=response['data'];result.update(usage=data.get('usage') or {},finish=data['choices'][0].get('finish_reason'))
   try:
    edits=p.validate_tool(data['choices'][0]['message']);result['protocolValid']=True;application=p.adapter({'mode':'edits','base':base,'edits':edits});p.save(step/'application.json',application);result.update(applied=application['accepted'],rejection=application['diagnostics'])
    if application['accepted']:
     result['compile']=p.compile_pair(json.loads(application['source']),c['catalog'],step/'check',out/'classes');result['outcome']='compile_pass' if result['compile']['passed'] else 'compile_rejected'
    else:result['outcome']='edit_rejected'
   except (ValueError,KeyError,TypeError) as e:result.update(outcome='protocol_rejected',error=str(e))
  p.save(step/'result.json',result);results.append(result);p.save(out/'continuations.json',results);print(run.name,result['outcome'],flush=True)
if __name__=='__main__':main()
