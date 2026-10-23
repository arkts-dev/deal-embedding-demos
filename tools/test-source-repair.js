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
function frame(edits,baseDigest,token='patch') {return `DEAL-PATCH/1 ${token} ${baseDigest}`+edits.map(e=>`\n<<<${token}:${e.file}-search>>>\n${e.search}\n<<<${token}:replacement>>>\n${e.replacement}`).join('')+`\n<<<${token}:end>>>`;}
function digest(p){return crypto.createHash('sha256').update(`${[...p.deal].length}:${p.deal}${[...p.dealui].length}:${p.dealui}`).digest('hex');}
(async()=>{
 const p={deal:'aaa\nmisplaced \\"UI\\" 🦔',dealui:'actual UI'};const base=JSON.stringify(p),patch=edits=>frame(edits,digest(p));
 const instruction=await call('instruction',base);assert(instruction.includes('FILE deal (experience.deal)'));assert(instruction.includes(p.deal));assert(instruction.includes(p.dealui));assert(!instruction.includes(base));assert(instruction.includes('DEAL-PATCH/1'));assert(instruction.includes('including literal quotes/backslashes'));
 let r=await call('apply',base,patch([{file:'dealui',search:'misplaced \\"UI\\" 🦔',replacement:''}]));assert(!r.get('accepted'));assert(r.get('diagnostics').startsWith('PATCH_NO_MATCH:'));assert(r.get('diagnostics').includes('editIndex=0'));assert(r.get('diagnostics').includes('file=dealui, matchCount=0, otherFile=deal, otherFileMatchCount=1'));assert.equal(r.get('source'),'');
 r=await call('apply',base,patch([{file:'deal',search:'aa',replacement:'b'}]));assert(r.get('diagnostics').startsWith('PATCH_MULTIPLE_MATCHES:'));assert(r.get('diagnostics').includes('matchCount=2'));
 r=await call('apply',base,patch([{file:'deal',search:'aaa',replacement:'one'},{file:'dealui',search:'absent',replacement:'no'}]));assert(!r.get('accepted'));assert(r.get('diagnostics').includes('editIndex=1'));assert.equal(r.get('source'),'');
 r=await call('apply',base,patch([{file:'deal',search:'aaa',replacement:'one'},{file:'deal',search:'aa',replacement:'two'}]));assert(!r.get('accepted'));
 r=await call('apply',base,frame([{file:'deal',search:'aaa',replacement:'one'}],'stale'));assert.equal(r.get('diagnostics'),'PATCH_BASE');
 r=await call('apply',base,patch([{file:'deal',search:'misplaced \\"UI\\" 🦔',replacement:''}]));assert(r.get('accepted'));assert.deepEqual(JSON.parse(r.get('source')),{deal:'aaa\n',dealui:p.dealui});
 const overlap={deal:'abcdef',dealui:'UI'},ob=JSON.stringify(overlap);r=await call('apply',ob,frame([{file:'deal',search:'abc',replacement:'x'},{file:'deal',search:'bcd',replacement:'y'}],digest(overlap)));assert.equal(r.get('diagnostics'),'PATCH_OVERLAP');
 r=await call('apply',base,JSON.stringify({baseDigest:digest(p),edits:[{file:'deal',search:'aaa',replacement:'one'}]}));assert.equal(r.get('diagnostics'),'PATCH_FRAME','old JSON patch must be rejected');
 r=await call('apply',base,frame([],digest(p)));assert.equal(r.get('diagnostics'),'PATCH_FRAME');
 const exact={deal:'\r\nquote " \\n 🦔\n',dealui:'UI'},eb=JSON.stringify(exact);
 r=await call('apply',eb,frame([{file:'deal',search:exact.deal,replacement:'\n\r\n" \\ 🦔\n'}],digest(exact)));assert(r.get('accepted'));assert.equal(JSON.parse(r.get('source')).deal,'\n\r\n" \\ 🦔\n');assert.equal(r.get('editCount'),1);
 for(const invalid of ['', ' \n\t',patch([{file:'deal',search:'aaa',replacement:'x'}])+'junk','```\n'+patch([{file:'deal',search:'aaa',replacement:'x'}]),patch([{file:'deal',search:'aaa',replacement:'x'}]).replace('deal-search','unknown'),patch([{file:'deal',search:'aaa',replacement:'x'}]).replace('<<<patch:end>>>','')]) {r=await call('apply',base,invalid);assert.equal(r.get('diagnostics'),'PATCH_FRAME');}
 vm.runInContext("globalThis.framing=dealLoad(factories,'source-framing.js').entry",ctx);
 const source=(a,b,t='files')=>`DEAL-SOURCE/1 ${t}\n<<<${t}:deal>>>\n${a}\n<<<${t}:dealui>>>\n${b}\n<<<${t}:end>>>`;
 ctx.raw='\n\t '+source(exact.deal,p.dealui)+'\n\n ';const parsed=vm.runInContext('framing.source.$f(raw)',ctx);assert.equal(parsed.get('deal'),exact.deal);assert.equal(parsed.get('dealui'),p.dealui);
 for(const raw of ['', ' \n\t',base,source('',p.dealui),source(p.deal,p.dealui)+'tail',source(p.deal,p.dealui).replace(':dealui>>>',':deal>>>'),source('a\n<<<files:dealui>>>\ninside',p.dealui),source(p.deal,p.dealui).replace('DEAL-SOURCE/1 files','DEAL-SOURCE/1 bad_token')]) {ctx.raw=raw;assert.throws(()=>vm.runInContext('framing.source.$f(raw)',ctx));}
 for(const raw of ['x','DEAL-SOURCE/1','DEAL-SOURCE/1 t\n<<<t:', 'DEAL-SOURCE/1 t\n<<<t:deal>>>\ntext\n<<<t:end>>>', ...Array.from({length:100},(_,i)=>'x'.repeat(i))]) {ctx.raw=raw;try {vm.runInContext('framing.source.$f(raw)',ctx);assert.fail('Malformed frame accepted');}catch(e){assert.equal(e.code,'INVALID_FRAME');}}
 ctx.raw=source('valid\n<<<other:deal>>>\ntext',p.dealui);assert.equal(vm.runInContext('framing.source.$f(raw).get("deal")',ctx),'valid\n<<<other:deal>>>\ntext');
 console.log('PASS compiled raw framing and source repair: old JSON rejection, exact whitespace/backslash/Unicode payload, missing/duplicate/unknown/colliding boundaries, decoded labelled base, zero/multiple/overlapping matches, wrong-file counts, edit index, atomic rejection, Unicode digest, exact application');
})().catch(e=>{console.error(e);process.exitCode=1});
