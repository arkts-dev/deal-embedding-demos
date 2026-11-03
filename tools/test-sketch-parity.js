#!/usr/bin/env node
'use strict';
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const [referencePath, outputPath] = process.argv.slice(2).map(p => path.resolve(p));
const {loadVera} = require(path.join(referencePath, 'tools/vera-harness/source-loader.js'));
const reference = loadVera('VeraCompose');
const needs = loadVera('NeedsPlan');
const generated = require(path.join(outputPath, 'js/parity.js'));
// Preserve Map order and numeric states that ordinary JSON would erase.
const project = value => JSON.parse(JSON.stringify(value, (_, v) => {
    if (v instanceof Map) return [...v];
    if (typeof v === 'number') {
        if (Number.isNaN(v)) return '#NaN';
        if (Object.is(v,-0)) return '#-0';
        if (!Number.isFinite(v)) return v < 0 ? '#-Infinity' : '#Infinity';
    }
    return v;
}));
const fixtures = [];
const add = (name, doc) => fixtures.push({name, reply:JSON.stringify(doc)});
for (const [name, reply] of [['empty','{}'], ['wrapped','prefix {"title":"App"} suffix'],
    ['broken','{"inputs":[}'], ['missing','words'], ['multiple','{} {}']]) fixtures.push({name, reply});
