#!/usr/bin/env node
'use strict';
const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict');
const [reference,output]=process.argv.slice(2).map(p=>path.resolve(p));
const {loadVera}=require(path.join(reference,'tools/vera-harness/source-loader.js'));
const ts=require(process.env.VERA_TYPESCRIPT_PATH),templates=loadVera('VeraTemplates');
let source=fs.readFileSync(path.join(reference,'entry/src/main/ets/vera/VeraCompose.ets'),'utf8')+'\nexport { EFFECTS };\n';
// Observe the real emitter, rather than replacing assembly with a sentinel.
source=source.replace('result.source = assembleProgram(comp)','result.source = observeAssembly(comp)');
source+='\nlet assemblies=0; function observeAssembly(comp: Composition): string { assemblies++; return assembleProgram(comp); } export function assemblyCount(): number { return assemblies; }\n';
const mod={exports:{}};
new Function('require','module','exports',ts.transpileModule(source,{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2020}}).outputText)(name=>{
    if(name.startsWith('./'))return loadVera(name.slice(2));if(name==='@ohos.net.http')return {};throw Error(name);
},mod,mod.exports);
const ref=mod.exports,generated=require(path.join(output,'flow-js/flow-runner.js')),cases=[];
const real=templates.vetoes().find(v=>v.key==='needs_real_data');
const catalogue={sections:templates.fragments().map(f=>({key:f.kind,description:f.criteria})),realDataInstructions:real.instructions,
    realDataCriteria:[...real.criteria].map(([key,description])=>({key,description}))};
