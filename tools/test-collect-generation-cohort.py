#!/usr/bin/env python3
"""Synthetic archive checks; no device, provider or hidden-reasoning data."""
import base64
import io
import json
import pathlib
import subprocess
import sys
import tarfile
import tempfile
import unittest

COLLECTOR = pathlib.Path(__file__).with_name('collect-generation-cohort.py')

class CollectorTest(unittest.TestCase):
    def test_latest_summary_missing_capture_and_unrendered_result(self):
        with tempfile.TemporaryDirectory() as directory:
            root = pathlib.Path(directory); cohort = root / 'cohort'; cohort.mkdir()
            result = [{'scenario':'fixture','trace':'trace','experience':'example','runMs':100,'generationToRenderMs':90,'semantic':'passed_supplied_assertions'}, {'scenario':'discovery-failure'}]
            def summary(tokens):
                return {'tokens':{'initial':{'in':tokens,'out':2,'reasoning':None,'cached':0},'repair':{'in':0,'out':0,'reasoning':0,'cached':0}},'initialModelMs':10,'repairModelMs':2,'checkingMs':3}
            events = [
                {'sequence':1,'trace':'trace','stage':'request-summary','outcome':'completed','target':json.dumps(summary(999))},
                {'sequence':2,'trace':'trace','stage':'source','outcome':'response','artifact':'evicted'},
                {'sequence':3,'trace':'trace','stage':'request-summary','outcome':'completed','target':json.dumps(summary(7))},
                {'sequence':4,'trace':'trace','stage':'composition','outcome':'assembled','target':json.dumps({'route':'custom-section','customCharacters':23,'operations':{'add':2},'reason':'fixture'}),'artifact':'evicted-route'},
                {'sequence':5,'trace':'trace','stage':'composition','outcome':'checked','target':json.dumps({'route':'custom-section','fallbacks':1,'repairs':0})},
                {'sequence':6,'trace':'trace','stage':'source','outcome':'response','artifact':'maintained'}]
            entries = {'files/framework-e2e-results.json':json.dumps(result),
                       'files/embedding-log/events.jsonl':'\n'.join(json.dumps(e) for e in events),
                       'files/embedding-log/maintained.json':json.dumps({'tool_calls':[{'function':{'name':'submit_compact','arguments':json.dumps({'title':'Fixture','behaviors':[{'kind':'comparison'}]})}}]}),
                       'shared_prefs/fixture.xml':'<map><string name="workspace.example.sourceEncoding">base64</string><string name="workspace.example.deal">'+base64.b64encode(b'canonical bytes').decode()+'</string></map>'}
            with tarfile.open(cohort/'final.tar','w') as archive:
                for name, content in entries.items():
                    raw = content.encode(); entry = tarfile.TarInfo(name); entry.size = len(raw); archive.addfile(entry,io.BytesIO(raw))
            subprocess.run([sys.executable,str(COLLECTOR),str(root)],check=True,stdout=subprocess.DEVNULL)
            numbers = json.loads((cohort/'numbers.json').read_text())
            self.assertEqual(numbers[0]['tokens']['in'],7)
            self.assertIsNone(numbers[0]['tokens']['reasoning'])
            self.assertEqual(numbers[0]['modelMs'],12)
            self.assertEqual(numbers[1]['status'],'not_rendered')
            self.assertNotIn('runMs',numbers[1])
            self.assertEqual((cohort/'fixture-accepted.deal').read_bytes(),b'canonical bytes')
            self.assertEqual(json.loads((root/'verification.json').read_text())['missingArtifacts'],['evicted','evicted-route'])
            self.assertEqual(numbers[0]['behaviorRoute']['customCharacters'],23)
            self.assertEqual(numbers[0]['behaviorRoute']['operations'],{'add':2})
            self.assertEqual(numbers[0]['behaviorRoute']['reason'],'fixture')
            observations=json.loads((cohort/'native-tool-observations.json').read_text())
            self.assertEqual(observations[0]['responses'][1]['calls'][0]['operations'],{'comparison':1})

if __name__ == '__main__': unittest.main()
