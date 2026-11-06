#!/usr/bin/env python3
"""Connect an emitted portable-composer program to the actual DEAL/Deal UI session.

Consumes an existing parity fixture receipt; no model calls or Android mutation.
This is a compiler/session integration check, not production-route acceptance.
"""
import argparse
import json
from pathlib import Path
import shutil
import subprocess
import time

ROOT = Path(__file__).resolve().parents[1]
EMBEDDING = ROOT / 'dependencies/deal-embedding'
DEAL = ROOT / 'dependencies/deal'
UI = ROOT / 'dependencies/deal-ui'


def run(*args):
    subprocess.run([str(a) for a in args], cwd=ROOT, check=True)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('parity', type=Path, help='Completed parity evidence directory')
    args = parser.parse_args()
    evidence = args.parity.resolve()
    work = ROOT / 'build/portable-integration' / str(time.time_ns())
    classes = ROOT / 'build/embedding-library/classes'
    (work / 'classes').mkdir(parents=True)
    fixture = json.loads((evidence / 'flow-cases.json').read_text())[0]
    (work / 'fixture.json').write_text(json.dumps(fixture))
    (work / 'reference.deal').write_text(fixture['expected']['source'])
    lower = work / 'lowering'
    (lower / 'src').mkdir(parents=True)
    for name in ['portable-source-lowering', 'plan-decoding', 'source-literals']:
        shutil.copyfile(EMBEDDING / f'core/{name}.deal', lower / f'src/{name}.deal')
    (lower / 'deal.json').write_text(json.dumps({'languageVersion': '1.5', 'backend': 'js', 'moduleRoots': ['src']}))
    run('javac', '--release', '21', '-cp', classes, '-d', work / 'classes', EMBEDDING / 'tools/PortableSyntax.java', ROOT / 'tools/PortableUiCompile.java')
    run('java', f'-Ddeal.home={DEAL}', '-cp', classes, 'deal.Main', 'compile', lower / 'src/portable-source-lowering.deal', '--backend', 'js', '--output', lower / 'js')
    run('node', ROOT / 'tools/test-portable-ui.js', work, evidence, classes, 'lower')
    # The Android bridge compiles this small unit independently on JVM.
    jvm = work / 'jvm'
    (jvm / 'src').mkdir(parents=True)
    for name in ['portable-backend', 'portable-source-lowering', 'plan-decoding', 'source-literals']:
        shutil.copyfile(EMBEDDING / f'core/{name}.deal', jvm / f'src/{name}.deal')
    shutil.copyfile(EMBEDDING / 'core/backend/syntax.d.deal', jvm / 'syntax.d.deal')
    (jvm / 'deal.json').write_text(json.dumps({'languageVersion': '1.5', 'backend': 'jvm', 'moduleRoots': ['src'], 'externals': {'portable/syntax': {'declaration': 'syntax.d.deal'}}}))
    run('java', f'-Ddeal.home={DEAL}', '-cp', classes, 'deal.Main', 'compile', jvm / 'src/portable-backend.deal', '--backend', 'jvm', '--output', jvm / 'java')
    generated = jvm / 'java/Portable_backend.java'
    generated.write_text('package dev.deal.embedding.portable; ' + generated.read_text())
    run('javac', '--release', '21', '-proc:none', '-cp', classes, '-d', work / 'classes', generated, EMBEDDING / 'tools/PortableBackendBridge.java')
    pair = json.loads(subprocess.check_output(['java', '-cp', f'{work}/classes:{classes}', 'PortableBackendBridge', str(work / 'reference.deal')], text=True))
    candidate = work / 'candidate'
    assert pair['deal'] == (candidate / 'src/experience.deal').read_text()
    assert pair['dealui'] == (candidate / 'src/experience.dealui').read_text()
    for name in ['portable-presentation', 'plan-decoding']:
        shutil.copyfile(EMBEDDING / f'core/{name}.deal', candidate / f'src/{name}.deal')
    shutil.copyfile(EMBEDDING / 'core/backend/numbers.d.deal', candidate / 'portable-numbers.d.deal')
    (candidate / 'deal.json').write_text(json.dumps({'languageVersion': '1.5', 'backend': 'js', 'moduleRoots': ['src'], 'externals': {'portable/numbers': {'declaration': 'portable-numbers.d.deal'}}}))
    run('java', '-cp', f'{work}/classes:{classes}', 'PortableUiCompile', candidate, UI, EMBEDDING / 'ui/platform.dealui-pack')
    run('java', f'-Ddeal.home={DEAL}', '-cp', classes, 'deal.Main', 'compile', candidate / 'src/experience_entry.deal', '--backend', 'js', '--output', candidate / 'js')
    (candidate / 'js/portable').mkdir(parents=True, exist_ok=True)
    shutil.copyfile(EMBEDDING / 'core/portable-numbers.js', candidate / 'js/portable/numbers.js')
    run('node', ROOT / 'tools/test-portable-ui.js', work, evidence, classes, 'session')
    print(f'Integration evidence: {work.relative_to(ROOT)}')


if __name__ == '__main__':
    main()
