#!/usr/bin/env python3
"""Offline continuation admission: operational diagnostics never spend source repairs."""
import ast
import json
import pathlib
import tempfile
import unittest

source = pathlib.Path(__file__).with_name('continue-repair-benchmark.py')
function = next(n for n in ast.parse(source.read_text()).body if isinstance(n, ast.FunctionDef))
namespace = {'json': json, 'pathlib': pathlib}
exec(compile(ast.Module(body=[function], type_ignores=[]), str(source), 'exec'), namespace)
feedback = namespace['rejected_source_feedback']


class RepairAdmissionTest(unittest.TestCase):
    def test_source_only(self):
        with tempfile.TemporaryDirectory() as directory:
            project = pathlib.Path(directory)
            def report(file, code='E1035', line=1):
                (project / 'diagnostics.json').write_text(json.dumps({'version': 1, 'diagnostics': [
                    {'severity': 'error', 'code': code, 'message': 'Rejected',
                     'range': {'file': str(project / 'src' / file), 'startLine': line, 'startColumn': 2}}
                ]}))
            base = {'deal': 'broken source', 'dealui': 'view source'}
            report('experience.deal')
            self.assertIn('Exact source window:\nbroken source', feedback(project, 2, base))
            for file, code, status, line in [('experience_entry.deal', 'E2001', 1, 1),
                    ('experience.deal', 'E2010', 1, 1), ('ui/values.deal', 'E2001', 1, 1),
                    ('experience.deal', 'E1035', 3, 1), ('experience.deal', 'E1035', 1, 99)]:
                report(file, code, line)
                with self.assertRaises(ValueError):
                    feedback(project, status, base)


if __name__ == '__main__':
    unittest.main()
