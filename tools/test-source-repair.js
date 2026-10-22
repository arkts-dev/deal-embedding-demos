#!/usr/bin/env node
// Exercise real compiled exact-patch policy, including decoded file context and failed targeting.
const fs=require('fs'),path=require('path'),vm=require('vm'),assert=require('assert'),crypto=require('crypto');
const assets=path.resolve(process.argv[2]||'build/embedding-library/aar/assets'),ctx=vm.createContext({});
for(const n of ['sandbox.js','generation-contracts.js','protocol-contracts.js','protocol-host.js'])vm.runInContext(fs.readFileSync(path.join(assets,'embedding/bindings',n),'utf8'),ctx);
const sources={};function walk(dir){for(const n of fs.readdirSync(dir)){const f=path.join(dir,n);if(fs.statSync(f).isDirectory())walk(f);else if(n.endsWith('.js'))sources[path.relative(path.join(assets,'generation'),f)]=fs.readFileSync(f,'utf8');}}walk(path.join(assets,'generation'));ctx.sources=sources;
vm.runInContext("configureDealCapabilities(Object.values(dealHostContracts));const factories=Object.fromEntries(Object.entries(sources).map(([k,v])=>[k,new Function('require','module','exports','process','console',v)]));globalThis.entry=dealLoad(factories,'source-repair.js').entry;",ctx);
async function call(method,...args){ctx.args=args;ctx.method=method;vm.runInContext('globalThis.done=false;globalThis.problem=null;entry[method].$f(...args).then(v=>{globalThis.result=v;done=true}).catch(e=>{problem=e;done=true});',ctx);
 for(let i=0;i<2000&&!ctx.done;i++){await new Promise(r=>setImmediate(r));const requests=JSON.parse(vm.runInContext('dealCapabilities.take()',ctx));ctx.replies=requests.map(r=>{assert.equal(r.module,'embedding/policy-data');try{const value=r.function==='parse'?JSON.parse(r.args[0]):r.function==='sha256'?crypto.createHash('sha256').update(r.args[0]).digest('hex'):undefined;assert.notEqual(value,undefined);return {id:r.id,ok:true,value};}catch(e){return {id:r.id,ok:false,error:{code:'INVALID_JSON',message:'fixture'}};}});vm.runInContext('dealCapabilities.deliver(replies)',ctx);}
 assert(ctx.done);if(ctx.problem)throw ctx.problem;return ctx.result;
}
function digest(p){return crypto.createHash('sha256').update(`${[...p.deal].length}:${p.deal}${[...p.dealui].length}:${p.dealui}`).digest('hex');}
(async()=>{
 const p={deal:'aaa\nmisplaced \\"UI\\" 🦔',dealui:'actual UI'};const base=JSON.stringify(p),patch=edits=>JSON.stringify({baseDigest:digest(p),edits});
 const instruction=await call('instruction',base);assert(instruction.includes('FILE deal (experience.deal)'));assert(instruction.includes(p.deal));assert(instruction.includes(p.dealui));assert(!instruction.includes(base));assert(instruction.includes('JSON-escape it exactly once'));
 let r=await call('apply',base,patch([{file:'dealui',search:'misplaced \\"UI\\" 🦔',replacement:''}]));assert(!r.get('accepted'));assert(r.get('diagnostics').startsWith('PATCH_NO_MATCH:'));assert(r.get('diagnostics').includes('editIndex=0'));assert(r.get('diagnostics').includes('file=dealui, matchCount=0, otherFile=deal, otherFileMatchCount=1'));assert.equal(r.get('source'),'');
 r=await call('apply',base,patch([{file:'deal',search:'aa',replacement:'b'}]));assert(r.get('diagnostics').startsWith('PATCH_MULTIPLE_MATCHES:'));assert(r.get('diagnostics').includes('matchCount=2'));
 r=await call('apply',base,patch([{file:'deal',search:'aaa',replacement:'one'},{file:'dealui',search:'absent',replacement:'no'}]));assert(!r.get('accepted'));assert(r.get('diagnostics').includes('editIndex=1'));assert.equal(r.get('source'),'');
 r=await call('apply',base,patch([{file:'deal',search:'aaa',replacement:'one'},{file:'deal',search:'aa',replacement:'two'}]));assert(!r.get('accepted'));
 r=await call('apply',base,JSON.stringify({baseDigest:'stale',edits:[{file:'deal',search:'aaa',replacement:'one'}]}));assert.equal(r.get('diagnostics'),'PATCH_BASE');
 r=await call('apply',base,patch([{file:'deal',search:'misplaced \\"UI\\" 🦔',replacement:''}]));assert(r.get('accepted'));assert.deepEqual(JSON.parse(r.get('source')),{deal:'aaa\n',dealui:p.dealui});
 const overlap={deal:'abcdef',dealui:'UI'},ob=JSON.stringify(overlap);r=await call('apply',ob,JSON.stringify({baseDigest:digest(overlap),edits:[{file:'deal',search:'abc',replacement:'x'},{file:'deal',search:'bcd',replacement:'y'}]}));assert.equal(r.get('diagnostics'),'PATCH_OVERLAP');
 console.log('PASS compiled source repair: decoded labelled base, zero/multiple/overlapping matches, wrong-file counts, edit index, atomic rejection, Unicode digest, exact application');
})().catch(e=>{console.error(e);process.exitCode=1});
