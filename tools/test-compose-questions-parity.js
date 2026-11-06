#!/usr/bin/env node
'use strict';
const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict');
const [reference,output]=process.argv.slice(2).map(p=>path.resolve(p));
const {loadVera}=require(path.join(reference,'tools/vera-harness/source-loader.js'));
const ref=loadVera('VeraCompose'),templates=loadVera('VeraTemplates');
const ts=require(process.env.VERA_TYPESCRIPT_PATH);
let source=fs.readFileSync(path.join(reference,'entry/src/main/ets/vera/VeraCompose.ets'),'utf8')+'\nexport { EFFECTS };\n';
const mod={exports:{}};
new Function('require','module','exports',ts.transpileModule(source,{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2020}}).outputText)(name=>{
    if(name.startsWith('./'))return loadVera(name.slice(2));if(name==='@ohos.net.http')return {};throw Error(name);
},mod,mod.exports);
const effects=mod.exports.EFFECTS,real=templates.vetoes().find(v=>v.key==='needs_real_data');
const catalogue={sections:templates.fragments().map(f=>({key:f.kind,description:f.criteria})),
    realDataInstructions:real.instructions,realDataCriteria:[...real.criteria].map(([key,description])=>({key,description}))};
const generated=require(path.join(output,'events-js/event-runner.js')),cases=[];
function check(name,raw,widget=false,settled=[],selected={}){
    const text=JSON.stringify(raw),value=ref.parseSketch(text),numbers=[];
    const toString=Number.prototype.toString;
    Number.prototype.toString=function(...args){const result=toString.apply(this,args);numbers.push({number:Number(this),text:result});return result;};
    let body;
    try{body=JSON.parse(ref.buildComposeRequest('test request',value,widget,settled,new Map(Object.entries(selected))));}
    finally{Number.prototype.toString=toString;}
    const expected={body,questionOrder:Object.keys(body.questions),optionOrder:Object.fromEntries(Object.entries(body.questions).map(([k,q])=>[k,Object.keys(q.criteria)]))};
    const sections=Object.entries(selected).map(([question,kind])=>({question,kind,owns:templates.fragments().find(f=>f.kind===kind)?.owns||''}));
    const lengths=Object.fromEntries(value.values.flatMap(v=>[v.formula,...v.formula]).map(s=>[s,s.length]));
    const reply=JSON.stringify({mode:'questions',sketch:text,effects,catalogue,sections,served:settled,prompt:'test request',widget,numbers,lengths});
    assert.deepEqual(JSON.parse(generated.inspectEvents.$f(reply)),expected,name);
    cases.push({name,reply,expected});
}
const base={title:'Tool',inputs:[{id:'a',label:'A',kind:'count',value:8},{id:'b',label:'B',kind:'count',value:2}],
    values:[{id:'v',label:'Result',means:'a result',formula:'a+b'}],other:['a slider for A','a table of figures'],ready:['Facts']};
for(const formula of ['a+b','max(0,a-b)','min(a,b,4)','-a','a/b','a<b','a<=b','a>b','a>=b','a==b','a!=b','sqrt(a)','Math.pow(a,2)','missing+1',''])
    for(const widget of [false,true])check('formula '+formula+' widget '+widget,{...base,values:[{...base.values[0],formula}]},widget);
for(const kind of ['amount','count','percent','choice','toggle','text','time'])check('control '+kind,{...base,inputs:[{id:'a',kind,options:[{label:'One',value:60},{label:'Two',value:120}],measure:'number',unit:'s'}]});
for(const measure of ['number','seconds','minutes','money','percent'])check('countdown '+measure,{...base,countdown:'Timer',
    inputs:[{id:'a',kind:'choice',measure,options:[{label:'Short',value:60},{label:'Long',value:120}]}],constants:[{id:'fixed',value:30,measure:'seconds'},{id:'zero',value:0,measure:'minutes'}],
    values:[{id:'v',format:'minutes',formula:'fixed/60'}]});
check('list stopwatch and events',{...base,stopwatch:'Laps',list:{label:'Laps',fields:[{id:'name',kind:'text'},{id:'n',kind:'number'},{id:'time',kind:'time'},{id:'flag',kind:'toggle'}]},
    events:[{id:'beep',when:'at zero',what:'beep',asked:true},{id:'extra',when:'on tap',what:'add event',asked:false}]},true);
check('questions and scenes',{...base,questions:[{ask:'Which time?',options:['Now','Later']}],other:['a scene the app draws itself: scenery','ordinary item']},true);
check('selected section ownership',base,false,[],{'ready.0':'key_facts'});
check('settled questions',base,true,['theme','asked.a','written.v','expr.v','ready.0','within.other.0'],{'ready.0':'key_facts'});
check('deadline',{...base,values:[{id:'v',format:'clock',formula:'a+b'}]});
check('empty',{});
fs.writeFileSync(path.join(output,'question-cases.json'),JSON.stringify(cases,null,2)+'\n');
console.log(`PASS ${cases.length} full composer request/order cases`);
