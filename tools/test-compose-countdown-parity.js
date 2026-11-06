#!/usr/bin/env node
'use strict';
const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict');
const [reference,output]=process.argv.slice(2).map(p=>path.resolve(p));
const {loadVera}=require(path.join(reference,'tools/vera-harness/source-loader.js'));
const ts=require(process.env.VERA_TYPESCRIPT_PATH);
let source=fs.readFileSync(path.join(reference,'entry/src/main/ets/vera/VeraCompose.ets'),'utf8');
const start=source.indexOf('  if (sketch.hasCountdown) {',source.indexOf('comp.bindings.set(v.id, binding)',source.indexOf('export function readComposition(')));
const end=source.indexOf('\n  let effectIds:',start);
source+='\nexport function inspectCountdown(sketch: Sketch, answers: Record<string,Object>, selected: Object[]): Object {\n'+
    'let comp=new Composition(sketch); for(let binding of selected) { comp.bindings.set(binding.id,binding); }\n'+source.slice(start,end)+'\nreturn comp;\n}\n';
const mod={exports:{}};
new Function('require','module','exports',ts.transpileModule(source,{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2020}}).outputText)(name=>{
    if(name.startsWith('./'))return loadVera(name.slice(2));if(name==='@ohos.net.http')return {};throw Error(name);
},mod,mod.exports);
const ref=mod.exports,generated=require(path.join(output,'events-js/event-runner.js')),cases=[];
const pick=(choice,confidence=1)=>({choice,confidence});
function check(name,raw,answers={},bindings=[]){
    const text=JSON.stringify(raw),value=ref.parseSketch(text),before=value.inputs.length,numbers=[{number:5,text:'5'}],fixed=[];
    const toString=Number.prototype.toString,toFixed=Number.prototype.toFixed;
    Number.prototype.toString=function(...args){const result=toString.apply(this,args);numbers.push({number:Number(this),text:result});return result;};
    Number.prototype.toFixed=function(...args){const result=toFixed.apply(this,args);fixed.push({number:Number(this),text:result});return result;};
    let expected;
    try{
        const comp=ref.inspectCountdown(value,answers,bindings),input=comp.sketch.inputs[before]||null;
        expected={from:comp.countdownFrom,unit:comp.countdownUnit,input,widget:input?comp.widgets.get(input.id):'',defaulted:comp.defaulted,answers:comp.answers};
    }finally{Number.prototype.toString=toString;Number.prototype.toFixed=toFixed;}
    const reply=JSON.stringify({mode:'countdown',sketch:text,effects:[],served:[],answers,bindings,numbers,fixed});
    assert.deepEqual(JSON.parse(generated.inspectEvents.$f(reply)),JSON.parse(JSON.stringify(expected)),name);
    cases.push({name,reply,expected});
}
for(const number of [0,5,120,121,300])for(const kind of ['count','choice'])for(const unit of ['',null,'seconds','minutes','neither'])for(const confidence of [0.49,0.5]){
    const answers={'countdown.length':pick('length')};if(unit!==null)answers['unit.length']=pick(unit,confidence);
    check([number,kind,unit,confidence].join(' '),{countdown:'Tea',inputs:[{id:'length',kind,measure:'seconds',value:number,
        options:kind==='choice'?[{label:'First',value:number},{label:'Second',value:number+1}]:[]}]},answers);
}
check('missing length',{countdown:'Tea'});
check('collision',{countdown:'Tea',inputs:[{id:'countdownLength'}],list:{fields:[{id:'countdownLengthItem'}]}});
for(const source of ['remaining','elapsed','duration','unknown'])check('dependency '+source,{countdown:'Tea',values:[{id:'v',format:'seconds'}]},
    {'countdown.length':pick('v')},[{id:'v',a:'earlier',b:'',reads:[]},{id:'earlier',a:source,b:'',reads:[]}]);
check('constant',{countdown:'Tea',constants:[{id:'fixed',value:60,measure:'seconds'}]},{'countdown.length':pick('fixed')});
check('no countdown',{});
fs.writeFileSync(path.join(output,'countdown-cases.json'),JSON.stringify(cases,null,2)+'\n');
console.log(`PASS ${cases.length} countdown dependency/unit/default cases`);
