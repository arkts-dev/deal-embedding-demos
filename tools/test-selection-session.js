"use strict";
// Real compiled DEAL and production loader/session. Mock only external providers/time.
const fs=require('fs'),path=require('path'),vm=require('vm'),assert=require('assert');
const base=path.resolve(process.argv[2]),bindings=path.resolve('build/embedding-library/aar/assets/embedding/bindings');
for(const n of ['sandbox.js','dealui-session.js','ui-session.js'])vm.runInThisContext(fs.readFileSync(path.join(bindings,n),'utf8'));
configureDealCapabilities(JSON.parse(fs.readFileSync(path.join(base,'plans/contracts.json'),'utf8')));
let time=1_000_000;const realNow=Date.now;Date.now=()=>time;
function mount(name,checkpoint=null){const dir=path.join(base,name,'check/js'),factories={};function walk(d){for(const n of fs.readdirSync(d)){const f=path.join(d,n);if(fs.statSync(f).isDirectory())walk(f);else if(n.endsWith('.js'))factories[path.relative(dir,f)]=new Function('require','module','exports','process','console',fs.readFileSync(f,'utf8'));}}walk(dir);mountDealUi(factories,'experience_entry.js',checkpoint);return dealUi;}
const state=s=>JSON.parse(s.state()),take=()=>JSON.parse(dealCapabilities.take()),tick=()=>new Promise(r=>setImmediate(r));
const flatten=n=>[n,...n.children.flatMap(flatten)],value=(n,k)=>{const p=n.props.find(p=>p.name===k);return p?.kind==='number'?p.numberValue:p?.kind==='boolean'?p.booleanValue:p?.stringValue;};
function slot(s,label){const n=flatten(JSON.parse(s.snapshot()).tree).find(n=>(value(n,'label')===label||value(n,'text')===label)&&n.props.some(p=>p.kind==='action'));assert(n,label);return n.props.find(p=>p.kind==='action').actionSlot;}
function clock(s){return flatten(JSON.parse(s.snapshot()).tree).find(n=>n.component.endsWith('.SessionClock')).props.find(p=>p.kind==='action').actionSlot;}
async function reply(req,value,ok=true){dealCapabilities.deliver([{id:req.id,ok,value,error:ok?undefined:{code:'UNAVAILABLE',message:'External failure'}}]);await tick();await tick();}
async function deliver(fn,value,ok=true){const rs=take();assert.equal(rs.length,1);assert.equal(rs[0].function,fn);await reply(rs[0],value,ok);return rs[0];}
const menu=[{id:'opaque-r',name:'Regular',cents:1000,vegan:false,diameterMm:300},{id:'opaque-v',name:'Plant',cents:1200,vegan:true,diameterMm:300},{id:'opaque-s',name:'Small',cents:1,vegan:true,diameterMm:180}];
const windows=[{id:'x',from:'17:45',until:'18:15',meetsRequirement:true,deliveryCents:500}];
const attrs=(kind,extra={})=>({kind,adjustable:false,clipAdapter:false,balanced:false,connectorFrom:'',connectorTo:'',lengthMm:0,...extra});
const equipment=[{id:'stand',name:'Opaque A',cents:800,available:2,attributes:attrs('stand',{adjustable:true,clipAdapter:true}),components:[]},{id:'cable',name:'Opaque B',cents:500,available:2,attributes:attrs('cable',{balanced:true,connectorFrom:'XLR female',connectorTo:'XLR male',lengthMm:10000}),components:[]},{id:'kit',name:'Opaque C',cents:1200,available:1,attributes:attrs('package'),components:[{itemId:'stand',quantity:1},{itemId:'cable',quantity:1}]}];
const lab=[{sku:'s',caption:'One',stock:1,spec:{role:'sensor'},contents:[]},{sku:'p',caption:'Two',stock:1,spec:{role:'power'},contents:[]},{sku:'l',caption:'Three',stock:1,spec:{role:'logger'},contents:[]},{sku:'bundle',caption:'Four',stock:1,spec:{role:'bundle'},contents:[{skuRef:'s',units:1},{skuRef:'p',units:1},{skuRef:'l',units:1}]}];
async function evaluateAll(fn,evaluator){let seen=[];for(;;){const rs=take();if(!rs.length)break;assert.equal(rs.length,1);assert.equal(rs[0].function,fn);seen.push(rs[0]);await reply(rs[0],evaluator(rs[0],seen.length));}return seen;}
(async()=>{
 if(process.argv[3]==='authored-food'){
  const s=mount('food');s.dispatch(slot(s,'Refresh'));await deliver('menu',menu);let req=take();assert.equal(req[0].args[0],'18:30');await reply(req[0],windows);assert(state(s).m_validResults.length);
  s.dispatch(slot(s,'Required by'),'1120');s.dispatch(slot(s,'Refresh'));await deliver('menu',menu);req=take();assert.equal(req[0].args[0],'18:40','Editable deadline must bind to the actual provider request');await reply(req[0],windows);s.dispose();return;
 }
 let s=mount('food');assert.equal(take().length,0);s.dispatch(slot(s,'Refresh'));await deliver('menu',menu);await deliver('delivery',windows);
 assert.equal(state(s).m_current,true);assert.deepEqual(state(s).m_validResults.map(x=>x.m_cost).sort((a,b)=>a-b),[6900,7100,7300,7500,7700]);assert.equal(state(s).m_items.length,3);
 // Independent exhaustive oracle: compare every feasible vector, not a hardcoded selected answer.
 const oracle=[];for(let a=0;a<=6;a++)for(let b=0;b<=6;b++)if(a+b===6&&b>=2&&a*1000+b*1200+500<=8000)oracle.push([a,b].join(','));
 assert.deepEqual(state(s).m_candidates.map(c=>c.m_items.slice(0,2).map(r=>r.m_quantity).join(',')).sort(),oracle.sort());
 s.dispatch(slot(s,'Budget'),'6800');assert.equal(state(s).m_validResults.length,0);s.dispatch(slot(s,'Refresh'));await deliver('menu',menu);await deliver('delivery',windows);assert.equal(state(s).m_candidates.length,0);
 s.dispatch(slot(s,'Budget'),'8000');s.dispatch(slot(s,'Refresh'));await deliver('menu',menu);await deliver('delivery',windows);assert(state(s).m_validResults.length);
 s.dispatch(slot(s,'Refresh'));await deliver('menu',null,false);assert.equal(state(s).m_current,false);assert.equal(state(s).m_validResults.length,0);assert(state(s).m_error);
 s.dispatch(slot(s,'Refresh'));s.dispatch(slot(s,'Required by'),'1120');await deliver('menu',menu);await deliver('delivery',windows);assert.equal(state(s).m_current,false);assert.equal(state(s).m_validResults.length,0);s.dispose();
 s=mount('food');s.dispatch(slot(s,'Refresh'));await deliver('menu',menu);await deliver('delivery',windows.map(w=>({...w,meetsRequirement:false})));assert.equal(state(s).m_validResults.length,0);s.dispose();
 s=mount('equipment');s.dispatch(slot(s,'Refresh'));await deliver('equipment',equipment);
 let seen=await evaluateAll('quote',r=>({cents:r.args[0]==='kit:1'?1200:1300,depositCents:5000,available:true,expiresInMinutes:1}));
 assert.deepEqual(seen.map(r=>r.args[0]).sort(),['kit:1','stand:1,cable:1']);assert.deepEqual(state(s).m_validResults.map(r=>r.m_cost).sort(),[1200,1300]);
 // Different receipt times; first quote must not borrow the last quote's TTL.
 s.dispatch(slot(s,'Refresh'));await deliver('equipment',equipment);let rs=take();assert.equal(rs.length,1);await reply(rs[0],{cents:1300,depositCents:5000,available:true,expiresInMinutes:1});time+=61000;rs=take();assert.equal(rs.length,1);await reply(rs[0],{cents:1200,depositCents:5000,available:true,expiresInMinutes:1});assert.equal(state(s).m_validResults.length,1);
 s.dispatch(clock(s),String(time-1000));assert.equal(state(s).m_validResults.length,0);s.dispose();
 s=mount('equipment');s.dispatch(slot(s,'Refresh'));await deliver('equipment',equipment);seen=await evaluateAll('quote',(r,i)=>({cents:100+i,depositCents:0,available:i!==1,expiresInMinutes:1}));assert.equal(seen.length,2);assert.equal(state(s).m_validResults.length,1);let saved=JSON.parse(s.checkpoint());s.dispose();s=mount('equipment',saved);s.dispatch(clock(s),String(-time));assert.equal(state(s).m_validResults.length,0);s.dispose();
 // Unrelated provider: two read arguments, nested schema without attributes, structured basket,
 // three evaluation arguments, three roles and millisecond TTL instead of minutes.
 s=mount('laboratory');s.dispatch(slot(s,'Refresh'));let req=take();assert.deepEqual(req[0].args,['north',true]);await reply(req[0],lab);
 seen=await evaluateAll('assess',r=>{assert.equal(r.args[0],'north');assert.equal(r.args[2],7);return {charge:r.args[1].length*100,ok:true,validForMs:5000};});
 assert.equal(seen.length,2);assert.deepEqual(seen.map(r=>r.args[1].map(x=>x.sku).sort().join(',')).sort(),['bundle','l,p,s']);assert.equal(state(s).m_validResults.length,2);
 s.dispatch(clock(s),String(time+5000));assert.equal(state(s).m_validResults.length,0);
 s.dispatch(slot(s,'Refresh'));req=take();s.dispatch(slot(s,'Zone'),'south');await reply(req[0],lab);await evaluateAll('assess',()=>({charge:100,ok:true,validForMs:5000}));assert.equal(state(s).m_zone,'south');assert.equal(state(s).m_current,false,'Text edits must fence pending completions');s.dispose();
 // Quantities beyond the retired task-scale ceiling and overlapping quotas.
 s=mount('food');s.dispatch(slot(s,'Required quantity'),'33');s.dispatch(slot(s,'Budget'),'100000');s.dispatch(slot(s,'Refresh'));await deliver('menu',[{...menu[1],cents:100}]);await deliver('delivery',windows);assert.equal(state(s).m_candidates.length,1);assert.equal(state(s).m_candidates[0].m_items[0].m_quantity,33);assert.equal(state(s).m_validResults[0].m_cost,3800);s.dispose();
 // Three roles at quantity two: separate, mixed and bundled baskets share physical stock.
 s=mount('laboratory');s.dispatch(slot(s,'Stations'),'2');s.dispatch(slot(s,'Refresh'));await deliver('inventory',lab.map(r=>({...r,stock:2})));seen=await evaluateAll('assess',()=>({charge:1,ok:true,validForMs:5000}));assert.equal(seen.length,3);assert(seen.some(r=>r.args[1].some(i=>i.sku==='bundle'&&i.units===1)&&r.args[1].length===4));s.dispose();
 // Unknown/duplicate components and aggregate stock must not produce authoritative calls.
 s=mount('laboratory');s.dispatch(slot(s,'Refresh'));await deliver('inventory',lab.map(r=>r.sku==='bundle'?{...r,contents:[...r.contents,{skuRef:'missing',units:1}]}:r));seen=await evaluateAll('assess',()=>({charge:1,ok:true,validForMs:5000}));assert.equal(seen.length,1);assert.equal(seen[0].args[1].length,3);s.dispose();
 s=mount('laboratory');s.dispatch(slot(s,'Refresh'));await deliver('inventory',lab.map(r=>r.sku==='s'?{...r,stock:0}:r));assert.equal(take().length,0);assert.equal(state(s).m_candidates.length,0);s.dispose();
 s=mount('laboratory');s.dispatch(slot(s,'Refresh'));await deliver('inventory',[...lab,lab[0]]);assert(state(s).m_error);assert.equal(take().length,0);s.dispose();
 console.log('PASS prototype: food allocation/oracle; two- and three-role baskets; distinct provider signatures; authoritative unavailable replies; edits/failure/stale completion; per-reply expiry/backward clock/restoration.');
})().catch(e=>{console.error(e);process.exitCode=1;}).finally(()=>{Date.now=realNow;});
