#!/usr/bin/env node
'use strict';
const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path');
const [reference,output]=process.argv.slice(2).map(p=>path.resolve(p));
const {loadVera}=require(path.join(reference,'tools/vera-harness/source-loader.js'));
const needs=loadVera('NeedsPlan'),compose=loadVera('VeraCompose'),choose=loadVera('NeedsChoose'),templates=loadVera('VeraTemplates');
const generated=require(path.join(output,'js/parity.js'));
// Actual catalogue data crosses the boundary, never reference eligibility or
// prebuilt question/options. The two implementations build those independently.
const real=templates.vetoes().find(v=>v.key==='needs_real_data');
const catalogue={sections:templates.fragments().map(f=>({key:f.kind,description:f.criteria})),
    realDataInstructions:real.instructions,realDataCriteria:[...real.criteria].map(([key,description])=>({key,description}))};
const cases=[];
function check(name,raw){
    const text=JSON.stringify(raw),plan=needs.parseNeedsPlan(text),sketch=needs.sketchFrom(plan);
    const numberTexts=[];
    for(const c of sketch.constants)numberTexts.push({value:c.value,text:String(c.value)});
    for(const i of sketch.inputs)for(const o of i.options)numberTexts.push({value:o.value,text:String(o.value)});
    const request=choose.buildChooseRequest('Make the requested screen',plan,sketch);
    const options=m=>m?[...m].map(([key,description])=>({key,description})):[];
    const body=request.body?JSON.parse(request.body):null;
    const questionKeys=body?Object.keys(body.questions):[];
    const criteriaKeys=Object.fromEntries(questionKeys.map(key=>[key,Object.keys(body.questions[key].criteria)]));
    const expected={body,questionKeys,criteriaKeys,settled:request.settled,
        timer:choose.timerMayTakeCountdown(plan),lists:choose.listSectionsFor(plan),description:compose.describeSketch(sketch),
        listNeeds:compose.listNeeds(sketch),clocks:options(compose.clockOptions(sketch)),listsOptions:options(compose.listOptions(sketch))};
    const reply=JSON.stringify({plan:text,prompt:'Make the requested screen',numberTexts,catalogue});
    assert.deepEqual(JSON.parse(generated.inspectChoice.$f(reply)),expected,name);
    cases.push({name,reply,expected});
}
check('empty',{});
const countdown={type:'countdown',label:'Timer',length:'duration'};
for(const constants of [[],[{id:'duration',label:'Duration',value:60,measure:'seconds'}],[{id:'other',value:1}],['primitive']]){
    for(const addition of [[],[{type:'action',what:'Ring',on:'countdown_end'}],
        [{type:'action',what:'Tap',on:'button'}], [{type:'figure',label:'Left',formula:'clock.remaining'}],
        [{type:'figure',label:'Other',formula:'duration+1'}]])check('timer '+JSON.stringify([constants,addition]),{constants,needs:[countdown,...addition]});
}
for(const action of ['', 'entry_added','row_tap','button','countdown_end']){
    for(const formula of ['', 'entries.total','entries.count','clock.duration','1+2']){
        check('list '+action+' '+formula,{needs:[{type:'entries',label:'Records',fields:[{id:'name',label:'Name',kind:'text'},
            {id:'score',label:'Score',kind:'number'},{id:'active',label:'Active',kind:'toggle'},{id:'when',label:'When',kind:'time'}]},
            ...(action?[{type:'action',what:'Act',on:action}]:[]),...(formula?[{type:'figure',id:'result',label:'Result',means:'Calculated',formula}]:[])]});
    }
}
check('ready ownership and all sections',{needs:[{type:'content',what:'Places'},{type:'content',what:'Contacts'},
    {type:'scene',what:'Moving picture'},{type:'other',what:'More'}]});
check('state description',{inputs:[{id:'pick',kind:'choice',label:'Pick',options:[{label:'Small',value:0.25},{label:'Huge',value:1e25}]},
    {id:'time',kind:'time',label:'Time'},{id:'text',kind:'text',label:'Message'},{id:'amount',kind:'amount',label:'Amount',unit:'kg'}],
    constants:[{id:'fixed',label:'Fixed',value:-3.5}],needs:[{type:'stopwatch',label:'Watch'},{type:'content',what:'Facts'}]});
check('duplicate fixed length ids',{constants:[{id:'duration',value:1},{id:'duration',value:2}],needs:[countdown]});
check('unicode and number spelling',{inputs:[{id:'select',kind:'choice',label:'選ぶ 😀',options:[{label:'Tiny',value:1e-7},{label:'Zero',value:-0}]}],
    needs:[{type:'content',what:'東京の場所'},{type:'entries',label:'記録'}]});
