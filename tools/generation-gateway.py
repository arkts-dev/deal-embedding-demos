#!/usr/bin/env python3
"""Loopback development gateway. Reads Pi credentials in memory; never logs provider data."""
import argparse, concurrent.futures, http.server, json, os, pathlib, subprocess, time, urllib.request, urllib.error
from jev_chooser import infer as infer_jev
from gateway_transport import stable_messages, read_stream, prefix_fingerprint
from native_source_tools import tools_for, source_messages, initial_write_batch, native_result, strip_reasoning
ROOT = pathlib.Path(__file__).resolve().parents[1]
# Credentials are loaded only for enabled backends, never packaged in an APK.
BACKENDS = {}
# Guidance is composed by the trusted core and travels in the request. This gateway only
# calls the model, so what runs here matches what would run on device.
# One inference at a time protects the development endpoint; no request bodies are logged.
POOL = concurrent.futures.ThreadPoolExecutor(max_workers=1)
# Demo is the default: recovery until acceptance/cancellation is owned by DEAL.
# Audit ceilings require explicit CLI arguments, never inherited environment state.
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--counts', type=pathlib.Path, help='Persist admission counts only; no content')
parser.add_argument('--audit', action='store_true', help='Explicit bounded autonomous-test mode, NOT a demo launcher')
parser.add_argument('--source-limit', type=int)
parser.add_argument('--choice-limit', type=int)
parser.add_argument('--chooser', choices=('llm', 'jev'), default='llm', help='Choice transport; source generation remains on the configured generative model')
parser.add_argument('--jev-key-file', type=pathlib.Path, default=pathlib.Path.home()/'jev.key', help='Read the Jev credential in memory; never packaged or logged')
parser.add_argument('--jev-model', default='jev-latest')
parser.add_argument('--backend', choices=('cortex', 'cerebras'), default='cortex', help='Default backend for tooling without an app build-selection header')
parser.add_argument('--enable-cortex', action='store_true', help='Also serve Cortex when Cerebras is the default')
parser.add_argument('--enable-cerebras', action='store_true', help='Also serve Cerebras when Cortex is the default')
parser.add_argument('--discovery-backend', choices=('disabled', 'deepseek-flash'), default='disabled', help='Explicit remote discovery; credential comes only from DEXTER_DEEPSEEK_API_KEY')
parser.add_argument('--cerebras-key-file', type=pathlib.Path, default=pathlib.Path.home()/'cerebras.key')
parser.add_argument('--initial-reasoning', choices=('low','medium','high'), default='low')
parser.add_argument('--repair-reasoning', choices=('low','medium','high'), default='low')
parser.add_argument('--initial-max-tokens', type=int, default=-1)
parser.add_argument('--repair-max-tokens', type=int, default=-1)
parser.add_argument('--stream-source', action=argparse.BooleanOptionalAction, default=True, help='Measure Cerebras stream phases; partial output never leaves gateway')
options = parser.parse_args()
if any(n != -1 and n <= 0 for n in (options.initial_max_tokens,options.repair_max_tokens)):
    parser.error('Source completion limits must be -1 (provider maximum) or positive')
if options.backend == 'cortex' or options.enable_cortex:
    config = json.loads((pathlib.Path.home()/'.pi/agent/models.json').read_text())['providers']['devagent']
    key = config['apiKey']
    key = subprocess.check_output(key[1:], shell=True, text=True).strip() if key.startswith('!') else os.environ.get(key, key)
    BACKENDS['cortex'] = {'key': key, 'model': 'cortex', 'endpoint': config['baseUrl'].rstrip('/')+'/chat/completions'}
if options.backend == 'cerebras' or options.enable_cerebras:
    key = options.cerebras_key_file.read_text().strip()
    if not key or any(c.isspace() for c in key): parser.error('Cerebras credential must be a nonempty single token')
    BACKENDS['cerebras'] = {'key': key, 'model': 'qwen-3.8-27b', 'endpoint': 'https://api.cerebras.ai/v1/chat/completions'}
DISCOVERY = None
if options.discovery_backend == 'deepseek-flash':
    key = os.environ.get('DEXTER_DEEPSEEK_API_KEY', '')
    if not key or any(c.isspace() for c in key): parser.error('DEXTER_DEEPSEEK_API_KEY must be a nonempty single token')
    DISCOVERY = {'key':key, 'model':'deepseek-flash', 'endpoint':'https://api.deepseek.com/chat/completions'}
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
    if body.get('discovery'): kind = 'discovery'
    counts.setdefault(kind, 0)
    limit = SOURCE_LIMIT if kind in ('source', 'discovery') else CHOICE_LIMIT
    if limit is not None and counts[kind] >= limit:
        raise GatewayFailure('AUDIT_LIMIT', 'Explicit audit call ceiling exhausted; start the gateway in demo mode for user demonstrations.')
    counts[kind] += 1
    path.parent.mkdir(parents=True, exist_ok=True)
    temporary = path.with_suffix('.tmp')
    temporary.write_text(json.dumps(counts)); temporary.replace(path)
