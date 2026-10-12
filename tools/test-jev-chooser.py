#!/usr/bin/env python3
"""Deterministic Jev protocol/normalization tests: no credentials or network."""
import copy
import json
import unittest
from jev_chooser import infer, normalize, payload_for

class JevChooserTest(unittest.TestCase):
    def setUp(self):
        self.questions = {'purpose': {'type':'choice','instructions':'Choose a workflow',
                                     'criteria':{'compare':'Compare options','unavailable':'Unsupported request'}}}
        self.issued = {'questions':{'purpose':['compare','unavailable']},'decisionQuestions':self.questions}
        self.body = {'input':'GUIDANCE\n\nUSER INTENT\nCompare equipment\nDISCLOSED CONTEXT\n{}\n\nISSUED OPTIONS\n'+json.dumps(self.issued),
                     'previous':'','diagnostics':'','capture':True}
        self.response = {'answers':{'purpose':{'type':'choice','choice':'compare','probabilities':{'compare':0.8,'unavailable':0.2},'confidence':0.6}},
                         'usage':{'input_tokens':12,'output_tokens':2}}
    def test_exchange_and_capture(self):
        seen=[]
        class Reply:
            def __enter__(inner):return inner
            def __exit__(inner,*args):pass
            def read(inner,limit):return json.dumps(self.response).encode()
        def open_url(request,timeout):
            seen.append(request);self.assertEqual(timeout,60);return Reply()
        result=json.loads(infer(self.body,'fixture-secret',open_url=open_url))
        payload=json.loads(seen[0].data)
        self.assertEqual(seen[0].full_url,'https://api.typesafe.ai/v1/systemone')
        self.assertEqual(seen[0].get_header('Authorization'),'Bearer fixture-secret')
        self.assertEqual(payload['questions'],self.questions)
        self.assertNotIn('GUIDANCE',payload['state'])
        self.assertEqual(result['request'],payload);self.assertEqual(result['response'],self.response)
        self.assertEqual(json.loads(result['content'])['answers'],{'purpose':'compare'})
        self.assertEqual(json.loads(result['content'])['typedAnswers'][0]['confidence'],0.6)
        self.assertNotIn('fixture-secret',json.dumps(result))
        self.body['capture']=False
        result=json.loads(infer(self.body,'fixture-secret',open_url=open_url))
        self.assertNotIn('request',result);self.assertNotIn('response',result)
        self.assertNotIn('Compare equipment',json.dumps(result))
        self.assertEqual(result['metadata']['in'],12)
    def test_unsupported_and_invalid_choice_preserved(self):
        for choice in ('unavailable','unissued'):
            self.response['answers']['purpose']['choice']=choice
            self.assertEqual(json.loads(normalize(self.response,self.questions))['answers']['purpose'],choice)
    def test_malformed_provider_answers_stop(self):
        cases=[]
        for key,value in [('type','score'),('choice',False),('probabilities',{'compare':True,'unavailable':0}),
                          ('probabilities',{'compare':1}),('probabilities',{'compare':0,'unavailable':0}),
                          ('probabilities',{'compare':float('nan'),'unavailable':0}),('confidence',1.1)]:
            data=copy.deepcopy(self.response);data['answers']['purpose'][key]=value;cases.append(data)
        cases.extend([{'answers':{}},{'answers':{'extra':{}}}])
        for data in cases:
            with self.assertRaises(ValueError):normalize(data,self.questions)
    def test_rounded_probabilities_and_nullable_confidence(self):
        questions={'q':{'criteria':{'a':'A','b':'B','c':'C'}}}
        data={'answers':{'q':{'type':'choice','choice':'a','probabilities':{'a':0.33,'b':0.33,'c':0.33},'confidence':None}}}
        self.assertEqual(json.loads(normalize(data,questions))['answers']['q'],'a')
    def test_invalid_offer_stops_before_network(self):
        for change in ('missing','mismatch','empty-description'):
            issued=copy.deepcopy(self.issued)
            if change=='missing':del issued['decisionQuestions']
            elif change=='mismatch':issued['questions']['purpose']=['other']
            else:issued['decisionQuestions']['purpose']['criteria']['compare']=''
            with self.assertRaises((ValueError,KeyError)):
                payload_for('state\n\nISSUED OPTIONS\n'+json.dumps(issued),'jev-latest')
    def test_capture_limit_preserves_valid_decision(self):
        self.response['extra']='x'*(2*1024*1024-500)
        class Reply:
            def __enter__(inner):return inner
            def __exit__(inner,*args):pass
            def read(inner,limit):return json.dumps(self.response).encode()
        result=json.loads(infer(self.body,'fixture-secret',open_url=lambda *a,**k:Reply()))
        self.assertTrue(result['captureOmitted']);self.assertNotIn('response',result)
        self.assertEqual(json.loads(result['content'])['answers']['purpose'],'compare')

if __name__=='__main__':unittest.main()
