#!/usr/bin/env python3
"""No credentials loaded, no network: exact provider exchange envelope contract."""
import ast, json, pathlib, types, unittest, tempfile, http.server, urllib.request, urllib.error, concurrent.futures, threading

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

    def test_exchange(self):
        tree=ast.parse(pathlib.Path('tools/generation-gateway.py').read_text())
        infer=next(n for n in tree.body if isinstance(n,ast.FunctionDef) and n.name=='infer')
        headers_seen=[]
        response={'choices':[{'message':{'content':'{"deal":"x","dealui":"y"}'},'finish_reason':'stop'}],'usage':{'prompt_tokens':10}}
        seen=[]
        class Reply:
            def __enter__(self):return self
            def __exit__(self,*args):pass
            def read(self,limit):return json.dumps(response).encode()
        def request(url,data,headers):
            seen.append(json.loads(data));headers_seen.append(headers);return object()
        namespace={'options':types.SimpleNamespace(chooser='jev',backend='cortex'),'GatewayFailure':ValueError,'BACKENDS':{'cortex':{'model':'fixture','endpoint':'unused','key':'private-key'},'cerebras':{'model':'qwen-3.8-27b','endpoint':'cerebras','key':'private-key'}},'admit': lambda body: None, 'json':json,'urllib':types.SimpleNamespace(request=types.SimpleNamespace(Request=request,urlopen=lambda *a,**k:Reply()))}
        exec(compile(ast.Module(body=[infer],type_ignores=[]),'gateway','exec'),namespace)
        body={'input':'prompt','previous':'prior','diagnostics':'E3003','capture':True}
        result=json.loads(namespace['infer'](body))
        self.assertEqual(result['request'],seen[0]);self.assertEqual(result['response'],response)
        self.assertEqual(headers_seen[0]['Authorization'],'Bearer private-key')
        self.assertNotIn('private-key',json.dumps(result));self.assertEqual(result['content'],response['choices'][0]['message']['content'])
        body['capture']=False
        omitted=json.loads(namespace['infer'](body))
        self.assertNotIn('request',omitted);self.assertNotIn('response',omitted)
        self.assertNotIn('prompt',json.dumps(omitted));self.assertEqual(omitted['metadata']['in'],10)
        self.assertEqual(omitted['metadata']['finish'],'stop')
        body['backend']='cerebras'
        cerebras=json.loads(namespace['infer'](body))
        self.assertEqual(cerebras['model'],'qwen-3.8-27b')
        self.assertEqual(seen[-1]['max_tokens'],32768)
        self.assertEqual(seen[-1]['reasoning_effort'],'low')
        self.assertEqual(seen[-1]['response_format'],{'type':'json_object'})
        self.assertEqual(headers_seen[-1]['User-Agent'],'deal-embed-development/1.0')
        self.assertEqual(seen[-1]['messages'],seen[0]['messages'])
        namespace['DISCOVERY']={'model':'deepseek-flash','endpoint':'deepseek','key':'private-deepseek-key'}
        body.update(backend='deepseek-flash',discovery=True,previous='',diagnostics='',capture=True)
        response['choices'][0]['message']['content']='null'
        discovery=json.loads(namespace['infer'](body))
        self.assertEqual(discovery['content'],'null')
        self.assertEqual(discovery['model'],'deepseek-flash')
        self.assertEqual(seen[-1]['thinking'],{'type':'disabled'})
        self.assertEqual(seen[-1]['max_tokens'],4096)
        self.assertNotIn('response_format',seen[-1])
        self.assertNotIn('private-deepseek-key',json.dumps(discovery))
        body.pop('discovery');response['choices'][0]['message']['content']='{"deal":"x","dealui":"y"}'
        body['backend']='unknown'
        with self.assertRaises(ValueError):namespace['infer'](body)
        body['backend']='cortex'
        body['capture']=True
        response['extra']='x'*(2*1024*1024)
        bounded=json.loads(namespace['infer'](body))
        self.assertTrue(bounded['captureOmitted']);self.assertNotIn('response',bounded)
        self.assertEqual(bounded['content'],response['choices'][0]['message']['content'])
        response['choices'][0]['message']['content']='x'*(256*1024+1)
        with self.assertRaises(ValueError): namespace['infer'](body)

if __name__=='__main__':unittest.main()
