#!/usr/bin/env python3
import importlib.util
import json
import pathlib
import unittest
spec=importlib.util.spec_from_file_location('metrics',pathlib.Path(__file__).with_name('generation-metrics.py'))
m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)

class MetricsTest(unittest.TestCase):
 def test_unknown_and_latest(self):
  s=dict(requestId='a',summaryRevision=1,initialCalls=1,repairCalls=0,tokens={'initial':dict(in_=None),'repair':{}},settings=[],failureCode='')
  s['tokens']['initial']={'in':100,'cached':None,'out':None,'reasoning':None}
  events=[dict(stage='request-summary',target=json.dumps(s))]
  s.update(summaryRevision=2,outcome='snapshot_published',timeToCheckedSourceMs=100)
  events.append(dict(stage='request-summary',target=json.dumps(s)))
  r=m.aggregate(events)
  self.assertEqual(r['requestCount'],1);self.assertEqual(r['requests'][0]['evaluation']['outcome'],'not_tested')
  self.assertEqual(r['tokens']['initial']['out']['unknownRequests'],1)
  self.assertIsNone(r['cacheHitFraction']['fraction'])
  self.assertEqual(r['distributions']['timeToTestedUsefulMs']['known'],0)
 def test_explicit_evaluation(self):
  s=dict(requestId='a',summaryRevision=1,initialCalls=0,repairCalls=0,tokens={'initial':{},'repair':{}},settings=[],failureCode='')
  r=m.aggregate([dict(stage='request-summary',target=json.dumps(s))],[dict(requestId='a',outcome='passed',timeToTestedUsefulMs=200,evidence='test.json')])
  self.assertEqual(r['distributions']['timeToTestedUsefulMs']['median'],200)
 def test_distribution(self):
  d=m.distribution([None,1,2,3,4]);self.assertEqual(d['unknown'],1);self.assertEqual(d['median'],2.5);self.assertEqual(d['p90'],4)

if __name__=='__main__':unittest.main()
