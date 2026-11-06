#!/usr/bin/env node
'use strict';
const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict');
const [reference,output]=process.argv.slice(2).map(p=>path.resolve(p));
const {loadVera}=require(path.join(reference,'tools/vera-harness/source-loader.js'));
const ts=require(process.env.VERA_TYPESCRIPT_PATH);
let source=fs.readFileSync(path.join(reference,'entry/src/main/ets/vera/VeraCompose.ets'),'utf8')+'\nexport { EFFECTS, compositionParts, READY_KINDS, inputFields, entryFields, effectFields, inputLocals, formulaExpr, statFor, figureRow, heroText, inputsPart, clockFields, figuresOf, figuresPart, heroPart, summaryPart, sectionsRead, clockPart, callsNeedLocals, effectCallsAt, blockDefaults };\n';
const mod={exports:{}};
new Function('require','module','exports',ts.transpileModule(source,{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2020}}).outputText)(name=>{
    if(name.startsWith('./'))return loadVera(name.slice(2));if(name==='@ohos.net.http')return {};throw Error(name);
},mod,mod.exports);
const ref=mod.exports,generated=require(path.join(output,'reader-js/reader-runner.js')),cases=[];
const pick=(choice,confidence=1)=>({choice,confidence});
const observe=n=>Number.isNaN(n)?'#NaN':n===Infinity?'#Infinity':n===-Infinity?'#-Infinity':Object.is(n,-0)?'#-0':n;
function document(value){
    return JSON.parse(JSON.stringify(value,(_,v)=>v instanceof Map?[...v].map(([k,n])=>[k,observe(n)]):typeof v==='number'?observe(v):v));
}
const alignmentRef=loadVera('agent/AgentAlign'),alignmentSpecs=loadVera('VeraTemplates').fragments(),realQuestion=loadVera('VeraTemplates').vetoes().find(v=>v.key==='needs_real_data');
async function check(name,raw,answers={},custom=true,widget=false,bodyOverride){
    const text=JSON.stringify(raw),value=ref.parseSketch(text),numbers=[],fixed=[],matches={},lower={},lengths={},remainders=[];
    const toString=Number.prototype.toString,toFixed=Number.prototype.toFixed,match=String.prototype.match,test=RegExp.prototype.test,toLower=String.prototype.toLowerCase;
    Number.prototype.toString=function(...args){const result=toString.apply(this,args);numbers.push({number:Number(this),text:result});return result;};
    Number.prototype.toFixed=function(...args){const result=toFixed.apply(this,args);fixed.push({number:Number(this),text:result});return result;};
    String.prototype.match=function(pattern){const result=match.call(this,pattern);matches[pattern.source+'\n'+String(this)]=result||[];return result;};
    RegExp.prototype.test=function(text){const result=test.call(this,text);matches[this.source+'\n'+text]=result?['match']:[];return result;};
    String.prototype.toLowerCase=function(){const result=toLower.call(this);lower[String(this)]=result;lengths[String(this)]=String(this).length;return result;};
    const body=bodyOverride===undefined?JSON.stringify({answers}):bodyOverride;
    let expected,contractExpected,screenExpected,sourceExpected,assemblyExpected,previewExpected,defaults,upper={},first14={},parts,blocks,sectionsRead;const alignmentCases=[],providedCases=[];
    try{
        ref.setCustomSectionsEnabled(custom);
        const comp=ref.readComposition(body,value,widget);
        const inputPart=ref.inputsPart(comp,[...ref.inputFields(comp),...ref.clockFields(),...(comp.sketch.list?ref.entryFields(comp.sketch.list):[]),...ref.effectFields(comp)]);
        const figures=ref.figuresOf(comp);sectionsRead=ref.sectionsRead(comp);
        const figureParts=[ref.figuresPart(comp),ref.heroPart(comp,figures),ref.summaryPart(comp,figures)].map(p=>({kind:p.kind,functions:p.functions,viewBlock:p.viewBlock,ports:p.ports}));
        const clockPart=ref.clockPart(comp,[...ref.inputFields(comp),...ref.clockFields(),...(comp.sketch.list?ref.entryFields(comp.sketch.list):[]),...ref.effectFields(comp)]);
        const calls=['countdown_end','entry_added','button','row_button'].map(moment=>({moment,locals:ref.callsNeedLocals(comp,moment),calls:ref.effectCallsAt(comp,moment)}));
        sourceExpected={clockPart:{functions:clockPart.functions,viewBlock:clockPart.viewBlock},calls,figures,figureParts,inputPart:{kind:inputPart.kind,stateFields:inputPart.stateFields,functions:inputPart.functions,viewBlock:inputPart.viewBlock,initEntries:inputPart.initEntries,ports:inputPart.ports},fields:ref.inputFields(comp),entries:comp.sketch.list?ref.entryFields(comp.sketch.list):[],
            formulas:[...comp.bindings.values()].map(ref.formulaExpr),displays:comp.sketch.values.map(v=>({stat:ref.statFor(v,v.id),row:ref.figureRow(v,v.id),hero:ref.heroText(v,v.id)})),
            locals:ref.inputLocals(comp),effectFields:ref.effectFields(comp)};
        const screen=ref.screenPlan(comp);parts=ref.compositionParts(comp);blocks=ref.compositionBlocks(comp);
        assemblyExpected={complete:loadVera('ScreenPlan').assemblePlan(screen),partial:loadVera('ScreenPlan').assemblePlan(screen,true)};
        defaults=Object.fromEntries(blocks.map(b=>[b.kind,[...ref.blockDefaults(b.kind)].map(([key,value])=>({key,value}))]));
        for(const component of screen.components){const spec=component.spec,words=spec.tab||spec.kind.split('_').join(' '),label=words.substring(0,1).toUpperCase()+words.substring(1);upper[words]=label;first14[label]=label.substring(0,14);}
        screenExpected={widget:screen.widget,globals:[...screen.globals].map(([key,value])=>({key,value})),state:screen.state,
            components:screen.components.map(c=>({fill:c.fill,kind:c.spec.kind})),slots:screen.slots};
        previewExpected={source:ref.provisionalProgram(value,widget),plan:ref.sketchPlan(value)};
        const whole=ref.wholeScreenPlan(value,widget);
        contractExpected={parts:ref.partsContract(comp),only:ref.inputsOnlyForCustom(comp),inputs:ref.customInputs(comp),
            whole:{widget:whole.widget,globals:[...whole.globals].map(([key,value])=>({key,value})),slots:whole.slots.map(s=>({id:s.id,need:s.need,phone:s.phone,fill:s.fill,detail:s.detail}))}};
        expected={sketch:document(comp.sketch),original:document(value),deadline:comp.deadline,theme:comp.theme,
            widgets:[...comp.widgets].map(([id,value])=>({id,value})),bindings:[...comp.bindings].map(([id,b])=>({id,...b})),
            countdownFrom:comp.countdownFrom,countdownUnit:comp.countdownUnit,
            events:comp.events.map(e=>({id:e.id,effect:comp.effects.get(e.id),moment:comp.moments.get(e.id),args:[...comp.args.get(e.id)].map(([name,source])=>({name,source}))})),
            dropped:comp.dropped,defaulted:comp.defaulted,inputs:comp.inputs.map(i=>i.id),unbound:comp.unbound,unboundNeeds:comp.unboundNeeds,
            answers:comp.answers,provisional:comp.provisional,widget:comp.widget,figures:comp.figures,blocks:comp.blocks,readyKinds:comp.readyKinds,
            realData:comp.realData,custom:comp.custom,customReads:comp.customReads,phoneNeeds:comp.phoneNeeds,
            customEffects:[...comp.customEffects].map(([need,effect])=>({need,effect})),claimed:comp.claimed,replaced:comp.replaced};
        if(comp.events.length||comp.customEffects.size){for(const provided of [[],['calendar_add'],ref.EFFECTS.map(e=>e.id)]){
            const next=ref.readComposition(body,value,widget),gone=ref.dropProvided(next,provided);
            providedCases.push({provided,expected:{gone,events:next.events.map(e=>({id:e.id,effect:next.effects.get(e.id),moment:next.moments.get(e.id)})),customEffects:[...next.customEffects].map(([need,effect])=>({need,effect})),custom:next.custom,phoneNeeds:next.phoneNeeds,unboundNeeds:next.unboundNeeds,dropped:next.dropped}});
        }}
        if(comp.custom.length){for(const wide of [false,true])for(const selected of ['none','checklist','reminders']){
            alignmentRef.setAlignWide(wide);const next=ref.readComposition(body,value,widget),calls=[],candidates=alignmentRef.alignCandidates(next),readers=alignmentRef.readerKinds(next),kinds=alignmentRef.alignKinds(),replies=[];
            const now=Date.now;let tick=0;Date.now=()=>{tick+=10;return tick;};let result;
            try{result=await alignmentRef.alignLeftovers(next,'test',async request=>{
                const parsed=JSON.parse(request);calls.push(parsed);const answers={};
                for(const key of Object.keys(parsed.questions))answers[key]=key.startsWith('need.')?{choice:selected,confidence:0.75}:key.startsWith('whose.')?{choice:'written',probabilities:{own:0.19}}:{choice:'true',confidence:0.35};
                const reply=JSON.stringify({answers});replies.push(reply);return reply;
            });}finally{Date.now=now;}
            for(let n=0;n<40;n++)numbers.push({number:n,text:String(n)});fixed.push({number:0.75,text:'0.75'});
            alignmentCases.push({wide,replies,expected:{candidates,readers,kinds,calls,result,comp:{blocks:next.blocks,custom:next.custom,customReads:next.customReads,phoneNeeds:next.phoneNeeds,unboundNeeds:next.unboundNeeds,inputs:next.inputs,dropped:next.dropped,realData:next.realData,sketch:document(next.sketch),replaced:next.replaced}}});
        }}

    }finally{Number.prototype.toString=toString;Number.prototype.toFixed=toFixed;String.prototype.match=match;RegExp.prototype.test=test;String.prototype.toLowerCase=toLower;}
    for(const s of [...value.values.flatMap(v=>[v.formula,...v.formula]),...Object.keys(lower).flatMap(s=>s.toLowerCase().split(' '))])lengths[s]=s.length;
    for(const c of value.constants){numbers.push({number:c.value,text:String(c.value)});const s=c.measure==='minutes'?c.value*60:c.value;for(const b of [3600,60])remainders.push({a:s,b,value:s%b});numbers.push(...[s,s/60,s/3600,Math.round(s)].map(number=>({number,text:String(number)})));}
    const reply=JSON.stringify({mode:'reader',sketch:text,body,effects:ref.EFFECTS,served:[],widget,custom,numbers,fixed,matches,lower,lengths,remainders});
    const actual=JSON.parse(await generated.inspectReader.$f(reply));
    try{assert.deepEqual(actual,JSON.parse(JSON.stringify(expected)),name);}catch(e){fs.writeFileSync(path.join(output,'reader-failure.json'),JSON.stringify({name,reply:JSON.parse(reply),actual,expected},null,2));throw e;}
    cases.push({name,reply,expected});
    for(const entry of providedCases){const input=JSON.stringify({...JSON.parse(reply),mode:'provided',provided:entry.provided});
        assert.deepEqual(JSON.parse(await generated.inspectReader.$f(input)),JSON.parse(JSON.stringify(entry.expected)),name+' provided');cases.push({name:name+' provided',reply:input,expected:entry.expected});
    }
    for(const alignment of alignmentCases){
        const input=JSON.stringify({...JSON.parse(reply),mode:'alignment',wide:alignment.wide,replies:alignment.replies,catalogue:alignmentSpecs,readyKinds:ref.READY_KINDS,realQuestion:{instructions:realQuestion.instructions,criteria:[...realQuestion.criteria].map(([key,value])=>({key,value}))}});
        assert.deepEqual(JSON.parse(await generated.inspectReader.$f(input)),JSON.parse(JSON.stringify(alignment.expected)),name+' alignment');cases.push({name:name+' alignment',reply:input,expected:alignment.expected});
    }
    numbers.push(...Array.from({length:100},(_,number)=>({number,text:String(number)})));
    const previewReply=JSON.stringify({...JSON.parse(reply),mode:'preview',numbers,upper,first14,banner:loadVera('VeraTemplates').bannerSectionView()});
    assert.deepEqual(JSON.parse(await generated.inspectReader.$f(previewReply)),JSON.parse(JSON.stringify(previewExpected)),name+' preview');cases.push({name:name+' preview',reply:previewReply,expected:previewExpected});
    const contractReply=JSON.stringify({...JSON.parse(reply),mode:'contracts',numbers});
    assert.deepEqual(JSON.parse(await generated.inspectReader.$f(contractReply)),JSON.parse(JSON.stringify(contractExpected)),name+' contracts');
    cases.push({name:name+' contracts',reply:contractReply,expected:contractExpected});
    const sourceReply=JSON.stringify({...JSON.parse(reply),mode:'source',numbers,sectionsRead});
    assert.deepEqual(JSON.parse(await generated.inspectReader.$f(sourceReply)),JSON.parse(JSON.stringify(sourceExpected)),name+' source');
    cases.push({name:name+' source',reply:sourceReply,expected:sourceExpected});
    const partsReply=JSON.stringify({...JSON.parse(reply),mode:'parts',numbers,sectionsRead});
    const projectedParts=parts.map(({contentFields,covers,coversIf,listFields,slots,...part})=>{
        for(const field of [contentFields,covers,coversIf,listFields,slots])assert.deepEqual(field,[],'generated parts have no catalogue metadata');return part;
    });
    assert.deepEqual(JSON.parse(await generated.inspectReader.$f(partsReply)),JSON.parse(JSON.stringify({parts:projectedParts})),name+' parts');
    cases.push({name:name+' parts',reply:partsReply,expected:{parts:projectedParts}});
    const assemblyReply=JSON.stringify({...JSON.parse(reply),mode:'assembly',numbers,blocks,readyKinds:ref.READY_KINDS,defaults,upper,first14,banner:loadVera('VeraTemplates').bannerSectionView()});
    assert.deepEqual(JSON.parse(await generated.inspectReader.$f(assemblyReply)),JSON.parse(JSON.stringify(assemblyExpected)),name+' assembly');
    cases.push({name:name+' assembly',reply:assemblyReply,expected:assemblyExpected});
    const screenReply=JSON.stringify({...JSON.parse(reply),mode:'screen',numbers,parts,blocks,readyKinds:ref.READY_KINDS});
    assert.deepEqual(JSON.parse(await generated.inspectReader.$f(screenReply)),JSON.parse(JSON.stringify(screenExpected)),name+' screen');
    cases.push({name:name+' screen',reply:screenReply,expected:screenExpected});
}
const base={title:'Tool',inputs:[{id:'a',label:'A',kind:'count',value:8},{id:'b',label:'B',kind:'count',value:2}],
    values:[{id:'v',label:'Result',means:'a result',formula:'a+b',example:{given:{a:8,b:2},result:10}}]};
