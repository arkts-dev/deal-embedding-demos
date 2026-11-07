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
        const timeSource = `import * as ui from "std/ui";
import * as math from "std/math";
class AppState { minute: number; }
function init(): AppState { return {minute: 960}; }
function set_minute(state: AppState, value: number): AppState { return {minute: value}; }
function view(state: AppState): ui.View {
    let sections: ui.View[] = [];
    // Literal text is not a syntax reference, including after non-BMP text.
    let label: string = "🌍 ui.clockToString(math.floor(1))";
    let minute: number = math.floor(state.minute);
    sections.push(ui.TimeField("default", label, minute, "set_minute"));
    sections.push(ui.Stat("default", "Clock", ui.clockToString(minute), "", "clock"));
    sections.push(ui.Stat("default", "Duration", ui.durationToString(minute), "", "chart"));
    sections.push(ui.Stat("default", "Number", ui.numberToString(minute), "", "chart"));
    return ui.AppTheme("clean", "", sections);
}`;
        for (const rejected of [timeSource.replace('math.floor(state.minute)', 'math.sqrt(state.minute)'), timeSource.replace('as math from', 'as other from')]) {
            assert.throws(() => lower.lower.$f(rejected, syntax), e => e.code === 'PORTABLE_UNSUPPORTED');
        }
        const timePair = lower.lower.$f(timeSource, syntax);
        assert.ok(timePair.dealui.includes('ui.TimeField('));
        assert.ok(timePair.deal.includes('"🌍 ui.clockToString(math.floor(1))"'));
        assert.ok(timePair.deal.includes('presentation.floor(state.minute)'));
        const timeDir = path.join(work, 'time-candidate/src'); fs.mkdirSync(timeDir, {recursive: true});
        fs.writeFileSync(path.join(timeDir, 'experience.deal'), timePair.deal);
        fs.writeFileSync(path.join(timeDir, 'experience.dealui'), timePair.dealui);
        fs.writeFileSync(path.join(work, 'time-reference.deal'), timeSource);
        for (const rejected of [actual.source.replace('ui.NumberStat(', 'ui.Unsupported('), actual.source.replace('"clean"', '"playful"'), actual.source.replace('ui.MetricGroup("default", 1,', 'ui.MetricGroup("default", 0,')]) {
            assert.throws(() => lower.lower.$f(rejected, syntax), e => e.code === 'PORTABLE_UNSUPPORTED');
        }
        assert.throws(() => lower.lower.$f(actual.source + '\nfunction invalid(', syntax), e => e.code === 'PORTABLE_SYNTAX');
        console.log('PASS portable composer → checked-source lowering; unsupported components reject');
    })().catch(error => {console.error(error); process.exitCode = 1;});
} else if (mode === 'session' || mode === 'time-session') {
    const output = path.join(work, mode === 'time-session' ? 'time-candidate/js' : 'candidate/js'); fs.mkdirSync(path.join(output, 'portable'), {recursive: true});
    // Numeric mechanisms are copied from the library; policy executes in DEAL.
    require('../dependencies/deal-ui/runtime-js/session.js');
    const entry = require(path.join(output, 'experience_entry.js')), rt = require(path.join(output, 'deal/runtime.js'));
    const schedule = () => {throw Error('Unexpected effect');};
    const session = createDealUiSession(entry, rt, null, schedule);
    const find = (node, component) => node.component === component ? node : node.children.map(child => find(child, component)).find(Boolean);
    const prop = (node, name) => node.props.find(p => p.name === name);
    function snapshot(current) {const value = JSON.parse(current.snapshot()); assert.equal(value.fault, ''); return value;}
    function metric(current) {return prop(find(snapshot(current).tree, 'ui.Metric'), 'value').stringValue;}
    if (mode === 'time-session') {
        const all = (node, component) => (node.component === component ? [node] : []).concat(node.children.flatMap(child => all(child, component)));
        const metrics = current => all(snapshot(current).tree, 'ui.Metric').map(node => prop(node, 'value').stringValue);
        assert.deepEqual(metrics(session), ['16:00', '16:00', '960']);
        const timeField = find(snapshot(session).tree, 'ui.TimeField');
        assert.equal(prop(timeField, 'label').stringValue, '🌍 ui.clockToString(math.floor(1))');
        session.dispatch(prop(timeField, 'onChange').actionSlot, '1501');
        assert.deepEqual(metrics(session), ['01:01', '25:01', '1501']);
        assert.equal(snapshot(session).version, 2);
        const restored = createDealUiSession(entry, rt, JSON.parse(session.state()), schedule);
        assert.deepEqual(metrics(restored), metrics(session));
        const p = require(path.join(output, 'portable-presentation.js'));
        for (const n of [NaN, Infinity, -Infinity, -0, -1, 0, 59.9, 60, 3601, 86401, 1e20, Number.MAX_VALUE]) {
            const valid = Number.isFinite(n) && n > 0 ? Math.floor(n) : 0;
            const clock = valid % 1440;
            assert.equal(p.clockToString.$f(n), `${String(Math.floor(clock/60)).padStart(2,'0')}:${String(clock%60).padStart(2,'0')}`);
            const h=Math.floor(valid/3600), m=Math.floor((valid%3600)/60), s=String(valid%60).padStart(2,'0');
            assert.equal(p.durationToString.$f(n), h === 0 ? `${m}:${s}` : `${h}:${String(m).padStart(2,'0')}:${s}`);
            assert.ok(Object.is(p.round.$f(n), Math.round(n)));
        }
        for (const [a,b] of [[0,-0],[-0,0],[-0,-0],[NaN,1],[1,NaN],[-3,2]]) {
            assert.ok(Object.is(p.min.$f(a,b), Math.min(a,b)));
            assert.ok(Object.is(p.max.$f(a,b), Math.max(a,b)));
        }
        for (const n of [-1.5,-0.5,-0.1,0.49,0.5,1.5,4503599627370495]) assert.ok(Object.is(p.round.$f(n),Math.round(n)));
        session.dispose(); restored.dispose();
        console.log('PASS actual Deal UI time input/edit/restore, clock/duration formatting, numeric edge cases and literal preservation; no travel substitute');
        return;
    }
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
