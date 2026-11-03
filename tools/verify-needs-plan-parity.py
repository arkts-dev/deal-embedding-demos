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
    shutil.copy(lib / 'core/needs-plan.deal', source)
    (out / 'deal.json').write_text(json.dumps({
        'languageVersion': '1.5', 'backend': 'js', 'moduleRoots': ['src']}))
    # Projection is explicit: nominal classes are not generic JSON Records.
    (source / 'parity.deal').write_text('''import * as needs from "./needs-plan";
import * as json from "std/json";

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
    (out / 'NeedsParityHost.java').write_text('''import deal.codegen.jvm.JvmRuntime;
import java.util.Base64;
import java.nio.charset.StandardCharsets;
import java.io.BufferedReader;
import java.io.InputStreamReader;
public class NeedsParityHost {
    public static void main(String[] args) throws Exception {
        Parity.dealMain();
        var inspect = (JvmRuntime.FunctionValue) JvmRuntime.exportSurface("parity").read("inspect");
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
    cases = json.loads((out / 'cases.json').read_text())
    requests = '\n'.join(base64.b64encode(case['reply'].encode()).decode() for case in cases) + '\n'
    replies = run(['java', '-cp', str(out / 'classes') + os.pathsep + str(classpath),
                   'NeedsParityHost'], 'parity-jvm', input=requests).splitlines()
    if len(replies) != len(cases):
        raise AssertionError('JVM response count differs from fixture count')
    for case, reply in zip(cases, replies):
        if json.loads(base64.b64decode(reply)) != case['expected']:
            raise AssertionError('JVM differential mismatch: ' + case['name'])
    differential_count = sum(not case.get('intentionalBoundaryDifference') for case in cases)
    print(f'PASS {differential_count} JVM reference differential cases; 2 strict-JSON boundary cases')
    receipt = {'referenceRevision': PIN, 'passed': True, 'differentialCasesPerBackend': differential_count,
               'backends': ['js', 'jvm'],
               'scope': 'NeedsPlan parsing, pre-parseSketch conversion, needOf and plainScenes; not production integration or full needs/composition parity',
               'remaining': ['parseSketch normalization', 'dropUnasked/asksFor', 'capForWidget',
                             'NeedsChoose retry/review/choice orchestration', 'production replacement'],
               'intentionalBoundaryDifferences': ['duplicate JSON keys reject', 'unpaired Unicode surrogates reject'],
               'portSha256': hashlib.sha256((source / 'needs-plan.deal').read_bytes()).hexdigest(),
               'hashes': {name: hashlib.sha256((reference / name).read_bytes()).hexdigest() for name in reference_files}}
    (out / 'receipt.json').write_text(json.dumps(receipt, indent=2) + '\n')
    print('Evidence:', out.relative_to(ROOT))


if __name__ == '__main__':
    main()
