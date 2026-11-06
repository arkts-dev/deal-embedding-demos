#!/usr/bin/env node
'use strict';
const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict');
const [reference,output]=process.argv.slice(2).map(p=>path.resolve(p));
const ts=require(process.env.VERA_TYPESCRIPT_PATH),{loadVera}=require(path.join(reference,'tools/vera-harness/source-loader.js'));
const original=fs.readFileSync(path.join(reference,'entry/src/main/ets/vera/VeraTemplateContent.ets'),'utf8')+'\nexport {checkedNumber,checkedCoordinate};';
const mod={exports:{}};new Function('require','module','exports',ts.transpileModule(original,{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2020}}).outputText)(n=>n==='./VeraTemplates'?loadVera('VeraTemplates'):{},mod,mod.exports);
const ref=mod.exports,generated=require(path.join(output,'reader-js/reader-runner.js')),cases=[];
async function check(name,payload,invoke){
    const samples=new Map(),strings=new Set(),save={},put=(key,args,result)=>{samples.set(key+JSON.stringify(args),{key,args,result});for(const arg of args)if(typeof arg==='string')strings.add(arg);if(typeof result==='string')strings.add(result);};
    for(const key of ['substring','charAt','lastIndexOf','trim','replace','match','toLowerCase'])save[key]=String.prototype[key];
    const test=RegExp.prototype.test,num=Number.prototype.toString,pfloat=global.parseFloat,pint=global.parseInt;
    String.prototype.toLowerCase=function(){const result=save.toLowerCase.call(this);put('lower',[String(this)],result);return result;};
    String.prototype.substring=function(a,b){const result=save.substring.call(this,a,b);put('slice',[String(this),a,b??String(this).length],result);return result;};
    String.prototype.charAt=function(a){const result=save.charAt.call(this,a);put('slice',[String(this),a,a+1],result);return result;};
    String.prototype.lastIndexOf=function(...args){strings.add(String(this));return save.lastIndexOf.apply(this,args);};
    String.prototype.trim=function(){strings.add(String(this));const result=save.trim.call(this);strings.add(result);return result;};
    String.prototype.replace=function(pattern,replacement){const result=save.replace.call(this,pattern,replacement);if(pattern instanceof RegExp)put('replace',[String(this),pattern.source,pattern.flags,replacement],result);return result;};
    String.prototype.match=function(pattern){const result=save.match.call(this,pattern);put('matches',[String(this),pattern.source,pattern.flags],result?[...result].map(x=>x??''):[]);return result;};
    RegExp.prototype.test=function(value){const result=test.call(this,value);put('matches',[value,this.source,this.flags],result?['match']:[]);return result;};
    Number.prototype.toString=function(...args){const result=num.apply(this,args);put('number',[Number(this)],result);return result;};
    global.parseFloat=function(value){const result=pfloat(value);put('parse',[value],result);return result;};global.parseInt=function(value,...args){const result=pint(value,...args);put('parse',[value],result);return result;};
    let expected;try{expected=await invoke();}finally{for(const key of Object.keys(save))String.prototype[key]=save[key];RegExp.prototype.test=test;Number.prototype.toString=num;global.parseFloat=pfloat;global.parseInt=pint;}
    for(const value of strings){put('length',[value],value.length);for(let i=0;i<=value.length;i++){const prefix=value.substring(0,i);if(prefix.isWellFormed())put('length',[prefix],i);}}
    for(let n=0;n<20;n++)put('number',[n],String(n));
    const reply=JSON.stringify({...payload,samples:[...samples.values()]});cases.push({name,reply,expected});
}
(async()=>{
for(const text of ['', 'Nanjing Road Pedestrian Street and the', 'a\\b"c\n\r__TOKEN__', '1234567890123456', '-176.5 cm', '31.2397','-91','144,104,100', '12--3', '🍃 alpha beta gamma'])for(const maximum of [6,12,24]){
    // Avoid deliberately splitting a surrogate: DEAL rejects that boundary;
    // the strict Unicode difference is already tracked separately.
    if(text.codePointAt(maximum-1)>0xffff)continue;
    await check('content text '+text+' '+maximum,{mode:'content-text',text,maximum},()=>({cut:ref.cutAtWord(text,maximum),escaped:ref.escapeForVeraString(text,maximum),integer:ref.sanitizeVeraInt(text,'fallback'),number:ref.checkedNumber(text),coordinate:ref.checkedCoordinate(text,90)}));
}
for(const spec of loadVera('VeraTemplates').fragments().flatMap(s=>s.listFields))for(const rows of [null,{},[],['alpha',' ',123], [['Name','176.5','31.2','90','true'],['Next',12,null,5],[]]])await check('content list '+spec.key+' '+cases.length,{mode:'content-list',spec,rows},()=>ref.expandListRows(rows,spec));
const catalogue=loadVera('VeraTemplates').fragments().filter(s=>s.listFields.length===0);
for(const generationMode of ['deepseek','server'])for(const reply of [null,'bad', '{}', JSON.stringify({title:'A real title',eyebrow:'Date',supporting:'Words',section1_title:'Actual name'})])for(const search of ['', 'Source data']){
    const kinds=catalogue.slice(0,3).map(s=>s.kind),specs=catalogue.map(s=>({kind:s.kind,contentFields:s.contentFields}));
    await check('content flow '+cases.length,{mode:'content-flow',generationMode,reply,search,kinds,specs},async()=>{
        const dispatched=[],filled=await ref.fillSectionsContent(kinds,'test',generationMode,search,async(system,user,budget)=>{dispatched.push({system,user,budget});return reply;});
        return {globals:[...filled.globalValues].map(([key,value])=>({key,value})),values:filled.instanceValues.map(m=>[...m].map(([key,value])=>({key,value}))),note:filled.note,dispatched};
    });
}
const fullCatalogue=loadVera('VeraTemplates').fragments(),specData=fullCatalogue.map(s=>({kind:s.kind,contentFields:s.contentFields,listFields:s.listFields,slots:s.slots.map(s=>({token:s.token,values:s.values}))}));
const pairs=m=>[...m].map(([key,value])=>({key,value}));
for(const spec of fullCatalogue){
    await check('block defaults '+spec.kind,{mode:'content-flow',defaults:true,kinds:[spec.kind],specs:specData},()=>({defaults:[pairs(ref.blockDefaults(spec.kind))]}));
    if(!spec.listFields.length)continue;
    for(const generationMode of ['deepseek','server'])for(const shape of ['missing','filled','wrong']){
        const kinds=['none',spec.kind,spec.kind,'not-a-kind'],data={title:'Specific title',eyebrow:'🍃 date',supporting:'A short line'};
        for(const i of [2,3]){
            for(const field of spec.contentFields)data['section'+i+'_'+field.key]=field.numeric?'17.5 cm':'Specific content and the';
            for(const list of spec.listFields)data['section'+i+'_'+list.key]=shape==='missing'?undefined:shape==='wrong'?{}:list.fallbackRows;
        }
        const reply='prefix 🍃\n'+JSON.stringify(data)+'\ntrailer',search='Real source data';
        await check('list content flow '+spec.kind+' '+generationMode+' '+shape,{mode:'content-flow',generationMode,reply,search,kinds,specs:specData},async()=>{
            const dispatched=[],filled=await ref.fillSectionsContent(kinds,'test',generationMode,search,async(system,user,budget)=>{dispatched.push({system,user,budget});return reply;});
            return {globals:pairs(filled.globalValues),values:filled.instanceValues.map(pairs),note:filled.note,dispatched};
        });
    }
}
const live=loadVera('VeraLive');
for(const search of ['', '2025 and 11.25 RUB at https://www.cbr.ru/daily/', '5. or 5, 6 then 1,2 RUB at https://www.cbr.ru/daily/', '5 and 1.2 RUB at https://www.cbr.ru/daily/'])for(const figure of ['5','1.2','11.25 RUB','-', 'RUB'])for(const url of ['https://www.cbr.ru/daily/','https://fake.invalid/a']){
    const entries=[['__SRC_URL_IDX__',url],['__SRC_NAME_IDX__','Invented'],['__L1_IDX__','Rate'],['__V1_IDX__',figure],['__L2_IDX__',''],['__V2_IDX__','99'],['__L3_IDX__','Other'],['__V3_IDX__','5'],['__QUERY_IDX__','']],request='Lookup actual rates 🍃';
    await check('live provenance '+cases.length,{mode:'content-provenance',values:entries.map(([key,value])=>({key,value})),search,figure,url,request},()=>{
        const values=new Map(entries),note=live.checkLiveProvenance(values,search,request);
        return {note,values:pairs(values),found:live.foundIn(figure,search),domain:live.domainOf(url)};
    });
}
const sectionSource=fs.readFileSync(path.join(reference,'entry/src/main/ets/vera/SectionFill.ets'),'utf8');
let sectionEvents=[];
const sectionHoleModule={exports:{}},sectionModule={exports:{}};
new Function('require','module','exports',ts.transpileModule(fs.readFileSync(path.join(reference,'entry/src/main/ets/vera/VeraHoles.ets'),'utf8'),{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2020}}).outputText)(n=>n==='./VeraCompiler'?{compileVeraSource(source){sectionEvents.push({type:'compile',source});}}:n.startsWith('./')?loadVera(n.slice(2)):{},sectionHoleModule,sectionHoleModule.exports);
new Function('require','module','exports',ts.transpileModule(sectionSource,{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2020}}).outputText)(name=>name==='./VeraHoles'?sectionHoleModule.exports:name.startsWith('./')?loadVera(name.slice(2)):{},sectionModule,sectionModule.exports);
for(const kinds of [[],['checklist'],['trend_chart'],['live_facts'],['filter_box','master_list']])for(const variant of ['normal','no-search','empty-search','cancel-search','cancel-content','server']){
    const generationMode=variant==='server'?'server':'deepseek',hasSearch=variant!=='no-search',realData=kinds.length===0,search=variant==='empty-search'?'No results.':'5 at https://www.cbr.ru/daily/',cancelAt=variant==='cancel-search'?1:variant==='cancel-content'?2:-1,reply=JSON.stringify({title:'Screen',section1_value1:'5'}),holeReply=JSON.stringify({holes:{rowMatches4:'return true;'}});
    await check('section fill '+kinds.join(',')+' '+variant,{mode:'content-sections',generationMode,hasSearch,realData,search,cancelAt,reply,holeReply,kinds,specs:fullCatalogue},async()=>{
        const events=[],mod=sectionModule;sectionEvents=events;let tick=0,cancellations=0;
        const request=new mod.exports.BlockRequest();Object.assign(request,{kinds,prompt:'original',work:'rewrite',mode:generationMode,realData,offset:2,slotValues:kinds.map(()=>new Map([['choice','true']])),globals:new Map([['__TITLE__','underlying'],['theme','blue']]),compose:()=> 'composed'});
        const calls=new mod.exports.BlockCalls();Object.assign(calls,{search:hasSearch?async(query,chars)=>{events.push({type:'search',query,chars});return search;}:null,
            content:async(system,user,budget)=>{events.push({type:'content',system,user,budget});return reply;},hole:async(system,user)=>{events.push({type:'hole',system,user});return holeReply;},
            sdkContext:async prompt=>{events.push({type:'sdk',prompt});return 'phone functions';},status:text=>events.push({type:'status',text}),cancelled:()=>++cancellations===cancelAt});
        const now=Date.now;Date.now=()=>{tick+=10;return tick;};let result;try{result=await mod.exports.fillSections(request,calls);}finally{Date.now=now;}
        return {...result,values:result.values.map(pairs),baseValues:result.baseValues.map(pairs),globals:pairs(result.globals),holeBodies:pairs(result.holeBodies),events};
    });
}
for(const item of cases){const actual=JSON.parse(await generated.inspectReader.$f(item.reply));assert.deepEqual(actual,JSON.parse(JSON.stringify(item.expected)),item.name);}
fs.writeFileSync(path.join(output,'content-cases.json'),JSON.stringify(cases,null,2));console.log('PASS '+cases.length+' catalogue content text/list cases');})().catch(e=>{console.error(e);process.exit(1);});
