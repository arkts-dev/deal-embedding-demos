#!/usr/bin/env python3
"""Pinned local source -> real UI/DEAL compiler -> JS VM/session. No live inference.
Artifacts remain in ignored build/toolkit/host; native visuals are a separate check.
"""
import hashlib
import json
import pathlib
import subprocess
import sys
import tempfile
import zipfile

ROOT = pathlib.Path(__file__).resolve().parents[1]
LIB = ROOT / 'dependencies/deal-embedding'
OUT = ROOT / 'build/toolkit/host'
CLASSES = ROOT / 'build/embedding-library/classes'
OUT.mkdir(parents=True, exist_ok=True)
# Isolate each run: stale compiler output or a former corpus member must not pass.
# Keep older runs (including rejected source evidence) intact.
OUT = pathlib.Path(tempfile.mkdtemp(prefix='run-', dir=OUT))


def run(args, **kw):
    return subprocess.run(list(map(str, args)), cwd=ROOT, text=True, capture_output=True, **kw)


result = run(['javac', '--release', '21', '-cp', CLASSES, '-d', OUT, LIB / 'tools/ToolkitHarness.java'])
assert result.returncode == 0, result.stderr
pack = LIB / 'ui/platform.dealui-pack'
with zipfile.ZipFile(ROOT / 'build/embedding-library/deal-embedding.aar') as aar:
    assert aar.read('assets/embedding/platform.dealui-pack') == pack.read_bytes()
    for source in list((LIB / 'ui').glob('*.deal')) + list((LIB / 'ui').glob('*.dealui')):
        assert aar.read('assets/ui/' + source.name) == source.read_bytes()


def compile_pair(name, deal, ui, expected=None, pack_source=None):
    inputs = OUT / name / 'input'
    inputs.mkdir(parents=True, exist_ok=True)
    (inputs / 'experience.deal').write_text(deal)
    (inputs / 'experience.dealui').write_text(ui)
    result = run(['java', '-Ddeal.home=' + str(ROOT / 'dependencies/deal'), '-cp', str(CLASSES) + ':' + str(OUT), 'ToolkitHarness', pack_source or pack, inputs, OUT / name, ROOT / 'build/embedding-consumer/assets/distribution'])
    (OUT / name / 'compile.log').write_text(result.stdout + result.stderr)
    if expected:
        assert result.returncode != 0 and expected in result.stderr, (name, result.stdout, result.stderr)
    else:
        assert result.returncode == 0, (name, result.stdout, result.stderr)


for name in ('comparison', 'values', 'generation-example'):
    compile_pair(name, (LIB / 'ui' / (name + '.deal')).read_text(), (LIB / 'ui' / (name + '.dealui')).read_text())
# Compatibility against the actual pre-enrichment authored example, not a recreated approximation.
legacy = '0e54a5760f25bd29a9d5602eb8f75befe53a1a68'
old_pack = OUT / 'legacy.dealui-pack'
old_pack.write_bytes(subprocess.check_output(['git', '-C', str(LIB), 'show', legacy + ':android/assets/embedding/platform.dealui-pack']))
compatibility = run(['java', '-cp', str(CLASSES) + ':' + str(OUT), 'ToolkitHarness', 'compatibility', old_pack, pack])
assert compatibility.returncode == 0, compatibility.stderr
print(compatibility.stdout.strip())
deal = subprocess.check_output(['git', '-C', str(LIB), 'show', legacy + ':core/generation-example.deal'], text=True)
ui = subprocess.check_output(['git', '-C', str(LIB), 'show', legacy + ':core/generation-example.dealui'], text=True)
compile_pair('legacy', deal, ui)
values = (LIB / 'ui/values.dealui').read_text()
source = (LIB / 'ui/values.deal').read_text()
for name, replacement in [('loop', 'ui.Text(value: amount.label)'), ('branch', 'When(true) { ui.Text(value: amount.label) }')]:
    compile_pair('reject-' + name, source, values.replace('ui.Money(label: amount.label, minorUnits: amount.minorUnits)', replacement), 'UI2048')
compile_pair('reject-label', source, values.replace('ui.Money(label: "Refundable deposit", minorUnits: 5000)', 'ui.Money(minorUnits: 5000)'), 'UI2028')
compile_pair('reject-status', source, values.replace('ui.Comparison(status: state.status)', 'ui.Comparison()'), 'UI2028')
compile_pair('reject-property', source, values.replace('ui.Metric(label: "Travel", value: "29", unit: "min")', 'ui.Metric(label: "Travel", value: "29", format: "money")'), 'UI2029')
# Optional private saved-source corpus; files stay in ignored output, never public fixtures.
if len(sys.argv) > 1:
    import re
    for folder in sorted(pathlib.Path(sys.argv[1]).iterdir()):
        if not (folder / 'state.json').exists():
            continue
        d, u = (folder / 'experience.deal').read_text(), (folder / 'experience.dealui').read_text()
        if any(not module.startswith('std/') for module in re.findall(r'from "([^"]+)"', d)):
            continue
        name = 'saved-' + hashlib.sha256(folder.name.encode()).hexdigest()[:12]
        try:
            compile_pair(name + '-baseline', d, u, pack_source=old_pack)
        except AssertionError:
            # Preserve baseline-invalid saved attempts; they are not compatibility passes.
            print('BASELINE REJECTED', name, '(see preserved compile.log)')
            continue
        compile_pair(name, d, u)
        (OUT / name / 'state.json').write_bytes((folder / 'state.json').read_bytes())
result = run(['node', ROOT / 'tools/verify-toolkit-session.js', OUT, ROOT / 'build/embedding-consumer/assets/embedding/bindings'])
(OUT / 'session.log').write_text(result.stdout + result.stderr)
assert result.returncode == 0, result.stdout + result.stderr
receipt = {'revisions': {r: subprocess.check_output(['git', '-C', str(ROOT / r), 'rev-parse', 'HEAD'], text=True).strip() for r in ('.', 'dependencies/deal-embedding', 'dependencies/deal-ui', 'dependencies/deal')},
           'inputsSha256': {str(p.relative_to(ROOT)): hashlib.sha256(p.read_bytes()).hexdigest() for p in sorted((LIB / 'ui').glob('*')) if p.is_file()},
           'savedSourcesCompared': sum((p / 'state.json').exists() for p in OUT.glob('saved-*')),
           'scope': 'Real compiler and JS session, mocked async provider. No device outcome, fresh inference or visual approval.'}
(OUT / 'receipt.json').write_text(json.dumps(receipt, indent=2) + '\n')
print(result.stdout.strip())
print('PASS packaging identity, legacy source, exact examples, dynamic composition rejection and required props. Native visuals pending separately.')
print('Receipts:', OUT.relative_to(ROOT))
