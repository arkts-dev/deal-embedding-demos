#!/usr/bin/env node
'use strict';
const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict');
const [reference,output]=process.argv.slice(2).map(p=>path.resolve(p));
const {loadVera}=require(path.join(reference,'tools/vera-harness/source-loader.js'));
const ts=require(process.env.VERA_TYPESCRIPT_PATH);
let source=fs.readFileSync(path.join(reference,'entry/src/main/ets/vera/VeraCompose.ets'),'utf8');
for(const name of ['paramSources','momentsOf'])source=source.replace('function '+name+'(','export function '+name+'(');
source+='\nexport { EFFECTS };\n';
// Execute the unchanged reference event loop in isolation, with catalogue
// setup left outside this observation. All policy statements remain original.
const start=source.indexOf('  let effectIds: string[] = []',source.indexOf('export function readComposition('));
const end=source.indexOf('\n  chooseSections(comp, answers)',start);
assert.ok(start>0&&end>start);
source+='\nexport function inspectEventLoop(sketch: Sketch, answers: Record<string,Object>, served: string[], widget: boolean): Object {\n'+
    'let comp = new Composition(sketch); comp.widget = widget;\n'+source.slice(start,end)+'\nreturn {comp, sectionInputs};\n}\n';
const mod={exports:{}};
new Function('require','module','exports',ts.transpileModule(source,{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2020}}).outputText)(name=>{
    if(name.startsWith('./'))return loadVera(name.slice(2));if(name==='@ohos.net.http')return {};throw Error(name);
},mod,mod.exports);
const ref=mod.exports,generated=require(path.join(output,'events-js/event-runner.js')),cases=[];
const pick=(choice,confidence=1)=>({choice,confidence});
function check(name,raw,answers={},custom=true,widget=false,served=[]){
    const text=JSON.stringify(raw),value=ref.parseSketch(text),numbers=[],fixed=[];
    const toString=Number.prototype.toString,toFixed=Number.prototype.toFixed;
    Number.prototype.toString=function(...args){const result=toString.apply(this,args);numbers.push({number:Number(this),text:result});return result;};
    Number.prototype.toFixed=function(...args){const result=toFixed.apply(this,args);fixed.push({number:Number(this),text:result});return result;};
    let expected;
    try{
        ref.setCustomSectionsEnabled(custom);
        const {comp,sectionInputs}=ref.inspectEventLoop(value,answers,served,widget);
        const questions=JSON.parse(ref.buildComposeRequest('test',value)).questions;
        expected={events:comp.events.map(e=>({id:e.id,effect:comp.effects.get(e.id),moment:comp.moments.get(e.id),
            args:[...comp.args.get(e.id)].map(([name,source])=>({name,source}))})),sectionInputs,dropped:comp.dropped,
            claimed:comp.claimed,custom:comp.custom,phoneNeeds:comp.phoneNeeds,unboundNeeds:comp.unboundNeeds,
            customEffects:[...comp.customEffects].map(([need,effect])=>({need,effect})),unbound:comp.unbound,answers:comp.answers,
            sources:ref.EFFECTS.flatMap(fx=>fx.params.map(p=>({effect:fx.id,param:p.name,
                options:[...ref.paramSources(value,p)].map(([key,description])=>({key,description}))}))),
            questions:Object.fromEntries(Object.entries(questions).filter(([k])=>/^(effect|arg|moment)\./.test(k)))};
    }finally{Number.prototype.toString=toString;Number.prototype.toFixed=toFixed;}
    for(const c of value.constants)numbers.push({number:c.value,text:String(c.value)});
    value.ready.forEach((_,i)=>numbers.push({number:i,text:String(i)}));
    const reply=JSON.stringify({sketch:text,effects:ref.EFFECTS,answers,served,widget,custom,numbers,fixed});
    assert.deepEqual(JSON.parse(generated.inspectEvents.$f(reply)),expected,name);
    cases.push({name,reply,expected});
}
const base={title:'Calendar',countdown:'Timer',list:{label:'Entries',fields:[{id:'title',kind:'text',label:'Title'},
    {id:'when',kind:'time',label:'When'},{id:'daily',kind:'toggle',label:'Daily'},{id:'lead',kind:'number',measure:'minutes',label:'Lead'},
    {id:'lat',kind:'number',label:'Latitude'}]},inputs:[{id:'title',kind:'text'},{id:'when',kind:'time'},{id:'repeat',kind:'toggle'},
    {id:'lead',kind:'count',measure:'seconds'},{id:'lat',kind:'count'},{id:'choice',kind:'choice',options:[{label:'One',value:1}]}],
    constants:[{id:'zero',value:0,measure:'minutes'},{id:'five',value:5,measure:'number'}],events:[{id:'go',when:'at the time',what:'add an event',asked:true}]};
