"use strict";
// Current compiler output and production loader/session. Only provider boundaries are mocked.
const fs=require('fs'),path=require('path'),vm=require('vm'),assert=require('assert');
const base=path.resolve(process.argv[2]),bindings=path.resolve('build/embedding-library/aar/assets/embedding/bindings');
for(const name of ['sandbox.js','dealui-session.js','ui-session.js']) vm.runInThisContext(fs.readFileSync(path.join(bindings,name),'utf8'));
configureDealCapabilities(JSON.parse(fs.readFileSync(path.join(base,'plans/contracts.json'),'utf8')));
function mount(name,checkpoint=null){const dir=path.join(base,name,'check/js'),factories={};function walk(d){for(const n of fs.readdirSync(d)){const f=path.join(d,n);if(fs.statSync(f).isDirectory())walk(f);else if(n.endsWith('.js'))factories[path.relative(dir,f)]=new Function('require','module','exports','process','console',fs.readFileSync(f,'utf8'));}}walk(dir);mountDealUi(factories,'experience_entry.js',checkpoint);return dealUi;}
const snapshot=s=>JSON.parse(s.snapshot()),nodes=n=>[n,...n.children.flatMap(nodes)],prop=(n,k)=>n.props.find(p=>p.name===k);
const value=(n,k)=>{const p=prop(n,k);return p?.kind==='number'?p.numberValue:p?.kind==='boolean'?p.booleanValue:p?.stringValue;};
function slot(s,label){const n=nodes(snapshot(s).tree).find(n=>(value(n,'label')===label||value(n,'text')===label||value(n,'title')===label)&&n.props.some(p=>p.kind==='action'));assert(n,label);const a=n.props.find(p=>p.kind==='action');assert(a,'No action '+label);return a.actionSlot;}
const state=s=>JSON.parse(s.state()),requests=()=>JSON.parse(dealCapabilities.take()),tick=()=>new Promise(r=>setImmediate(r));
async function deliver(fn,v,ok=true){const req=requests();assert.equal(req.length,1);assert.equal(req[0].function,fn);dealCapabilities.deliver([{id:req[0].id,ok,value:v,error:ok?undefined:{code:'UNAVAILABLE',message:'Private provider error'}}]);await tick();await tick();}
(async()=>{
 let s=mount('budget');assert.equal(state(s).m_difference,1750);s.dispatch(slot(s,'Approved'),'20000');assert.equal(state(s).m_difference,-250);assert.equal(state(s).m_status,'Under budget');s.dispatch(slot(s,'Committed'),'20000');assert.equal(state(s).m_status,'Equal');s.dispatch(slot(s,'Approved'),'1.5');assert.equal(state(s).m_approved,20000);const cp=JSON.parse(s.checkpoint());s.dispose();s=mount('budget',cp);assert.equal(state(s).m_difference,0);s.dispose();
 s=mount('departure');assert.equal(requests().length,0);s.dispatch(slot(s,'Get travel estimate'));await deliver('estimate',29);assert.equal(state(s).m_leave,916);assert.equal(state(s).m_inspection,906);s.dispatch(slot(s,'Start'),'985');assert.equal(state(s).m_leave,941);assert.equal(state(s).m_inspection,931);s.dispatch(slot(s,'Distance (km)'),'13');assert.equal(state(s).m_loaded,false);s.dispatch(slot(s,'Get travel estimate'));await deliver('estimate',34);assert.equal(state(s).m_leave,936);s.dispose();
 s=mount('food');assert.equal(requests().length,0);s.dispatch(slot(s,'Load menu and delivery'));
 await deliver('menu',[{id:'opaque-a',name:'Standard',cents:1000,vegan:false,diameterMm:300},{id:'opaque-b',name:'Plant',cents:1200,vegan:true,diameterMm:300},{id:'opaque-c',name:'Small',cents:100,vegan:true,diameterMm:180}]);
 await deliver('delivery',[{id:'timely',from:'17:45',until:'18:15',meetsRequirement:true,deliveryCents:500},{id:'late',from:'18:30',until:'19:00',meetsRequirement:false,deliveryCents:500}]);
 assert.equal(state(s).m_valid,true);assert.equal(state(s).m_count,6);assert.equal(state(s).m_diet,2);assert.equal(state(s).m_total,6900);assert.equal(state(s).m_rows.length,2);
 s.dispatch(slot(s,'Standard'),'3');assert.equal(state(s).m_valid,false);const before=state(s).m_rows;s.dispatch(slot(s,'Standard'),'1.5');assert.deepEqual(state(s).m_rows,before);s.dispatch(slot(s,'19:00'),'late');assert.equal(state(s).m_valid,false);
 s.dispatch(slot(s,'18:15'),'timely');s.dispatch(slot(s,'Budget'),'100');s.dispatch(slot(s,'Recalculate allocation'));assert.equal(state(s).m_count,0);assert.equal(state(s).m_valid,false);assert.equal(state(s).m_rows.length,2);
 s.dispatch(slot(s,'Load menu and delivery'));await deliver('menu',null,false);assert.equal(state(s).m_loading,false);assert.equal(state(s).m_error,'Check access and try again');assert.equal(requests().length,0);
 s.dispose();
 s=mount('equipment');s.dispatch(slot(s,'Read equipment and quote alternatives'));
 const attrs=(kind,extra={})=>({kind,adjustable:false,clipAdapter:false,balanced:false,connectorFrom:'',connectorTo:'',lengthMm:0,...extra});
 const items=[{id:'stand-opaque',name:'Unrelated display name',cents:800,available:2,attributes:attrs('stand',{adjustable:true,clipAdapter:true}),components:[]},{id:'cable-opaque',name:'Other prose',cents:500,available:3,attributes:attrs('cable',{balanced:true,connectorFrom:'XLR female',connectorTo:'XLR male',lengthMm:10000}),components:[]},{id:'package-opaque',name:'Bundle',cents:1200,available:1,attributes:attrs('package'),components:[{itemId:'stand-opaque',quantity:1},{itemId:'cable-opaque',quantity:1}]}];
 await deliver('equipment',items);
 let req=requests();assert.equal(req.length,1);assert.equal(req[0].function,'quote');assert.equal(req[0].args[0],'package-opaque:1');dealCapabilities.deliver([{id:req[0].id,ok:true,value:{cents:1200,depositCents:5000,available:true,expiresInMinutes:15}}]);await tick();await tick();
 req=requests();assert.equal(req.length,1);assert.equal(req[0].args[0],'stand-opaque:1,cable-opaque:1');dealCapabilities.deliver([{id:req[0].id,ok:true,value:{cents:1300,depositCents:5000,available:true,expiresInMinutes:15}}]);await tick();await tick();
 assert.equal(state(s).m_packageUsable,true);assert.equal(state(s).m_separateUsable,true);assert.equal(state(s).m_packageQuote.m_depositCents,5000);s.dispatch(slot(s,'Hire budget'),'1250');assert.equal(state(s).m_packageUsable,true);assert.equal(state(s).m_separateUsable,false);
 const clock=()=>nodes(snapshot(s).tree).find(n=>n.component.endsWith('.SessionClock')).props.find(p=>p.kind==='action').actionSlot;
 s.dispatch(clock(),'1000');s.dispatch(clock(),'901000');assert.equal(state(s).m_packageUsable,false);assert.equal(state(s).m_fresh,false);
 const saved=JSON.parse(s.checkpoint());s.dispose();s=mount('equipment',saved);s.dispatch(clock(),'-1000000');assert.equal(state(s).m_fresh,false);
 s.dispatch(slot(s,'Read equipment and quote alternatives'));s.dispatch(clock(),'-1100000');await deliver('equipment',items.filter(i=>i.id==='stand-opaque'));assert.equal(state(s).m_fresh,false);assert.equal(state(s).m_loading,false);s.dispatch(slot(s,'Read equipment and quote alternatives'));await deliver('equipment',items.filter(i=>i.id==='stand-opaque'));assert.equal(requests().length,0);assert.equal(state(s).m_packageUsable,false);assert.equal(state(s).m_separateUsable,false);assert.equal(state(s).m_packageQuote.m_available,false);s.dispose();
 s=mount('join');assert.deepEqual(state(s).m_joined.map(r=>[r.m_id,r.m_total]),[['opaque-b',11],['opaque-a',10]]);s.dispose();
 s=mount('custom-budget');assert.equal(state(s).m_difference,1750);s.dispose();
 s=mount('optional');s.dispatch(slot(s,'Read optional'));await deliver('read',{});assert.equal(state(s).m_present,false);assert.equal(state(s).m_value,0);s.dispatch(slot(s,'Read optional'));await deliver('read',{amount:1.25});assert.equal(state(s).m_present,true);assert.equal(state(s).m_value,1.25);s.dispose();
 console.log('PASS maintained four-plan compiler/runtime: edits, ownership, provider sequencing, allocation, component resolution, whole-basket quotes, invalidation, restore.');
})().catch(e=>{console.error(e);process.exitCode=1;});
