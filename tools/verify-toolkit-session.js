"use strict";
// Current compiler output and production loader/session; mock only provider replies.
const fs=require('fs'),path=require('path'),vm=require('vm'),assert=require('assert');
const base=path.resolve(process.argv[2]),bindings=path.resolve(process.argv[3]);
for(const name of ['sandbox.js','dealui-session.js','ui-session.js']) vm.runInThisContext(fs.readFileSync(path.join(bindings,name),'utf8'));
configureDealCapabilities([{module:'host/sample',functions:[{name:'value',parameters:[],result:{kind:'int'}}]}]);
function mount(name,state=null){
 const dir=path.join(base,name,'js'),factories={};
 function walk(d){for(const name of fs.readdirSync(d)){const f=path.join(d,name);if(fs.statSync(f).isDirectory())walk(f);else if(name.endsWith('.js'))factories[path.relative(dir,f).split(path.sep).join('/')]=new Function('require','module','exports','process','console',fs.readFileSync(f,'utf8'));}}
 walk(dir);mountDealUi(factories,'experience_entry.js',state);return dealUi;
}
const snapshot=s=>JSON.parse(s.snapshot()),nodes=n=>[n,...n.children.flatMap(nodes)],prop=(n,k)=>n.props.find(p=>p.name===k);
const value=(n,k)=>{const p=prop(n,k);return p?.kind==='int'?p.intValue:p?.kind==='boolean'?p.booleanValue:p?.stringValue;};
function slot(s,label){const n=nodes(snapshot(s).tree).find(n=>value(n,'label')===label||value(n,'text')===label);assert(n,label);return n.props.find(p=>p.kind==='action').actionSlot;}
function result(s,status,amount){const ns=nodes(snapshot(s).tree);assert.equal(value(ns.find(n=>n.component.endsWith('.Comparison')),'status'),status);assert.equal(value(ns.find(n=>value(n,'label')==='Difference'),'minorUnits'),amount);}
const tick=()=>new Promise(resolve=>setImmediate(resolve));
(async()=>{
 let s=mount('comparison');result(s,'Over budget',1750);
 const edit=(label,v)=>s.dispatch(slot(s,label),String(v));
 edit('Approved budget (€)',20000);result(s,'Under budget',250);
 edit('Commitments (€)',20000);result(s,'On budget',0);
 edit('Approved budget (€)',-1);result(s,'On budget',0);
 edit('Approved budget (€)',10000001);result(s,'On budget',0);
 edit('Approved budget (€)',10000000);result(s,'Under budget',9980000);
 edit('Approved budget (€)',18300);result(s,'Over budget',1700);
 const checkpoint=JSON.parse(s.checkpoint());s.dispose();s=mount('comparison',checkpoint);result(s,'Over budget',1700);
 s.dispatch(slot(s,'Reset amounts'));result(s,'Over budget',1750);assert.throws(()=>s.dispatch(999999),/Invalid|stale/);s.dispose();
 s=mount('values');assert.deepEqual(nodes(snapshot(s).tree).filter(n=>n.component.endsWith('.Money')).map(n=>value(n,'minorUnits')),[1200,1300,5000]);s.dispose();
 s=mount('generation-example');assert(nodes(snapshot(s).tree).some(n=>value(n,'value')==='No reading yet'));assert.equal(JSON.parse(dealCapabilities.take()).length,0);
 s.dispatch(slot(s,'Read value'));assert(nodes(snapshot(s).tree).some(n=>n.component.endsWith('.Progress')));assert.equal(value(nodes(snapshot(s).tree).find(n=>n.component.endsWith('.PrimaryAction')),'enabled'),false);assert.equal(s.checkpoint(),null);
 let req=JSON.parse(dealCapabilities.take());assert.equal(req.length,1);dealCapabilities.deliver([{id:req[0].id,ok:true,value:42}]);await tick();await tick();assert.equal(JSON.parse(s.checkpoint()).value,42);
 s.dispatch(slot(s,'Read value'));req=JSON.parse(dealCapabilities.take());dealCapabilities.deliver([{id:req[0].id,ok:false,error:{code:'UNAVAILABLE',message:'private failure'}}]);await tick();await tick();
 const failure=nodes(snapshot(s).tree).find(n=>n.component.endsWith('.Failure'));assert.equal(value(failure,'text'),'Check access and try again');assert(!JSON.stringify(snapshot(s).tree).includes('private failure'));
 s.dispatch(prop(failure,'onRetry').actionSlot);req=JSON.parse(dealCapabilities.take());dealCapabilities.deliver([{id:req[0].id,ok:true,value:43}]);await tick();await tick();assert.equal(JSON.parse(s.checkpoint()).value,43);s.dispose();
 s=mount('primitives');assert.equal(JSON.parse(s.state()).text,'-2147483648|0|15:16||');assert.equal(JSON.parse(s.state()).bounded,true);s.dispose();
 s=mount('rows');
 const rowValues=()=>JSON.parse(s.state()).rows.map(r=>[r.id,r.quantity]);
 const oldA=slot(s,'a'),oldB=slot(s,'b');
 s.dispatch(oldA,'7');assert.deepEqual(rowValues(),[['a',7],['b',2]]);
 assert.throws(()=>s.dispatch(oldB,'9'),/Invalid|stale/);
 s.dispatch(slot(s,'Reverse rows'));assert.deepEqual(rowValues(),[['b',2],['a',7]]);
 s.dispatch(slot(s,'b'),'8');assert.deepEqual(rowValues(),[['b',8],['a',7]]);
 const removed=slot(s,'a');s.dispatch(slot(s,'Remove a'));assert.deepEqual(rowValues(),[['b',8]]);
 assert.throws(()=>s.dispatch(removed,'11'),/Invalid|stale/);s.dispose();
 console.log('PASS real compiler/VM: comparison over/under/equal, bounds, typed payloads, reset, settled restore; alternatives/deposit; empty/pending/success/failure/retry.');
})().catch(e=>{console.error(e);process.exitCode=1});
