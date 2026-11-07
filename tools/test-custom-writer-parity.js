#!/usr/bin/env node
'use strict';
const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict');
const [reference,output]=process.argv.slice(2).map(p=>path.resolve(p));
const {loadVera}=require(path.join(reference,'tools/vera-harness/source-loader.js')),ts=require(process.env.VERA_TYPESCRIPT_PATH);
const original=fs.readFileSync(path.join(reference,'entry/src/main/ets/vera/agent/AgentCustom.ets'),'utf8');
const valid='<classes></classes><fields>c_x: number = 0;</fields><functions>f</functions><view>v</view>';
const generated=require(path.join(output,'writer-js/writer-runner.js')),cases=[];
async function check(name,replies,problems=[],verification=[],whole=false,phone=''){
    let checked=0,verified=0,time=0;const dispatched=[],matches={},lower={};
    // Explicit approved difference: three calls (initial, diagnostic, thinking),
    // rather than pinned reference's four. No other orchestration is replaced.
    const source=original.replace('const REPAIRS: number = 2','const REPAIRS: number = 1')+'\nexport {parseSection};\n';
    const mod={exports:{}};
    new Function('require','module','exports',ts.transpileModule(source,{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2020}}).outputText)(name=>{
        if(name==='../VeraCompiler')return {compileVeraSource:()=>({unreadFields:[]}),CompileError:class extends Error{}};
        if(name==='../VeraSmoke')return {smokeRun:()=>problems[checked++]||''};
        if(name==='../VeraSurfacePlay')return {playSurface:()=>''};
        if(name==='./AgentScene')return {SCENE_GUIDE:'scene',SCENE_TOKENS:6000};
        if(name.startsWith('../'))return loadVera(name.slice(3));throw Error(name);
    },mod,mod.exports);
    const request=new mod.exports.CustomRequest();Object.assign(request,{needs:['one','two'],prompt:'test',inputs:['input'],parts:['part'],facts:['fact'],phone,whole,compose:s=>s?'composed':'base',verify:async()=>verification[verified++]||'{}'});
    const exec=RegExp.prototype.exec,toLower=String.prototype.toLowerCase,now=Date.now;
    RegExp.prototype.exec=function(value){const match=exec.call(this,value),key=this.source.replaceAll(String.fromCharCode(92,34),'"').replaceAll(String.fromCharCode(92,47),'/')+'\n'+value;if(!matches[key])matches[key]=[];if(match)matches[key].push([...match]);return match;};
    String.prototype.toLowerCase=function(){const result=toLower.call(this);lower[String(this)]=result;return result;};Date.now=()=>time+=10;
    let result;try{result=await mod.exports.writeSection(request,'language',async(system,user,budget,think)=>{const i=dispatched.length;dispatched.push({system,user,budget,think});return replies[i]??null;});}finally{RegExp.prototype.exec=exec;String.prototype.toLowerCase=toLower;Date.now=now;}
    const remaining=3-result.attempts,expected={section:result.section?result.section.viewBlock:null,cannot:result.cannot,declined:[...result.declined].map(([key,value])=>({key,value})),missed:result.missed,thought:result.thought,attempts:result.attempts,failed:result.failed,ms:result.ms,dispatched,checked,verification:verified,remaining,splitRemaining:remaining-1,unrelatedRemaining:3};
    const reply=JSON.stringify({phone,whole,clock:false,list:false,needs:request.needs,replies,problems,verification,matches,lower});
    assert.deepEqual(JSON.parse(await generated.inspectWriter.$f(reply)),expected,name);cases.push({name,reply,expected});
}
(async()=>{await check('success',[valid]);await check('empty',[null]);await check('refused',['<cannot>No provider.</cannot>']);await check('format repair',['bad',valid]);await check('diagnostic thinking',['bad','bad',valid]);await check('exhausted',['bad','bad','bad']);await check('compile repair',[valid,valid],['broken','']);await check('partial retained',[valid,'bad','bad'],[],['{"answers":{"does.0":{"probabilities":{"yes":0.1}}}}']);await check('declined',[valid+'<cannot need="one">No provider.</cannot>']);await check('whole',[valid],[],[],true);await check('target repair',[valid.replace('f</functions>','sdk.call("invented", x)</functions>'),valid],[],[],false,'{"target":"alpha"}');
// Exercise the actual pinned hole orchestrator with real custom writing, a
// shared dispatch allowance inserted at the model mechanism boundary.
for(const whole of [false,true])for(const known of [[],['one'],['one','two']])for(const replies of [[valid],['<cannot>No provider.</cannot>',valid,valid],['bad','bad','bad']]){
    const cmod={exports:{}};let checks=0;
    new Function('require','module','exports',ts.transpileModule(original.replace('const REPAIRS: number = 2','const REPAIRS: number = 1'),{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2020}}).outputText)(name=>{
        if(name==='../VeraCompiler')return {compileVeraSource:()=>({unreadFields:[]}),CompileError:class extends Error{}};
        if(name==='../VeraSmoke')return {smokeRun:()=>{checks++;return '';}};if(name==='../VeraSurfacePlay')return {playSurface:()=>''};if(name==='./AgentScene')return {SCENE_GUIDE:'scene',SCENE_TOKENS:6000};return loadVera(name.slice(3));
    },cmod,cmod.exports);
    const hmod={exports:{}},hsource=fs.readFileSync(path.join(reference,'entry/src/main/ets/vera/agent/AgentHole.ets'),'utf8');
    new Function('require','module','exports',ts.transpileModule(hsource,{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2020}}).outputText)(name=>name==='./AgentCustom'?cmod.exports:name==='./AgentHost'?loadVera('agent/AgentHost'):name==='../ScreenPlan'?{...loadVera('ScreenPlan'),assemblePlan:()=> 'composed'}:loadVera(name.slice(3)),hmod,hmod.exports);
    const p=new (loadVera('ScreenPlan').ScreenPlan)(),records=[],lookups=[],dispatched=[];p.globals.set('__SUPPORTING__','promise');
    if(!whole)p.components.push(new (loadVera('ScreenPlan').PlanComponent)('part',cmod.exports.parseSection('<fields></fields><functions></functions><view>base</view>'),new Map()));
    const needs=['one','two'];for(const need of needs)p.addToWrite(need,need,true);const used=new Map();
    const host={language:'language',cannot:new Map(known.map(n=>[n,'Known unavailable.'])),looked:new Map(),askJev:async()=> '{}',lookup:async need=>{lookups.push(need);return '';},onHole:r=>records.push({needs:r.needs,outcome:r.outcome,attempts:r.attempts,whole:r.whole}),askCode:async(system,user,tokens,think)=>{
        // Need identities are read from the actual reference request's issued
        // list, not inferred from labels or scenarios.
        const asked=user.split('The section must do:\n- ')[1].split('\n\n')[0].split('\n- ');for(const n of asked)used.set(n,(used.get(n)||0)+1);
        dispatched.push({tokens,think});return replies[dispatched.length-1]??null;
    }};
    const result=await hmod.exports.writeHole(p,'test',['input'],host,['fact']);
    const expected={attempts:result?result.attempts:-1,failed:result?result.failed:'',slots:p.slots.map(s=>({fill:s.fill,by:s.by,detail:s.detail})),components:p.components.map(c=>({fill:c.fill,kind:c.spec.kind})),records,lookups,dispatched,remaining:Math.min(...needs.map(n=>3-(used.get(n)||0))),supporting:p.globals.get('__SUPPORTING__')};
    // No fixture here exhausts a split child: the separate budget checks
    // assert that the grouped call was already charged to each stable need.
    const lengths={'Known unavailable.':18,'No provider.':12},reply=JSON.stringify({mode:'hole',whole,known,replies,needs,phone:'',clock:false,list:false,problems:[],verification:[],matches:{},lower:{c_x:'c_x'},lengths});
    assert.deepEqual(JSON.parse(await generated.inspectWriter.$f(reply)),expected,'hole '+cases.length);cases.push({name:'hole '+cases.length,reply,expected});
}
for(const compile of ['', '2: expected View, found View[]'])for(const smoke of ['', '1: expected View[], found View'])for(const play of ['', '2: surface failed'])for(const scene of [false,true])for(const fields of [[],['plain'],['c_one'],['c_one','c_two']]){
    const order=[],source='first\n  second  ',mod={exports:{}};
    new Function('require','module','exports',ts.transpileModule(original+'\nexport {check};\n',{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2020}}).outputText)(name=>{
        if(name==='../VeraCompiler'){class CompileError extends Error{};return {CompileError,compileVeraSource:()=>{order.push('compile');if(compile)throw new CompileError(compile);return {unreadFields:fields};}};}
        if(name==='../VeraSmoke')return {smokeRun:()=>{order.push('smoke');return smoke;}};if(name==='../VeraSurfacePlay')return {playSurface:()=>{order.push('play');return play;}};
        if(name==='./AgentScene')return {};return loadVera(name.slice(3));
    },mod,mod.exports);
    const expected={problem:mod.exports.check(source,scene),order},reply=JSON.stringify({mode:'check',source,compile,smoke,play,scene,fields});
    assert.deepEqual(JSON.parse(await generated.inspectWriter.$f(reply)),expected,'check '+cases.length);cases.push({name:'check '+cases.length,reply,expected});
}
for(const needs of [[],['one','two']])for(const reply of ['', '{','{"answers":{}}',...[-1,0.49,0.5,1].map(yes=>JSON.stringify({answers:{'does.0':{probabilities:{yes}},'does.1':{probabilities:{yes:1}}}}))]){
    const ref=loadVera('agent/AgentClaims'),calls=[],ask=async body=>{calls.push(body);return reply;},undone=await ref.claimsUndone(needs,'test','source',ask),done=await ref.claimsDone(needs,'test','source',ask),plan=new (loadVera('ScreenPlan').ScreenPlan)();ref.putBack(plan,undone);
    const expected={undone,done,calls,slots:plan.slots.map(s=>({id:s.id,need:s.need,phone:s.phone,fill:s.fill,detail:s.detail}))},input=JSON.stringify({mode:'claims',needs,reply});
    assert.deepEqual(JSON.parse(await generated.inspectWriter.$f(input)),expected,'claims '+cases.length);cases.push({name:'claims '+cases.length,reply:input,expected});
}
const catalogueSpecs=loadVera('VeraTemplates').fragments(),holeSource=fs.readFileSync(path.join(reference,'entry/src/main/ets/vera/VeraHoles.ets'),'utf8');
for(const spec of catalogueSpecs.filter(s=>s.holes.length))for(const scenario of ['success','none','bad','repair','outside','exhaust','partial']){
    const kinds=['none',spec.kind,...spec.holes.map(h=>h.when).filter(Boolean)],sdk='phone functions',diagnostics=[],replies=[],lengths={};let compiled=0;
    class CompileError extends Error{constructor(diagnostics){super('compile');this.diagnostics=diagnostics;}}
    const mod={exports:{}};
    new Function('require','module','exports',ts.transpileModule(holeSource,{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2020}}).outputText)(name=>name==='./VeraCompiler'?{CompileError,compileVeraSource(){const ds=diagnostics[compiled++]||[];if(ds.length)throw new CompileError(ds.map(d=>({span:{start:{line:d.line}},message:d.message})));}}:name.startsWith('./')?loadVera(name.slice(2)):{},mod,mod.exports);
    const ref=mod.exports,all=ref.holesOf(kinds),model=all.filter(h=>h.spec.byModel),answer={holes:Object.fromEntries(model.map(h=>[h.key,'return true;']))};
    if(scenario==='none')replies.push(null);else if(scenario==='bad')replies.push('not json');else replies.push(JSON.stringify(answer));
    if(scenario==='repair'){diagnostics.push([{line:2,message:'bad body'}],[]);replies.push(JSON.stringify(answer));}
    if(scenario==='outside')diagnostics.push([{line:1,message:'outside'}]);
    if(scenario==='exhaust'||scenario==='partial'){diagnostics.push(...Array.from({length:3},()=>[{line:2,message:'bad body'}]));replies.push(JSON.stringify(answer),JSON.stringify(answer));if(scenario==='exhaust')diagnostics.push([{line:1,message:'still bad'}]);}
    const calls=[],build=bodies=>all.map(h=>'//@hole '+h.key+' begin\n'+(bodies.get(h.key)??h.spec.defaultBody)+'\n//@hole '+h.key+' end\n').join('');
    for(const h of model)lengths['return true;']=12;
    const result=await ref.fillHoles('test',kinds,async(system,user)=>{const n=calls.length;calls.push({system,user});return replies[n]??null;},build,null,sdk),pairs=m=>[...m].map(([key,value])=>({key,value}));
    const expected={keys:all.map(h=>h.key),bodies:pairs(result.bodies),note:result.note,repairs:result.repairs,calls,thoughts:calls.map((_,i)=>i===2),compiled,marked:ref.holeValues(kinds,result.bodies).map(pairs),bySlot:ref.holeBodiesBySlot(kinds,result.bodies).map(pairs)};
    const input=JSON.stringify({mode:'catalogue-holes',kinds,specs:catalogueSpecs.map(s=>({kind:s.kind,holes:s.holes})),values:null,sdk,replies,diagnostics,lengths});
    assert.deepEqual(JSON.parse(await generated.inspectWriter.$f(input)),expected,'catalogue holes '+spec.kind+' '+scenario);cases.push({name:'catalogue holes '+spec.kind+' '+scenario,reply:input,expected});
}
const repairRef=loadVera('VeraRepair');
for(const source of ['', 'alpha\nbeta\ngamma','  alpha\n  beta\n  gamma','same\nsame'])for(const reply of ['', '```vera\nreplacement\n```','<<<<<<< SEARCH\nbeta\n=======\nnew\n>>>>>>> REPLACE','<<<<<<< SEARCH\nsame\n=======\nnew\n>>>>>>> REPLACE','<<<<<<< SEARCH\n beta \n=======\nnew\n>>>>>>> REPLACE','<<<<<<< SEARCH\nmissing\n=======\nnew\n>>>>>>> REPLACE','<<<<<<< SEARCH\nbeta','<<<<<<< SEARCH\nbeta\n=======\nnext\n>>>>>>> REPLACE\n<<<<<<< SEARCH\nnext\n=======\nfinal\n>>>>>>> REPLACE'])for(const diagnostics of ['2:3 E123 wrong','The program compiled, but failed','0:0 E invalid\n3:1 E thing\n4:0 E outside']){
    const expected={...repairRef.applyRepair(source,reply),quoted:repairRef.quoteDiagnostics(source,diagnostics),prompt:repairRef.repairPrompt('Request: test',diagnostics,true,source)},input=JSON.stringify({mode:'repair-text',source,reply,diagnostics});
    assert.deepEqual(JSON.parse(await generated.inspectWriter.$f(input)),expected,'provisional repair '+cases.length);cases.push({name:'provisional repair '+cases.length,reply:input,expected});
}
const settlementSource=fs.readFileSync(path.join(reference,'entry/src/main/ets/vera/agent/AgentRepair.ets'),'utf8');
for(const source of ['good','bad','runtime'])for(const replies of [[null],['good'],['<<<<<<< SEARCH\nabsent\n=======\ngood\n>>>>>>> REPLACE','good']])for(const variant of ['normal','no-model','cancelled','no-language']){
    const events=[],mod={exports:{}};class CompileError extends Error{}
    new Function('require','module','exports',ts.transpileModule(settlementSource,{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2020}}).outputText)(name=>name==='../VeraCompiler'?{CompileError,compileVeraSource(source){events.push({type:'compile',source});if(source==='bad')throw new CompileError('1:1 E bad');return {vbc2:'compiled:'+source};}}:name==='../VeraSmoke'?{smokeRun(compiled){events.push({type:'smoke',identity:compiled.vbc2});return compiled.vbc2==='compiled:runtime'?'The program compiled, but failed':'';}}:name==='../VeraRepair'?repairRef:{},mod,mod.exports);
    let cursor=0,tick=0;const language=variant==='no-language'?'':'language',hasModel=variant!=='no-model',cancelled=variant==='cancelled',host={language,askCode:hasModel?async(system,user,tokens,think)=>{events.push({type:'ask',system,user,tokens,think});return replies[cursor++]??null;}:null,cancelled:()=>cancelled,status:text=>events.push({type:'status',text})},now=Date.now;Date.now=()=>{tick+=10;return tick;};let result;try{result=await mod.exports.settleProgram(source,'test',host);}finally{Date.now=now;}
    const expected={source:result.source,executable:result.vbc2,errors:result.errors,repair:result.repair?{source:result.repair.source,executable:result.repair.vbc2,rounds:result.repair.rounds,failed:result.repair.failed,ms:result.repair.ms}:null,checkMs:result.checkMs,note:result.note,events},input=JSON.stringify({mode:'program-settlement',source,replies,language,hasModel,cancelled});
    assert.deepEqual(JSON.parse(await generated.inspectWriter.$f(input)),expected,'program settlement '+cases.length);cases.push({name:'program settlement '+cases.length,reply:input,expected});
}
const questionRef=loadVera('agent/AgentQuestions'),parseSketch=loadVera('VeraCompose').parseSketch;
for(const reply of ['bad','{}',...[-1,0.59,0.6,1].map(ask=>JSON.stringify({answers:{'blocking.0':{probabilities:{ask}},'blocking.1':{probabilities:{ask:1}}}}))])for(const picked of [null,-1,0,1,9,'  typed answer  ','  ']){
    const sketch=JSON.stringify({questions:[{ask:'Which?',options:['One','Two'],fallback:0},{ask:'When?',options:['Now','Later'],fallback:1}]}),value=parseSketch(sketch),log=[],shown=[];let tick=0;const now=Date.now;Date.now=()=>{tick+=10;return tick;};let result;try{result=await questionRef.askBlocking('test',value.questions,async()=>reply,async q=>{shown.push(q);return picked;},log);}finally{Date.now=now;}
    const expected={request:JSON.parse(questionRef.blockingRequest('test',value.questions)),result,log,shown},input=JSON.stringify({mode:'questions',sketch,reply,picked});
    assert.deepEqual(JSON.parse(await generated.inspectWriter.$f(input)),JSON.parse(JSON.stringify(expected)),'questions '+cases.length);cases.push({name:'questions '+cases.length,reply:input,expected});
}
const surfaceRef=loadVera('agent/AgentSurface'),surfaceCatalogue=loadVera('VeraUiCatalog').describeCatalogForPrompt(new Set(['AppTheme','Surface','Column','Row','Text','Header','Button','ActionBar']));
for(const prompt of ['todo list','snake game','signature form','画板','balloon','play music'])for(const reply of ['bad','{"surface":true,"title":" A ","design":" Play "}','{"surface":"part","scene":" wheel "}','{"surface":"whole"}','```vera\nsource\n```'])for(const language of ['base','prefix {{UI_CATALOG}} old Clock -- time. rest']){
    const tests={},test=RegExp.prototype.test;RegExp.prototype.test=function(value){const result=test.call(this,value);tests[this.source+'\n'+value]=result;return result;};let looks;try{looks=surfaceRef.looksLikeSurface(prompt);}finally{RegExp.prototype.test=test;}
    const expected={brief:surfaceRef.parseSurfaceBrief(reply),looks,source:surfaceRef.sourceIn(reply),language:surfaceRef.surfaceLanguage(language,'guide'),classify:surfaceRef.SURFACE_CLASSIFY_PROMPT,components:['AppTheme','Surface','Column','Row','Text','Header','Button','ActionBar']},input=JSON.stringify({mode:'surface',prompt,reply,language,catalogue:surfaceCatalogue,tests});
    assert.deepEqual(JSON.parse(await generated.inspectWriter.$f(input)),JSON.parse(JSON.stringify(expected)),'surface '+cases.length);cases.push({name:'surface '+cases.length,reply:input,expected});
}
for(const replies of [['good'],['bad','good'],['runtime','good'],[null,'good'],['bad','bad','good']])for(const cancelled of [false,true]){
    const order=[],dispatches=[],statuses=[],mod={exports:{}},repairMod={exports:{}};class CompileError extends Error{}
    const compiler={CompileError,compileVeraSource(source){order.push('compile:'+source);if(source==='bad')throw new CompileError('1:1 E bad');return {vbc2:'compiled:'+source};}},smoke={smokeRun(compiled){order.push('smoke:'+compiled.vbc2);return '';}};
    new Function('require','module','exports',ts.transpileModule(settlementSource,{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2020}}).outputText)(name=>name==='../VeraCompiler'?compiler:name==='../VeraSmoke'?smoke:name==='../VeraRepair'?repairRef:{},repairMod,repairMod.exports);
    const surfaceSource=fs.readFileSync(path.join(reference,'entry/src/main/ets/vera/agent/AgentSurface.ets'),'utf8');
    new Function('require','module','exports',ts.transpileModule(surfaceSource,{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2020}}).outputText)(name=>name==='../VeraCompiler'?compiler:name==='../VeraSmoke'?smoke:name==='../VeraSurfacePlay'?{playSurface(compiled){order.push('play:'+compiled.vbc2);return compiled.vbc2==='compiled:runtime'?'The program compiled, but failed':'';}}:name==='./AgentRepair'?repairMod.exports:name.startsWith('../')?loadVera(name.slice(3)):{},mod,mod.exports);
    let cursor=0,tick=0;const now=Date.now;Date.now=()=>{tick+=10;return tick;};let result;try{result=await mod.exports.writeSurfaceApp('test',{kind:'whole',title:'Game',design:'Design',scene:''},{language:'language',surfaceGuide:'guide',cancelled:()=>cancelled,status:text=>statuses.push(text),askCode:async(system,user,tokens,think)=>{const at=cursor++;dispatches.push({system,user,tokens,think});return replies[at]??null;}});}finally{Date.now=now;}
    // Approved stable budget changes only final thinking in these <=3-call cases.
    const expected={source:result.source,executable:result.vbc2,attempts:result.attempts,rounds:result.rounds,failed:result.failed,writeMs:result.writeMs,checkMs:result.checkMs,note:result.note,dispatches:dispatches.map((call,i)=>({...call,think:call.think||i===2})),order,statuses,remaining:3-dispatches.length};
    const input=JSON.stringify({mode:'surface',action:'write',replies,cancelled,catalogue:surfaceCatalogue});assert.deepEqual(JSON.parse(await generated.inspectWriter.$f(input)),expected,'surface write '+cases.length);cases.push({name:'surface write '+cases.length,reply:input,expected});
    if(!cancelled&&replies[0]==='good'){
        const exhausted=JSON.stringify({...JSON.parse(input),replies:['bad','bad','bad','good']}),result=JSON.parse(await generated.inspectWriter.$f(exhausted));
        assert.equal(result.source,'');assert.equal(result.remaining,0);assert.equal(result.dispatches.length,3);assert.deepEqual(result.dispatches.map(x=>x.think),[false,false,true]);assert.equal(result.rounds,2);
        cases.push({name:'surface stable exhaustion',reply:exhausted,expected:result});
    }
}
for(const used of [0,1,2,3])for(const replies of [['good'],['bad','bad'],[null]]){
    const payload={mode:'program-settlement',action:'budget',source:'bad',language:'language',hasModel:true,cancelled:false,used,replies},reply=JSON.stringify(payload),result=JSON.parse(await generated.inspectWriter.$f(reply)),asks=result.events.filter(e=>e.type==='ask'),count=Math.min(2,3-used,replies[0]==='bad'?2:1);
    assert.equal(asks.length,count);assert.equal(result.remaining,3-used-count);assert.deepEqual(asks.map((_,i)=>asks[i].think),asks.map((_,i)=>used+i===2));assert.deepEqual(asks.map(e=>e.tokens),asks.map((_,i)=>used+i===2?6000:2000));
    assert.equal(result.errors==='',count>0&&replies[0]==='good');cases.push({name:'construction settlement shared budget '+used+' '+JSON.stringify(replies),reply,expected:result});
}
for(const operational of [false,true]){
    const reply=JSON.stringify({mode:'budget',operational,needs:['one','two'],phone:'',whole:false,clock:false,list:false,replies:[],matches:{},lower:{},verification:[],problems:[]});
    const expected=operational?{attempts:[],failures:['TRANSPORT','TRANSPORT','TRANSPORT'],dispatched:[false,false,true],finalAttempts:0,remaining:0,siblingRemaining:2,unrelatedRemaining:3}:
        {attempts:[3,0,0],failures:[],dispatched:[false,false,true],finalAttempts:0,remaining:0,siblingRemaining:0,unrelatedRemaining:3};
    assert.deepEqual(JSON.parse(await generated.inspectWriter.$f(reply)),expected,'request budget '+operational);cases.push({name:'request budget '+operational,reply,expected});
}
// Execute pinned VeraSmoke policy with scripted VM/decoder mechanisms. This
// compares ordering/diagnostics, not a substitute interpreter or Android proof.
const smokeSource=fs.readFileSync(path.join(reference,'entry/src/main/ets/vera/VeraSmoke.ets'),'utf8');
for(const scenario of ['ok','empty','one','init','view','decode','surface','gfx','ordinary','runaway','redraw','history'])for(const requireContent of [false,true]){
    const events=[],executions=[],frames=[],graphics=[],handlers=Array.from({length:scenario==='history'?8:4},(_,i)=>({name:'h'+i,kind:['number','pointer','boolean','string'][i%4]}));
    const trap={code:scenario==='runaway'?'R0017':'R0001',message:'broken',line:7,loopLine:3};
    let count=0,draws=0,allowed=false;const queue=(name,args)=>{
        events.push({type:'execute',name,args});let fault=null;
        if(scenario==='init'&&name==='init'||scenario==='view'&&name==='view'||scenario==='surface'&&name==='draw')fault=trap;
        if(scenario==='ordinary'&&name==='h0')fault=trap;
        if(scenario==='runaway'&&name==='h1'||scenario==='history'&&name==='h7')fault={...trap,code:'R0022'};
        const value=name==='init'?'s0':name==='view'?'v'+draws++:name==='draw'?'gfx':'s'+ ++count;
        executions.push({value,trap:fault});if(fault)throw Object.assign(new RuntimeTrap(),{trapCode:fault.code,message:fault.message,line:fault.line,loopLine:fault.loopLine});return value;
    };
    class RuntimeTrap extends Error{}
    class NumberValue{constructor(v){this.handle='number:'+v;}}
    class StringValue{constructor(v){this.handle='string:'+v;}}
    class BooleanValue{constructor(v){this.handle='boolean:'+v;}}
    class Host{constructor(){this.allowed=false;}set effectsAllowed(value){this.allowed=value;events.push({type:'effects',allowed:value});}set pendingEffects(value){events.push({type:'clear'});}}
    class VM{constructor(program,host){this.host=host;}execute(name,args=[]){return queue(name,args.map(v=>typeof v==='string'?v:v.handle));}}
    const mod={exports:{}};new Function('require','module','exports',ts.transpileModule(smokeSource,{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2020}}).outputText)(name=>{
        if(name==='./VeraInterpreter')return {VirtualMachine:VM,deserializeVbc2:()=>null,RuntimeTrap,VeraNumberValue:NumberValue,VeraStringValue:StringValue,VeraBooleanValue:BooleanValue};
        if(name==='./VeraUi')return {UiHostEnvironment:Host,decodeVeraUi:value=>{
            events.push({type:'ui',value});const fault=scenario==='decode'||scenario==='redraw'&&count>0?trap:null;
            const node={kind:'text',label:'',children:[]};const nodes=scenario==='empty'?[]:scenario==='one'?[node]:[{kind:'column',label:'',children:[['surface','gfx'].includes(scenario)?{kind:'surface',label:'draw',children:[node]}:node]}];
            frames.push({nodes,trap:fault});if(fault)throw Object.assign(new RuntimeTrap(),{trapCode:fault.code,message:fault.message,line:fault.line,loopLine:fault.loopLine});return nodes;
        }};
        if(name==='./VeraGfx')return {pointerValue:(kind,id,x,y)=>({handle:`pointer:${kind}:${id}:${x}:${y}`}),decodeGfx:value=>{events.push({type:'gfx',value});const fault=scenario==='gfx'?trap:null;graphics.push(fault);if(fault)throw Object.assign(new RuntimeTrap(),{trapCode:fault.code,message:fault.message,line:fault.line,loopLine:fault.loopLine});}};
        return {};
    },mod,mod.exports);
    const problem=mod.exports.smokeRun({vbc2:'',handlers:new Map(handlers.map(h=>[h.name,h.kind]))},requireContent),expected={problem,events,error:''};
    const reply=JSON.stringify({mode:'smoke',handlers,requireContent,executions,frames,graphics});
    assert.deepEqual(JSON.parse(await generated.inspectWriter.$f(reply)),expected,'smoke '+scenario+' '+requireContent);cases.push({name:'smoke '+scenario+' '+requireContent,reply,expected});
}
{
    const handlers=[{name:'h',kind:'number'}],executions=[{value:'s'},{value:'v'},{fault:'unavailable'}],frames=[{nodes:[{kind:'column',label:'',children:[{kind:'text',label:'',children:[]}]}]}];
    const expected={problem:'',error:'INFRA',events:[{type:'execute',name:'init',args:[]},{type:'execute',name:'view',args:['s']},{type:'ui',value:'v'},{type:'effects',allowed:true},{type:'execute',name:'h',args:['s','number:1']},{type:'effects',allowed:false},{type:'clear'}]};
    const reply=JSON.stringify({mode:'smoke',handlers,executions,frames,graphics:[],requireContent:true});assert.deepEqual(JSON.parse(await generated.inspectWriter.$f(reply)),expected,'smoke infrastructure propagates with effects cleared');cases.push({name:'smoke infrastructure',reply,expected});
}
const playSource=fs.readFileSync(path.join(reference,'entry/src/main/ets/vera/VeraSurfacePlay.ets'),'utf8');
for(const scenario of ['ok','static','absent','dimensions','blank','finger','tick','draw','gfx','button','buttons-static','no-frame','no-touch'])for(const pressButtons of [false,true])for(const requireSurface of scenario==='absent'?[false,true]:[true]){
    const events=[],executions=[],frames=[],graphics=[],numbers=[];let ticks=0,started=false,draws=0;
    class RuntimeTrap extends Error{}
    const trap={code:'R0001',message:'broken',line:7,loopLine:3},raise=()=>{throw Object.assign(new RuntimeTrap(),{trapCode:trap.code,message:trap.message,line:trap.line,loopLine:trap.loopLine});};
    const node=(kind,extra={})=>({kind,label:'',action:'',value:'',minimum:100,maximum:200,enabled:true,text:'',eventValue:0,children:[],...extra});
    const op=x=>({kind:'circle',x,y:0,w:1,h:2,r:3,start:0,sweep:90,size:1,color:'blue',text:'',points:[1,2],children:[]});
    class Host{set effectsAllowed(allowed){events.push({type:'effects',allowed});}set pendingEffects(value){events.push({type:'clear'});}}
    class NumberValue{constructor(number){this.handle='number:'+number.toString();}}
    class VM{execute(name,args=[]){
        const values=args.map(v=>typeof v==='string'?v:v.handle);events.push({type:'execute',name,args:values});let failed=scenario==='finger'&&name==='touch'||scenario==='tick'&&name==='tick'&&ticks===3||scenario==='draw'&&name==='draw'&&draws===3;
        if(name==='tick')ticks++;if(name==='button')started=true;if(name==='draw')draws++;
        const value=name==='init'?'s0':name==='view'?'view':name==='draw'?'draw'+draws:'s'+ticks;
        executions.push({value,trap:failed?trap:null});if(failed)raise();return value;
    }}
    const mod={exports:{}};new Function('require','module','exports',ts.transpileModule(playSource,{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2020}}).outputText)(name=>{
        if(name==='./VeraInterpreter')return {VirtualMachine:VM,deserializeVbc2:()=>null,RuntimeTrap,VeraNumberValue:NumberValue};
        if(name==='./VeraUi')return {UiHostEnvironment:Host,decodeVeraUi:value=>{
            events.push({type:'ui',value});const nodes=scenario==='absent'?[node('text')]:[node('column',{children:[node('surface',{label:'draw',action:scenario==='no-frame'?'':'tick',value:scenario==='no-touch'?'':'touch',minimum:scenario==='dimensions'?0:100}),...Array.from({length:8},(_,i)=>node('button',{action:'button',text:'Start'+i,eventValue:i}))]})];frames.push({nodes,trap:null});return nodes;
        }};
        if(name==='./VeraGfx')return {pointerValue:(kind,id,x,y)=>({handle:'pointer:'+kind+':'+id.toString()+':'+x.toString()+':'+y.toString()}),decodeGfx:value=>{
            events.push({type:'gfx',value});const failed=scenario==='gfx',moving=scenario==='ok'||scenario==='button'&&started;const ops=scenario==='blank'?[]:[op(moving?ticks:0)];graphics.push({ops,trap:failed?trap:null});if(failed)raise();return ops;
        }};return {};
    },mod,mod.exports);
    const saved=Number.prototype.toString;Number.prototype.toString=function(...args){const text=saved.apply(this,args);if(!numbers.some(pair=>pair.number===Number(this)))numbers.push({number:Number(this),text});return text;};
    let problem;try{problem=mod.exports.playSurface({vbc2:''},requireSurface,pressButtons);}finally{Number.prototype.toString=saved;}
    const expected={problem,events,error:''},reply=JSON.stringify({mode:'play',requireSurface,pressButtons,executions,frames,graphics,numbers});
    assert.deepEqual(JSON.parse(await generated.inspectWriter.$f(reply)),expected,'play '+scenario+' '+pressButtons);cases.push({name:'play '+scenario+' '+pressButtons,reply,expected});
}
{
    const mod={exports:{}};new Function('require','module','exports',ts.transpileModule(playSource+'\nexport {fingerprint,script};\n',{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2020}}).outputText)(()=>({}),mod,mod.exports);
    for(const value of [-0.05,-0,0,0.049999999999999996,0.05,0.15,4503599627370496]){
        const child={kind:'point',x:value,y:-value,w:value,h:value,r:value,start:value,sweep:value,size:value,color:'blue',text:'α',points:[value,-value,0.5],children:[]};
        const ops=[{...child,children:[child]}],numbers=[],saved=Number.prototype.toString;Number.prototype.toString=function(...args){const text=saved.apply(this,args);numbers.push({number:Number(this),text});return text;};
        let expected;try{expected={fingerprint:mod.exports.fingerprint(ops),script:JSON.parse(JSON.stringify(mod.exports.script()))};}finally{Number.prototype.toString=saved;}
        const reply=JSON.stringify({mode:'play',action:'fingerprint',ops,numbers});assert.deepEqual(JSON.parse(await generated.inspectWriter.$f(reply)),expected,'surface fingerprint '+value);cases.push({name:'surface fingerprint '+value,reply,expected});
    }
}
{
    const seed=JSON.parse(cases.find(c=>c.name==='play ok false').reply);
    const failAt=seed.executions.findIndex((entry,i)=>i>2&&entry.value.startsWith('s'));
    seed.executions[failAt]={fault:'unavailable'};
    const before=cases.find(c=>c.name==='play ok false').expected.events;
    let executed=0,cut=-1;for(let i=0;i<before.length;i++){if(before[i].type==='execute'){if(executed===failAt){cut=i;break;}executed++;}}
    const expected={problem:'',error:'INFRA',events:[...before.slice(0,cut+1),{type:'effects',allowed:false},{type:'clear'}]},reply=JSON.stringify(seed);
    assert.deepEqual(JSON.parse(await generated.inspectWriter.$f(reply)),expected,'surface infrastructure clears effects');cases.push({name:'surface infrastructure',reply,expected});
}
for(const original of [...cases]){
    const raw=JSON.parse(original.reply);if(raw.mode==='smoke'&&raw.requireContent===true||raw.mode==='play'&&!raw.action){
        for(const action of raw.mode==='smoke'?['adapter','settlement']:['adapter']){
            const reply=JSON.stringify({...raw,action}),expected=original.expected;
            assert.deepEqual(JSON.parse(await generated.inspectWriter.$f(reply)),expected,'execution adapter '+original.name+' '+action);cases.push({name:'execution adapter '+original.name+' '+action,reply,expected});
        }
    }
}
{
    const reply=JSON.stringify({mode:'identities'}),expected={all:['request','one','two'],failed:'HOLE_IDENTITY',remaining:1,one:1,two:2};
    assert.deepEqual(JSON.parse(await generated.inspectWriter.$f(reply)),expected,'original identity inventory');cases.push({name:'original identity inventory',reply,expected});
}
{
    const reply=JSON.stringify({mode:'planning'}),expected={origins:['event.first','event.last'],all:['event.first','event.middle','event.last'],unissued:'HOLE_IDENTITY',first:2,last:0};
    assert.deepEqual(JSON.parse(await generated.inspectWriter.$f(reply)),expected,'issued planning provenance survives compaction and renaming');cases.push({name:'issued planning provenance',reply,expected});
}
fs.writeFileSync(path.join(output,'writer-cases.json'),JSON.stringify(cases,null,2));console.log('PASS '+cases.length+' custom writer flows (approved three-call delta explicit)');})().catch(e=>{console.error(e);process.exit(1);});