const sections=templates.fragments().map(f=>({kind:f.kind,owns:f.owns}));
async function check(name,raw,replies,pre=null,settled=[]){
    const text=JSON.stringify(raw),value=ref.parseSketch(text),numbers=[],fixed=[],lengths={},matches={},lower={},requests=[],upper={},first14={};
    const toString=Number.prototype.toString,toFixed=Number.prototype.toFixed,match=String.prototype.match,toLower=String.prototype.toLowerCase,test=RegExp.prototype.test,now=Date.now;
    let calls=0,timeCalls=0,result,expected;
    Number.prototype.toString=function(...args){const result=toString.apply(this,args);numbers.push({number:Number(this),text:result});return result;};
    Number.prototype.toFixed=function(...args){const result=toFixed.apply(this,args);fixed.push({number:Number(this),text:result});return result;};
    String.prototype.match=function(pattern){const result=match.call(this,pattern);matches[pattern.source+'\n'+String(this)]=result||[];return result;};
    String.prototype.toLowerCase=function(){const result=toLower.call(this);lower[String(this)]=result;lengths[String(this)]=String(this).length;return result;};
    RegExp.prototype.test=function(text){const result=test.call(this,text);matches[this.source+'\n'+text]=result?['match']:[];return result;};
    Date.now=()=>++timeCalls*10;
    const before=ref.assemblyCount();
    try{
        const initial=new ref.ComposeResult();initial.jevMs=7;
        result=await ref.composeFromSketch(initial,'test',value,async body=>{requests.push(JSON.parse(body));return replies[calls++];},false,pre,settled);
        if(result.composition){for(const component of ref.screenPlan(result.composition).components){const spec=component.spec,words=spec.tab||spec.kind.split('_').join(' '),label=words.substring(0,1).toUpperCase()+words.substring(1);upper[words]=label;first14[label]=label.substring(0,14);}}
        expected={source:result.source,note:result.note,jevMs:result.jevMs,answers:result.answers,dropped:result.dropped,
            unboundNeeds:result.unboundNeeds,composed:result.composition!==null,parsed:result.parsed!==null,requests,calls,assembled:ref.assemblyCount()-before};
    }finally{Number.prototype.toString=toString;Number.prototype.toFixed=toFixed;String.prototype.match=match;String.prototype.toLowerCase=toLower;RegExp.prototype.test=test;Date.now=now;}
    function samples(value) {
        if (typeof value==='string') { lower[value]=value.toLowerCase(); lengths[value]=value.length; }
        else if (Array.isArray(value)) value.forEach(samples);
        else if (value&&typeof value==='object') Object.values(value).forEach(samples);
    }
    samples(value); samples(expected); if (result.composition) samples(result.composition.custom);
    for(const s of value.values.flatMap(v=>[v.formula,...v.formula]))lengths[s]=s.length;
    const reply=JSON.stringify({mode:'flow',sketch:text,effects:ref.EFFECTS,served:settled,widget:false,custom:true,numbers,fixed,lengths,matches,lower,
        catalogue,sections,pre,replies,remainders:[],upper,first14,banner:templates.bannerSectionView()});
    try { assert.deepEqual(JSON.parse(await generated.inspectFlow.$f(reply)),expected,name); }
    catch (error) { fs.writeFileSync(path.join(output,'flow-failure.json'),JSON.stringify({name,reply:JSON.parse(reply),expected},null,2)); throw error; }
    cases.push({name,reply,expected});
}
async function captureContent(invoke){
    const samples=new Map(),strings=new Set(),saved={};const put=(key,args,result)=>{samples.set(key+JSON.stringify(args),{key,args,result});for(const v of [...args,result])if(typeof v==='string')strings.add(v);};
    for(const key of ['substring','replace','match','toLowerCase'])saved[key]=String.prototype[key];const test=RegExp.prototype.test,number=Number.prototype.toString,float=global.parseFloat,int=global.parseInt;
    String.prototype.substring=function(a,b){const out=saved.substring.call(this,a,b);put('slice',[String(this),a,b??String(this).length],out);return out;};
    String.prototype.replace=function(p,r){const out=saved.replace.call(this,p,r);if(p instanceof RegExp)put('replace',[String(this),p.source,p.flags,r],out);return out;};
    String.prototype.match=function(p){const out=saved.match.call(this,p);put('matches',[String(this),p.source,p.flags],out?[...out].map(v=>v??''):[]);return out;};
    String.prototype.toLowerCase=function(){const out=saved.toLowerCase.call(this);put('lower',[String(this)],out);return out;};
    RegExp.prototype.test=function(v){const out=test.call(this,v);put('matches',[v,this.source,this.flags],out?['match']:[]);return out;};
    Number.prototype.toString=function(...args){const out=number.apply(this,args);put('number',[Number(this)],out);return out;};global.parseFloat=v=>{const out=float(v);put('parse',[v],out);return out;};global.parseInt=(v,...args)=>{const out=int(v,...args);put('parse',[v],out);return out;};
    let result;try{result=await invoke();}finally{for(const key of Object.keys(saved))String.prototype[key]=saved[key];RegExp.prototype.test=test;Number.prototype.toString=number;global.parseFloat=float;global.parseInt=int;}
    for(const body of ['return 0;','return missing;'])put('length',[body],body.length);for(let n=0;n<=120;n++)put('number',[n],String(n));for(const v of [...strings])put('length',[v],v.length);
    return {result,samples:[...samples.values()]};
}
function sourceModule(name,resolve){
    const mod={exports:{}};
    const source=fs.readFileSync(path.join(reference,'entry/src/main/ets/vera',name+'.ets'),'utf8');
    const compiled=ts.transpileModule(source,{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2020}}).outputText;
    new Function('require','module','exports',compiled)(resolve,mod,mod.exports);return mod.exports;
}
// Keep content instrumentation away from compiler execution: collecting parser
// strings creates huge, irrelevant samples and obscures the mechanism boundary.
function fillingOracle(onSamples){
    const compiler=loadVera('VeraCompiler');
    const holes=sourceModule('VeraHoles',name=>name==='./VeraCompiler'?{
        ...compiler,compileVeraSource(source){
            const line=source.split('\n').findIndex(s=>s.includes('return missing;'))+1;
            if(line){
                const span={start:{line,column:1},end:{line,column:2},sourceName:'<source>'};
                throw new compiler.CompileError([
                    new compiler.Diagnostic('E2012','unknown name missing',span),
                    new compiler.Diagnostic('E2019','expected number, found void',span)
                ]);
            }
            return {};
        }
    }:loadVera(name.slice(2)));
    const content=loadVera('VeraTemplateContent');
    return sourceModule('SectionFill',name=>{
        if(name==='./VeraHoles')return holes;
        if(name==='./VeraTemplateContent')return {...content,async fillSectionsContent(...args){
            const captured=await captureContent(()=>content.fillSectionsContent(...args));
            const defaults=await captureContent(()=>content.blockDefaults('timer'));
            onSamples([...captured.samples,...defaults.samples]);return captured.result;
        }};
        return loadVera(name.slice(2));
    });
}
async function main(){
    const raw={inputs:[{id:'a',kind:'count',value:8},{id:'b',kind:'count',value:2}],values:[{id:'v',formula:'a+b'}]};
    const valid=JSON.stringify({answers:{'expr.v':{choice:'add:a:b',confidence:1}}});
    await check('direct',raw,[valid]);
    await check('empty retries',raw,['',valid]);
    await check('empty twice',raw,['','']);
    await check('whitespace no retry',raw,[' ']);
    await check('malformed no retry',raw,['{']);
    await check('custom deferred',raw,['{}']);
    await check('pre wins',raw,['{}'],{'expr.v':{choice:'add:a:b',confidence:1}},['expr.v']);
    await check('pre cannot repair malformed',raw,['{'],{'expr.v':{choice:'add:a:b',confidence:1}},['expr.v']);
    await check('ready section',{ready:['Facts']},[JSON.stringify({answers:{'ready.0':{choice:'key_facts',confidence:1}}})],
        {'ready.0':{choice:'key_facts',confidence:1}},['ready.0']);
    await check('empty plan',{},['{}']);
    const native=loadVera('VeraCompose'),needsRef=loadVera('NeedsChoose');
    for(const widget of [false,true])for(const sceneAllowed of [false,true])for(const failure of [false,true])for(const ready of [false,'key_facts','spotlight','trend_chart','timer'])for(const custom of [false,true]){
        if(['trend_chart','timer'].includes(ready)&&(widget||sceneAllowed||failure||custom))continue;
        const customReply='<classes></classes><fields>c_x: number = 0;</fields><functions></functions><view>sections.push(ui.Text("default", "extra"));</view>';
        const plan=JSON.stringify({title:'Countdown',...(ready==='timer'?{constants:[{id:'length',label:'Length',measure:'seconds',value:5}]}:{inputs:[{id:'length',label:'Length',kind:'count',measure:'seconds',value:5,min:1,max:120}]}),needs:[...(ready&&ready!=='timer'?[{type:'content',what:'Facts'}]:[]),...(custom?[{type:'other',what:'Show an extra counter'}]:[]),{type:'countdown',label:'Timer',length:'length'}]}),plans=failure?['bad',plan]:[plan],prompt='count down 10 seconds',replies=[JSON.stringify({answers:{...(ready?{'ready.0':{choice:ready,confidence:1}}:{}),'clock.part':{choice:ready==='timer'?'timer':'clock',confidence:1}}}),JSON.stringify({answers:{'countdown.length':{choice:'length',confidence:1},'unit.length':{choice:'seconds',confidence:1},'input.length':{choice:'stepper',confidence:1}}})];
        const numbers=[],fixed=[],lengths={},matches={},lower={},normalized={},filterMatches={},characters={},heads={bad:'bad'},trace=[],requests=[],upper={},first14={};
        const save={number:Number.prototype.toString,fixed:Number.prototype.toFixed,match:String.prototype.match,lower:String.prototype.toLowerCase,normalize:String.prototype.normalize,test:RegExp.prototype.test};let cursor=0,choiceCursor=0;
        Number.prototype.toString=function(...a){const text=save.number.apply(this,a);numbers.push({number:Number(this),text});return text;};
        Number.prototype.toFixed=function(...a){const text=save.fixed.apply(this,a);fixed.push({number:Number(this),text});return text;};
        String.prototype.match=function(p){const found=save.match.call(this,p);matches[p.source+'\n'+String(this)]=found||[];return found;};
        String.prototype.toLowerCase=function(){const text=save.lower.call(this);lower[String(this)]=text;lengths[String(this)]=String(this).length;return text;};
        String.prototype.normalize=function(form){const text=save.normalize.call(this,form);normalized[String(this)]=save.lower.call(text);return text;};
        RegExp.prototype.test=function(text){const found=save.test.call(this,text);matches[this.source+'\n'+text]=found?['match']:[];filterMatches[this.source+'\n'+text]=found;return found;};
        let result;try{result=await needsRef.composeNeeds(prompt,async(system,user,partial,thinking)=>{trace.push({type:'plan',user,thinking:!!thinking});return plans[cursor++]??null;},async body=>{requests.push(JSON.parse(body));return replies[choiceCursor++]??'';},s=>trace.push({type:'preview',title:s.title}),widget,null,'',sceneAllowed);}finally{Number.prototype.toString=save.number;Number.prototype.toFixed=save.fixed;String.prototype.match=save.match;String.prototype.toLowerCase=save.lower;String.prototype.normalize=save.normalize;RegExp.prototype.test=save.test;}
        for(const s of [prompt,...Object.keys(normalized),...Object.values(normalized)])for(const c of s)characters[c]=Array.from({length:c.length},(_,i)=>c.charCodeAt(i));
        for(const c of result.composition?native.screenPlan(result.composition).components:[]){const words=c.spec.tab||c.spec.kind.split('_').join(' '),label=words.substring(0,1).toUpperCase()+words.substring(1);upper[words]=label;first14[label]=label.substring(0,14);}
        for(let n=0;n<=120;n++)numbers.push({number:n,text:String(n)});
        function sampleStrings(value){if(typeof value==='string'){lower[value]=value.toLowerCase();lengths[value]=value.length;}else if(value&&typeof value==='object'){for(const item of Object.values(value))sampleStrings(item);}}
        sampleStrings(JSON.parse(plan));sampleStrings(result);sampleStrings(['seconds','length','','c_x','SPOTLIGHT_STYLE','CHART_STYLE','FINISH_ACTION']);
        upper.More='More';first14.More='More';
        const expected={source:result.source,note:result.note,sketch:result.sketch,tries:result.planTries,replanned:result.replanned,dropped:result.dropped,unboundNeeds:result.unboundNeeds,composed:result.composition!==null,requests,trace,assembled:result.source?1:0};
        const input=JSON.stringify({mode:'needs-flow',sketch:'{}',effects:ref.EFFECTS,served:[],widget,custom:true,numbers,fixed,lengths,matches,lower,normalized,filterMatches,characters,heads,catalogue,sections,plans,replies,prompt,sceneAllowed,remainders:[],upper,first14,banner:templates.bannerSectionView()});
        assert.deepEqual(JSON.parse(await generated.inspectFlow.$f(input)),expected,'needs composition '+cases.length);cases.push({name:'needs composition '+cases.length,reply:input,expected});
        for(const cancelled of [false,true])for(const alignment of custom&&!cancelled?[false,true]:[false])for(const contentReply of ready==='timer'&&!cancelled?['{}','{"section1_presets":[["Five","5"]]}','{"section1_presets":[["Broken","not a number"]]}']:['{}'])for(const holeMode of ready==='timer'&&!cancelled&&contentReply==='{}'?['good','repair','exhaust']:['good']){
            const agentSource=fs.readFileSync(path.join(reference,'entry/src/main/ets/vera/agent/VeraAgent.ets'),'utf8'),agentModule={exports:{}};const agentTrace=[],agentRequests=[],agentReplies=[];let planCursor=0,jevCursor=0,contentSamples=[],holeCursor=0;
            new Function('require','module','exports',ts.transpileModule(agentSource,{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2020}}).outputText)(name=>{
                if(name==='./AgentAlign')return {...loadVera('agent/AgentAlign'),alignIsEnabled:()=>alignment};
                if(name==='../VeraCompiler')return {compileVeraSource:source=>({vbc2:'compiled:'+source})};
                if(name==='./AgentHole'&&custom){
                    const customMod={exports:{}},holeMod={exports:{}};
                    new Function('require','module','exports',ts.transpileModule(fs.readFileSync(path.join(reference,'entry/src/main/ets/vera/agent/AgentCustom.ets'),'utf8'),{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2020}}).outputText)(n=>n==='../VeraCompiler'?{compileVeraSource:()=>({unreadFields:[]})}:n==='../VeraSmoke'?{smokeRun:()=>''}:n==='../VeraSurfacePlay'?{playSurface:()=>''}:n==='./AgentScene'?{SCENE_GUIDE:'',SCENE_TOKENS:6000}:loadVera(n.slice(3)),customMod,customMod.exports);
                    new Function('require','module','exports',ts.transpileModule(fs.readFileSync(path.join(reference,'entry/src/main/ets/vera/agent/AgentHole.ets'),'utf8'),{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2020}}).outputText)(n=>n==='./AgentCustom'?customMod.exports:n.startsWith('../')?loadVera(n.slice(3)):loadVera('agent/'+n.slice(2)),holeMod,holeMod.exports);
                    return holeMod.exports;
                }
                if(name==='./AgentRepair')return {settleProgram:async source=>({source,vbc2:'compiled:'+source,errors:'',repair:null,checkMs:0,note:''})};
                if(name.startsWith('../'))return loadVera(name.slice(3));if(name.startsWith('./'))return loadVera('agent/'+name.slice(2));throw Error(name);
            },agentModule,agentModule.exports);
            const host={cancelled:()=>cancelled,status:text=>agentTrace.push({type:'status',text}),preview:identity=>agentTrace.push({type:'preview',identity}),
                askPlan:async(system,user,partial,thinking)=>{agentTrace.push({type:'plan',user,thinking:!!thinking});return plans[planCursor++]??null;},
                askJev:async body=>{const request=JSON.parse(body);agentRequests.push(request);let reply;if(Object.keys(request.questions).length===0)reply='{}';else if(Object.keys(request.questions)[0].startsWith('need.'))reply='{"answers":{"need.0":{"choice":"note","confidence":1},"whose.0":{"choice":"written","confidence":1}}}';else if(Object.keys(request.questions)[0].startsWith('section')&&ready==='timer')reply=JSON.stringify({answers:Object.fromEntries(Object.keys(request.questions).map(k=>[k,{noul:1}]))});else if(Object.keys(request.questions)[0].startsWith('section'))reply='{"answers":{"section1.spotlight.spotlight_style":{"choice":"accent","confidence":1}}}';else if(Object.keys(request.questions)[0].startsWith('claim.'))reply=JSON.stringify({answers:Object.fromEntries(Object.keys(request.questions).map(k=>[k,{probabilities:{yes:1}}]))});else if(Object.keys(request.questions)[0].startsWith('does.'))reply=JSON.stringify({answers:{'does.0':{probabilities:{yes:(ready==='timer'||request.state.includes('<view>'))?1:0}}}});else reply=replies[jevCursor++]??'';agentReplies.push(reply);return reply;},ask:async()=>null,language:custom?'language':'',askCode:custom?async(system,user,tokens,think)=>{agentTrace.push({type:'custom',system,user,tokens,think});return customReply;}:null,
                looked:new Map(),cannot:new Map(),lookup:async need=>{agentTrace.push({type:'lookup',need});return '';},onHole:()=>{},fillBlocks:async request=>{const section=fillingOracle(samples=>{contentSamples=samples;}),calls=new section.BlockCalls();calls.status=host.status;calls.search=async(query,chars)=>{agentTrace.push({type:'search',query,chars});return 'Source data';};calls.content=async(system,user,tokens)=>{agentTrace.push({type:'content',system,user,tokens});return contentReply;};calls.sdkContext=async prompt=>{agentTrace.push({type:'sdk',prompt});return '';};calls.hole=async(system,user)=>{agentTrace.push({type:'hole',system,user,think:holeCursor===2});const bad=holeMode==='exhaust'||(holeMode==='repair'&&holeCursor===0);holeCursor++;return JSON.stringify({holes:{whenFinished7:bad?'return missing;':'return 0;'}});};const filled=await section.fillSections(request,calls);if(!['trend_chart','timer'].includes(ready))contentSamples=[];return filled;}};
            loadVera('SectionFill');loadVera('VeraCompiler');loadVera('VeraSmoke');
            const agent=await agentModule.exports.runAgent({prompt,widget,scene:'',sceneAllowed},host);
            if(!cancelled){
                if(ready)assert.ok(agent.blocks.includes(ready),'issued '+ready+' must actually reach catalogue filling');
                if(alignment){assert.ok(agent.blocks.includes('note'),'alignment must add its chosen block');assert.ok(agentRequests.some(r=>Object.hasOwn(r.questions,'need.0')),'alignment question dispatched');}
                if(custom&&!alignment)assert.ok(agentTrace.some(e=>e.type==='custom'),'custom fixture must dispatch writer');
                if(ready==='timer'){
                    const writes=agentTrace.filter(e=>e.type==='hole');assert.equal(writes.length,holeMode==='good'?1:holeMode==='repair'?2:3,'catalogue function dispatches');
                    assert.deepEqual(writes.map(e=>e.think),writes.map((_,i)=>i===2),'last original allowance thinks');
                    if(contentReply.includes('Five'))assert.ok(agent.source.includes('seconds: 5}'),'list content reaches emitted timer');
                    if(contentReply.includes('Broken'))assert.ok(agent.note.includes('default'),'invalid list row retains default disposition');
                }
                if(ready==='trend_chart')assert.ok(agentTrace.some(e=>e.type==='search'&&e.chars===2400),'grounded chart must dispatch search');
                if(ready==='spotlight')assert.ok(agentRequests.some(r=>Object.hasOwn(r.questions,'section1.spotlight.spotlight_style')),'cosmetic slot question dispatched');
            }
            const agentExpected={source:agent.source,executable:agent.vbc2,note:agent.note,errors:agent.errors,plan:agent.plan,composed:agent.composed.composition!==null,record:agent.record!==null,blocks:agent.blocks,asked:agent.asked.length,previews:agent.previews,requests:agentRequests,trace:agentTrace};
            fixed.push({number:1,text:'1.00'});upper.note='Note';first14.Note='Note';
            const agentInput=JSON.stringify({...JSON.parse(input),fixed,upper,first14,align:alignment,mode:'agent',cancelled,agentCatalogue:alignment?templates.fragments().map(spec=>({...spec,slots:spec.slots.map(slot=>({...slot,criteria:Object.fromEntries(slot.criteria)}))})):ready?[(()=>{const spec=templates.fragments().find(s=>s.kind===ready);return {...spec,slots:spec.slots.map(slot=>({...slot,criteria:Object.fromEntries(slot.criteria)}))};})()]:[],contentReply,...(contentSamples.length?{samples:contentSamples}:{}),holeMode,searchText:'Source data',...(custom?{customReply}:{}),replies:agentReplies});
            if(agent.source){const checked=loadVera('VeraCompiler').compileVeraSource(agent.source);assert.equal(loadVera('VeraSmoke').smokeRun(checked),'','reference emitted program smoke '+cases.length);}
            assert.deepEqual(JSON.parse(await generated.inspectFlow.$f(agentInput)),agentExpected,'agent '+cases.length);cases.push({name:'agent '+cases.length,reply:agentInput,expected:agentExpected});
            if(!cancelled)for(const wholeEnabled of [false,true]){
                const routeModule={exports:{}},routeSource=fs.readFileSync(path.join(reference,'entry/src/main/ets/vera/agent/VeraBuildRoute.ets'),'utf8');
                new Function('require','module','exports',ts.transpileModule(routeSource,{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2020}}).outputText)(name=>name==='./VeraAgent'?{...agentModule.exports,runAgent:async()=>agent}:name==='./AgentSurface'?{briefSurface:async()=>null}:name.startsWith('../')?loadVera(name.slice(3)):loadVera('agent/'+name.slice(2)),routeModule,routeModule.exports);
                routeModule.exports.setWholeScreenEnabled(wholeEnabled);const outcome=await routeModule.exports.buildScreen({agent:{prompt,widget,scene:'',sceneAllowed},mode:'deepseek'},host);
                const expectedRoute={kind:outcome.kind,reason:outcome.reason,...agentExpected},routeInput=JSON.stringify({...JSON.parse(agentInput),mode:'route',wholeEnabled});
                assert.deepEqual(JSON.parse(await generated.inspectFlow.$f(routeInput)),expectedRoute,'route '+cases.length);cases.push({name:'route '+cases.length,reply:routeInput,expected:expectedRoute});
                if(!widget&&!failure&&!sceneAllowed&&!wholeEnabled)for(const kind of ['custom','notice'])for(const fill of ['hole','part','reported','open'])for(const scenario of ['valid','diagnostic','smoke','infrastructure']){
                    const task=kind!=='notice'&&['hole','part'].includes(fill),infrastructure=scenario==='infrastructure',diagnostics=scenario==='diagnostic'?'1:1 E bad':'',smoke=scenario==='smoke'?'runtime':'';
                    const expected={runnable:task&&infrastructure?null:task&&!diagnostics&&!smoke,error:task&&infrastructure?'INFRA':''};
                    const request=JSON.stringify({...JSON.parse(routeInput),action:'runnable',kind,fill,infrastructure,diagnostics,smoke});
                    assert.deepEqual(JSON.parse(await generated.inspectFlow.$f(request)),expected,'fallback guard '+cases.length);cases.push({name:'fallback guard '+cases.length,reply:request,expected});
                }
            }
        }
    }
    const integratedSeed=JSON.parse(cases.find(c=>c.name.startsWith('agent')&&JSON.parse(c.reply).agentCatalogue.length&&!JSON.parse(c.reply).align&&JSON.parse(c.reply).customReply&&!JSON.parse(c.reply).cancelled).reply);
    for(const agentFault of ['plan','jev','compiler','content','custom','lookup','smoke']){
        const code=['plan','jev','content','custom'].includes(agentFault)?'TRANSPORT':'INFRA',expected={error:code,message:agentFault+' unavailable'},reply=JSON.stringify({...integratedSeed,mode:'route',wholeEnabled:true,agentFault});
        assert.deepEqual(JSON.parse(await generated.inspectFlow.$f(reply)),expected,'integrated failure '+agentFault);cases.push({name:'integrated operational failure '+agentFault,reply,expected});
    }
    const seed=JSON.parse(cases.find(c=>c.name.startsWith('agent')&&JSON.parse(c.reply).customReply&&!JSON.parse(c.reply).cancelled).reply);
    for(const cancelled of [false,true]){
        const trace=[],module={exports:{}},surfaceModule={exports:{}},brief=JSON.stringify({surface:'whole',title:'Game',design:'Design'}),surfaceCatalogue=loadVera('VeraUiCatalog').describeCatalogForPrompt(new Set(['AppTheme','Surface','Column','Row','Text','Header','Button','ActionBar']));
        const compiler={compileVeraSource:source=>({vbc2:'compiled:'+source})};
        new Function('require','module','exports',ts.transpileModule(fs.readFileSync(path.join(reference,'entry/src/main/ets/vera/agent/AgentSurface.ets'),'utf8'),{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2020}}).outputText)(n=>n==='../VeraCompiler'?compiler:n==='../VeraSmoke'?{smokeRun:()=>''}:n==='../VeraSurfacePlay'?{playSurface:compiled=>{trace.push({type:'play',identity:compiled.vbc2});return '';}}:n.startsWith('../')?loadVera(n.slice(3)):loadVera('agent/'+n.slice(2)),surfaceModule,surfaceModule.exports);
        new Function('require','module','exports',ts.transpileModule(fs.readFileSync(path.join(reference,'entry/src/main/ets/vera/agent/VeraBuildRoute.ets'),'utf8'),{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2020}}).outputText)(n=>n==='./AgentSurface'?surfaceModule.exports:n.startsWith('../')?loadVera(n.slice(3)):loadVera('agent/'+n.slice(2)),module,module.exports);
        const host={language:'language',surfaceGuide:'guide',cancelled:()=>cancelled,status:text=>trace.push({type:'status',text}),askPlan:async(system,user,partial,think)=>{trace.push({type:'classify',system,user,think});return brief;},askCode:async(system,user,tokens,think)=>{trace.push({type:'custom',system,user,tokens,think});return 'good';}},now=Date.now;let tick=0,outcome;Date.now=()=>tick+=10;try{outcome=await module.exports.buildScreen({agent:{prompt:'snake game',widget:false,scene:'',sceneAllowed:true},mode:'deepseek'},host);}finally{Date.now=now;}
        if(cancelled)continue;const a=outcome.agent,expected={kind:outcome.kind,reason:outcome.reason,source:a.source,executable:a.vbc2,note:a.note,errors:a.errors,plan:a.plan,composed:a.composed.composition!==null,record:a.record!==null,blocks:a.blocks,asked:a.asked.length,previews:a.previews,requests:[],trace};
        const reply=JSON.stringify({...seed,action:'surface',mode:'route',prompt:'snake game',widget:false,cancelled,customReply:'good',surfaceBrief:brief,surfaceCatalogue,wholeEnabled:true});assert.deepEqual(JSON.parse(await generated.inspectFlow.$f(reply)),expected,'actual surface build route');cases.push({name:'actual surface build route',reply,expected});
    }
    for(const originalUsed of [0,1,2,3])for(const good of [false,true]){
        const reply=JSON.stringify({...seed,mode:'route',wholeEnabled:true,plans:[null,null,null],replies:['{}'],originalUsed,customReply:good?'<classes></classes><fields></fields><functions></functions><view>sections.push(ui.Text("default", "whole"));</view>':'bad'}),result=JSON.parse(await generated.inspectFlow.$f(reply)),dispatches=result.trace.filter(e=>e.type==='custom');
        assert.equal(dispatches.length,good?Math.min(1,3-originalUsed):3-originalUsed);assert.deepEqual(dispatches.map(e=>e.think),dispatches.map((_,i)=>originalUsed+i===2));
        assert.equal(result.kind,good&&originalUsed<3?'agent':'not-built');if(result.kind==='agent')assert.ok(result.source.includes('ui.Text("default", "whole")'));cases.push({name:'whole fallback original budget '+originalUsed+' '+good,reply,expected:result});
    }
    const recordRef=loadVera('VeraComposition'),editSpecs=templates.fragments().filter(s=>['key_facts','note'].includes(s.kind)),recordSource=fs.readFileSync(path.join(reference,'entry/src/main/ets/vera/VeraComposition.ets'),'utf8');
    for(const op of ['none','add_block','remove_block','replace_block','move_to_top','move_to_bottom'])for(const generationMode of ['deepseek','server'])for(const fault of ['valid','diagnostic','smoke']){
        const events=[],oracle={exports:{}},text=JSON.stringify({v:1,layout:'stack',prompt:'test',globals:{},blocks:[{kind:'key_facts',values:{},holes:{}}]}),comp=recordRef.compositionFromJson(text),answer=JSON.stringify({answers:{'edit.op':{choice:op,confidence:1},'edit.target':{choice:'key_facts',confidence:1},'edit.kind':{choice:'note',confidence:1}}});
        class CompileError extends Error{}
        new Function('require','module','exports',ts.transpileModule(recordSource,{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2020}}).outputText)(name=>name==='./VeraCompiler'?{compileVeraSource(source){events.push({type:'compile',source});if(fault==='diagnostic')throw new CompileError('bad');return {vbc2:'compiled:'+source};}}:name==='./VeraSmoke'?{smokeRun(compiled){events.push({type:'smoke',identity:compiled.vbc2});return fault==='smoke'?'runtime':'';}}:name==='./VeraTemplateContent'?{fillSectionsContent:(kinds,prompt,mode)=>loadVera('VeraTemplateContent').fillSectionsContent(kinds,prompt,mode,'',async(system,user,tokens)=>{events.push({type:'content',system,user,tokens});return '{}';})}:name.startsWith('./')?loadVera(name.slice(2)):{},oracle,oracle.exports);
        const result=await oracle.exports.editComposition('test',comp,generationMode,async request=>{events.push({type:'select',request});return recordRef.readEditAnswer(answer,comp);}),expected={applied:result.applied,note:result.note,composition:result.composition?JSON.parse(recordRef.compositionToJson(result.composition)):null,source:result.source,executable:result.vbc2,events,error:''};
        const upper={},first14={};for(const spec of editSpecs){const words=spec.tab||spec.kind.split('_').join(' '),label=words.substring(0,1).toUpperCase()+words.substring(1);upper[words]=label;first14[label]=label.substring(0,14);}
        const payload={mode:'composition-edit',agentCatalogue:editSpecs,text,answer,generationMode,diagnostics:fault==='diagnostic'?'bad':'',smoke:fault==='smoke'?'runtime':'',upper,first14,banner:templates.bannerSectionView()},reply=JSON.stringify(payload);
        assert.deepEqual(JSON.parse(await generated.inspectFlow.$f(reply)),expected,'composition edit flow '+cases.length);cases.push({name:'composition edit flow '+cases.length,reply,expected});
        if(op==='add_block'&&generationMode==='deepseek'&&fault==='valid')for(const infrastructure of ['selector','compiler']){
            const request=JSON.stringify({...payload,fault:infrastructure}),events=expected.events.slice(0,infrastructure==='selector'?1:-1),errorExpected={error:'INFRA',events};
            assert.deepEqual(JSON.parse(await generated.inspectFlow.$f(request)),errorExpected,'composition edit infrastructure '+cases.length);cases.push({name:'composition edit infrastructure '+cases.length,reply:request,expected:errorExpected});
        }
    }
    fs.writeFileSync(path.join(output,'flow-cases.json'),'[\n'+cases.map(c=>JSON.stringify(c)).join(',\n')+'\n]\n');
    console.log(`PASS ${cases.length} composer chooser-flow cases`);
}
main().catch(e=>{console.error(e);process.exitCode=1;});
