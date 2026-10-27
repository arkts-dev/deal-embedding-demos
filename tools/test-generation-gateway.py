#!/usr/bin/env python3
"""No credentials loaded, no network: exact provider exchange envelope contract."""
import ast, json, pathlib, types, unittest, tempfile, http.server, urllib.request, urllib.error, concurrent.futures, threading, time
from gateway_transport import stable_messages, read_stream, prefix_fingerprint
from native_source_tools import SOURCE_TOOLS, COMPACT_TOOLS, tools_for, source_messages, initial_write_batch, native_result, strip_reasoning

class GatewayTest(unittest.TestCase):
    def test_budget(self):
        tree=ast.parse(pathlib.Path('tools/generation-gateway.py').read_text())
        admit=next(n for n in tree.body if isinstance(n,ast.FunctionDef) and n.name=='admit')
        with tempfile.TemporaryDirectory() as directory:
            path=pathlib.Path(directory)/'counts.json'
            failure=type('GatewayFailure',(Exception,),{})
            namespace={'json':json,'pathlib':pathlib,'COUNTS_PATH':str(path),'SOURCE_LIMIT':8,'CHOICE_LIMIT':2,'GatewayFailure':failure}
            exec(compile(ast.Module(body=[admit],type_ignores=[]),'budget','exec'),namespace)
            for _ in range(8): namespace['admit']({'input':'DEAL RULES source'})
            for _ in range(2): namespace['admit']({'input':'DEAL RULES\n\nISSUED OPTIONS\nx'})
            self.assertEqual(json.loads(path.read_text()),{'source':8,'choice':2})
            for text in ('source','\n\nISSUED OPTIONS\nx'):
                with self.assertRaises(failure): namespace['admit']({'input':text})
            namespace.update(SOURCE_LIMIT=None, CHOICE_LIMIT=None)
            namespace['admit']({'input':'source'}); namespace['admit']({'input':'\n\nISSUED OPTIONS\nx'})
            self.assertEqual(json.loads(path.read_text()),{'source':9,'choice':3})
    def test_safe_failures_and_health(self):
        tree=ast.parse(pathlib.Path('tools/generation-gateway.py').read_text())
        definitions=[n for n in tree.body if isinstance(n,ast.ClassDef) and n.name in ('GatewayFailure','Handler')]
        namespace={'json':json,'http':types.SimpleNamespace(server=http.server),'urllib':types.SimpleNamespace(error=urllib.error),
                   'concurrent':types.SimpleNamespace(futures=concurrent.futures),'options':types.SimpleNamespace(audit=False,chooser='llm',backend='cortex'),'BACKENDS':{'cortex':{'key':'private-key','model':'cortex'}},'DISCOVERY':None}
        exec(compile(ast.Module(body=definitions,type_ignores=[]),'safe-handler','exec'),namespace)
        class Failed:
            def result(self,**kwargs):raise namespace['GatewayFailure']('AUDIT_LIMIT','Explicit audit ceiling exhausted')
        routed=[]
        namespace.update(POOL=types.SimpleNamespace(submit=lambda fn,body:routed.append(body) or Failed()),infer=lambda body:None)
        server=http.server.ThreadingHTTPServer(('127.0.0.1',0),namespace['Handler']);thread=threading.Thread(target=server.serve_forever,daemon=True);thread.start()
        try:
            base='http://127.0.0.1:'+str(server.server_port)
            with urllib.request.urlopen(base+'/health') as response:
                health=json.load(response);self.assertEqual(health,{'mode':'demo','configured':True,'automaticRetries':False,'chooser':'llm','defaultBackend':'cortex','sourceModels':{'cortex':'cortex'}});self.assertNotIn('private-key',json.dumps(health))
            request=urllib.request.Request(base+'/generate',data=json.dumps({'input':'x','previous':'','diagnostics':'','capture':False}).encode())
            with self.assertRaises(urllib.error.HTTPError) as caught:urllib.request.urlopen(request)
            self.assertEqual(caught.exception.code,503);self.assertEqual(json.load(caught.exception)['error']['code'],'AUDIT_LIMIT')
            self.assertEqual(routed[-1]['backend'],'cortex')
            request.add_header('X-Deal-Generation-Backend','cerebras')
            with self.assertRaises(urllib.error.HTTPError) as caught:urllib.request.urlopen(request)
            self.assertEqual(json.load(caught.exception)['error']['code'],'BACKEND_UNAVAILABLE')
            self.assertEqual(len(routed),1,'unavailable backend must not fall back')
            discovery=urllib.request.Request(base+'/discover',data=json.dumps({'input':'facts','previous':'','diagnostics':'','capture':True}).encode(),headers={'X-Deal-Generation-Backend':'deepseek-flash'})
            with self.assertRaises(urllib.error.HTTPError) as caught:urllib.request.urlopen(discovery)
            self.assertEqual(json.load(caught.exception)['error']['code'],'BACKEND_UNAVAILABLE')
            namespace['DISCOVERY']={'model':'deepseek-flash'}
            with self.assertRaises(urllib.error.HTTPError):urllib.request.urlopen(discovery)
            self.assertTrue(routed[-1]['discovery']);self.assertEqual(routed[-1]['backend'],'deepseek-flash')
            previous=len(routed)
            bad=urllib.request.Request(base+'/discover',data=json.dumps({'input':'facts','previous':'old source','diagnostics':'','capture':False}).encode(),headers={'X-Deal-Generation-Backend':'deepseek-flash'})
            with self.assertRaises(urllib.error.HTTPError) as caught:urllib.request.urlopen(bad)
            self.assertEqual(json.load(caught.exception)['error']['code'],'GATEWAY_FAILURE')
            self.assertEqual(len(routed),previous,'discovery must not replay source')
        finally:server.shutdown();server.server_close();thread.join()
    def test_jev_routing(self):
        tree=ast.parse(pathlib.Path('tools/generation-gateway.py').read_text())
        infer=next(n for n in tree.body if isinstance(n,ast.FunctionDef) and n.name=='infer')
        calls=[]
        namespace={'options':types.SimpleNamespace(chooser='jev',jev_model='jev-test',backend='cortex'),'BACKENDS':{'cortex':{}},'JEV_KEY':'private-key',
                   'admit':lambda body:calls.append('admit'),'infer_jev':lambda *args:calls.append(args) or b'jev'}
        exec(compile(ast.Module(body=[infer],type_ignores=[]),'routing','exec'),namespace)
        body={'input':'state\n\nISSUED OPTIONS\n{}'}
        self.assertEqual(namespace['infer'](body),b'jev')
        self.assertEqual(calls,['admit',(body,'private-key','jev-test')])
        namespace['infer_jev']=lambda *args:(_ for _ in ()).throw(urllib.error.URLError('unreachable'))
        with self.assertRaises(urllib.error.URLError):namespace['infer'](body)

    def test_compact_tool_phase(self):
        body={'input':'rules\nNATIVE TOOL TURN\n'+json.dumps({'messages':[],'initialWrites':True,'compact':True})}
        self.assertEqual(tools_for(body),COMPACT_TOOLS)
        self.assertFalse(initial_write_batch(body))
        body['input']='rules\nNATIVE TOOL TURN\n'+json.dumps({'messages':[],'initialWrites':False,'compact':False})
        self.assertNotIn('write_file',[t['function']['name'] for t in tools_for(body)])
        self.assertNotIn('submit_compact',[t['function']['name'] for t in tools_for(body)])

    def test_behavior_contract_is_closed_and_no_raw_maintained_source(self):
        maintained=next(t['function']['parameters'] for t in COMPACT_TOOLS if t['function']['name']=='submit_compact')
        self.assertFalse(maintained['additionalProperties'])
        self.assertNotIn('functions',maintained['properties'])
        self.assertNotIn('view',maintained['properties'])
        self.assertEqual(set(maintained['properties']),{'title','behaviors'})
        variants=maintained['properties']['behaviors']['items']['anyOf']
        self.assertEqual({v['properties']['kind']['enum'][0] for v in variants},{'comparison','providerSchedule','allocation','quotedAlternatives'})
        for v in variants:self.assertFalse({'state','nodes','actions','views'} & set(v['properties']))
        self.assertEqual(set(maintained['required']),set(maintained['properties']))
        def check(schema):
            if isinstance(schema,dict):
                if schema.get('type')=='object':
                    self.assertFalse(schema['additionalProperties'])
                    self.assertEqual(set(schema['required']),set(schema['properties']))
                for value in schema.values():check(value)
            elif isinstance(schema,list):
                for value in schema:check(value)
        for tool in COMPACT_TOOLS:check(tool['function']['parameters'])
        self.assertIn('submit_custom',[t['function']['name'] for t in COMPACT_TOOLS])

    def test_read_schema(self):
        schema=next(t['function']['parameters'] for t in SOURCE_TOOLS if t['function']['name']=='read_file')
        self.assertEqual(set(schema['required']),{'file','start_line','end_line'})
        self.assertFalse(schema['additionalProperties'])
        for bound in ('start_line','end_line'):
            self.assertEqual(schema['properties'][bound],{'type':['integer','null'],'minimum':1})

    def test_exchange(self):
        tree=ast.parse(pathlib.Path('tools/generation-gateway.py').read_text())
        infer=next(n for n in tree.body if isinstance(n,ast.FunctionDef) and n.name=='infer')
        call={'id':'c','type':'function','function':{'name':'write_file','arguments':json.dumps({'file':'deal','source':'literal " € \\n'})}}
        response={'choices':[{'message':{'content':None,'tool_calls':[call],'reasoning_content':'PRIVATE'},'finish_reason':'tool_calls'}],'usage':{'prompt_tokens':10,'completion_tokens':100,'completion_tokens_details':{'reasoning_tokens':70}}}
        seen=[]
        class Reply:
            def __enter__(self):return self
            def __exit__(self,*args):pass
            def read(self,limit=-1):return json.dumps(response).encode()
        def request(url,data,headers):seen.append(json.loads(data));return object()
        namespace={'options':types.SimpleNamespace(chooser='llm',backend='cerebras'),'GatewayFailure':ValueError,'BACKENDS':{'cerebras':{'model':'qwen-3.8-27b','endpoint':'unused','key':'private-key'}},'admit':lambda b:None,'time':time,'stable_messages':stable_messages,'prefix_fingerprint':prefix_fingerprint,'read_stream':read_stream,'json':json,'urllib':types.SimpleNamespace(request=types.SimpleNamespace(Request=request,urlopen=lambda *a,**k:Reply())), 'tools_for':tools_for,'source_messages':source_messages,'initial_write_batch':initial_write_batch,'native_result':native_result,'strip_reasoning':strip_reasoning}
        exec(compile(ast.Module(body=[infer],type_ignores=[]),'gateway','exec'),namespace)
        body={'input':'rules\nSOURCE OPERATION\ninitial\nNATIVE TOOL TURN\n{"messages":[],"initialWrites":true}','previous':'','diagnostics':'','capture':True}
        result=json.loads(namespace['infer'](body))
        self.assertEqual(json.loads(result['content'])['tool_calls'],[call])
        self.assertEqual(result['metadata']['reasoning'],70)
        self.assertEqual(seen[-1]['tools'],SOURCE_TOOLS);self.assertEqual(seen[-1]['tool_choice'],'required')
        self.assertTrue(seen[-1]['parallel_tool_calls']);self.assertNotIn('max_tokens',seen[-1]);self.assertNotIn('response_format',seen[-1])
        self.assertNotIn('PRIVATE',json.dumps(result));self.assertNotIn('private-key',json.dumps(result))
        body['capture']=False;result=json.loads(namespace['infer'](body));self.assertNotIn('request',result);self.assertNotIn('response',result)
        history=[{'role':'assistant','content':'','tool_calls':[call]},{'role':'tool','tool_call_id':'c','content':'{"accepted":true}'}]
        # A single staged file still has no complete `previous` pair, but batching is off.
        body.update(input='rules\nSOURCE OPERATION\ninitial\nNATIVE TOOL TURN\n'+json.dumps({'messages':history,'initialWrites':False}))
        namespace['infer'](body);self.assertFalse(seen[-1]['parallel_tool_calls'])
        body.update(input='rules\nSOURCE OPERATION\nrepair\nNATIVE TOOL TURN\n'+json.dumps({'messages':history,'initialWrites':False}),previous='host-bound-pair')
        namespace['infer'](body);self.assertEqual(seen[-1]['messages'][-2:],history);self.assertFalse(seen[-1]['parallel_tool_calls'])
        self.assertTrue(any('CURRENT CANONICAL SOURCE' in m.get('content','') and 'host-bound-pair' in m['content'] for m in seen[-1]['messages']))
        self.assertFalse(initial_write_batch({'input':'rules\nNATIVE TOOL TURN\n{"messages":[]}'}))
        namespace['options'].repair_reasoning='none'
        with self.assertRaisesRegex(ValueError,'enabled'):namespace['infer'](body)
        namespace['options'].repair_reasoning='low'
        response['choices'][0]={'message':{'content':'{}'},'finish_reason':'stop'}
        body.update(input='rules\n\nISSUED OPTIONS\n{}',previous='')
        namespace['infer'](body);self.assertEqual(seen[-1]['response_format'],{'type':'json_object'});self.assertNotIn('tools',seen[-1])
        namespace['DISCOVERY']={'model':'deepseek-flash','endpoint':'unused','key':'private-key'}
        body.update(input='facts',backend='deepseek-flash',discovery=True)
        response['choices'][0]['message']['content']='null'
        result=json.loads(namespace['infer'](body));self.assertEqual(result['content'],'null');self.assertNotIn('tools',seen[-1]);self.assertNotIn('response_format',seen[-1])

if __name__=='__main__':unittest.main()
