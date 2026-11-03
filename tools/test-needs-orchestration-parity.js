#!/usr/bin/env node
'use strict';
const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path');
const [referencePath,outputPath]=process.argv.slice(2).map(p=>path.resolve(p));
const {loadVera}=require(path.join(referencePath,'tools/vera-harness/source-loader.js'));
const needs=loadVera('NeedsPlan'),compose=loadVera('VeraCompose'),choose=loadVera('NeedsChoose');
const generated=require(path.join(outputPath,'js/parity.js'));
// Record only language primitives while the actual reference executes its
// policy. Feeding these maps back into DEAL does not duplicate that policy.
function recordPrimitives(action){
    const normalized={},characters={},matches={};
    const normalize=String.prototype.normalize,lower=String.prototype.toLowerCase,test=RegExp.prototype.test;
    String.prototype.normalize=function(form){const text=String(this);const value=normalize.call(text,form);normalized[text]=lower.call(value);return value;};
    RegExp.prototype.test=function(text){const result=test.call(this,text);matches[this.source+'\n'+text]=result;return result;};
    let expected;
    try{expected=action();}finally{String.prototype.normalize=normalize;RegExp.prototype.test=test;}
    // Include all input/output characters; units() itself is a plain UTF-16 mechanism.
    return {expected,tools:{normalized,characters,matches}};
}
function addCharacters(tools,text){for(const c of text){tools.characters[c]=Array.from({length:c.length},(_,i)=>c.charCodeAt(i));}}
const filters=[];
function filterCase(name,args){
    const reply=args.reply;
    const {expected,tools}=recordPrimitives(()=>{
        if(args.operation==='asks')return {answer:needs.asksFor(args.request,args.quote)};
        const plan=needs.parseNeedsPlan(reply);
        let unscened=0,unasked=[],cut=[],questions=[];
        if(args.operation==='filter'){
            const original=needs.sketchFrom(plan);original.questions=[];
            if(!args.sceneAllowed)unscened=needs.plainScenes(plan);
            unasked=needs.dropUnasked(plan,args.request);
            if(args.widget)cut=needs.capForWidget(plan,5);
            questions=original.questions.map(q=>q.id);
        }
        const dropped=args.operation==='filter'?[]:args.operation==='drop'?needs.dropUnasked(plan,args.request):needs.capForWidget(plan,args.room);
        let document;const parse=compose.parseSketch;
        compose.parseSketch=text=>{document=JSON.parse(text);return parse(text);};
        try{needs.sketchFrom(plan);}finally{compose.parseSketch=parse;}
        if(args.operation==='filter')return {unscened,unasked,cut,document,questions};
        return {dropped,kept:plan.needs.map(n=>n.words),document};
    });
    addCharacters(tools,JSON.stringify(args)+Object.values(tools.normalized).join(''));
    const input=JSON.stringify({...args,tools});
    assert.deepEqual(JSON.parse(generated.inspectFilter.$f(input)),expected,name);
    filters.push({name,reply:input,expected});
}
for(const [request,quote] of [
    ['Keep notes','notes'],['Keep notes','remind every morning'],['Store contacts','contact storage'],
    ['Read the news','the and for'],['Anything',''],['Anything','!!!'],['ＳＴＯＲＥ ＮＯＴＥＳ','store notes'],
    ['CAFÉ résumé','café'],['東京の天気予報','天気予報'],['東京','京都の天気'],['รับข้อความ','ข้อความ'],
    ['カタカナ','カナ'],['한글 기록','기록'],['中文😀記錄','中文😀'],['ABC','ＡＢＣ'],['Straße','STRASSE'],
    ['A reminder','reminders unknown'],['hello','😀'],['fox','foxy'],['records','record']
])filterCase('asks '+request+' / '+quote,{operation:'asks',request,quote});
const plan=ns=>JSON.stringify({title:'Plan',needs:ns});
filterCase('drop only signed content/actions',{operation:'drop',request:'Keep notes',reply:plan([
    {type:'content',what:'Kept',from:'notes'},{type:'content',what:'Unsigned'},{type:'action',what:'Invented',from:'send notifications'},
    {type:'scene',what:'Not filtered',from:'unasked'},{type:'other',what:'Other'},
    {type:'content',what:'x'.repeat(65),from:'unrelated weather'}])});
