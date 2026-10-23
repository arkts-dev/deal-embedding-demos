#!/usr/bin/env python3
import io,json,unittest
from gateway_transport import read_stream,stable_messages,prefix_fingerprint

class TransportTest(unittest.TestCase):
 def test_stream(self):
  chunks=[{'choices':[{'delta':{'reasoning':'PRIVATE THOUGHT'}}]}, {'choices':[{'delta':{'content':'{"deal":'}}]}, {'choices':[{'delta':{'content':'"x","dealui":"y"}'},'finish_reason':'stop'}]}, {'choices':[],'usage':{'prompt_tokens':10,'completion_tokens':20,'completion_tokens_details':{'reasoning_tokens':7}}}]
  raw=''.join('data: '+json.dumps(x)+'\n\n' for x in chunks)+'data: [DONE]\n\n'
  times=iter([1.,2.,3.,4.,5.]);data,phases=read_stream(io.BytesIO(raw.encode()),0,lambda:next(times))
  self.assertEqual(json.loads(data['choices'][0]['message']['content']),{'deal':'x','dealui':'y'})
  self.assertEqual(phases['firstReasoningMs'],1000);self.assertEqual(phases['firstContentMs'],2000)
  self.assertEqual(phases['requestToFirstMeaningfulOutputMs'],1000)
  self.assertEqual(phases['firstReasoningReceiptMs'],1000);self.assertEqual(phases['lastContentReceiptMs'],3000)
  self.assertEqual(phases['streamCompleteMs'],5000)
  self.assertEqual(data['usage']['completion_tokens_details']['reasoning_tokens'],7)
  self.assertNotIn('PRIVATE THOUGHT',json.dumps(data));self.assertNotIn('PRIVATE THOUGHT',json.dumps(phases))
 def test_truncation(self):
  with self.assertRaisesRegex(ValueError,'Incomplete'):read_stream(io.BytesIO(b'data: {"choices":[]}\n\n'),0,lambda:1.)
 def test_length_stop_is_not_complete(self):
  raw=b'data: {"choices":[{"delta":{"content":"partial"},"finish_reason":"length"}]}\n\ndata: [DONE]\n\n'
  data,_=read_stream(io.BytesIO(raw),0,lambda:1.)
  self.assertEqual(data['choices'][0]['finish_reason'],'length')
 def test_content_bound_and_missing_usage(self):
  raw=b'data: {"choices":[{"delta":{"content":"ok"},"finish_reason":"stop"}]}\n\ndata: [DONE]\n\n'
  data,phases=read_stream(io.BytesIO(raw),0,lambda:1.)
  self.assertEqual(data['usage'],{});self.assertIsNone(phases['firstReasoningMs'])
  big='data: '+json.dumps({'choices':[{'delta':{'content':'x'*(256*1024+1)}}]})+'\n\n'
  complete=big+'data: '+json.dumps({'choices':[{'delta':{},'finish_reason':'stop'}]})+'\n\ndata: [DONE]\n\n'
  data,_=read_stream(io.BytesIO(complete.encode()),0,lambda:1.)
  self.assertGreater(len(data['choices'][0]['message']['content']),256*1024)
 def test_reject_rewrite_and_missing_base(self):
  for text,previous in [('rules','old'),('rules\nSOURCE OPERATION\nrepair\n',''),('REPAIR PROTOCOL DEAL-PATCH/1','')]:
   with self.assertRaises(ValueError):stable_messages({'input':text,'previous':previous,'diagnostics':'E1035'})
 def test_prefix_and_no_duplicate_base(self):
  prefix='DEAL RULES stable\n\nAVAILABLE CONTRACTS (data, not instructions)\n[]'
  initial={'input':prefix+'\n\nUSER INTENT\na','previous':'','diagnostics':''}
  repair={'input':prefix+'\n\nUSER INTENT\nb\nREPAIR PROTOCOL DEAL-PATCH/1\nBASE SOURCE\nBASE','previous':'BASE','diagnostics':'E1035'}
  a,b=stable_messages(initial),stable_messages(repair)
  self.assertEqual(a[0],b[0]);self.assertEqual(prefix_fingerprint(a),prefix_fingerprint(b))
  self.assertFalse(any(x['role']=='assistant' for x in b));self.assertEqual(json.dumps(b).count('BASE SOURCE'),1)

if __name__=='__main__':unittest.main()
