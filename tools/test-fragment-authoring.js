"use strict";
// Inference-free reassessment of exact model-authored source, never repair or retiming.
const fs=require('fs'),path=require('path'),vm=require('vm'),assert=require('assert');
const root=path.resolve(process.argv[2]),task=process.argv[3],plan=JSON.parse(fs.readFileSync(path.join(root,task+'.json')));
for(const n of ['sandbox.js','dealui-session.js','ui-session.js'])vm.runInThisContext(fs.readFileSync(path.join('build/embedding-library/aar/assets/embedding/bindings',n),'utf8'));
configureDealCapabilities(JSON.parse(fs.readFileSync(path.join(root,'contracts.json'))));
const dir=path.join(root,task,'check/js'),factories={};
function walk(d){for(const n of fs.readdirSync(d)){const f=path.join(d,n);if(fs.statSync(f).isDirectory())walk(f);else if(n.endsWith('.js'))factories[path.relative(dir,f)]=new Function('require','module','exports','process','console',fs.readFileSync(f,'utf8'));}}walk(dir);
mountDealUi(factories,'experience_entry.js',null);const s=dealUi;
const state=()=>JSON.parse(s.state()),get=(f,p)=>state()['m_'+f.id+p],nodes=n=>[n,...n.children.flatMap(nodes)];
const prop=(n,k)=>{const p=n.props.find(p=>p.name===k);return p?.kind==='number'?p.numberValue:p?.kind==='boolean'?p.booleanValue:p?.stringValue;};
function dispatch(label,payload){const n=nodes(JSON.parse(s.snapshot()).tree).find(n=>['label','text','title'].some(k=>prop(n,k)===label)&&n.props.some(p=>p.kind==='action'));assert(n,'Missing control '+label);s.dispatch(n.props.find(p=>p.kind==='action').actionSlot,payload);}
const tick=()=>new Promise(r=>setImmediate(r)),requests=()=>JSON.parse(dealCapabilities.take());
async function reply(fn,value){const req=requests();assert.equal(req.length,1);assert.equal(req[0].function,fn);dealCapabilities.deliver([{id:req[0].id,ok:true,value}]);await tick();await tick();assert.equal(JSON.parse(s.snapshot()).fault,'');return req[0];}
const find=(kind,p=()=>true)=>{const f=plan.fragments.find(f=>f.kind===kind&&p(f));assert(f,'Missing '+kind);return f;};
const input=value=>find('input',f=>f.config.value===value);
const read=fn=>find('read',f=>f.config.binding.function===fn);
const start=()=>{assert.equal(plan.commands.length,1);dispatch(plan.commands[0].label);};
(async()=>{
 assert.equal(requests().length,0,'No implicit provider calls on mount');
 if(task==='budget'){
  const c=find('comparison');assert.equal(get(c,'Difference'),1750);dispatch(input(18000).config.label,'20000');assert.equal(get(c,'Difference'),-250);dispatch(input(19750).config.label,'20000');assert.equal(get(c,'Difference'),0);
 }else if(task==='departure'){
  const travel=read('estimate');start();const request=await reply('estimate',29);assert.deepEqual(request.args,[12]);
  const outputs=plan.fragments.filter(f=>f.kind==='result');assert(outputs.some(f=>get(f,'Value')===916),'Departure must use provider travel');assert(outputs.some(f=>get(f,'Value')===906),'Inspection must precede departure');
  dispatch(input(960).config.label,'985');assert(outputs.some(f=>get(f,'Value')===941));assert(outputs.some(f=>get(f,'Value')===931));dispatch(input(12).config.label,'13');assert.equal(get(travel,'Present'),false);assert(outputs.every(f=>get(f,'Present')===false));
 }else if(task==='food'){
  const alloc=find('allocation'),choice=find('choice');start();await reply('menu',[{id:'model-standard',name:'Standard',cents:1000,vegan:false,diameterMm:300},{id:'model-vegan',name:'Plant',cents:1200,vegan:true,diameterMm:300}]);
  const req=await reply('delivery',[{id:'on-time',from:'17:45',until:'18:15',meetsRequirement:true,deliveryCents:500},{id:'too-late',from:'18:30',until:'19:00',meetsRequirement:false,deliveryCents:500}]);assert.deepEqual(req.args,['18:30']);assert.equal(get(alloc,'Valid'),true);assert.equal(get(alloc,'Count'),6);assert.equal(get(choice,'Selected').m_cost,500);
  const actualCost=get(alloc,'Rows').reduce((n,r)=>n+r.m_cost*r.m_quantity,0)+get(choice,'Selected').m_cost;assert.equal(actualCost,6900);
  const deadline=find('input',f=>f.config.value===1110||f.config.value==='18:30');dispatch(deadline.config.label,deadline.config.format==='time'?'1120':'18:40');assert.equal(get(alloc,'Valid'),false,'Edited deadline must revoke readiness');
  start();await reply('menu',[{id:'model-vegan',name:'Plant',cents:1200,vegan:true,diameterMm:300}]);const next=await reply('delivery',[{id:'late',from:'18:30',until:'19:00',meetsRequirement:false,deliveryCents:500}]);assert.deepEqual(next.args,['18:40']);assert.equal(get(alloc,'Valid'),false,'Late-only delivery must not be Ready');
 }else if(task==='equipment'){
  const budget=find('input',f=>f.config.format==='money');assert.equal(budget.config.value,2000,'Keep disclosed cents unchanged');
  const attrs=(kind,x={})=>({kind,adjustable:false,clipAdapter:false,balanced:false,connectorFrom:'',connectorTo:'',lengthMm:0,...x});
  const items=[{id:'stand-varied',name:'Rig A',cents:800,available:1,attributes:attrs('stand',{adjustable:true,clipAdapter:true}),components:[]},{id:'cable-varied',name:'Rig B',cents:500,available:1,attributes:attrs('cable',{balanced:true,connectorFrom:'XLR female',connectorTo:'XLR male',lengthMm:10000}),components:[]},{id:'bundle-varied',name:'Rig C',cents:1200,available:1,attributes:attrs('package'),components:[{itemId:'stand-varied',quantity:1},{itemId:'cable-varied',quantity:1}]}];
  start();const cat=await reply('equipment',items);assert.deepEqual(cat.args,['2026-10-23T14:15:00Z','2026-10-23T20:30:00Z']);let total=0;
  for(;;){const batch=requests();if(!batch.length)break;assert.equal(batch.length,1);const q=batch[0];assert.equal(q.function,'quote');assert.deepEqual(q.args.slice(1),cat.args);const bundled=q.args[0]==='bundle-varied:1';dealCapabilities.deliver([{id:q.id,ok:true,value:{cents:bundled?1200:1300,depositCents:5000,available:true,expiresInMinutes:15}}]);await tick();await tick();total++;assert(total<20);}
  assert.equal(total,2,'Minimal package and separate alternatives');const quotes=find('quotes');assert(get(quotes,'Current').some(q=>q.m_cost===1200));assert(get(quotes,'Current').some(q=>q.m_cost===1300));assert(get(quotes,'Current').every(q=>q.m_deposit===5000));
 }else if(task==='laboratory'){
  const alt=find('alternatives');start();await reply('catalogue',[{code:'unit-varied',title:'Universal unit',stock:1,features:{compatible:true},parts:[]}]);assert.equal(get(alt,'Baskets').length,0);start();await reply('catalogue',[{code:'unit-varied',title:'Universal unit',stock:3,features:{compatible:true},parts:[]}]);assert.equal(get(alt,'Baskets').length,1);assert.equal(get(alt,'Baskets')[0].m_items[0].m_quantity,3);
 }
 console.log('PASS exact model-authored '+task+' runtime reassessment (not latency evidence)');s.dispose();
})().catch(e=>{console.error(e);s.dispose();process.exitCode=1;});
