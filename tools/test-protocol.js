#!/usr/bin/env node
// Actual packaged application linkage; native mechanisms are the only mock boundary.
'use strict';
const fs=require('fs'),path=require('path'),vm=require('vm'),assert=require('assert');
const assets=path.resolve('build/embedding-library/aar/assets'),base=path.join(assets,'protocol-application');
const context=vm.createContext({});
for(const name of ['sandbox.js','protocol-contracts.js','protocol-host.js','public-protocol.js'])vm.runInContext(fs.readFileSync(path.join(assets,'embedding/bindings',name),'utf8'),context);
const sources={};
function walk(dir){for(const name of fs.readdirSync(dir)){const file=path.join(dir,name);if(fs.statSync(file).isDirectory())walk(file);else if(name.endsWith('.js'))sources[path.relative(base,file)]=fs.readFileSync(file,'utf8');}}
walk(base);context.sources=sources;
vm.runInContext(`const factories=Object.fromEntries(Object.entries(sources).map(([id,source])=>[id,new Function('require','module','exports','process','console',source)]));protocolInitialize(factories,'application.js',{seed:'fixture',saved:[],contracts:[],health:{incomplete:false,active:false,code:'',lost:0}});`,context);
let sequence=0;
async function call(name,args,handler){
 const key=String(++sequence);context.args=args;context.key=key;context.name=name;
 vm.runInContext('protocolCall(key,name,args)',context);
 for(let turn=0;turn<1000;turn++){
  await new Promise(resolve=>setImmediate(resolve));
  const requests=JSON.parse(vm.runInContext('dealCapabilities.take()',context));
  for(const request of requests){assert(handler,'Unexpected native invocation: '+request.function);context.reply={id:request.id,ok:true,value:handler(request)};vm.runInContext('dealCapabilities.deliver([reply])',context);}
  const result=JSON.parse(vm.runInContext('JSON.stringify(publicResults.get(key))',context));
  if(result.done){if(result.error)throw result.error;return result.value;}
 }
 throw Error('Protocol did not settle');
}
(async()=>{
 const source={deal:'literal source',dealui:'literal view'};
 const experience=await call('load',['Fixture',source.deal,source.dealui],request=>{assert.equal(request.function,'load');assert.deepEqual(request.args,['Fixture',source]);return {experience:{id:'e',title:'Fixture',origin:'supplied',attempts:0},source};});
 assert.equal(experience.id,'e');
 await call('run',['e'],request=>{assert.equal(request.function,'run');return {id:'s',trace:'trace'};});
 const prop={name:'value',kind:'number',stringValue:'',numberValue:1.5,booleanValue:false,actionSlot:-1,actionBindings:{}};
 const tree={component:'Text',structural:'root',keyKind:'number',keyNumber:1.25,keyString:'',props:[prop],children:[]};
 const update=await call('next',['s',''],request=>{assert.equal(request.function,'next');return {snapshot:{tree,version:1,fault:''},cursor:'cursor'};});
 assert.equal(update.snapshot.tree.keyNumber,1.25);assert.equal(update.snapshot.tree.props[0].numberValue,1.5);
 for(const [name,args] of [['dispatch',['s',0.5,null]],['published',['s',0.5]],['events',['','',1.5]]]){
  await assert.rejects(call(name,args),error=>error.code==='INVALID_ARGUMENT');
 }
 const event={trace:'t',span:'s',parent:'',stage:'snapshot',outcome:'produced',code:'',experience:'e',session:'s',operation:'',target:'',durationMs:1.5,version:1,sequence:1,time:'0',elapsed:'0',artifact:'',content:'omitted',bytes:0,sha256:''};
 const page=await call('events',['','',20],request=>{assert.equal(request.function,'events');return {events:[event],cursor:'',incomplete:false};});
 assert.equal(page.events[0].sha256,'');assert.equal(page.events[0].durationMs,1.5);
 context.bad={...event};delete context.bad.sha256;
 assert.throws(()=>vm.runInContext("protocolRecord('@$external/embedding/Event',bad)",context),error=>error.code==='E8001'&&error.message.includes('Invalid Event transport'));
 console.log('PASS packaged public linkage: explicit records, fractional values/keys, snapshot/event transport, strict integral controls and missing-field rejection');
})().catch(error=>{console.error(error);process.exitCode=1;});
