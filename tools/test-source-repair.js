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
function frame(edits,baseDigest) {return JSON.stringify({baseDigest,edits});}
function digest(p){return crypto.createHash('sha256').update(`${[...p.deal].length}:${p.deal}${[...p.dealui].length}:${p.dealui}`).digest('hex');}
(async()=>{
 const p={deal:'aaa\nmisplaced \\"UI\\" 🦔',dealui:'actual UI'};const base=JSON.stringify(p),patch=edits=>frame(edits,digest(p));
 const instruction=await call('instruction',base);assert(instruction.includes('FILE deal (experience.deal)'));assert(instruction.includes(p.deal));assert(instruction.includes(p.dealui));assert(!instruction.includes(base));
 let r=await call('apply',base,patch([{file:'dealui',search:'misplaced \\"UI\\" 🦔',replacement:''}]));assert(!r.get('accepted'));assert(r.get('diagnostics').startsWith('PATCH_NO_MATCH:'));assert(r.get('diagnostics').includes('editIndex=0'));assert(r.get('diagnostics').includes('file=dealui, matchCount=0, otherFile=deal, otherFileMatchCount=1'));assert.equal(r.get('source'),'');
 r=await call('apply',base,patch([{file:'deal',search:'aa',replacement:'b'}]));assert(r.get('diagnostics').startsWith('PATCH_MULTIPLE_MATCHES:'));assert(r.get('diagnostics').includes('matchCount=2'));
 assert(r.get('diagnostics').includes('Occurrence 1: lines 1-1'));assert(r.get('diagnostics').includes('Occurrence 2: lines 1-1'));assert.equal(r.get('source'),'');
 const repeated={deal:'🦔 head\r\nrepeat\nbody A\n\nrepeat\nbody B\n',dealui:'untouched'};
 r=await call('apply',JSON.stringify(repeated),frame([{file:'deal',search:'repeat',replacement:'x'}],digest(repeated)));
 assert(!r.get('accepted'));assert(r.get('diagnostics').includes('Occurrence 1: lines 2-2'));assert(r.get('diagnostics').includes('Occurrence 2: lines 5-5'));assert(r.get('diagnostics').includes('1 | 🦔 head\r'));assert(r.get('diagnostics').includes('6 | body B'));assert.equal(r.get('source'),'');
 const many={deal:Array.from({length:10},(_,i)=>'line '+i+'\nrepeat').join('\n'),dealui:'UI'};
 r=await call('apply',JSON.stringify(many),frame([{file:'deal',search:'repeat',replacement:'x'}],digest(many)));
 assert(r.get('diagnostics').includes('Occurrence 10: lines 20-20'));assert(r.get('diagnostics').includes('Only the first eight contexts'));assert(!r.get('diagnostics').includes('20 | repeat'));
 r=await call('apply',JSON.stringify(repeated),frame([{file:'deal',search:'repeat\nbody',replacement:'x'}],digest(repeated)));assert(r.get('diagnostics').includes('Occurrence 2: lines 5-6'));
 r=await call('apply',base,patch([{file:'deal',search:'aaa',replacement:'one'},{file:'dealui',search:'absent',replacement:'no'}]));assert(!r.get('accepted'));assert(r.get('diagnostics').includes('editIndex=1'));assert.equal(r.get('source'),'');
 r=await call('apply',base,patch([{file:'deal',search:'aaa',replacement:'one'},{file:'deal',search:'aa',replacement:'two'}]));assert(!r.get('accepted'));
 r=await call('apply',base,frame([{file:'deal',search:'aaa',replacement:'one'}],'stale'));assert.equal(r.get('diagnostics'),'PATCH_BASE');
 r=await call('apply',base,patch([{file:'deal',search:'misplaced \\"UI\\" 🦔',replacement:''}]));assert(r.get('accepted'));assert.deepEqual(JSON.parse(r.get('source')),{deal:'aaa\n',dealui:p.dealui});
 const overlap={deal:'abcdef',dealui:'UI'},ob=JSON.stringify(overlap);r=await call('apply',ob,frame([{file:'deal',search:'abc',replacement:'x'},{file:'deal',search:'bcd',replacement:'y'}],digest(overlap)));assert.equal(r.get('diagnostics'),'PATCH_OVERLAP');
 r=await call('apply',base,frame([],digest(p)));assert.equal(r.get('diagnostics'),'PATCH_LIMIT');
 const exact={deal:'\r\nquote " \\n 🦔\n',dealui:'UI'},eb=JSON.stringify(exact);
 r=await call('apply',eb,frame([{file:'deal',search:exact.deal,replacement:'\n\r\n" \\ 🦔\n'}],digest(exact)));assert(r.get('accepted'));assert.equal(JSON.parse(r.get('source')).deal,'\n\r\n" \\ 🦔\n');assert.equal(r.get('editCount'),1);
 for(const invalid of ['', ' \n\t', 'not a native edit']) {r=await call('apply',base,invalid);assert.equal(r.get('diagnostics'),'PATCH_SHAPE');}
 console.log('PASS compiled host-bound exact source edits: decoded labelled base, zero/multiple/overlapping matches, wrong-file counts, edit index, atomic rejection, Unicode digest, exact application');
})().catch(e=>{console.error(e);process.exitCode=1});