const chain=[{type:'other',what:'Visible'},{type:'figure',id:'a',label:'A',shown:false,formula:'b + 1'},
    {type:'figure',id:'b',label:'B',shown:false,formula:'c + 1'},{type:'figure',id:'c',label:'C',formula:'1'}];
for(const room of [0,1,2,5])filterCase('transitive cut '+room,{operation:'cap',room,reply:plan(chain)});
filterCase('missing suppliers',{operation:'cap',room:1,reply:plan([{type:'figure',label:'Count',formula:'entries.count',shown:false},
    {type:'action',what:'At end',on:'countdown_end'},{type:'entries',label:'Rows'},{type:'countdown',label:'Timer'}])});
filterCase('supplier retained',{operation:'cap',room:2,reply:plan([{type:'stopwatch',label:'Watch'},
    {type:'figure',label:'Elapsed',formula:'clock.elapsed',shown:false},{type:'action',what:'Added',on:'entry_added'}])});
for(const formula of ['a + 1','data.a','aa','a.more','(a)','x_a','ba'])filterCase('id boundary '+formula,{operation:'cap',room:0,
    reply:plan([{type:'figure',id:'dependent',label:'Dependent',shown:false,formula},{type:'figure',id:'a',label:'Cut'}])});
for(const sceneAllowed of [false,true])filterCase('filter order '+sceneAllowed,{operation:'filter',sceneAllowed,widget:true,request:'Keep notes',reply:JSON.stringify({questions:[{ask:'Where?',options:['A','B']}],needs:[
    {type:'scene',what:'Moving'}, {type:'content',what:'Unasked',from:'weather reminder'},
    ...Array.from({length:5},(_,i)=>({type:'other',what:'Other '+i})), {type:'figure',label:'Count',formula:'entries.count',shown:false}]})});