for(const kind of ['text','time','toggle','number'])for(const measure of ['number','seconds','minutes','money'])
    check('sources '+kind+' '+measure,{...base,list:{label:'Entries',fields:[{id:'field',kind,measure}]}});
for(const effect of [...ref.EFFECTS.map(x=>x.id),'unavailable',''])for(const confidence of [0.24,0.25,1])
    check('effect '+effect+' '+confidence,base,{'effect.go':pick(effect,confidence),'moment.go':pick('button')});
for(const moment of ['countdown_end','entry_added','row_button','button','unavailable'])for(const source of ['entry.title','input.title','fixed.title','invalid']){
    const answers={'effect.go':pick('calendar_add'),'moment.go':pick(moment),
        'arg.go.calendar_add.title':pick(source),'arg.go.calendar_add.time':pick('input.when'),
        'arg.go.calendar_add.repeat':pick('fixed.once'),'arg.go.calendar_add.remind':pick('fixed.none')};
    check('calendar '+moment+' '+source,base,answers);
}
for(const confidence of [0.39,0.4])check('argument threshold '+confidence,base,{'effect.go':pick('navigate'),'moment.go':pick('button'),
    'arg.go.navigate.place':pick('input.title',confidence),'arg.go.navigate.latitude':pick('fixed.blank'),'arg.go.navigate.longitude':pick('fixed.blank')});
for(const asked of [false,true])for(const custom of [false,true])for(const widget of [false,true])for(const confidence of [0.69,0.7])
    check('missing '+[asked,custom,widget,confidence],{...base,events:[{...base.events[0],asked}]},{'detail.event.go':pick('detail',confidence)},custom,widget);
check('duplicate custom need',{...base,events:[base.events[0],{...base.events[0],id:'next'}]},Object.fromEntries(['go','next'].flatMap(id=>[
    ['effect.'+id,pick('calendar_add')],['moment.'+id,pick('entry_added')],['arg.'+id+'.calendar_add.title',pick('entry.title')],
    ['arg.'+id+'.calendar_add.time',pick('entry.when')],['arg.'+id+'.calendar_add.repeat',pick('fixed.daily')],
    ['arg.'+id+'.calendar_add.remind',pick('fixed.zero')]])));
check('two answers for entry',{...base,events:[base.events[0],{...base.events[0],id:'next',what:'another event'}]},Object.fromEntries(['go','next'].flatMap(id=>[
    ['effect.'+id,pick('calendar_add')],['moment.'+id,pick('entry_added')],['arg.'+id+'.calendar_add.title',pick('entry.title')],
    ['arg.'+id+'.calendar_add.time',pick('entry.when')],['arg.'+id+'.calendar_add.repeat',pick('fixed.daily')],
    ['arg.'+id+'.calendar_add.remind',pick('const.zero')]])));
for(const confidence of [0.49,0.5])check('section '+confidence,{...base,ready:['Calendar']},{'within.event.go':pick('ready.0',confidence),
    'effect.go':pick('calendar_add'),'arg.go.calendar_add.title':pick('input.title')},true,false,['ready.0']);
check('no list, no timer',{events:base.events});
check('zero minute constant absent',{...base,constants:[]});
fs.writeFileSync(path.join(output,'event-cases.json'),JSON.stringify(cases,null,2)+'\n');
console.log(`PASS ${cases.length} event source/question/binding cases`);
