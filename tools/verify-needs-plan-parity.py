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
    for name in ['needs-plan', 'plan-decoding', 'sketch', 'needs-filtering', 'needs-drawing', 'needs-start', 'needs-choice',
                 'compose-expressions', 'compose-answers', 'compose-formula', 'compose-bindings', 'compose-events', 'compose-questions', 'compose-countdown', 'compose-state', 'compose-sections', 'compose-pruning', 'compose-reader', 'compose-parts', 'compose-lengths', 'compose-flow', 'screen-plan', 'template-spec', 'section-text', 'section-emission', 'compose-contracts', 'compose-screen', 'custom-section', 'hole-settlement', 'custom-calls', 'custom-prompts', 'custom-writer', 'hole-flow', 'custom-check', 'compose-source', 'compose-input-emission', 'compose-figure-emission', 'compose-call-emission', 'compose-clock-emission', 'compose-effect-emission', 'compose-list-emission', 'compose-part-emission', 'compose-assembly', 'catalogue-contracts', 'catalogue-slots', 'catalogue-content-text', 'catalogue-content-lists', 'catalogue-content', 'compose-claims', 'catalogue-layouts', 'catalogue-provenance', 'catalogue-holes', 'catalogue-fill', 'compose-alignment', 'needs-composition', 'compose-provided', 'provisional-repair-text', 'program-settlement', 'compose-preview', 'agent-questions', 'composition-record', 'run-stages', 'portable-agent', 'agent-surface', 'portable-build-route', 'composition-record-json', 'composition-record-edits', 'composition-edit-flow', 'program-smoke', 'program-surface-play', 'program-execution', 'construction-identities']:
        shutil.copy(lib / f'core/{name}.deal', source)
    shutil.copy(ROOT / 'tools/fixtures/sketch-parity.deal', source / 'sketch-parity.deal')
    shutil.copy(ROOT / 'tools/fixtures/needs-orchestration-parity.deal', source / 'needs-orchestration-parity.deal')
    shutil.copy(ROOT / 'tools/fixtures/needs-choice-parity.deal', source / 'needs-choice-parity.deal')
    shutil.copy(ROOT / 'tools/fixtures/compose-expressions-parity.deal', source / 'compose-expressions-parity.deal')
    shutil.copy(ROOT / 'tools/fixtures/compose-formula-parity.deal', source / 'compose-formula-parity.deal')
    shutil.copy(ROOT / 'tools/fixtures/compose-bindings-parity.deal', source / 'compose-bindings-parity.deal')
    shutil.copy(ROOT / 'tools/fixtures/compose-events-parity.deal', source / 'compose-events-parity.deal')
    shutil.copy(ROOT / 'tools/fixtures/compose-reader-parity.deal', source / 'compose-reader-parity.deal')
    shutil.copy(ROOT / 'tools/fixtures/compose-flow-parity.deal', source / 'compose-flow-parity.deal')
    shutil.copy(ROOT / 'tools/fixtures/composition-edit-parity.deal', source / 'composition-edit-parity.deal')
    shutil.copy(ROOT / 'tools/fixtures/screen-plan-parity.deal', source / 'screen-plan-parity.deal')
    shutil.copy(ROOT / 'tools/fixtures/catalogue-content-parity.deal', source / 'catalogue-content-parity.deal')
    shutil.copy(ROOT / 'tools/fixtures/catalogue-slots-parity.deal', source / 'catalogue-slots-parity.deal')
    shutil.copy(ROOT / 'tools/fixtures/custom-writer-parity.deal', source / 'custom-writer-parity.deal')
    shutil.copy(ROOT / 'tools/fixtures/program-smoke-parity.deal', source / 'program-smoke-parity.deal')
    shutil.copy(ROOT / 'tools/fixtures/program-play-parity.deal', source / 'program-play-parity.deal')
    (out / 'deal.json').write_text(json.dumps({
        'languageVersion': '1.5', 'backend': 'js', 'moduleRoots': ['src']}))
    # Projection is explicit: nominal classes are not generic JSON Records.
    (source / 'parity.deal').write_text('''import * as needs from "./needs-plan";
import * as json from "std/json";
import * as sketchParity from "./sketch-parity";
import * as orchestration from "./needs-orchestration-parity";
import * as choiceParity from "./needs-choice-parity";
import * as expressionsParity from "./compose-expressions-parity";
import * as formulaParity from "./compose-formula-parity";
import * as bindingsParity from "./compose-bindings-parity";

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
export function inspectChoice(input: string): string { return choiceParity.inspect(input); }
export async function inspectChoiceFlow(input: string): Promise<string> { return await choiceParity.inspectFlow(input); }
export function inspectExpressions(input: string): string { return expressionsParity.inspect(input); }
export function inspectAnswers(input: string): string { return expressionsParity.inspectAnswers(input); }
export function inspectFormula(input: string): string { return formulaParity.inspect(input); }
export function inspectWritten(input: string): string { return formulaParity.inspectWritten(input); }
export function inspectBindings(input: string): string { return bindingsParity.inspect(input); }
export function main(): null { return null; }
''')
    (source / 'event-runner.deal').write_text('''import * as eventsParity from "./compose-events-parity";
export function inspectEvents(input: string): string { return eventsParity.inspect(input); }
export function main(): null { return null; }
''')
    (source / 'reader-runner.deal').write_text('''import * as readerParity from "./compose-reader-parity";
export async function inspectReader(input: string): Promise<string> { return await readerParity.inspect(input); }
export function main(): null { return null; }
''')
    (source / 'flow-runner.deal').write_text('''import * as flowParity from "./compose-flow-parity";
export async function inspectFlow(input: string): Promise<string> { return await flowParity.inspect(input); }
export function main(): null { return null; }
''')
    (source / 'screen-runner.deal').write_text('''import * as screenParity from "./screen-plan-parity";
export function inspectScreen(input: string): string { return screenParity.inspect(input); }
export function main(): null { return null; }
''')
    (source / 'writer-runner.deal').write_text('''import * as writerParity from "./custom-writer-parity";
export async function inspectWriter(input: string): Promise<string> { return await writerParity.inspect(input); }
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
    event_compiler = compiler[:-1] + [source / 'event-runner.deal']
    run(event_compiler + ['--backend', 'js', '--output', out / 'events-js'], 'events-compile-js')
    run(event_compiler + ['--backend', 'jvm', '--output', out / 'events-jvm'], 'events-compile-jvm')
    reader_compiler = compiler[:-1] + [source / 'reader-runner.deal']
    run(reader_compiler + ['--backend', 'js', '--output', out / 'reader-js'], 'reader-compile-js')
    run(reader_compiler + ['--backend', 'jvm', '--output', out / 'reader-jvm'], 'reader-compile-jvm')
    flow_compiler = compiler[:-1] + [source / 'flow-runner.deal']
    run(flow_compiler + ['--backend', 'js', '--output', out / 'flow-js'], 'flow-compile-js')
    run(flow_compiler + ['--backend', 'jvm', '--output', out / 'flow-jvm'], 'flow-compile-jvm')
    screen_compiler = compiler[:-1] + [source / 'screen-runner.deal']
    run(screen_compiler + ['--backend', 'js', '--output', out / 'screen-js'], 'screen-compile-js')
    run(screen_compiler + ['--backend', 'jvm', '--output', out / 'screen-jvm'], 'screen-compile-jvm')
    writer_compiler = compiler[:-1] + [source / 'writer-runner.deal']
    run(writer_compiler + ['--backend', 'js', '--output', out / 'writer-js'], 'writer-compile-js')
    run(writer_compiler + ['--backend', 'jvm', '--output', out / 'writer-jvm'], 'writer-compile-jvm')
    import os
    env = dict(os.environ, VERA_TYPESCRIPT_PATH=str(args.typescript.resolve()))
    print(run(['node', ROOT / 'tools/test-needs-plan-parity.js', reference, out], 'parity-js', env=env).strip())
    print(run(['node', ROOT / 'tools/test-sketch-parity.js', reference, out], 'sketch-parity-js', env=env).strip())
    print(run(['node', ROOT / 'tools/test-needs-orchestration-parity.js', reference, out], 'orchestration-js', env=env).strip())
    print(run(['node', ROOT / 'tools/test-needs-choice-parity.js', reference, out], 'choice-js', env=env).strip())
    print(run(['node', ROOT / 'tools/test-compose-expressions-parity.js', reference, out], 'expressions-js', env=env).strip())
    print(run(['node', ROOT / 'tools/test-compose-formula-parity.js', reference, out], 'formula-js', env=env).strip())
    print(run(['node', ROOT / 'tools/test-compose-bindings-parity.js', reference, out], 'bindings-js', env=env).strip())
    print(run(['node', ROOT / 'tools/test-compose-events-parity.js', reference, out], 'events-js', env=env).strip())
    print(run(['node', ROOT / 'tools/test-compose-questions-parity.js', reference, out], 'questions-js', env=env).strip())
    print(run(['node', ROOT / 'tools/test-compose-countdown-parity.js', reference, out], 'countdown-js', env=env).strip())
    print(run(['node', ROOT / 'tools/test-compose-reader-parity.js', reference, out], 'reader-js', env=env).strip())
    print(run(['node', ROOT / 'tools/test-catalogue-content-parity.js', reference, out], 'content-js', env=env).strip())
    print(run(['node', ROOT / 'tools/test-compose-flow-parity.js', reference, out], 'flow-js', env=env).strip())
    print(run(['node', ROOT / 'tools/test-screen-plan-parity.js', reference, out], 'screen-js', env=env).strip())
    print(run(['node', ROOT / 'tools/test-custom-writer-parity.js', reference, out], 'writer-js', env=env).strip())
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
    (out / 'EventsParityHost.java').write_text((out / 'NeedsParityHost.java').read_text()
        .replace('class NeedsParityHost', 'class EventsParityHost').replace('Parity.dealMain()', 'Event_runner.dealMain()')
        .replace('exportSurface("parity")', 'exportSurface("event-runner")'))
    (out / 'ReaderParityHost.java').write_text((out / 'NeedsParityHost.java').read_text()
        .replace('class NeedsParityHost', 'class ReaderParityHost').replace('Parity.dealMain()', 'Reader_runner.dealMain()')
        .replace('exportSurface("parity")', 'exportSurface("reader-runner")'))
    (out / 'FlowParityHost.java').write_text((out / 'NeedsParityHost.java').read_text()
        .replace('class NeedsParityHost', 'class FlowParityHost').replace('Parity.dealMain()', 'Flow_runner.dealMain()')
        .replace('exportSurface("parity")', 'exportSurface("flow-runner")'))
    (out / 'ScreenParityHost.java').write_text((out / 'NeedsParityHost.java').read_text()
        .replace('class NeedsParityHost', 'class ScreenParityHost').replace('Parity.dealMain()', 'Screen_runner.dealMain()')
        .replace('exportSurface("parity")', 'exportSurface("screen-runner")'))
    (out / 'WriterParityHost.java').write_text((out / 'NeedsParityHost.java').read_text()
        .replace('class NeedsParityHost', 'class WriterParityHost').replace('Parity.dealMain()', 'Writer_runner.dealMain()')
        .replace('exportSurface("parity")', 'exportSurface("writer-runner")'))
    classpath = ROOT / 'build/embedding-library/classes'
    run(['javac', '--release', '21', '-cp', classpath, '-d', out / 'classes',
         out / 'jvm/Parity.java', out / 'NeedsParityHost.java'], 'javac')
    run(['javac', '--release', '21', '-cp', classpath, '-d', out / 'events-classes',
         out / 'events-jvm/Event_runner.java', out / 'EventsParityHost.java'], 'events-javac')
    run(['javac', '--release', '21', '-cp', classpath, '-d', out / 'reader-classes',
         out / 'reader-jvm/Reader_runner.java', out / 'ReaderParityHost.java'], 'reader-javac')
    run(['javac', '--release', '21', '-cp', classpath, '-d', out / 'flow-classes',
         out / 'flow-jvm/Flow_runner.java', out / 'FlowParityHost.java'], 'flow-javac')
    run(['javac', '--release', '21', '-cp', classpath, '-d', out / 'screen-classes',
         out / 'screen-jvm/Screen_runner.java', out / 'ScreenParityHost.java'], 'screen-javac')
    run(['javac', '--release', '21', '-cp', classpath, '-d', out / 'writer-classes',
         out / 'writer-jvm/Writer_runner.java', out / 'WriterParityHost.java'], 'writer-javac')
    import base64
    counts = {}
    for filename, export in [('cases', 'inspect'), ('sketch-cases', 'inspectSketch'), ('sketch-needs-cases', 'sketchFromNeeds'),
                             ('filter-cases', 'inspectFilter'), ('partial-cases', 'inspectPartial'),
                             ('drawing-cases', 'inspectDrawing'), ('start-cases', 'inspectStart'),
                             ('choice-cases', 'inspectChoice'), ('choice-flow-cases', 'inspectChoiceFlow'),
                             ('expression-cases', 'inspectExpressions'), ('answer-cases', 'inspectAnswers'),
                             ('formula-cases', 'inspectFormula'), ('written-cases', 'inspectWritten'),
                             ('binding-cases', 'inspectBindings'), ('event-cases', 'inspectEvents'), ('question-cases', 'inspectEvents'), ('countdown-cases', 'inspectEvents'), ('reader-cases', 'inspectReader'), ('content-cases', 'inspectReader'), ('flow-cases', 'inspectFlow'), ('screen-cases', 'inspectScreen'), ('writer-cases', 'inspectWriter')]:
        cases = json.loads((out / (filename + '.json')).read_text())
        requests = '\n'.join(base64.b64encode(case['reply'].encode()).decode() for case in cases) + '\n'
        classes = 'writer-classes' if filename == 'writer-cases' else 'screen-classes' if filename == 'screen-cases' else 'flow-classes' if filename == 'flow-cases' else 'reader-classes' if filename in ('reader-cases', 'content-cases') else 'events-classes' if filename in ('event-cases', 'question-cases', 'countdown-cases') else 'classes'
        replies = run(['java', '-cp', str(out / classes) + os.pathsep + str(classpath),
                       'WriterParityHost' if filename == 'writer-cases' else 'ScreenParityHost' if filename == 'screen-cases' else 'FlowParityHost' if filename == 'flow-cases' else 'ReaderParityHost' if filename in ('reader-cases', 'content-cases') else 'EventsParityHost' if filename in ('event-cases', 'question-cases', 'countdown-cases') else 'NeedsParityHost', export], filename + '-jvm', input=requests).splitlines()
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
               'scope': 'Needs/source assembly, catalogue filling/layouts/alignment, repair/questions/previews/record serialization and structural edit flows, surface route and agent catalogue-plus-custom chains; not emitted-source backend execution or production integration',
               'caseCountsPerBackend': counts,
               'smokeBoundary': 'Pinned smoke/surface-play policy and custom/settlement adapters execute in DEAL over scripted VM/decoder mechanisms; real VM/rendering integration remains unfinished',
               'remaining': ['complete transitive dependency coverage and broad integrated failure-chain coverage',
                             'broader catalogue function-hole/provenance and alignment/failure paths inside agent, backend mechanisms and production replacement/deletion'],
               'constructionRepairBoundary': 'SEARCH/REPLACE text compatibility is provisional only; canonical materialization continues to require atomic revision-bound unique/disjoint exact edits',
               'fallbackBoundary': 'Approved guard prevents widening over an independently checked retained task; notices alone do not authorize a task claim',
               'executionBoundary': 'Selected emitted agent programs compile and smoke on the pinned VERA runtime; this is not DEAL v1.5/Deal UI backend execution',
               'catalogueBoundary': 'Actual reference descriptions/veto data supplied; eligibility and options constructed independently in DEAL',
               'mechanisms': 'Unicode/UTF-16, regex, numeric text, pow and ECMAScript remainder are adapter primitives; policy runs in DEAL',
               'compilerEmitterSha256': hashlib.sha256((ROOT / 'dependencies/deal/deal/codegen/jvm/JvmSemanticEmitter.java').read_bytes()).hexdigest(),
               'approvedPolicyDifference': 'Three dispatched calls per stable need/request with shared grouping/splitting/re-entry accounting, versus four local custom-section reference calls; final remaining call uses thinking, including catalogue function holes, surface writing, whole-screen widening and final provisional repairs',
               'operationalBoundary': 'Transport/compiler infrastructure failures propagate, unlike reference broad catches; they do not establish repair/default/fallback authority',
               'intentionalBoundaryDifferences': ['duplicate JSON keys reject', 'unpaired Unicode surrogates reject',
                                                  'formula diagnostics cannot contain a split supplementary scalar'],
               'portHashes': {name: hashlib.sha256((source / f'{name}.deal').read_bytes()).hexdigest()
                              for name in ['needs-plan', 'plan-decoding', 'sketch', 'needs-filtering', 'needs-drawing', 'needs-start', 'needs-choice',
                                           'compose-expressions', 'compose-answers', 'compose-formula', 'compose-bindings', 'compose-events', 'compose-questions', 'compose-countdown', 'compose-state', 'compose-sections', 'compose-pruning', 'compose-reader', 'compose-parts', 'compose-lengths', 'compose-flow', 'screen-plan', 'template-spec', 'section-text', 'section-emission', 'compose-contracts', 'compose-screen', 'custom-section', 'hole-settlement', 'custom-calls', 'custom-prompts', 'custom-writer', 'hole-flow', 'custom-check', 'compose-source', 'compose-input-emission', 'compose-figure-emission', 'compose-call-emission', 'compose-clock-emission', 'compose-effect-emission', 'compose-list-emission', 'compose-part-emission', 'compose-assembly', 'catalogue-contracts', 'catalogue-slots', 'catalogue-content-text', 'catalogue-content-lists', 'catalogue-content', 'compose-claims', 'catalogue-layouts', 'catalogue-provenance', 'catalogue-holes', 'catalogue-fill', 'compose-alignment', 'needs-composition', 'compose-provided', 'provisional-repair-text', 'program-settlement', 'compose-preview', 'agent-questions', 'composition-record', 'run-stages', 'portable-agent', 'agent-surface', 'portable-build-route', 'composition-record-json', 'composition-record-edits', 'composition-edit-flow']},
               'unicodeBoundaryDifference': 'A notice truncation that bisects a surrogate rejects instead of creating an invalid DEAL string',
               'testHashes': {str(p.relative_to(ROOT)): hashlib.sha256(p.read_bytes()).hexdigest() for p in
                              [pathlib.Path(__file__), ROOT / 'tools/test-sketch-parity.js',
                               ROOT / 'tools/test-needs-plan-parity.js', ROOT / 'tools/fixtures/sketch-parity.deal',
                               ROOT / 'tools/test-needs-orchestration-parity.js', ROOT / 'tools/fixtures/needs-orchestration-parity.deal',
                               ROOT / 'tools/test-needs-choice-parity.js', ROOT / 'tools/fixtures/needs-choice-parity.deal',
                               ROOT / 'tools/test-compose-expressions-parity.js', ROOT / 'tools/fixtures/compose-expressions-parity.deal',
                               ROOT / 'tools/test-compose-formula-parity.js', ROOT / 'tools/fixtures/compose-formula-parity.deal',
                               ROOT / 'tools/test-compose-bindings-parity.js', ROOT / 'tools/fixtures/compose-bindings-parity.deal',
                               ROOT / 'tools/test-compose-events-parity.js', ROOT / 'tools/fixtures/compose-events-parity.deal',
                               ROOT / 'tools/test-compose-questions-parity.js', ROOT / 'tools/test-compose-countdown-parity.js', ROOT / 'tools/test-compose-reader-parity.js', ROOT / 'tools/fixtures/compose-reader-parity.deal', ROOT / 'tools/test-compose-flow-parity.js', ROOT / 'tools/fixtures/compose-flow-parity.deal', ROOT / 'tools/fixtures/composition-edit-parity.deal', ROOT / 'tools/fixtures/screen-plan-parity.deal', ROOT / 'tools/test-screen-plan-parity.js', ROOT / 'tools/fixtures/custom-writer-parity.deal', ROOT / 'tools/test-custom-writer-parity.js', ROOT / 'tools/fixtures/catalogue-slots-parity.deal', ROOT / 'tools/fixtures/catalogue-content-parity.deal', ROOT / 'tools/test-catalogue-content-parity.js']},
               'hashes': {name: hashlib.sha256((reference / name).read_bytes()).hexdigest() for name in reference_files}}
    (out / 'receipt.json').write_text(json.dumps(receipt, indent=2) + '\n')
    print('Evidence:', out.relative_to(ROOT))


if __name__ == '__main__':
    main()
