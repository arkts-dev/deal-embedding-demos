#!/usr/bin/env node
// Preserved-candidate behavior oracle: real compiler/loader/session, provider data boundary only.
const fs=require('fs'),path=require('path'),vm=require('vm'),assert=require('assert');
const dir=path.resolve(process.argv[2]),bindings=path.resolve(process.argv[3]);
for(const n of ['sandbox.js','dealui-session.js','ui-session.js'])vm.runInThisContext(fs.readFileSync(path.join(bindings,n),'utf8'));
const type=kind=>({kind}),record=fields=>({kind:'record',fields:Object.entries(fields).map(([name,kind])=>({name,type:type(kind),optional:false}))});
configureDealCapabilities([{module:'host/pizza',functions:[{name:'menu',parameters:[],result:{kind:'array',element:record({id:'string',name:'string',size:'string',cents:'number',vegan:'boolean'})}},{name:'delivery',parameters:[{name:'arrival',type:{kind:'string'}}],result:{kind:'array',element:record({id:'string',from:'string',until:'string',meetsRequirement:'boolean',deliveryCents:'number'})}}]}]);
const factories={};function walk(d){for(const n of fs.readdirSync(d)){const f=path.join(d,n);if(fs.statSync(f).isDirectory())walk(f);else if(n.endsWith('.js'))factories[path.relative(dir,f)]=new Function('require','module','exports','process','console',fs.readFileSync(f,'utf8'));}}walk(dir);
const menu=[{id:'margherita',name:'Margherita',size:'30 cm',cents:1000,vegan:false},{id:'garden',name:'Garden Vegan',size:'30 cm',cents:1200,vegan:true},{id:'pepperoni',name:'Pepperoni',size:'30 cm',cents:1300,vegan:false}];
const windows=[{id:'w1',from:'17:45',until:'18:15',meetsRequirement:true,deliveryCents:500},{id:'w2',from:'18:15',until:'18:45',meetsRequirement:false,deliveryCents:500}];
const tick=()=>new Promise(r=>setImmediate(r));
const nodes=n=>[n,...n.children.flatMap(nodes)],prop=(n,k)=>n.props.find(p=>p.name===k),value=(n,k)=>{const p=prop(n,k);return p?.kind==='number'?p.numberValue:p?.stringValue};
(async()=>{
 mountDealUi(factories,'experience_entry.js');const s=dealUi,tree=()=>JSON.parse(s.snapshot()).tree;
 const slot=label=>{const n=nodes(tree()).find(n=>value(n,'text')===label||value(n,'label')===label);assert(n,label);return n.props.find(p=>p.kind==='action').actionSlot};
 assert.equal(JSON.parse(dealCapabilities.take()).length,0);s.dispatch(slot('Load menu'));let calls=0;
 for(let i=0;i<100;i++){
  const requests=JSON.parse(dealCapabilities.take());
  for(const r of requests){assert.equal(r.module,'host/pizza');calls++;assert(['menu','delivery'].includes(r.function));if(r.function==='delivery')assert.equal(r.args[0],'18:30');dealCapabilities.deliver([{id:r.id,ok:true,value:r.function==='menu'?menu:windows}]);}
  await tick();if(s.checkpoint()!==null&&calls===2)break;
 }
 assert.equal(calls,2);assert.equal(JSON.parse(s.state()).grandTotal,7100);assert.equal(JSON.parse(s.state()).totalPizzas,6);assert.equal(JSON.parse(s.state()).veganCount,3);assert.equal(JSON.parse(s.state()).feasible,true);
 const row=nodes(tree()).find(n=>value(n,'accessibilityLabel')?.startsWith('Margherita'));assert(row);s.dispatch(row.props.find(p=>p.kind==='action').actionSlot,'4');assert.equal(JSON.parse(s.state()).grandTotal,8100);assert.equal(JSON.parse(s.state()).totalPizzas,7);assert.equal(JSON.parse(s.state()).feasible,false);assert.equal(JSON.parse(dealCapabilities.take()).length,0);
 s.dispose();console.log('PASS preserved actual menu/delivery arithmetic, bound row edit and no hidden provider execution');
})().catch(e=>{console.error(e);process.exitCode=1});
