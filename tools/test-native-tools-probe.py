#!/usr/bin/env python3
"""Offline tests for the isolated protocol experiment; no network or device."""
import importlib.util,json,pathlib,tempfile,unittest
spec=importlib.util.spec_from_file_location('probe',pathlib.Path(__file__).with_name('probe-native-tools.py'));p=importlib.util.module_from_spec(spec);spec.loader.exec_module(p)
def message(edits):return {'content':None,'tool_calls':[{'id':'fixture','type':'function','function':{'name':'edit_sources','arguments':json.dumps({'edits':edits})}}]}
class Probe(unittest.TestCase):
 def test_shape_and_escaping(self):
  edits=[{'file':'deal','search':'quote " \\n 🦔\r\n','replacement':''}];self.assertEqual(p.validate_tool(message(edits)),edits)
  for invalid in [{'content':'text'},message([{'file':'../other','search':'a','replacement':'b'}]),message([{'file':'deal','search':1,'replacement':''}]),message([{'file':'deal','search':'a','replacement':'b','extra':0}])]:
   with self.assertRaises((ValueError,KeyError,TypeError)):p.validate_tool(invalid)
  two=message(edits);two['tool_calls']*=2
  with self.assertRaises(ValueError):p.validate_tool(two)
 def test_authoritative_application(self):
  base={'deal':'aaa\nquote " \\n 🦔\r\n','dealui':'UI'}
  valid=p.adapter({'mode':'edits','base':base,'edits':[{'file':'deal','search':'quote " \\n 🦔\r\n','replacement':'€\n'}]})
  self.assertTrue(valid['accepted']);self.assertEqual(json.loads(valid['source']),{'deal':'aaa\n€\n','dealui':'UI'})
  for edits,code in [([], 'PATCH_FRAME'),([{'file':'deal','search':'aa','replacement':'b'}],'PATCH_MULTIPLE_MATCHES'),([{'file':'dealui','search':'aaa','replacement':'b'}],'PATCH_NO_MATCH'),([{'file':'deal','search':'aaa','replacement':'b'},{'file':'deal','search':'absent','replacement':''}],'PATCH_NO_MATCH')]:
   r=p.adapter({'mode':'edits','base':base,'edits':edits});self.assertFalse(r['accepted']);self.assertTrue(r['diagnostics'].startswith(code));self.assertEqual(r['source'],'')
 def test_marker_collision_is_host_owned(self):
  source='\n<<<host:replacement>>>\n';base={'deal':source,'dealui':'UI'}
  r=p.adapter({'mode':'edits','base':base,'edits':[{'file':'deal','search':source,'replacement':'literal'}]});self.assertTrue(r['accepted']);self.assertEqual(json.loads(r['source'])['deal'],'literal')
 def test_native_not_text_fallback(self):
  with self.assertRaises(ValueError):p.validate_tool({'content':'DEAL-PATCH/1 ...','tool_calls':[]})
if __name__=='__main__':unittest.main()
