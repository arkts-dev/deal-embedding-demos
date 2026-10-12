#!/usr/bin/env python3
"""Loopback development gateway. Reads Pi credentials in memory; never logs provider data."""
import argparse, concurrent.futures, http.server, json, os, pathlib, subprocess, time, urllib.request, urllib.error
from jev_chooser import infer as infer_jev
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
# Demo is the default: deliberate requests, with bounded repairs owned by DEAL.
# Audit ceilings require explicit CLI arguments, never inherited environment state.
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--counts', type=pathlib.Path, help='Persist admission counts only; no content')
parser.add_argument('--audit', action='store_true', help='Explicit bounded autonomous-test mode, NOT a demo launcher')
parser.add_argument('--source-limit', type=int)
parser.add_argument('--choice-limit', type=int)
parser.add_argument('--chooser', choices=('llm', 'jev'), default='llm', help='Choice transport; source generation remains on the configured generative model')
parser.add_argument('--jev-key-file', type=pathlib.Path, default=pathlib.Path.home()/'jev.key', help='Read the Jev credential in memory; never packaged or logged')
parser.add_argument('--jev-model', default='jev-latest')
options = parser.parse_args()
JEV_KEY = options.jev_key_file.read_text().strip() if options.chooser == 'jev' else ''
if options.chooser == 'jev' and (not JEV_KEY or any(c.isspace() for c in JEV_KEY)):
    parser.error('Jev credential must be a nonempty single token')
if options.audit and (not options.counts or options.source_limit is None or options.choice_limit is None or options.source_limit <= 0 or options.choice_limit <= 0):
    parser.error('--audit requires --counts and positive source/choice limits')
if not options.audit and (options.source_limit is not None or options.choice_limit is not None):
    parser.error('Call ceilings require explicit --audit mode')
COUNTS_PATH = options.counts
SOURCE_LIMIT = options.source_limit if options.audit else None
CHOICE_LIMIT = options.choice_limit if options.audit else None
class GatewayFailure(Exception):
    def __init__(self, code, message, status=503):
        super().__init__(message); self.code=code; self.status=status

def admit(body):
    if not COUNTS_PATH: return
    path = pathlib.Path(COUNTS_PATH)
    counts = json.loads(path.read_text()) if path.exists() else {'choice': 0, 'source': 0}
    kind = 'choice' if '\n\nISSUED OPTIONS\n' in body['input'] else 'source'
    limit = SOURCE_LIMIT if kind == 'source' else CHOICE_LIMIT
    if limit is not None and counts[kind] >= limit:
        raise GatewayFailure('AUDIT_LIMIT', 'Explicit audit call ceiling exhausted; start the gateway in demo mode for user demonstrations.')
    counts[kind] += 1
    path.parent.mkdir(parents=True, exist_ok=True)
    temporary = path.with_suffix('.tmp')
    temporary.write_text(json.dumps(counts)); temporary.replace(path)
# Return provider request/response to the caller's unified private logger; no parallel logs.
def infer(body):
    admit(body)
    if options.chooser == 'jev' and '\n\nISSUED OPTIONS\n' in body['input']:
        return infer_jev(body, JEV_KEY, options.jev_model)
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
    def reply(self, status, body):
        output=json.dumps(body).encode()
        self.send_response(status); self.send_header('Content-Type','application/json'); self.send_header('Content-Length',str(len(output))); self.end_headers(); self.wfile.write(output)
    def do_GET(self):
        if self.path != '/health': self.send_error(404); return
        # Readiness of this transport/configuration only, NOT a provider success claim.
        self.reply(200, {'mode':'audit' if options.audit else 'demo', 'configured':bool(KEY) and (options.chooser != 'jev' or bool(JEV_KEY)), 'automaticRetries':False, 'chooser':options.chooser})
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
        except GatewayFailure as error:
            self.reply(error.status, {'error':{'code':error.code, 'message':str(error)}})
        except urllib.error.HTTPError as error:
            # Never reflect provider bodies, URLs, credentials or authentication headers.
            self.reply(502, {'error':{'code':'PROVIDER_HTTP', 'message':'Model provider rejected the request', 'providerStatus':error.code}})
        except (TimeoutError, concurrent.futures.TimeoutError):
            self.reply(504, {'error':{'code':'MODEL_TIMEOUT', 'message':'Model provider did not finish within the request deadline'}})
        except urllib.error.URLError:
            self.reply(503, {'error':{'code':'PROVIDER_UNREACHABLE', 'message':'Cannot connect to the model provider'}})
        except Exception:
            self.reply(502, {'error':{'code':'GATEWAY_FAILURE', 'message':'Development gateway could not complete the request'}})
if __name__=='__main__':
    print('Development generation gateway listening on loopback:8787 · '+('AUDIT limits enabled' if options.audit else 'DEMO · no audit ceiling')+' · chooser '+options.chooser+' · no automatic retries',flush=True)
    http.server.ThreadingHTTPServer(('127.0.0.1',8787),Handler).serve_forever()
