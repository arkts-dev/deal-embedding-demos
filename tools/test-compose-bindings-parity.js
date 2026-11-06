#!/usr/bin/env node
'use strict';
const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict');
const [reference,output]=process.argv.slice(2).map(p=>path.resolve(p));
const {loadVera}=require(path.join(reference,'tools/vera-harness/source-loader.js'));
const ts=require(process.env.VERA_TYPESCRIPT_PATH),compose=loadVera('VeraCompose');
let source=fs.readFileSync(path.join(reference,'entry/src/main/ets/vera/VeraCompose.ets'),'utf8');
// Observe the actual value-binding loop before later pruning/defaults mutate
// it. No copied reference decision loop; expose only intermediate locals.
const start=source.indexOf('export function readComposition(');
const stop=source.indexOf('\n  if (sketch.hasCountdown) {',source.indexOf('comp.bindings.set(v.id, binding)',start));
assert.ok(stop>start);
source=source.slice(0,stop)+'\n return {comp, failed, shownBySection};\n'+source.slice(stop);
source=source.replace('function exampleNumbers(', 'export function exampleNumbers(');
const mod={exports:{}};
new Function('require','module','exports',ts.transpileModule(source,{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2020}}).outputText)(name=>{
    if(name.startsWith('./'))return loadVera(name.slice(2));if(name==='@ohos.net.http')return {};throw Error(name);
},mod,mod.exports);
const ref=mod.exports,generated=require(path.join(output,'js/parity.js')),cases=[];
const observe=n=>Number.isNaN(n)?'#NaN':n===Infinity?'#Infinity':n===-Infinity?'#-Infinity':Object.is(n,-0)?'#-0':n;
function check(name,raw,received={}){
    const text=JSON.stringify(raw),value=compose.parseSketch(text),matches={},numbers=[],fixed=[];
    const match=String.prototype.match,toString=Number.prototype.toString,toFixed=Number.prototype.toFixed;
    String.prototype.match=function(pattern){const result=match.call(this,pattern);matches[pattern.source+'\n'+String(this)]=result||[];return result;};
    Number.prototype.toString=function(...args){const result=toString.apply(this,args);numbers.push({number:Number(this),text:result});return result;};
    Number.prototype.toFixed=function(...args){const result=toFixed.apply(this,args);fixed.push({number:Number(this),text:result});return result;};
    let result,environments;
    try{
        result=ref.readComposition(JSON.stringify({answers:received}),value);
        environments=value.values.map((v,i)=>[...ref.exampleNumbers(result.comp,i)].map(([name,value])=>({name,value:observe(value)})));
    }finally{String.prototype.match=match;Number.prototype.toString=toString;Number.prototype.toFixed=toFixed;}
    const comp=result.comp;
    // The isolated port loop excludes theme/ready/widget setup, but uses its
    // actual served sections and answer trace suffix from that setup.
    const first=comp.answers.findIndex(a=>a.startsWith('show.'));
    const expected={bindings:[...comp.bindings].map(([id,b])=>({id,...b})),failed:[...result.failed].map(([id,reason])=>({id,reason})),
        shownBySection:result.shownBySection,answers:first<0?[]:comp.answers.slice(first),environments};
    const lengths=Object.fromEntries(value.values.flatMap(v=>[v.formula,...v.formula]).map(s=>[s,s.length]));
    // Numeric fragments discovered by the reference regex are parsed in DEAL.
    for(const list of Object.values(matches))for(const s of list)if(typeof s==='string')lengths[s]=s.length;
    // Expressions may format constants multiple times; unique samples suffice.
    for(const c of value.constants)numbers.push({number:c.value,text:String(c.value)});
    const served=value.ready.map((_,i)=>'ready.'+i).filter(k=>received[k]?.choice && received[k].choice!=='none');
    const reply=JSON.stringify({sketch:text,answers:received,lengths,numbers,fixed,matches,served,hasBlocks:comp.blocks.length>0});
    assert.deepEqual(JSON.parse(generated.inspectBindings.$f(reply)),expected,name);
    cases.push({name,reply,expected});
}
const base={inputs:[{id:'a',label:'A',kind:'count',value:8},{id:'b',label:'B',kind:'count',value:2}],values:[
    {id:'first',label:'First',formula:'a+b',example:{given:{a:8,b:2},result:10}},
    {id:'second',label:'Second',formula:'first*2',example:{result:20}}]};
for(const pick of ['add:a:b','subtract:a:b','multiply:a:b','divide:a:b','divide_up:a:b','copy:a:','round_up:a:','invalid','']){
    for(const confidence of [0.39,0.4,0.5,1])check(pick+' '+confidence,base,{'expr.first':{choice:pick,confidence},
        'written.first':{choice:'right',confidence:0.5},'written.second':{choice:'right',confidence:0.5}});
}
for(const more of [0.49,0.5])for(const written of [0.49,0.5])check('more '+more+' written '+written,base,{
    'expr.first':{choice:'add:a:b',confidence:1},'steps.first':{choice:'more',confidence:more},
    'written.first':{choice:'right',confidence:written},'written.second':{choice:'right',confidence:written}});
for(const formula of ['max(0,a-b)','ceil(a/b)','sqrt(a)','missing+1','a+b'])check('written override '+formula,{...base,values:[{id:'v',formula,example:{result:6}}]},
    {'expr.v':{choice:'subtract:a:b',confidence:1},'written.v':{choice:'right',confidence:1}});
for(const means of ['divide by 100','multiply by 2','subtract 60','unrelated'])check('stray '+means,{...base,values:[{id:'v',means}]},{'expr.v':{choice:'add:a:b',confidence:1}});
check('example own override',{...base,values:[base.values[0],{...base.values[1],example:{given:{first:7},result:14}}]},
    {'expr.first':{choice:'multiply:a:b',confidence:1},'written.second':{choice:'right',confidence:1}});
check('nothing to bind',{values:[{id:'v',formula:'2+1',example:{result:3}}]},{'written.v':{choice:'right',confidence:1}});
check('section owns value',{...base,ready:['Facts']},{'ready.0':{choice:'key_facts',confidence:1},'whose.0':{choice:'written',confidence:1},
    'within.value.first':{choice:'ready.0',confidence:0.5}});
for(const unit of ['seconds','minutes'])check('time '+unit,{inputs:[{id:'a',kind:'count',value:120,measure:'seconds'}],values:[{id:'v',format:unit}]},
    {'expr.v':{choice:'copy:a:',confidence:1},'show.v':{choice:'hero',confidence:0.25}});
fs.writeFileSync(path.join(output,'binding-cases.json'),JSON.stringify(cases,null,2)+'\n');
console.log(`PASS ${cases.length} value-binding/environment cases`);
