#!/usr/bin/env node
// Execute the authoritative compiled patch policy. No tolerant matching or Python clone.
const fs=require('fs'),path=require('path'),vm=require('vm'),assert=require('assert'),crypto=require('crypto');
const assets=path.resolve(process.argv[2]),ctx=vm.createContext({});
for(const n of ['sandbox.js','generation-contracts.js','protocol-contracts.js','protocol-host.js'])vm.runInContext(fs.readFileSync(path.join(assets,'embedding/bindings',n),'utf8'),ctx);
const factories={};
function walk(dir){for(const n of fs.readdirSync(dir)){const f=path.join(dir,n);if(fs.statSync(f).isDirectory())walk(f);else if(n.endsWith('.js'))factories[path.relative(path.join(assets,'generation'),f)]=fs.readFileSync(f,'utf8');}}
walk(path.join(assets,'generation'));ctx.sources=factories;ctx.base=fs.readFileSync(process.argv[3],'utf8');ctx.patch=fs.readFileSync(process.argv[4],'utf8');
vm.runInContext("configureDealCapabilities(Object.values(dealHostContracts));const factories=Object.fromEntries(Object.entries(sources).map(([k,v])=>[k,new Function('require','module','exports','process','console',v)]));const entry=dealLoad(factories,'source-repair.js').entry;globalThis.done=false;entry.apply.$f(base,patch).then(v=>{globalThis.result=v;done=true}).catch(e=>{globalThis.problem=e;done=true});",ctx);
(async()=>{
 for(let i=0;i<1000&&!ctx.done;i++){
  await new Promise(r=>setImmediate(r));
  const requests=JSON.parse(vm.runInContext('dealCapabilities.take()',ctx));
  const replies=requests.map(r=>{
   assert.equal(r.module,'embedding/policy-data');
   try{
    const value=r.function==='parse'?JSON.parse(r.args[0]):r.function==='sha256'?crypto.createHash('sha256').update(r.args[0]).digest('hex'):r.function==='utf16Length'?r.args[0].length:undefined;
    assert.notEqual(value,undefined);return {id:r.id,ok:true,value};
   }catch(e){return {id:r.id,ok:false,error:{code:'INVALID_JSON',message:'invalid JSON'}};}
  });ctx.replies=replies;vm.runInContext('dealCapabilities.deliver(replies)',ctx);
 }
 assert(ctx.done);if(ctx.problem)throw ctx.problem;
 process.stdout.write(JSON.stringify(ctx.result instanceof Map ? Object.fromEntries(ctx.result) : {accepted:ctx.result.get('accepted'),source:ctx.result.get('source'),diagnostics:ctx.result.get('diagnostics')}));
})().catch(e=>{console.error(e);process.exitCode=1});
