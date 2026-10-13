#!/usr/bin/env python3
"""Compare authoritative document declarations with shipped API and core mirrors."""
import pathlib,re,sys
reference=pathlib.Path(sys.argv[1]).read_text()
blocks=re.findall(r'```deal\s*\n(.*?)```',reference,re.S)
assert len(blocks)==2,'Expected two authoritative declaration blocks'
def tokens(s):
    s=re.sub(r'//[^\n]*','',s)
    return re.findall(r'"(?:\\.|[^"\\])*"|[A-Za-z_][A-Za-z0-9_]*|-?\d+(?:\.\d+)?|[^\s]',s)
root=pathlib.Path('dependencies/deal-embedding')
for name,block in zip(('embedding.d.deal','catalogue-policy.d.deal'),blocks):
    expected=tokens(block)
    for directory in ('api','core/host'):
        actual=tokens((root/directory/name).read_text())
        assert actual==expected,f'{directory}/{name} differs from authoritative document'
print('Exact declarations/defaults/async markers match authoritative document in API and core mirrors. Behavioral conformance is separate.')