# Return provider request/response to the caller's unified private logger; no parallel logs.
def infer(body):
    backend = body.get('backend', options.backend)
    discovery = body.get('discovery', False)
    if discovery:
        if DISCOVERY is None or backend != 'deepseek-flash':
            raise GatewayFailure('BACKEND_UNAVAILABLE', 'Requested discovery backend is not enabled')
    elif backend not in BACKENDS:
        raise GatewayFailure('BACKEND_UNAVAILABLE', 'Requested build-time backend is not enabled in this gateway')
    admit(body)
    if not discovery and options.chooser == 'jev' and '\n\nISSUED OPTIONS\n' in body['input']:
        return infer_jev(body, JEV_KEY, options.jev_model)
    source = not discovery and '\n\nISSUED OPTIONS\n' not in body['input']
    messages=source_messages(body) if source else stable_messages(body)
    repairing=bool(body.get('previous')) or '\nSOURCE OPERATION\nrepair\n' in body['input']
    operation='discovery' if discovery else 'repair' if repairing else 'initial'
    provider = DISCOVERY if discovery else BACKENDS[backend]
    payload={'model':provider['model'],'messages':messages,'max_tokens':8192,'temperature':0.2,'stream':False}
    if backend == 'cerebras':
        effort=getattr(options,'repair_reasoning','low') if repairing else getattr(options,'initial_reasoning','low')
        limit=getattr(options,'repair_max_tokens',-1) if repairing else getattr(options,'initial_max_tokens',-1)
        if effort not in ('low','medium','high'): raise ValueError('Source reasoning must be enabled')
        payload.update(max_tokens=limit, temperature=1, top_p=0.95, reasoning_effort=effort, clear_thinking=True)
        # Source operations use the provider-native tool channel; choices remain independent.
        if '\n\nISSUED OPTIONS\n' in body['input']:
            payload['response_format'] = {'type':'json_object'}
        if limit == -1: payload.pop('max_tokens',None)
        if getattr(options,'stream_source',False):
            payload.update(stream=True, stream_options={'include_usage':True})
    if source:
        payload.update(tools=tools_for(body), tool_choice='required', parallel_tool_calls=initial_write_batch(body))
    if discovery:
        # Discovery can return literal null; JSON-object mode would change the existing contract.
        payload.update(max_tokens=4096, temperature=0, thinking={'type':'disabled'})
    # Cerebras rejects urllib's default User-Agent; explicit identity is required.
    request=urllib.request.Request(provider['endpoint'],data=json.dumps(payload).encode(),headers={'Content-Type':'application/json','Authorization':'Bearer '+provider['key'],'User-Agent':'deal-embed-development/1.0'})
    started=time.monotonic()
    phases=None
    with urllib.request.urlopen(request,timeout=None) as response:
        if payload['stream']: data,phases=read_stream(response,started)
        else: data=json.loads(response.read())
    total_ms=(time.monotonic()-started)*1000
    strip_reasoning(data)
    content=native_result(data['choices'][0]['message']) if source else data['choices'][0]['message']['content']
    if not isinstance(content,str): raise ValueError('No text result')
    usage=data.get('usage') or {}
    def count(value): return value if type(value) is int and 0<=value<=2147483647 else None
    finish=data['choices'][0].get('finish_reason')
    metadata={'in':count(usage.get('prompt_tokens')), 'out':count(usage.get('completion_tokens')),
              'cached':count((usage.get('prompt_tokens_details') or {}).get('cached_tokens')),
              'finish':finish if finish in ('stop','length','content_filter','tool_calls') else 'unknown',
              'temperature':payload['temperature'], 'max_tokens':payload.get('max_tokens',-1),
              'reasoning':count((usage.get('completion_tokens_details') or {}).get('reasoning_tokens'))}
    if discovery: metadata['thinking'] = 'disabled'
    metadata.update(operation=operation,totalMs=total_ms,prefixSha256=prefix_fingerprint(messages))
    if phases is not None: metadata['phases']=phases
    if isinstance(data.get('time_info'),dict): metadata['providerTime']=data['time_info']
    if backend == 'cerebras': metadata['reasoning_effort'] = payload['reasoning_effort']
    result={'content':content, 'model':provider['model'], 'metadata':metadata}
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
        health = {'mode':'audit' if options.audit else 'demo', 'configured':all(bool(p['key']) for p in BACKENDS.values()) and (options.chooser != 'jev' or bool(JEV_KEY)), 'automaticRetries':False, 'chooser':options.chooser, 'defaultBackend':options.backend, 'sourceModels':{name:p['model'] for name,p in BACKENDS.items()}}
        if DISCOVERY is not None: health['discoveryModel'] = DISCOVERY['model']
        self.reply(200, health)
    def do_POST(self):
        if self.path not in ('/generate', '/discover'): self.send_error(404); return
        try:
            length=int(self.headers.get('Content-Length','0'))
            if length<=0: raise ValueError('Empty request')
            body=json.loads(self.rfile.read(length))
            if set(body)!={'input','previous','diagnostics','capture'} or not all(isinstance(body[v],str) for v in ('input','previous','diagnostics')) or type(body['capture']) is not bool: raise ValueError('Invalid request')
            backend = self.headers.get('X-Deal-Generation-Backend', options.backend)
            if self.path == '/discover':
                if DISCOVERY is None or backend != 'deepseek-flash': raise GatewayFailure('BACKEND_UNAVAILABLE', 'Requested discovery backend is not enabled')
                if body['previous'] or body['diagnostics']: raise ValueError('Discovery cannot repair or replay')
                body['discovery'] = True
            elif backend not in BACKENDS: raise GatewayFailure('BACKEND_UNAVAILABLE', 'Requested build-time backend is not enabled in this gateway')
            body['backend'] = backend
            output=POOL.submit(infer,body).result()
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
    print('Development generation gateway listening on loopback:8787 · '+('AUDIT limits enabled' if options.audit else 'DEMO · no audit ceiling')+' · chooser '+options.chooser+' · source backends '+','.join(BACKENDS)+' · no automatic retries',flush=True)
    http.server.ThreadingHTTPServer(('127.0.0.1',8787),Handler).serve_forever()