add('defaults and primitives', {title:null, inputs:[false, 7, 'word', [], {}, {id:'alpha'}], constants:[{id:'fixed',value:3}], values:[{id:'answer'}]});
add('identifier renaming', {inputs:['total','total','totalItem','duration','duration','a_b','A','x','x','xItem','x'].map(id=>({id})), constants:[{id:'total',value:7}], values:[{id:'total'},{id:'total'},{id:'x'}]});
const reserved = ['state','value','elapsedMs','running','finished','effectStatus','remaining','elapsed','duration','view','init','ui','math','strings','sdk','time','entries','entryCount','entryTotal','draftText','draftNumberText','entry','out','rows','i','e','emptyEntries','total','next','startAt','effectNote','share','countdownSeconds','addEntry','removeEntry','ignored','onEntryAnswer','answered','marked','note','sub','undoRemove','expireUndo','removedEntries','removedAt','gone','none'];
add('all reserved identifiers', {inputs:reserved.map(id=>({id}))});
for (const kind of ['amount','percent','count','choice','toggle','text','time','bad']) {
    add('input kind '+kind,{inputs:[{id:'input',kind,options:[{label:'One',value:10},{label:'Two',value:20}],value:20}]});
}
for (const [measure,min,max] of [['seconds',0,3600],['minutes',15,240],['minutes',0,1439],['minutes',300,1440],['minutes',1439,1440],['clock',0,100],['number',0,100]]) {
    for (const value of [undefined,-1,0,0.49999999999999994,0.5,480.5,1800]) {
        add(`time ${measure} ${min} ${max} ${value}`,{inputs:[{id:'start',kind:'time',measure,min,max,value}]});
    }
}
for (const value of [-1,0,1,2,1.5,600,900,999]) {
    add('choice starting value '+value,{inputs:[{id:'length',kind:'choice',measure:'minutes',value,options:[{label:'10 MINUTES',value:600},{label:'15 minutes',value:900}]}]});
}
add('choice unit inconsistencies',{inputs:[{id:'length',kind:'choice',measure:'minutes',options:[{label:'10 minutes',value:600},{label:'15 minutes',value:15}]},{id:'empty',kind:'choice',options:[]},{id:'spaces',kind:'choice',options:[{label:' ',value:1}]}]});
const currencies = ['canadian dollar','AUD','NZD','hkd','SGD','mxn','BRL','Chinese Yuan','USD','US\u00a0\t DOLLARS','euros','GBP','JPY','INR','KRW','RUB','TRY','ILS','VND','THB','PHP','CHF','kr','€','  ','İNR','KRW'];
add('currency aliases',{inputs:currencies.map((unit,i)=>({id:'currency'+i,measure:'money',unit})),values:currencies.map((unit,i)=>({id:'amount'+i,format:'money',unit}))});
for (const label of ['Laps','Splits','Rounds','Elapsed time','lap time','lap,','\u00a0LAP\u00a0','Klaps']) {
    for (const fields of [[],[{id:'name',kind:'text'}],[{id:'total',kind:'number'}],['toggle','time','toggle','time'].map((kind,i)=>({id:'field'+i,kind}))]) {
        add('lap normalization '+label+' '+fields.length,{stopwatch:{label:'Stopwatch'},list:{label,fields}});
    }
}
add('one clock',{countdown:{label:'Rest',length:'length'},stopwatch:{label:'Laps'},list:{label:'Laps',fields:[]},other:['  extra  ']});
add('array clocks but not array list',{countdown:[],stopwatch:[],list:[]});
add('legacy list',{list:{label:'Records',text:'Name',number:'Cost',measure:'money',unit:'usd'}});
add('explicit fields override legacy',{list:{text:'Ignore',number:'Ignore',fields:[]}});
add('numeric key enumeration',{values:[{id:'samples',example:{given:{'12':12,'2':2,z:3,'1':1},result:18}}],ready:[{'12':'twelve','2':'two',z:'last','1':'one'}]});
add('long leading number',{inputs:[{id:'length',kind:'choice',measure:'minutes',options:[{label:'12345678901234567890 min',value:Number.parseInt('12345678901234567890')*60}]}]});
add('list field naming and limits',{list:{fields:['minute','status','number','string','boolean','last'].map((id,i)=>({id,kind:['number','time','unknown'][i%3],value:-0.5,decimals:i}))}});
for (const format of ['money','percent','count','seconds','minutes','clock','time_of_day','fraction','plain','bad']) {
    for (const decimals of [-1,0,1.5,6,7]) add('figure '+format+' '+decimals,{values:[{id:'answer',format,unit:'USD',decimals,formula:'\u00a0 a + b \ufeff',shown:false,example:{given:{b:2,a:1,wrong:'3'},result:3}}]});
}
for (const fallback of [-0.5,-0.49999999999999994,0.49999999999999994,0.5,1.5,2.5]) {
    add('question rounding '+fallback,{questions:[{ask:'Pick',options:['A','B','C','D'],default:fallback}]});
}
add('unicode whitespace everywhere',{ready:['\ufeff Ready \u3000'],questions:[{ask:'\u2028Pick\u2029',options:['\u00a0A\u00a0','\u2003B\u2003']}],values:[{id:'v',formula:'\u202f 1 + 2 \u205f'}]});
add('ready and questions',{ready:[' words ',{need:'A',what:'B',number:5},['C','D'],null,''], questions:[null,{}, {ask:' Which? ',options:[' One ',null,'Two'],default:0.5},{id:'where',ask:'Where?',options:['A','B','C'],default:99},{ask:'Ignored third',options:['A','B']}],events:[{},false,{what:' ',asked:false},{id:'share',when:'Now',what:'Share',asked:0}]});
for (const key of ['inputs','constants','values','events']) add('null item rejects '+key,{[key]:[null]});
add('null option rejects',{inputs:[{id:'choice',options:[null]}]});
add('null field rejects',{list:{fields:[null]}});
fixtures.push({name:'signed zero',reply:'{"inputs":[{"id":"x","value":-0},{"id":"t","kind":"time","value":-0.5}],"constants":[{"id":"c","value":-0}],"values":[{"id":"v","decimals":-0,"example":{"given":{"x":-0},"result":-0}}]}'});
// JSON number overflow is admitted as binary64 Infinity by both parsers.
fixtures.push({name:'nonfinite input fallback',reply:'{"inputs":[{"id":"x","min":1e999,"max":1e999,"value":1e999}],"constants":[{"id":"c","value":1e999}],"questions":[{"ask":"Pick","options":["A","B"],"default":1e999}],"values":[{"id":"a","example":{"given":{"x":1e999},"result":1e999}}]}'});
// Seeded shapes vary independent branches without relying on live generation.
let seed=173;
const pick=xs=>{seed=(Math.imul(seed,1664525)+1013904223)>>>0;return xs[seed%xs.length];};
for(let i=0;i<180;i++) add('varied '+i,{
    inputs:Array.from({length:5},()=>({id:pick(['a','a','duration','total','bad_id','x9']),kind:pick(['time','count','amount','choice','bad']),measure:pick(['minutes','seconds','money','clock','bad']),unit:pick(currencies),min:pick([-1,0,300]),max:pick([100,1439,1440,3600]),value:pick([-0.5,0,1,480,900]),options:[{label:'15 minutes',value:900},{label:'30 min',value:1800}]})),
    values:[{id:'total',format:pick(['clock','time_of_day','money','plain']),decimals:pick([0,1.5,6,7]),example:{given:{z:2,a:3},result:5}}],
    stopwatch:pick([null,{label:'Laps'}]),list:{label:pick(['Laps','Notes']),fields:[{id:'status',kind:pick(['text','number','time']),measure:'money',unit:'EUR'}]},
    questions:[{ask:' Which? ',options:['A','B'],default:pick([-1,0.5,3])}], ready:[{what:'read',need:'items'}]
});
const results=[];
for(const fixture of fixtures){
    let expected;
    try {expected=project(reference.parseSketch(fixture.reply));} catch(error){assert(error instanceof TypeError);expected={shapeRejected:true};}
    const actual=JSON.parse(generated.inspectSketch.$f(fixture.reply));
    assert.deepEqual(actual,expected,fixture.name);
    results.push({...fixture,expected});
}
const needsCases=JSON.parse(fs.readFileSync(path.join(outputPath,'cases.json'),'utf8')).filter(c=>!c.intentionalBoundaryDifference);
const integrated=[];
for(const {name,reply} of needsCases){
    const plan=needs.parseNeedsPlan(reply);
    const expected=plan===null?null:project(needs.sketchFrom(plan));
    assert.deepEqual(JSON.parse(generated.sketchFromNeeds.$f(reply)),expected,'needs→sketch '+name);
    integrated.push({name,reply,expected});
}
fs.writeFileSync(path.join(outputPath,'sketch-cases.json'),JSON.stringify(results,null,2)+'\n');
fs.writeFileSync(path.join(outputPath,'sketch-needs-cases.json'),JSON.stringify(integrated,null,2)+'\n');
// Sentinel checks keep the oracle/projection honest for consequential branches.
const byName = name => results.find(c => c.name === name).expected;
assert.equal(byName('one clock').hasStopwatch,false);
assert.equal(byName('choice starting value 900').inputs[0].value,1);
assert.equal(byName('choice starting value 900').inputs[0].measure,'seconds');
assert.equal(byName('explicit fields override legacy').list.fields.length,0);
assert.equal(byName('figure time_of_day 0').values[0].format,'clock');
assert.equal(byName('figure clock 0').values[0].format,'plain');
assert.equal(byName('null item rejects inputs').shapeRejected,true);
console.log(`PASS ${results.length} sketch normalization cases and ${integrated.length} needs→sketch cases`);