async function main(){
for(const custom of [false,true])for(const added of [false,true])for(const confidence of [0.49,0.5])await check('failed '+[custom,added,confidence],base,
    {'asked.value.v':pick(added?'added':'asked',confidence)},custom);
for(const custom of [false,true])await check('dependent failure '+custom,{...base,values:[base.values[0],{id:'later',label:'Later',formula:'v+b'}]},
    {'expr.later':pick('add:v:b'),'asked.value.v':pick('added'),'asked.value.later':pick('asked')},custom);
for(const own of [0.19,0.2])for(const ready of [0.49,0.5])for(const custom of [false,true])await check('ready '+[own,ready,custom],{...base,ready:['Facts']},
    {'ready.0':pick('key_facts',ready),'whose.0':pick('own',own),'real_data':pick('true',0.35)},custom);
for(const coverage of ['value.v','bar.v','widget.a.slider','figures','state','invalid'])for(const confidence of [0.49,0.5])await check('cover '+coverage+' '+confidence,
    {...base,other:['other need']},{'expr.v':pick('add:a:b'),'covers.0':pick(coverage,confidence)});
for(const format of ['plain','clock','money','percent','minutes','seconds'])await check('custom display '+format,{...base,values:[{...base.values[0],format,unit:'EUR'}]});
for(const kind of ['record_log','reminders'])for(const custom of [false,true])await check('replace list '+kind+' '+custom,{...base,list:{label:'Log',fields:[{id:'n',kind:'number',measure:'minutes'}]},
    values:[{id:'v',label:'Total',means:'the total',format:'minutes',formula:'entryTotal'}]}, {'expr.v':pick('copy:entryTotal:'),'list.part':pick(kind)},custom);
for(const length of [1,60,90,3600,7200])await check('replace clock '+length,{countdown:'Tea',constants:[{id:'fixed',value:length,measure:'seconds'}]},
    {'countdown.length':pick('fixed'),'clock.part':pick('timer')});
await check('clock blocked by effect',{countdown:'Tea',constants:[{id:'fixed',value:60,measure:'seconds'}],events:[{id:'e',what:'beep',when:'at zero'}]},
    {'countdown.length':pick('fixed'),'clock.part':pick('timer'),'effect.e':pick('beep'),'moment.e':pick('countdown_end')});
await check('list fields',{list:{label:'Log',fields:[{id:'first',kind:'text'},{id:'second',kind:'text'},{id:'n',kind:'number'},{id:'later',kind:'number'},{id:'flag',kind:'toggle'}]}},
    Object.fromEntries(['first','second','n','later','flag'].map(id=>['asked.field.'+id,pick('added')])));
await check('section owns value',{...base,ready:['Facts']},{'ready.0':pick('key_facts'),'within.value.v':pick('ready.0'),'expr.v':pick('add:a:b')});
await check('section owns event',{...base,ready:['Facts'],events:[{id:'e',what:'add event',when:'on tap'}]},
    {'ready.0':pick('key_facts'),'within.event.e':pick('ready.0'),'effect.e':pick('calendar_add'),'arg.e.calendar_add.title':pick('input.a')});
await check('scene',{other:['a scene the app draws itself: scene']});
for(const kind of ['toggle','text','choice','amount','percent','time','count'])for(const widget of [false,true])await check('input source '+kind+' '+widget,
    {inputs:[{id:'entryValue',label:'Set value',kind,value:2,unit:'EUR',options:[{label:'First',value:3},{label:'Next',value:6}]}],other:['uses Set value']},{},true,widget);
for(const widget of [false,true])for(const fields of [[],[{id:'word',kind:'text'},{id:'amount',kind:'number',measure:'minutes'},{id:'at',kind:'time'},{id:'flag',kind:'toggle'}]])await check('list emission '+widget+' '+fields.length,{list:{label:'Log',fields}}, {},true,widget);
await check('laps emission',{stopwatch:'Lap timer',list:{label:'Laps',fields:[]}});
for(const widget of [false,true])for(const moment of ['button','entry_added','row_button'])await check('calendar emission '+widget+' '+moment,
    {inputs:[{id:'titleInput',kind:'text'},{id:'start',kind:'time',value:600},{id:'daily',kind:'toggle',value:1},{id:'notice',kind:'count',measure:'seconds',value:120}],
        list:{label:'Log',fields:[{id:'title',kind:'text'},{id:'at',kind:'time'},{id:'again',kind:'toggle'},{id:'lead',kind:'number',measure:'seconds'}]},
        events:[{id:'add',what:'add calendar event',when:'on tap'},{id:'clear',what:'clear added calendar events',when:'on tap'}]},
    {'effect.add':pick('calendar_add'),'moment.add':pick(moment),'arg.add.calendar_add.title':pick(moment==='button'?'input.titleInput':'entry.title'),
        'arg.add.calendar_add.time':pick(moment==='button'?'input.start':'entry.at'),'arg.add.calendar_add.repeat':pick(moment==='button'?'input.daily':'entry.again'),
        'arg.add.calendar_add.remind':pick(moment==='button'?'input.notice':'entry.lead'),'effect.clear':pick('calendar_clear'),'moment.clear':pick('button')},true,widget);
for(const widget of [false,true])await check('navigation emission '+widget,{inputs:[{id:'place',kind:'text'},{id:'lat',kind:'amount'},{id:'lon',kind:'amount'}],events:[{id:'go',what:'navigate',when:'on tap'}]},
    {'effect.go':pick('navigate'),'moment.go':pick('button'),'arg.go.navigate.place':pick('input.place'),'arg.go.navigate.latitude':pick('input.lat'),'arg.go.navigate.longitude':pick('fixed.blank')},true,widget);
await check('empty',{});await check('malformed',{}, {},true,false,'{');await check('no answers',{}, {},true,false,'{}');
await check('questions cleared',{...base,questions:[{ask:'Which?',options:['A','B']}]},{'expr.v':pick('add:a:b')});
await check('deadline',{...base,ready:['Facts'],values:[{id:'v',label:'Time',format:'clock',formula:'a+b'}]},
    {'expr.v':pick('add:a:b'),'deadline.v':pick('yes'),'ready.0':pick('key_facts')});
const catalogue=loadVera('VeraTemplates').fragments(),slotReader=loadVera('VeraSectionCompose');
for(const kinds of [[],['missing'],...catalogue.map(s=>[s.kind]),catalogue.map(s=>s.kind)])for(const probability of [0.25,0.251,0.5]){
    const answers={},lower={};
    for(let i=0;i<kinds.length;i++){const spec=catalogue.find(s=>s.kind===kinds[i]);if(!spec)continue;for(const slot of spec.slots){const trimmed=slot.token.replace(/^__/,'').replace(/__$/,'').split('_IDX').join('');lower[trimmed]=trimmed.toLowerCase();answers['section'+(i+1)+'.'+spec.kind+'.'+trimmed.toLowerCase()]=slot.kind==='noul'?{noul:probability}:{choice:slot.values[slot.values.length-1],confidence:probability};}}
    const body=JSON.stringify({answers}),expected={request:JSON.parse(slotReader.buildSlotRequest('test',kinds)),values:slotReader.readSlotValues(body,kinds).map(m=>[...m].map(([key,value])=>({key,value})))};
    const specs=catalogue.map(s=>({kind:s.kind,slots:s.slots.map(slot=>({...slot,criteria:[...slot.criteria].map(([key,value])=>({key,value}))}))}));
    const reply=JSON.stringify({mode:'catalogue-slots',specs,kinds,body,lower});
    assert.deepEqual(JSON.parse(await generated.inspectReader.$f(reply)),expected,'catalogue slots '+kinds+' '+probability);cases.push({name:'catalogue slots '+kinds+' '+probability,reply,expected});
}
fs.writeFileSync(path.join(output,'reader-cases.json'),JSON.stringify(cases,null,2)+'\n');
console.log(`PASS ${cases.length} composition-reader integration cases`);
}
main().catch(e=>{console.error(e);process.exitCode=1;});
