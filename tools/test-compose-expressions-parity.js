#!/usr/bin/env node
'use strict';
const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path');
const [reference,output]=process.argv.slice(2).map(p=>path.resolve(p));
const {loadVera}=require(path.join(reference,'tools/vera-harness/source-loader.js'));
const compose=loadVera('VeraCompose'),generated=require(path.join(output,'js/parity.js'));
const cases=[],answerCases=[];
function check(name,raw){
    const text=JSON.stringify(raw),sketch=compose.parseSketch(text);
    const allQuestions=JSON.parse(compose.buildComposeRequest('Make this',sketch)).questions;
    const expected={results:sketch.values.map((v,i)=>{
        const questionKeys=Object.keys(allQuestions).filter(key=>['expr.','steps.','show.'].some(prefix=>key===prefix+v.id));
        const questions=Object.fromEntries(questionKeys.map(key=>[key,allQuestions[key]]));
        const criteriaKeys=Object.fromEntries(questionKeys.map(key=>[key,Object.keys(questions[key].criteria)]));
        return {id:v.id,options:[...compose.expressionsFor(sketch,i)].map(([key,description])=>({key,description})),questions,questionKeys,criteriaKeys};
    })};
    const numberTexts=sketch.constants.map(c=>({value:c.value,text:String(c.value)}));
    const reply=JSON.stringify({sketch:text,numberTexts});
    assert.deepEqual(JSON.parse(generated.inspectExpressions.$f(reply)),expected,name);
    cases.push({name,reply,expected});
}
const formats=['plain','count','money','percent','fraction','seconds','minutes','clock'];
const measures=['number','count','money','percent','seconds','minutes'];
for(const a of measures)for(const b of measures)for(const format of formats){
    check('units '+a+' '+b+' -> '+format,{inputs:[{id:'a',kind:'amount',label:'First',measure:a},{id:'b',kind:'amount',label:'Second',measure:b}],
        values:[{id:'result',label:'Result',format}]});
}
for(const kind of ['amount','count','percent','choice','time','toggle','text'])check('input '+kind,{inputs:[{id:'a',label:'Input',kind,options:['One','Two']}],
    values:formats.map((format,i)=>({id:'v'+i,label:'Value '+i,format}))});
check('no suppliers',{values:[{id:'result',label:'Result'}]});
check('constant labels',{constants:[{id:'tiny',label:'Tiny',value:1e-7},{id:'large',label:'Large',value:1e25},{id:'negative',label:'Negative',value:-3.5}],
    values:[{id:'result',format:'plain'}]});
for(const countdownLength of ['length','second','missing']){
    for(const formulas of [['second + 1','minutes * 60'],['second + 1','length + 1'],['max(second, 1)','1'],['second.more','1'],['secondsuffix','1']]){
        check('feeders '+countdownLength+' '+formulas.join('/'),{inputs:[{id:'minutes',kind:'count',measure:'minutes'}],countdown:'Timer',countdownLength,
            values:[{id:'length',label:'Length',format:'seconds',formula:formulas[0]},
                {id:'second',label:'Second',format:'seconds',formula:formulas[1]},{id:'after',label:'After',format:'seconds'}]});
    }
}
for(const stopwatch of ['', 'Watch'])for(const fields of [[],[{id:'word',kind:'text'}],
    [{id:'amount',label:'Cost',kind:'number',measure:'money'},{id:'later',label:'Ignored',kind:'number',measure:'count'}]]){
    check('list/clock '+stopwatch+' '+JSON.stringify(fields),{stopwatch,list:{label:'Records',fields},values:formats.map((format,i)=>({id:'v'+i,format}))});
}
for(let i=0;i<50;i++)check('mixed '+i,{inputs:[{id:'n',label:'Number',kind:i%2?'count':'text',measure:measures[i%measures.length]}],
    constants:[{id:'k',label:'Fixed',value:i-10,measure:measures[(i+1)%measures.length]}],countdown:i%3?'Clock':'',countdownLength:'first',
    list:i%4?{label:'Rows',fields:[{id:'item',label:'Item',kind:'number',measure:measures[(i+2)%measures.length]}]}:null,
    values:[{id:'first',format:formats[i%formats.length],formula:'n+k'},{id:'next',format:formats[(i+1)%formats.length]}]});
for(const count of [8,9,10,11])check('criterion limit '+count,{inputs:Array.from({length:count},(_,i)=>({id:'n'+i,label:'Number '+i,kind:'count',measure:'number'})),
    values:[{id:'result',format:'plain'}]});
assert.ok(cases.find(c=>c.name==='criterion limit 9').expected.results[0].questionKeys.includes('expr.result'));
assert.deepEqual(cases.find(c=>c.name==='criterion limit 10').expected.results[0].questionKeys,[]);
const special=x=>x==='#NaN'?NaN:x==='#Infinity'?Infinity:x==='#-Infinity'?-Infinity:x;
for(const above of [0.2,0.25,0.35,0.4,0.5,0.7]){
    for(const confidence of [undefined,null,'0.9',false,-1,0,above-0.00000001,above,above+0.00000001,1,2,'#NaN','#Infinity','#-Infinity']){
        for(const picked of ['yes','no','invented',null,42]){
            const answer={choice:picked};if(confidence!==undefined)answer.confidence=confidence;
            const actualAnswer={...answer};if(confidence!==undefined)actualAnswer.confidence=special(confidence);
            const log=[],result=compose.answerOf({test:actualAnswer},'test',['yes','no'],log,above);
            const fixed=typeof actualAnswer.confidence==='number'?actualAnswer.confidence.toFixed(2):'1.00';
            const expectedConfidence=typeof actualAnswer.confidence==='number'?confidence:1;
            const reply=JSON.stringify({answers:{test:answer},key:'test',allowed:['yes','no'],above,fixed,expectedConfidence});
            const expected={picked:result,log};
            assert.deepEqual(JSON.parse(generated.inspectAnswers.$f(reply)),expected,'threshold '+reply);
            answerCases.push({name:'threshold '+above+' '+confidence+' '+picked,reply,expected});
        }
    }
}
for(const answer of [undefined,null,false,0,'yes',[],['yes'],{}]){
    const answers={};if(answer!==undefined)answers.test=answer;
    const log=[],picked=compose.answerOf(answers,'test',['yes'],log);
    const reply=JSON.stringify({answers,key:'test',allowed:['yes'],above:0.25,fixed:'1.00',expectedConfidence:1}),expected={picked,log};
    assert.deepEqual(JSON.parse(generated.inspectAnswers.$f(reply)),expected);
    answerCases.push({name:'shape '+JSON.stringify(answer),reply,expected});
}
fs.writeFileSync(path.join(output,'expression-cases.json'),JSON.stringify(cases,null,2)+'\n');
fs.writeFileSync(path.join(output,'answer-cases.json'),JSON.stringify(answerCases,null,2)+'\n');
console.log(`PASS ${cases.length} expression-menu cases and ${answerCases.length} confidence/answer cases`);