check('description no list fields',{needs:[{type:'entries',label:'Taps',fields:[]}]});
check('eight extra needs',{needs:[{type:'entries',label:'Rows'},...Array.from({length:12},(_,i)=>({type:'other',what:'Extra '+i}))]});
for(let i=0;i<60;i++)check('mixed '+i,{constants:i%2?[{id:'duration',label:'Duration',value:60+i}]:[],
    needs:[...(i%3?[countdown]:[]),...(i%4?[{type:'entries',label:'Notes'}]:[]),
        ...(i%5?[{type:'content',what:'Content '+i}]:[]),...(i%7?[{type:'figure',label:'Figure',formula:i%2?'entries.total':'clock.elapsed'}]:[]),
        ...(i%6?[{type:'action',what:'Action',on:i%2?'row_tap':'countdown_end'}]:[])]});
for(const body of ['', 'bad','null','[]','{}','{"answers":null}','{"answers":false}','{"answers":3}',
    '{"answers":"text"}','{"answers":[]}','{"answers":[{"choice":"timer"}]}','{"answers":{}}',
    '{"answers":{"clock.part":{"choice":"timer","confidence":0.49},"invented":{"choice":"anything"}}}']){
    const expected={answers:choose.readChoices(body)},reply=JSON.stringify({operation:'read',body});
    assert.deepEqual(JSON.parse(generated.inspectChoice.$f(reply)),expected,'read '+body);
    cases.push({name:'read '+body,reply,expected});
}
async function flow(name,extra){
    const planText=JSON.stringify({needs:[countdown,...(extra.noChoices?[]:[{type:'content',what:'Notes'}])]});
    const plan=needs.parseNeedsPlan(planText),sketch=needs.sketchFrom(plan);
    const request=choose.buildChooseRequest('Keep notes',plan,sketch);
    const args={body:request.body,settled:request.settled,...extra};
    const originalCompose=compose.composeFromSketch;
    const trace=[];let cursor=0,captured;
    // Only external model and downstream composer are mocked. Reference caller
    // decides retries, empty-vs-malformed behavior and which questions reopen.
    compose.composeFromSketch=async(result,prompt,sketch,ask,widget,answers,settled)=>{captured={answers,settled};return result;};
    let expected;
    try{
        await choose.composeNeeds('Keep notes',async()=>planText,async body=>{
            trace.push(body);const reply=args.replies[cursor++];if(reply.error)throw Object.assign(Error('operational'),{code:reply.error});return reply.text;
        });
        expected={trace,...captured};
    }catch(e){expected={trace,error:e.code};}
    finally{compose.composeFromSketch=originalCompose;}
    const reply=JSON.stringify(args);
    assert.deepEqual(JSON.parse(await generated.inspectChoiceFlow.$f(reply)),expected,name);
    return {name,reply,expected};
}
(async()=>{
    const flows=[];
    for(const [name,extra] of [
        ['no choices required',{noChoices:true,replies:[]}],['no answers twice',{replies:[{text:''},{text:''}]}],
        ['answer second',{replies:[{text:''},{text:'{"answers":{"ready.0":{"choice":"checklist"}}}'}]}],
        ['empty answers still settled',{replies:[{text:'{"answers":{}}'}]}],
        ['invalid not retried',{replies:[{text:'bad'}]}],['missing answers reopen',{replies:[{text:'{}'}]}],
        ['null answers reopen',{replies:[{text:'{"answers":null}'}]}],
        ['whitespace is not empty',{replies:[{text:'   '}]}],
        ['invalid second reopens',{replies:[{text:''},{text:'bad'}]}],
        ['partial low-confidence answers stay raw',{replies:[{text:'{"answers":{"ready.0":{"choice":"checklist","confidence":0.1}}}'}]}],
        ['array answers retained',{replies:[{text:'{"answers":[]}'}]}],
        ['provider failure',{replies:[{error:'PROVIDER_HTTP'}]}],['cancellation second',{replies:[{text:''},{error:'CANCELLED'}]}]
    ])flows.push(await flow(name,extra));
    fs.writeFileSync(path.join(output,'choice-cases.json'),JSON.stringify(cases,null,2)+'\n');
    fs.writeFileSync(path.join(output,'choice-flow-cases.json'),JSON.stringify(flows,null,2)+'\n');
    console.log(`PASS ${cases.length} candidate/question cases and ${flows.length} missing-answer flows`);
})().catch(error=>{console.error(error);process.exitCode=1;});