// An emoji wholly inside the reference truncation boundary remains valid.
filterCase('whole scalar notice',{operation:'drop',request:'notes',reply:plan([{type:'content',what:'😀'.repeat(31),from:'unrelated'}])});
const starts=[];
for(const prompt of ['10 minutes','2 HOURS','90 secs','1.5-hour','0005 min','0.001 seconds','-2 hours','1.2.3 minutes',
    '10  minutes','10\\tminutes','10 minutesx','10 mins 600 seconds','1 hour 2 hours','1e2 minutes','minutes 10','0 seconds',
    '90 minutes','10 minutesé','10 minutes_','.5 min']){
    for(const input of [{id:'length',label:'Length',kind:'count',measure:'seconds',min:0,max:200000,value:0},
        {id:'length',label:'Length',kind:'amount',measure:'minutes',min:0,max:500,value:0},
        {id:'length',label:'Length',kind:'choice',measure:'seconds',value:0,options:[{label:'Short',value:1},{label:'Ten',value:600}]},
        {id:'length',kind:'text',measure:'seconds'}]){
        const sketch=JSON.stringify({inputs:[input]}),value=compose.parseSketch(sketch);
        const note=choose.startOnRequested(value,prompt),seconds=choose.requestedSeconds(prompt);
        const expected={seconds,values:value.inputs.map(i=>i.value),note};
        const numberTexts=Object.fromEntries(value.inputs.map(i=>[String(i.value),String(i.value)]));
        const reply=JSON.stringify({prompt,sketch,numberTexts});
        assert.deepEqual(JSON.parse(generated.inspectStart.$f(reply)),expected,'start '+prompt);
        starts.push({name:'start '+prompt+' '+input.kind,reply,expected});
    }
}
const partials=[];
for(const text of ['text','{','{"title":"A",','prefix {"needs":[{"type":"content","what":"X"},',
    '{"title":"a\\\"b","needs":[', '{"a":[1,2],"b":"half', '{} trailing', '{]']){
    const expected={closed:compose.closePartialJson(text)};
    assert.deepEqual(JSON.parse(generated.inspectPartial.$f(text)),expected);
    partials.push({name:'partial '+text,reply:text,expected});
}
// Exercise every streaming prefix of one plan (including escaped strings).
const stream=plan([{type:'figure',id:'sum',label:'Sum',formula:'1 + 2'},{type:'content',what:'A "quoted" list'}]);
for(let i=0;i<=stream.length;i++){
    const text=stream.slice(0,i),expected={closed:compose.closePartialJson(text)};
    assert.deepEqual(JSON.parse(generated.inspectPartial.$f(text)),expected);
    partials.push({name:'prefix '+i,reply:text,expected});
}
async function drawingCase(name,args){
    const trace=[];let cursor=0,captured=null;
    const originalCompose=compose.composeFromSketch;
    // Mock only the not-yet-ported next phase. drawPlan/review and preview
    // deduplication run in the reference unmodified.
    compose.composeFromSketch=async(result,prompt,sketch)=>{captured={signature:compose.sketchSignature(sketch),questions:sketch.questions.map(q=>q.id)};return result;};
    const ask=async(system,user,partial,thinking)=>{
        assert.equal(system,needs.NEEDS_PROMPT);
        trace.push({event:'ask',system:args.system,user,thinking});
        const reply=args.replies[cursor++];
        for(const chunk of reply.chunks||[])partial(chunk);
        if(reply.error)throw Object.assign(Error('operational'),{code:reply.error});
        return reply.text;
    };
    let expected;
    try{
        const result=await choose.composeNeeds(args.prompt,ask,async()=>'',s=>trace.push({event:'preview',signature:compose.sketchSignature(s)}),args.widget,
            async s=>{trace.push({event:'review',questions:s.questions.map(q=>q.id)});if(args.reviewError)throw Object.assign(Error('review interrupted'),{code:args.reviewError});return args.review||'';},args.scene,true);
        expected={trace,text:result.sketch,prompt:args.prompt+(result.replanned?'\n\n'+args.review:''),replanned:result.replanned,
            tries:result.planTries,note:result.note,signature:captured?.signature??null,questions:captured?.questions??[]};
    }catch(error){expected={trace,error:error.code};}
    finally{compose.composeFromSketch=originalCompose;}
    const heads=Object.fromEntries(args.replies.filter(r=>typeof r.text==='string').map(r=>[r.text,r.text.substring(0,200)]));
    const input=JSON.stringify({...args,heads});
    assert.deepEqual(JSON.parse(await generated.inspectDrawing.$f(input)),expected,name);
    return {name,reply:input,expected};
}
(async()=>{
    const valid=plan([{type:'figure',id:'sum',label:'Sum',formula:'1+2'}]);
    const questioned=JSON.stringify({title:'Question',questions:[{id:'where',ask:'Where?',options:['Here','There']}],needs:[]});
    const base={system:'reference needs prompt',prompt:'Do the task',widget:false,scene:'',review:''};
    const drawings=[];
    for(const [name,extra] of [
        ['first usable',{replies:[{text:valid}]}],['diagnostic then usable',{replies:[{text:'wrong'},{text:valid}]}],
        ['thinking third',{replies:[{text:null},{text:'bad'},{text:valid}]}],['exhaustion',{replies:[{text:null},{text:'bad'},{text:'still bad'}]}],
        ['preview dedupe',{replies:[{text:valid,chunks:[valid,valid]}]}],
        ['progressive previews',{replies:[{text:valid,chunks:['{\"title\":\"A\",',valid]}]}],
        ['operational propagates',{replies:[{error:'PROVIDER_HTTP'}]}],
        ['review declined',{replies:[{text:questioned}]}],
        ['review cancelled',{reviewError:'CANCELLED',replies:[{text:questioned}]}],
        ['no review without questions',{review:'ignored',replies:[{text:valid}]}],
        ['thinking operational error',{replies:[{text:'bad'},{text:null},{error:'CANCELLED'}]}],
        ['review redraw',{review:'Place: here',replies:[{text:questioned},{text:questioned}]}],
        ['review independent retries',{review:'Place: there',replies:[{text:null},{text:questioned},{text:'bad'},{text:null},{text:valid}]}],
        ['review redraw exhausted',{review:'Place: there',replies:[{text:questioned},{text:null},{text:null},{text:null}]}],
        ['widget scene note',{widget:true,scene:'moving wheel',replies:[{text:valid}]}],
        ['feedback clipped',{replies:[{text:'x'.repeat(250)},{text:valid}]}]
    ])drawings.push(await drawingCase(name,{...base,...extra}));
    for(const [name,cases] of [['filter',filters],['partial',partials],['drawing',drawings],['start',starts]])fs.writeFileSync(path.join(outputPath,name+'-cases.json'),JSON.stringify(cases,null,2)+'\n');
    console.log(`PASS ${filters.length} filtering, ${partials.length} streaming and ${drawings.length} retry/review, ${starts.length} requested-start cases`);
})().catch(e=>{console.error(e);process.exitCode=1;});
