#!/usr/bin/env python3
"""Loopback development gateway. Reads Pi credentials in memory; never logs provider data."""
import concurrent.futures, http.server, json, os, pathlib, subprocess, time, urllib.request
ROOT = pathlib.Path(__file__).resolve().parents[1]
CONFIG = json.loads((pathlib.Path.home()/'.pi/agent/models.json').read_text())['providers']['devagent']
KEY = CONFIG['apiKey']
if KEY.startswith('!'):
    KEY = subprocess.check_output(KEY[1:], shell=True, text=True).strip()
else:
    KEY = os.environ.get(KEY, KEY)
MODEL = next(m['id'] for m in CONFIG['models'] if m['id']=='cortex')
ENDPOINT = CONFIG['baseUrl'].rstrip('/')+'/chat/completions'
# Guidance is composed by the trusted core and travels in the request. This gateway only
# calls the model, so what runs here matches what would run on device.
# One inference at a time protects the development endpoint; no request bodies are logged.
POOL = concurrent.futures.ThreadPoolExecutor(max_workers=1)
# Telemetry: shape and timing only. Never prompts, model output, credentials or request bodies.
def event(**fields):
    fields['t'] = round(time.time(), 3)
    print(json.dumps(fields, sort_keys=True), flush=True)
def infer(body):
    messages=[{'role':'user','content':body['input']}]
    if body.get('previous'):
        messages.append({'role':'assistant','content':body['previous']})
    if body.get('diagnostics') or body.get('previous'):
        instruction = 'Return only the exact-source-patch-v1 edits requested in the input; preserve unaffected source.' if 'REPAIR PROTOCOL exact-source-patch-v1' in body['input'] else 'Generate or repair both source files using these diagnostics. Preserve the requested behavior. Return the complete JSON envelope.'
        messages.append({'role':'user','content':instruction+'\n'+body['diagnostics']})
    payload={'model':MODEL,'messages':messages,'max_tokens':8192,'temperature':0.2,'stream':False}
    request=urllib.request.Request(ENDPOINT,data=json.dumps(payload).encode(),headers={'Content-Type':'application/json','Authorization':'Bearer '+KEY})
    with urllib.request.urlopen(request,timeout=420) as response:
        data=json.loads(response.read(2*1024*1024))
    content=data['choices'][0]['message']['content']
    if not isinstance(content,str): raise ValueError('No text result')
    usage=data.get('usage') or {}
    event(kind='model_usage', model=MODEL, temperature=0.2, max_tokens=8192, input_tokens=usage.get('prompt_tokens'), output_tokens=usage.get('completion_tokens'), cached_tokens=(usage.get('prompt_tokens_details') or {}).get('cached_tokens'), finish_reason=data['choices'][0].get('finish_reason'))
    return content.encode()
class Handler(http.server.BaseHTTPRequestHandler):
    def log_message(self,*args): pass
    def do_POST(self):
        if self.path!='/generate': self.send_error(404); return
        try:
            length=int(self.headers.get('Content-Length','0'))
            if not 0<length<=256*1024: raise ValueError('Request limit')
            body=json.loads(self.rfile.read(length))
            if set(body)!={'input','previous','diagnostics'} or not all(isinstance(v,str) for v in body.values()): raise ValueError('Invalid request')
            started=time.time()
            event(kind='request', bytes=length, input_chars=len(body['input']), repair=bool(body.get('previous')), diagnostics_chars=len(body.get('diagnostics','')))
            try:
                output=POOL.submit(infer,body).result(timeout=450)
            except Exception as error:
                event(kind='inference_failed', seconds=round(time.time()-started,1), error=type(error).__name__, detail=str(error)[:200])
                raise
            if len(output)>256*1024: raise ValueError('Output limit')
            event(kind='response', seconds=round(time.time()-started,1), output_chars=len(output))
            self.send_response(200); self.send_header('Content-Type','application/json'); self.send_header('Content-Length',str(len(output))); self.end_headers(); self.wfile.write(output)
        except (BrokenPipeError,ConnectionResetError): pass
        except Exception as error:
            event(kind='handler_failed', error=type(error).__name__, detail=str(error)[:200], bytes=self.headers.get('Content-Length','0'))
            self.send_error(502,'Generation failed')
if __name__=='__main__':
    print('Development generation gateway listening on loopback:8787',flush=True)
    http.server.ThreadingHTTPServer(('127.0.0.1',8787),Handler).serve_forever()
