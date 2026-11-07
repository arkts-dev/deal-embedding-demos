'use strict';
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const cp = require('node:child_process');
const [work, evidence, classes, mode] = process.argv.slice(2);
if (mode === 'lower') {
    const lower = require(path.join(work, 'lowering/js/portable-source-lowering.js'));
    const rt = require(path.join(work, 'lowering/js/deal/runtime.js'));
    const syntax = rt.function('(string)->string', source => {
        const input = path.join(work, 'parser.deal'); fs.writeFileSync(input, source);
        return cp.execFileSync('java', ['-cp', `${work}/classes:${classes}`, 'PortableSyntax', input], {encoding: 'utf8'});
    });
    // Execute the ported composer, not just the archived reference source.
    const fixture = JSON.parse(fs.readFileSync(path.join(work, 'fixture.json')));
    const flow = require(path.join(evidence, 'flow-js/flow-runner.js'));
    (async () => {
        const actual = JSON.parse(await flow.inspectFlow.$f(fixture.reply));
        assert.equal(actual.source, fixture.expected.source);
        const pair = lower.lower.$f(actual.source, syntax);
        const sourceDir = path.join(work, 'candidate/src'); fs.mkdirSync(sourceDir, {recursive: true});
        fs.writeFileSync(path.join(sourceDir, 'experience.deal'), pair.deal);
        fs.writeFileSync(path.join(sourceDir, 'experience.dealui'), pair.dealui);
        for (const columns of [2, 3]) {
            const grouped = lower.lower.$f(actual.source.replace('ui.MetricGroup("default", 1,', `ui.MetricGroup("default", ${columns},`), syntax);
            assert.ok(grouped.dealui.includes('ui.MetricGroup()'));
            assert.equal(grouped.deal, pair.deal, 'Layout hints must not alter state/calculation semantics');
        }
        for (const rejected of [actual.source.replace('ui.NumberStat(', 'ui.Unsupported('), actual.source.replace('"clean"', '"playful"'), actual.source.replace('ui.MetricGroup("default", 1,', 'ui.MetricGroup("default", 0,')]) {
            assert.throws(() => lower.lower.$f(rejected, syntax), e => e.code === 'PORTABLE_UNSUPPORTED');
        }
        assert.throws(() => lower.lower.$f(actual.source + '\nfunction invalid(', syntax), e => e.code === 'PORTABLE_SYNTAX');
        console.log('PASS portable composer → checked-source lowering; unsupported components reject');
    })().catch(error => {console.error(error); process.exitCode = 1;});
} else if (mode === 'session') {
    const output = path.join(work, 'candidate/js'); fs.mkdirSync(path.join(output, 'portable'), {recursive: true});
    // Numeric mechanisms are copied from the library; policy executes in DEAL.
    require('../dependencies/deal-ui/runtime-js/session.js');
    const entry = require(path.join(output, 'experience_entry.js')), rt = require(path.join(output, 'deal/runtime.js'));
    const schedule = () => {throw Error('Unexpected effect');};
    const session = createDealUiSession(entry, rt, null, schedule);
    const find = (node, component) => node.component === component ? node : node.children.map(child => find(child, component)).find(Boolean);
    const prop = (node, name) => node.props.find(p => p.name === name);
    function snapshot(current) {const value = JSON.parse(current.snapshot()); assert.equal(value.fault, ''); return value;}
    function metric(current) {return prop(find(snapshot(current).tree, 'ui.Metric'), 'value').stringValue;}
    assert.equal(metric(session), '10.0');
    const field = find(snapshot(session).tree, 'ui.TextField');
    session.dispatch(prop(field, 'onChange').actionSlot, '12');
    assert.equal(metric(session), '14.0');
    assert.equal(snapshot(session).version, 2);
    session.dispatch(prop(field, 'onChange').actionSlot, '1,5');
    assert.equal(metric(session), '3.5');
    session.dispatch(prop(field, 'onChange').actionSlot, 'not a number');
    assert.equal(metric(session), '2.0');
    const checkpoint = JSON.parse(session.state());
    const restored = createDealUiSession(entry, rt, checkpoint, schedule);
    assert.equal(metric(restored), '2.0');
    session.dispose(); restored.dispose();
    fs.writeFileSync(path.join(work, 'receipt.json'), JSON.stringify({composer: true, uiCheck: true, dealCompile: true, inputRecompute: true, restored: true, productionChanged: false, android: false}, null, 2));
    console.log('PASS actual Deal UI session: initial render, input recompute, decimal comma, invalid input, restore');
} else {throw Error('Expected lower or session');}
