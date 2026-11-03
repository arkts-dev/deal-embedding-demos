#!/usr/bin/env node
'use strict';
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const [referencePath, outputPath] = process.argv.slice(2).map(p => path.resolve(p));
const {loadVera} = require(path.join(referencePath, 'tools/vera-harness/source-loader.js'));
const reference = loadVera('NeedsPlan');
const composer = loadVera('VeraCompose');

// Capture the boundary of the actual sketchFrom implementation. No copy of
// its policy in the test; still call the actual composer to check the result.
const parseSketch = composer.parseSketch;
let document;
composer.parseSketch = function (text) {
    document = JSON.parse(text);
    return parseSketch(text);
};
function referenceResult(reply) {
    const plan = reference.parseNeedsPlan(reply);
    if (plan === null) return null;
    reference.sketchFrom(plan);
    const before = document;
    const needs = JSON.parse(JSON.stringify(plan.needs));
    const firstCountdown = reference.needOf(plan, 'countdown')?.words ?? null;
    const changed = reference.plainScenes(plan);
    reference.sketchFrom(plan);
    return {base: plan.base, needs, document: before, firstCountdown,
        plainScenes: changed, plainDocument: document};
}

// Use the ordinary generated runtime in an isolated process, not retagged
// host objects or an alternate evaluator. The entry accepts only strings.
const generated = require(path.join(outputPath, 'js/parity.js'));
const inspect = generated.inspect;
assert.equal(typeof inspect?.$f, 'function');
const fixtures = [
    ['no object', 'nothing usable'],
    ['empty', '{}'],
    ['wrapped', 'Here is the plan:\n```json\n{"title":"App","needs":[]}\n```'],
    ['broken', '{"needs":[}'],
    ['two objects', '{} {}'],
    ['absent needs', '{"title":null,"inputs":false,"ignored":17}'],
    ['nonarray needs', '{"needs":{"type":"content","what":"Not an array"}}'],
    ['mixed need values', JSON.stringify({needs:[null, 1, true, 'other', [], {}, {type:'unknown'}, {type:' content ', what:' list '}]})],
    ['all types', JSON.stringify({title:'Plan', eyebrow:'LABEL', subtitle:'Line', inputs:[], constants:[], questions:[], ignored:1,
        needs:[
            {type:'figure', id:'metric', label:' Total ', means:' counted ', formula:' entries.total + clock.remaining + entries.count + clock.elapsed + clock.duration ',
                example:{given:{'clock.remaining':10,'entries.total':3,'entries.count':2,clockwise:5},result:13,extra:true},extra:null},
            {type:'countdown', label:'First', length:'duration'}, {type:'countdown', label:'Second'},
            {type:'stopwatch', label:'Watch'}, {type:'stopwatch', label:'Another'},
            {type:'entries', label:'Notes', fields:[]}, {type:'entries', label:'More'},
            {type:'content', what:' Read these ', from:' exact words '},
            {type:'action', when:' Finish ', what:' Notify ', on:'countdown_end', from:' notify me ', asked:false},
            {type:'action', when:99, what:'Share', on:'unrecognized'},
            {type:'scene', what:' Spinning drawing ', from:' draw '}, {type:'other', what:' Something else '}
        ]})],
    ['quantity substring behavior', JSON.stringify({needs:[{type:'figure',formula:'xclock.remainingSuffix + clock.remaining',label:'Metric',example:{given:{'clock.remainingSuffix':7},result:7}}]})],
    ['array examples', JSON.stringify({needs:[{type:'figure',example:{given:[1,2],result:3}},{type:'figure',example:[]}]})],
    ['unicode whitespace', JSON.stringify({needs:[{type:'\u00a0figure\ufeff',label:'\u2003Value\u3000',means:'\u2028meaning\u2029',formula:'\u00a0clock.elapsed\ufeff'}]})],
    ['non-BMP strings', JSON.stringify({title:'🎨',needs:[{type:'scene',what:'🎲 move'},{type:'content',what:'👩🏽‍🔬 notes'}]})],
    ['keep raw fields', JSON.stringify({inputs:[{id:'duration',label:'Length',kind:'choice',options:[{label:'Ten',value:600}],value:600}],needs:[{type:'figure',id:'total',label:'Sum',shown:false,formula:'2 + 3',example:{given:{},result:5}}]})],
    ['nested primitives', JSON.stringify({needs:[{type:'figure',example:false},{type:'figure',example:{given:null}},{type:'action',on:null,from:7}]})]
];

// Deterministic varied inputs, not model answers or four-scenario fixtures.
let seed = 173;
function pick(values) {
    seed = (Math.imul(seed, 1664525) + 1013904223) >>> 0;
    return values[seed % values.length];
}
for (let i = 0; i < 160; i++) {
    const needs = [];
    for (let j = 0; j < 8; j++) {
        needs.push({type:pick(['figure','countdown','stopwatch','entries','content','action','scene','other','invalid']),
            label:pick([' Alpha ', '', null, 12]), means:pick(['Meaning',null,'']), what:pick([' Text ', 0, '🎨']),
            when:pick(['Now',null]), on:pick(['button','row_tap','other','bad']), from:pick([' words ',false]),
            formula:pick(['clock.remaining + entries.count','entries.total / clock.elapsed','','prefixclock.duration']),
            example:pick([{given:{'clock.elapsed':3},result:3},null,{given:[4,5]}])});
    }
    fixtures.push(['varied ' + i, JSON.stringify({title:'Varied',needs})]);
}
const results = [];
for (const [name, reply] of fixtures) {
    const expected = referenceResult(reply);
    const actual = JSON.parse(inspect.$f(reply));
    assert.deepEqual(actual, expected, name);
    // The boundary returned by DEAL must lead to the same normalized Sketch
    // when passed through the real reference composer, Maps included.
    if (actual !== null) {
        assert.deepEqual(parseSketch(JSON.stringify(actual.document)),
            parseSketch(JSON.stringify(expected.document)), name + ' normalized sketch');
    }
    results.push({name, reply, expected});
}
// Intentional checked-boundary differences, not silently claimed as parity.
for (const [name, reply] of [
    ['duplicate key boundary', '{"title":"first","title":"last"}'],
    ['invalid Unicode boundary', '{"title":"\\ud800"}']
]) {
    assert.notEqual(reference.parseNeedsPlan(reply), null);
    assert.equal(JSON.parse(inspect.$f(reply)), null);
    results.push({name, reply, expected: null, intentionalBoundaryDifference: true});
}
fs.writeFileSync(path.join(outputPath, 'cases.json'), JSON.stringify(results, null, 2) + '\n');
console.log(`PASS ${fixtures.length} reference differential cases; 2 explicit strict-JSON boundary differences`);
