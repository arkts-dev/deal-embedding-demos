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
# Return provider request/response to the caller's unified private logger; no parallel logs.
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
    if len(content.encode()) > 256*1024: raise ValueError('Model content limit')
    usage=data.get('usage') or {}
    def count(value): return value if type(value) is int and 0<=value<=2147483647 else None
    finish=data['choices'][0].get('finish_reason')
    metadata={'in':count(usage.get('prompt_tokens')), 'out':count(usage.get('completion_tokens')),
              'cached':count((usage.get('prompt_tokens_details') or {}).get('cached_tokens')),
              'finish':finish if finish in ('stop','length','content_filter','tool_calls') else 'unknown',
              'temperature':0.2, 'max_tokens':8192}
    result={'content':content, 'model':MODEL, 'metadata':metadata}
    if body['capture']: result.update(request=payload, response=data)
    encoded=json.dumps(result).encode()
    if len(encoded)>2*1024*1024 and body['capture']:
        # Observation must not reject otherwise valid model content.
        del result['request']; del result['response']; result['captureOmitted']=True
        encoded=json.dumps(result).encode()
    return encoded
class Handler(http.server.BaseHTTPRequestHandler):
    def log_message(self,*args): pass
    def do_POST(self):
        if self.path!='/generate': self.send_error(404); return
        try:
            length=int(self.headers.get('Content-Length','0'))
            if not 0<length<=256*1024: raise ValueError('Request limit')
            body=json.loads(self.rfile.read(length))
            if set(body)!={'input','previous','diagnostics','capture'} or not all(isinstance(body[v],str) for v in ('input','previous','diagnostics')) or type(body['capture']) is not bool: raise ValueError('Invalid request')
            output=POOL.submit(infer,body).result(timeout=450)
            if len(output)>2*1024*1024: raise ValueError('Output limit')
            self.send_response(200); self.send_header('Content-Type','application/json'); self.send_header('Content-Length',str(len(output))); self.end_headers(); self.wfile.write(output)
        except (BrokenPipeError,ConnectionResetError): pass
        except Exception as error:
            self.send_error(502,'Generation failed')
if __name__=='__main__':
    print('Development generation gateway listening on loopback:8787',flush=True)
    http.server.ThreadingHTTPServer(('127.0.0.1',8787),Handler).serve_forever()
