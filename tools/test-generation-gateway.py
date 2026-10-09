#!/usr/bin/env python3
"""No credentials loaded, no network: exact provider exchange envelope contract."""
import ast, json, pathlib, types, unittest

class GatewayTest(unittest.TestCase):
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
        namespace={'json':json,'MODEL':'fixture','ENDPOINT':'unused','KEY':'private-key','urllib':types.SimpleNamespace(request=types.SimpleNamespace(Request=request,urlopen=lambda *a,**k:Reply()))}
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
        body['capture']=True
        response['extra']='x'*(2*1024*1024)
        bounded=json.loads(namespace['infer'](body))
        self.assertTrue(bounded['captureOmitted']);self.assertNotIn('response',bounded)
        self.assertEqual(bounded['content'],response['choices'][0]['message']['content'])
        response['choices'][0]['message']['content']='x'*(256*1024+1)
        with self.assertRaises(ValueError): namespace['infer'](body)

if __name__=='__main__':unittest.main()
