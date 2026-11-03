#!/usr/bin/env python3
"""Compile the DEAL needs-plan port; compare it with the pinned local ArkTS source.

No model calls, device mutation or substitute policy evaluator. All receipts go
under ignored build/. The production generation route is not switched here.
"""
import argparse
import hashlib
import json
import pathlib
import shutil
import subprocess
import time

ROOT = pathlib.Path(__file__).resolve().parents[1]
PIN = '7ee06af1941729b00a110493ad9d1d9b5b127d17'


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('reference', type=pathlib.Path, help='local deal-studio-ohos checkout')
    parser.add_argument('--typescript', type=pathlib.Path, required=True,
                        help='installed typescript module (used by the reference source loader)')
    args = parser.parse_args()
    reference = args.reference.resolve()
    revision = subprocess.check_output(['git', '-C', str(reference), 'rev-parse', 'HEAD'], text=True).strip()
    if revision != PIN:
        raise RuntimeError(f'reference revision {revision} differs from {PIN}')
    if subprocess.check_output(['git', '-C', str(reference), 'diff', PIN, '--',
                                'entry/src/main/ets/vera', 'tools/vera-harness/source-loader.js']):
        raise RuntimeError('reference policy or loader differs from the pinned revision')
    reference_files = ['entry/src/main/ets/vera/NeedsPlan.ets',
                       'entry/src/main/ets/vera/VeraCompose.ets',
                       'entry/src/main/ets/vera/NeedsChoose.ets',
                       'tools/vera-harness/source-loader.js']
    # Refuse modified reference sources, rather than trusting HEAD alone.
    for name in reference_files:
        committed = subprocess.check_output(['git', '-C', str(reference), 'show', f'{PIN}:{name}'])
        if committed != (reference / name).read_bytes():
            raise RuntimeError(f'modified reference source: {name}')
    out = ROOT / 'build/needs-plan-parity' / str(time.time_ns())
    source = out / 'src'
    source.mkdir(parents=True)
    lib = ROOT / 'dependencies/deal-embedding'
    for name in ['needs-plan', 'plan-decoding', 'sketch', 'needs-filtering', 'needs-drawing', 'needs-start']:
        shutil.copy(lib / f'core/{name}.deal', source)
    shutil.copy(ROOT / 'tools/fixtures/sketch-parity.deal', source / 'sketch-parity.deal')
    shutil.copy(ROOT / 'tools/fixtures/needs-orchestration-parity.deal', source / 'needs-orchestration-parity.deal')
    (out / 'deal.json').write_text(json.dumps({
        'languageVersion': '1.5', 'backend': 'js', 'moduleRoots': ['src']}))
    # Projection is explicit: nominal classes are not generic JSON Records.
    (source / 'parity.deal').write_text('''import * as needs from "./needs-plan";
import * as json from "std/json";
import * as sketchParity from "./sketch-parity";
import * as orchestration from "./needs-orchestration-parity";

export function inspect(reply: string): string {
    let parsed: needs.NeedsPlan | null = needs.parseNeedsPlan(reply);
    if (parsed === null) { return "null"; }
    let plan: needs.NeedsPlan = parsed as needs.NeedsPlan;
    let items: Record<string, unknown>[] = [];
    for (let need of plan.needs) {
        items[items.length] = {
            type: need.type, words: need.words, raw: need.raw,
            on: need.on, reads: need.reads, length: need.length, from: need.from
        };
    }
    let before: string = json.stringify({base: plan.base, needs: items}) as string;
    let document: Record<string, unknown> = needs.sketchDocument(plan);
    let after: string = json.stringify({base: plan.base, needs: items}) as string;
    if (before !== after) {
        throw {code: "PARITY_MUTATION", message: "sketch conversion changed the original plan"};
    }
    let first: needs.Need | null = needs.needOf(plan, "countdown");
    let firstWords: string | null = null;
    if (first !== null) { firstWords = first.words; }
    let changed: number = needs.plainScenes(plan);
    let plain: Record<string, unknown> = needs.sketchDocument(plan);
    return json.stringify({base: plan.base, needs: items, document: document,
        firstCountdown: firstWords, plainScenes: changed, plainDocument: plain}) as string;
}
export function inspectSketch(reply: string): string { return sketchParity.inspect(reply); }
export function sketchFromNeeds(reply: string): string { return sketchParity.fromNeeds(reply); }
export function inspectFilter(input: string): string { return orchestration.inspectFilter(input); }
export function inspectPartial(input: string): string { return orchestration.inspectPartial(input); }
export function inspectStart(input: string): string { return orchestration.inspectStart(input); }
export async function inspectDrawing(input: string): Promise<string> { return await orchestration.inspectDrawing(input); }
export function main(): null { return null; }
''')
    compiler = ['java', '-Ddeal.home=' + str(ROOT / 'dependencies/deal'),
                '-cp', str(ROOT / 'build/embedding-library/classes'), 'deal.Main', 'compile', source / 'parity.deal']

    def run(command, name, **kwargs):
        result = subprocess.run([str(x) for x in command], cwd=ROOT, text=True,
                                stdout=subprocess.PIPE, stderr=subprocess.STDOUT, **kwargs)
        (out / (name + '.log')).write_text(result.stdout)
        if result.returncode:
            raise RuntimeError(name + ': ' + result.stdout[-6000:])
        return result.stdout

    run(compiler + ['--backend', 'js', '--output', out / 'js'], 'compile-js')
    run(compiler + ['--backend', 'jvm', '--output', out / 'jvm'], 'compile-jvm')
    import os
    env = dict(os.environ, VERA_TYPESCRIPT_PATH=str(args.typescript.resolve()))
    print(run(['node', ROOT / 'tools/test-needs-plan-parity.js', reference, out], 'parity-js', env=env).strip())
    print(run(['node', ROOT / 'tools/test-sketch-parity.js', reference, out], 'sketch-parity-js', env=env).strip())
    print(run(['node', ROOT / 'tools/test-needs-orchestration-parity.js', reference, out], 'orchestration-js', env=env).strip())
    (out / 'NeedsParityHost.java').write_text('''import deal.codegen.jvm.JvmRuntime;
import java.util.Base64;
import java.nio.charset.StandardCharsets;
import java.io.BufferedReader;
import java.io.InputStreamReader;
public class NeedsParityHost {
    public static void main(String[] args) throws Exception {
        Parity.dealMain();
        var inspect = (JvmRuntime.FunctionValue) JvmRuntime.exportSurface("parity").read(args[0]);
        var reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        for (String line; (line = reader.readLine()) != null;) {
            String reply = new String(Base64.getDecoder().decode(line), StandardCharsets.UTF_8);
            String result = (String) inspect.fn.invoke(new Object[]{reply});
            System.out.println(Base64.getEncoder().encodeToString(result.getBytes(StandardCharsets.UTF_8)));
        }
    }
}
''')
    classpath = ROOT / 'build/embedding-library/classes'
    run(['javac', '--release', '21', '-cp', classpath, '-d', out / 'classes',
         out / 'jvm/Parity.java', out / 'NeedsParityHost.java'], 'javac')
    import base64
    counts = {}
    for filename, export in [('cases', 'inspect'), ('sketch-cases', 'inspectSketch'), ('sketch-needs-cases', 'sketchFromNeeds'),
                             ('filter-cases', 'inspectFilter'), ('partial-cases', 'inspectPartial'),
                             ('drawing-cases', 'inspectDrawing'), ('start-cases', 'inspectStart')]:
        cases = json.loads((out / (filename + '.json')).read_text())
        requests = '\n'.join(base64.b64encode(case['reply'].encode()).decode() for case in cases) + '\n'
        replies = run(['java', '-cp', str(out / 'classes') + os.pathsep + str(classpath),
                       'NeedsParityHost', export], filename + '-jvm', input=requests).splitlines()
        if len(replies) != len(cases):
            raise AssertionError('JVM response count differs from fixture count')
        # Both language runtimes use binary64. Python's arbitrary-precision
        # JSON integers must not make equivalent decimal spellings disagree.
        expected_cases = json.loads((out / (filename + '.json')).read_text(), parse_int=float)
        for case, reply in zip(expected_cases, replies):
            if json.loads(base64.b64decode(reply), parse_int=float) != case['expected']:
                raise AssertionError('JVM differential mismatch: ' + case['name'])
        counts[filename] = len(cases)
        print(f'PASS {len(cases)} JVM cases ({filename})')
    differential_count = counts['cases'] - 2
    receipt = {'referenceRevision': PIN, 'passed': True, 'differentialCasesPerBackend': differential_count,
               'backends': ['js', 'jvm'],
               'scope': 'Needs parsing/normalization, filtering, streaming/retry/review and requested starts; not production integration or choice/composition parity',
               'caseCountsPerBackend': counts,
               'remaining': ['choice construction/answer handling', 'production replacement'],
               'mechanisms': 'Unicode NFKC/lowercase, UTF-16 units/head, regex matching and number text are adapter primitives; policy runs in DEAL',
               'intentionalBoundaryDifferences': ['duplicate JSON keys reject', 'unpaired Unicode surrogates reject'],
               'portHashes': {name: hashlib.sha256((source / f'{name}.deal').read_bytes()).hexdigest()
                              for name in ['needs-plan', 'plan-decoding', 'sketch', 'needs-filtering', 'needs-drawing', 'needs-start']},
               'unicodeBoundaryDifference': 'A notice truncation that bisects a surrogate rejects instead of creating an invalid DEAL string',
               'testHashes': {str(p.relative_to(ROOT)): hashlib.sha256(p.read_bytes()).hexdigest() for p in
                              [pathlib.Path(__file__), ROOT / 'tools/test-sketch-parity.js',
                               ROOT / 'tools/test-needs-plan-parity.js', ROOT / 'tools/fixtures/sketch-parity.deal',
                               ROOT / 'tools/test-needs-orchestration-parity.js', ROOT / 'tools/fixtures/needs-orchestration-parity.deal']},
               'hashes': {name: hashlib.sha256((reference / name).read_bytes()).hexdigest() for name in reference_files}}
    (out / 'receipt.json').write_text(json.dumps(receipt, indent=2) + '\n')
    print('Evidence:', out.relative_to(ROOT))


if __name__ == '__main__':
    main()
