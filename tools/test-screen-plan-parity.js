#!/usr/bin/env node
'use strict';
const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict');
const [reference,output]=process.argv.slice(2).map(p=>path.resolve(p));
const {loadVera}=require(path.join(reference,'tools/vera-harness/source-loader.js'));
const ts=require(process.env.VERA_TYPESCRIPT_PATH);
const source=fs.readFileSync(path.join(reference,'entry/src/main/ets/vera/ScreenPlan.ets'),'utf8');
const mod={exports:{}};let assembled=0;
new Function('require','module','exports',ts.transpileModule(source,{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2020}}).outputText)(name=>{
    if(name==='./VeraSectionCompose')return {composeFragments:()=>{assembled++;return 'assembled';}};
    return loadVera(name.slice(2));
},mod,mod.exports);
const ref=mod.exports,layouts=loadVera('VeraLayouts').layouts(),generated=require(path.join(output,'screen-js/screen-runner.js')),cases=[];
const port=(dir,name,type='number')=>({dir,name,type});
const spec=(kind,ports=[])=>({kind,criteria:kind+' section',ports});
for(const layout of ['stack','absent',...layouts.map(l=>l.id)])for(const shape of [[],[null],[spec('metrics',[port('writes','x'),port('writes','x','string')])],[null,spec('checklist',[port('reads','x'),port('reads','x')]),spec('metrics',[port('writes','x'),port('writes','y')])]])for(const holes of [[],[{id:'missing',need:'Need 🌿',phone:true,fill:false}],[{id:'gap',need:'Gap',phone:false,fill:true}]]){
    const values=[[{key:'token',value:'one'}]],globals=new Map([['theme','clean']]);
    const plan=ref.planFromBlocks('test',layout,shape,globals,values.map(entries=>new Map(entries.map(e=>[e.key,e.value]))));
    globals.set('theme','changed');
    for(const h of holes){const slot=plan.addToWrite(h.id,h.need,h.phone);slot.reads=['alpha'];slot.writes=['beta'];slot.when='tap';if(h.fill)plan.fillWith(slot,'reported',0,'unsupported');}
    assembled=0;const complete=ref.assemblePlan(plan),partial=ref.assemblePlan(plan,true);
    const expected=JSON.parse(JSON.stringify({origin:plan.origin,prompt:plan.prompt,layout:plan.layout,widget:plan.widget,global:plan.globals.get('theme'),slots:plan.slots,
        components:plan.components.map(c=>({fill:c.fill,kind:c.spec===null?null:c.spec.kind,values:[...c.values].map(([key,value])=>({key,value}))})),state:plan.state,open:plan.open().length,toWrite:plan.toWrite().length,description:ref.describePlan(plan),complete,partial,assembled}));
    const reply=JSON.stringify({specs:shape,layout,layouts,values,holes}),actual=JSON.parse(generated.inspectScreen.$f(reply));
    assert.deepEqual(actual,expected,layout+' '+cases.length);cases.push({name:layout+' '+cases.length,reply,expected});
}
const textModule={exports:{}};
const textSource=fs.readFileSync(path.join(reference,'entry/src/main/ets/vera/VeraSectionCompose.ets'),'utf8')+'\nexport {fillPassthrough, resolveInstanceText};\n';
new Function('require','module','exports',ts.transpileModule(textSource,{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2020}}).outputText)(name=>name.startsWith('./')?loadVera(name.slice(2)):{},textModule,textModule.exports);
for(const text of ['return {__PASSTHROUGH__};','return {a: 1, __PASSTHROUGH__};','return {a: {b: 2}, b: 3__PASSTHROUGH__};','"__PASSTHROUGH__" // __PASSTHROUGH__\nreturn {__PASSTHROUGH__};','return {"a": 1__PASSTHROUGH__};','return {a: "escaped\\\\\\\"🌿"__PASSTHROUGH__};','__PASSTHROUGH__','return {aIDX: __TOKEN_IDX____PASSTHROUGH__};','return {f: call(1, 2)__PASSTHROUGH__};','return {a: [1,2], __PASSTHROUGH__};'])for(const fields of [[],['a'],['a','b','c'],['a','a','paneTab']])for(const indexed of [true,false]){
    const values=[{key:'__TOKEN_IDX__',value:'first IDX'},{key:'first',value:'second'}];
    const reply=JSON.stringify({mode:'text',text,fields,values,indexed});
    const expected={filled:textModule.exports.fillPassthrough(text,fields),resolved:textModule.exports.resolveInstanceText(text,'3',new Map(values.map(e=>[e.key,e.value])),indexed),marked:textModule.exports.markedHole('key','3',text)};
    assert.deepEqual(JSON.parse(generated.inspectScreen.$f(reply)),expected,'text '+cases.length);cases.push({name:'text '+cases.length,reply,expected});
}
const templates=loadVera('VeraTemplates'),catalogue=templates.fragments();
for(const shape of [[],[null],...catalogue.map(s=>[s]),...catalogue.map(s=>[s,null,s]),[catalogue[0],catalogue[1],catalogue[2]]])for(const widget of [false,true]){
    const globals=[{key:'__WIDGET__',value:String(widget)},{key:'__TITLE__',value:'Title 🌿'},{key:'__BANNER_SECTION__',value:widget?'show':'hide'}],values=[],upper={},first14={};
    for(const spec of shape){if(!spec)continue;const words=spec.tab||spec.kind.split('_').join(' '),label=words.substring(0,1).toUpperCase()+words.substring(1);upper[words]=label;first14[label]=label.substring(0,14);}
    const expected={source:textModule.exports.composeFragments(shape,new Map(globals.map(e=>[e.key,e.value])),values)},reply=JSON.stringify({mode:'emission',specs:shape,globals,values,upper,first14,banner:templates.bannerSectionView()});
    assert.deepEqual(JSON.parse(generated.inspectScreen.$f(reply)),expected,'emission '+cases.length);cases.push({name:'emission '+cases.length,reply,expected});
}
const custom=loadVera('agent/AgentCustom');
for(const reply of ['','<functions></functions><view></view><fields></fields>','<classes>nothing</classes><fields>c_elapsed: number = 0;\nc_rows: string[] = [];</fields><functions>return 1;</functions><view>sections.push(ui.Ticker());</view>','<classes>any @jsonable record</classes><fields>c_name: string = "🌿";</fields><functions>f</functions><view>v</view>','<fields>bad</fields><functions>f</functions><view>v</view>'])for(const answer of ['{}','invalid','{"answers":{"does.0":{"probabilities":{"yes":0.49}},"does.1":{"probabilities":{"yes":0.5}}}}','{"answers":null}'])for(const clock of [false,true]){
    const needs=['first','second'],request=new custom.CustomRequest();request.prompt='test';request.needs=needs;request.ownsClock=clock;request.ownsList=!clock;
    const section=custom.parseSection(reply),lower={};if(section)for(const f of section.stateFields)lower[f.name]=f.name.toLowerCase();
    const expected={parsed:section?{kind:section.kind,supportClass:section.supportClass,fields:section.stateFields,initEntries:section.initEntries,functions:section.functions,viewBlock:section.viewBlock,indexed:section.indexed,tab:section.tab}:null,
        repeated:section?custom.repeats(section,request):'',request:JSON.parse(custom.verifyRequest(request,reply)),undone:custom.undone(request,answer)};
    const wire=JSON.stringify({mode:'custom',reply,answer,needs,clock,list:!clock,lower});assert.deepEqual(JSON.parse(generated.inspectScreen.$f(wire)),JSON.parse(JSON.stringify(expected)),'custom '+cases.length);cases.push({name:'custom '+cases.length,reply:wire,expected});
}
const holeModule={exports:{}},holeSource=fs.readFileSync(path.join(reference,'entry/src/main/ets/vera/agent/AgentHole.ets'),'utf8')+'\nexport {settle,quietSubtitle};\n';
new Function('require','module','exports',ts.transpileModule(holeSource,{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2020}}).outputText)(name=>name.startsWith('../')?loadVera(name.slice(3)):name==='./AgentCustom'?custom:name==='./AgentHost'?loadVera('agent/AgentHost'):{},holeModule,holeModule.exports);
for(const size of [1,2,3])for(const whole of [false,true])for(const reply of ['', '<fields>c_x: number = 0;</fields><functions>f</functions><view>v</view>'])for(const missed of [[],['one'],['one','two']])for(const declined of [[],[{key:'one',value:'Unavailable. More detail.'}],[{key:'one',value:'Unavailable.'},{key:'two',value:'Unavailable.'}]])for(const cannot of ['', 'No provider. Please connect one.']){
    const screen=new ref.ScreenPlan(),needs=[{id:'one',need:'one',phone:true},{id:'two',need:'two',phone:false},{id:'three',need:'three',phone:true}].slice(0,size),records=[],host={cannot:new Map(),onHole:r=>records.push(r)};
    screen.globals.set('__SUPPORTING__','promise');const slots=needs.map(n=>screen.addToWrite(n.id,n.need,n.phone)),written=new custom.CustomResult(),asked=new custom.CustomRequest();
    Object.assign(written,{section:custom.parseSection(reply),cannot,missed,declined:new Map(declined.map(e=>[e.key,e.value])),failed:'failed',attempts:3,thought:true,ms:20});Object.assign(asked,{whole,phone:'lookup',needs:needs.map(n=>n.need)});
    holeModule.exports.settle(screen,'test',slots,asked,written,host);holeModule.exports.quietSubtitle(screen);
    const expected={records,slots:slots.map(s=>({fill:s.fill,by:s.by,detail:s.detail})),components:screen.components.map(c=>({fill:c.fill,kind:c.spec.kind,view:c.spec.viewBlock})),cannot:[...host.cannot].map(([key,value])=>({key,value})),supporting:screen.globals.get('__SUPPORTING__')};
    const ends={},lengths={},prefix={};for(const value of [cannot,...declined.map(e=>e.value)]){for(const s of [value,value.trim(),value.trim().split(/(?<=[.!?])(?:\s|$)/)[0]]){ends[s]=s.search(/[.!?](\s|$)/);lengths[s]=s.length;prefix[s]=s.substring(0,137);for(const c of s)lengths[c]=c.length;}}
    const wire=JSON.stringify({mode:'settle',needs,reply,whole,missed,declined,cannot,ends,lengths,prefix});assert.deepEqual(JSON.parse(generated.inspectScreen.$f(wire)),JSON.parse(JSON.stringify(expected)),'settle '+cases.length);cases.push({name:'settle '+cases.length,reply:wire,expected});
}
const callModule={exports:{}},callSource=fs.readFileSync(path.join(reference,'entry/src/main/ets/vera/agent/AgentCustom.ets'),'utf8')+'\nexport {callProblem};\n';
new Function('require','module','exports',ts.transpileModule(callSource,{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2020}}).outputText)(name=>name.startsWith('../')?loadVera(name.slice(3)):{},callModule,callModule.exports);
for(const phone of ['', 'sdk.call("alpha", x) {"target":"beta"}', 'sdk.call(\\"escaped\\", x) {"target":"alpha"} {"target":"alpha"}'])for(const reply of ['', 'sdk.call("alpha", x)', 'sdk.call("invented", x)', 'sdk.call(dynamic, x)', 'sdk.call ("alpha", x)', '<cannot need=" one "> No app. </cannot><cannot need="one">Updated.</cannot><cannot need="invented">Ignore.</cannot>', '<cannot need="two"> </cannot><cannot need="one">Yes 🌿</cannot>']){
    const matches={},exec=RegExp.prototype.exec;RegExp.prototype.exec=function(value){const match=exec.call(this,value);const key=this.source.replaceAll(String.fromCharCode(92,34),'"').replaceAll(String.fromCharCode(92,47),'/')+'\n'+value;if(!matches[key])matches[key]=[];if(match)matches[key].push([...match]);return match;};
    let expected;try{const found=callModule.exports.foundTargets(phone);expected={found,problem:callModule.exports.callProblem(reply,found),declined:[...callModule.exports.declinedNeeds(reply,['one','two'])].map(([key,value])=>({key,value}))};}finally{RegExp.prototype.exec=exec;}
    const wire=JSON.stringify({mode:'calls',phone,reply,needs:['one','two'],matches});assert.deepEqual(JSON.parse(generated.inspectScreen.$f(wire)),expected,'calls '+cases.length);cases.push({name:'calls '+cases.length,reply:wire,expected});
}
const layoutRef=loadVera('VeraLayouts');
for(const layout of ['absent',...layouts.map(l=>l.id)])for(const shape of [[],['none'],['none','none','none','none'],['search_list','master_list','none','none'],['record_log','breakdown_table','series_chart','number_strip','none'],['itinerary','progress_line','list_summary','unknown'],['checklist','list_summary','checklist','none']])for(const wanted of [[],['master_list','detail_view'],['progress_line','list_summary','progress_line'],['unknown','unknown','none']]){
    const kinds=[...shape],before=layoutRef.arrangeInLayout(layout,kinds),swapped=layoutRef.swapForLayout(layout,kinds),completed=layoutRef.completeRequiredSlots(layout,kinds),fit=layoutRef.fitToSlots(layout,kinds,wanted),placed=layoutRef.placeKinds(kinds,fit);
    const expected={before,swapped,completed,fit,placed,kinds,order:layoutRef.arrangeInLayout(layout,kinds),all:layoutRef.layoutKinds(),named:layoutRef.kindsOfLayout(layout)},reply=JSON.stringify({mode:'layout',layout,layouts,kinds:shape,wanted});
    assert.deepEqual(JSON.parse(generated.inspectScreen.$f(reply)),expected,'layout '+cases.length);cases.push({name:'layout '+cases.length,reply,expected});
}
const recordRef=loadVera('VeraComposition');
for(const selected of [[],...catalogue.map(s=>[s]),[catalogue[0],catalogue[1]]])for(const literalKind of ['none','custom','notice','inputs']){
    const plan=new ref.ScreenPlan(),components=selected.map(spec=>({fill:'block',spec,values:new Map()}));
    if(literalKind!=='none'){const spec=new templates.FragmentSpec(literalKind,'old','',[], '', '', '', '{ ui.Text("hi"); }',[],[],[]);spec.indexed=false;spec.tab='Kept';spec.owns='own';components.push({fill:'written',spec,values:new Map()});}
    plan.components=components;const slot=plan.addToWrite('x','need',false);slot.by=components.length-1;
    const comp=recordRef.compositionFromPlan(plan,'test',[],[]),source=recordRef.composeFrom(comp),upper={},first14={};
    for(const b of comp.blocks){const spec=b.literal||catalogue.find(s=>s.kind===b.kind),words=spec.tab||spec.kind.split('_').join(' '),label=words.substring(0,1).toUpperCase()+words.substring(1);upper[words]=label;first14[label]=label.substring(0,14);}
    const expected={kinds:comp.kinds(),targets:comp.targetKinds(),count:comp.catalogueCount(),blocks:comp.blocks.map(b=>({kind:b.kind,literal:b.literal?{kind:b.literal.kind,criteria:b.literal.criteria,indexed:b.literal.indexed,tab:b.literal.tab,owns:b.literal.owns,view:b.literal.viewBlock}:null})),source,rebuilds:recordRef.rebuilds(comp,source)};
    const reply=JSON.stringify({mode:'record',components,slots:plan.slots,layout:plan.layout,catalogue,upper,first14,source,banner:templates.bannerSectionView()});
    assert.deepEqual(JSON.parse(generated.inspectScreen.$f(reply)),expected,'record '+cases.length);cases.push({name:'record '+cases.length,reply,expected});
}
for(const text of ['', 'bad', '{}',...['{}','null','[null]','5'].map(fields=>'{"v":2,"blocks":[{"kind":"custom","literal":{"stateFields":'+fields+'}}]}'), '{"v":1,"blocks":[]}', '{"v":1,"blocks":[{"kind":"unknown"}]}',...catalogue.map(s=>JSON.stringify({v:1,layout:'',prompt:'test',globals:{a:'b',number:1},blocks:[{kind:s.kind,values:{token:'value',wrong:2},holes:{key:'return true;'}}]})),...['custom','notice','inputs'].flatMap(kind=>[1,2].map(v=>JSON.stringify({v,blocks:[{kind,literal:{criteria:'written',stateFields:[{name:'c_x',type:'number'}],viewBlock:'view',imports:['std/time'],tab:'Kept',owns:'own'}}]})))]){
    const parsed=recordRef.compositionFromJson(text),expected={encoded:parsed?JSON.parse(recordRef.compositionToJson(parsed)):null},reply=JSON.stringify({mode:'record-json',text,catalogue});
    assert.deepEqual(JSON.parse(generated.inspectScreen.$f(reply)),expected,'record json '+cases.length);cases.push({name:'record json '+cases.length,reply,expected});
}
for(const kinds of [['checklist'],['checklist','key_facts'],Array.from({length:6},(_,i)=>catalogue[i].kind)])for(const op of ['none','add_block','remove_block','replace_block','move_to_top','move_to_bottom'])for(const target of ['checklist','absent'])for(const kind of ['note','checklist','unknown'])for(const defaults of [false,true]){
    const text=JSON.stringify({v:1,prompt:'test',layout:'stack',globals:{},blocks:kinds.map(kind=>({kind,values:{},holes:{}}))}),comp=recordRef.compositionFromJson(text),edit=Object.assign(new recordRef.CompositionEdit(),{op,target,kind}),next=recordRef.applyEdit(comp,edit,defaults?null:new Map()),answer=JSON.stringify({answers:{'edit.op':{choice:op,confidence:.6},'edit.kind':{choice:kind,confidence:.6},'edit.target':{choice:target,confidence:.6}}}),expected={applied:next?JSON.parse(recordRef.compositionToJson(next)):null,answer:recordRef.readEditAnswer(answer,comp),request:JSON.parse(recordRef.buildEditRequest('test',comp))},reply=JSON.stringify({mode:'record-json',action:'edit',text,catalogue,edit,answer,defaults});
    assert.deepEqual(JSON.parse(generated.inspectScreen.$f(reply)),JSON.parse(JSON.stringify(expected)),'record edit '+cases.length);cases.push({name:'record edit '+cases.length,reply,expected});
}
for(const ms of [-10,0,49.9,50,50.49,50.5,1200.8])for(const who of ['Model','Jev','Phone','Search','You','Other']){
    const ref=loadVera('RunStages'),input=[{name:'first',who,ms,note:'one'},{name:'second',who:'You',ms:100,note:''},{name:'third',who,ms:200,note:'two'}],stages=[];for(const s of input)ref.addStage(stages,s.name,s.who,s.ms,s.note);
    const expected={stages,grouped:ref.byWho(stages),app:ref.appMs(stages),you:ref.yourMs(stages)},reply=JSON.stringify({mode:'stages',stages:input});assert.deepEqual(JSON.parse(generated.inspectScreen.$f(reply)),JSON.parse(JSON.stringify(expected)),'stages '+cases.length);cases.push({name:'stages '+cases.length,reply,expected});
}
fs.writeFileSync(path.join(output,'screen-cases.json'),JSON.stringify(cases,null,2));
console.log('PASS '+cases.length+' screen-plan state/slot/assembly-gate cases');
