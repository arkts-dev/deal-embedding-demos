#!/usr/bin/env node
// Execute packaged portable agent with deterministic native tools and real capability pump.
const fs=require('fs'),path=require('path'),vm=require('vm'),assert=require('assert'),crypto=require('crypto');
const assets=path.resolve('build/embedding-library/aar/assets');
const generation=path.resolve(process.argv[2]||path.join(assets,'generation'));
async function scenario(replies,verify){
 const ctx=vm.createContext({});for(const n of ['sandbox.js','generation-contracts.js','protocol-contracts.js','protocol-host.js'])vm.runInContext(fs.readFileSync(path.join(assets,'embedding/bindings',n),'utf8'),ctx);
 const sources={};function walk(dir){for(const n of fs.readdirSync(dir)){const f=path.join(dir,n);if(fs.statSync(f).isDirectory())walk(f);else if(n.endsWith('.js'))sources[path.relative(generation,f)]=fs.readFileSync(f,'utf8');}}walk(generation);ctx.sources=sources;
 vm.runInContext("configureDealCapabilities(Object.values(dealHostContracts));const factories=Object.fromEntries(Object.entries(sources).map(([k,v])=>[k,new Function('require','module','exports','process','console',v)]));globalThis.agent=dealLoad(factories,'source-agent.js').entry;globalThis.done=false;agent.run.$f('fixture','fixture').then(v=>{globalThis.result=v;done=true}).catch(e=>{globalThis.problem=e;done=true});",ctx);
 let calls=0,checks=[],patches=[];
 for(let i=0;i<5000&&!ctx.done;i++){
  await new Promise(r=>setImmediate(r));const req=JSON.parse(vm.runInContext('dealCapabilities.take()',ctx));ctx.replies=req.map(r=>{try{
   let v;
   if(r.module==='embedding/policy-data'){
    if(r.function==='parse')v=JSON.parse(r.args[0]);else if(r.function==='sha256')v=crypto.createHash('sha256').update(r.args[0]).digest('hex');else throw Error(r.function);
   } else if(r.module==='embedding/model'){
    assert(calls<replies.length,'Unexpected retry '+r.args[0]);const state=JSON.parse(r.args[0].split('\nNATIVE TOOL TURN\n')[1]);verify?.(calls,r.args,state);v=replies[calls++];
   } else if(r.module==='embedding/checker'){
    checks.push(r.args);v={accepted:r.args[0]==='valid',candidateId:r.args[0]==='valid'?'checked':'',diagnostics:'[]'};
   } else if(r.module==='embedding/observer') {if(r.args[0]==='patch'&&r.args[1]==='completed')patches.push(JSON.parse(r.args[2]));v=null;}
   else throw Error('Unexpected '+r.module+'.'+r.function);
   return {id:r.id,ok:true,value:v};
  }catch(e){if(r.module==='embedding/policy-data'&&r.function==='parse'&&e instanceof SyntaxError)return {id:r.id,ok:false,error:{code:'INVALID_JSON',message:'fixture'}};throw e;}});vm.runInContext('dealCapabilities.deliver(replies)',ctx);
 }
 assert(ctx.done,'did not settle');if(ctx.problem)throw ctx.problem;assert.equal(ctx.result.get('candidateId'),'checked');return {calls,checks,patches};
}
const call=(name,args,id='c')=>({id,type:'function',function:{name,arguments:JSON.stringify(args)}});
const msg=(...tool_calls)=>JSON.stringify({role:'assistant',content:'',tool_calls});
(async()=>{
 let r=await scenario([msg(call('write_file',{file:'deal',source:'broken'})),msg(call('write_file',{file:'dealui',source:'UI'})),msg(call('edit_sources',{edits:[{file:'deal',search:'missing',replacement:'valid'}]})),msg(call('write_file',{file:'deal',source:'valid'})),msg(call('read_file',{file:'deal'})),msg(call('edit_sources',{edits:[{file:'deal',search:'broken',replacement:'valid'}]}))],(n,args,state)=>{
  if(n===1){assert.equal(state.messages.length,2);assert.equal(state.messages[1].role,'tool');assert.equal(args[1],'');}
  if(n>=2)assert.equal(JSON.parse(args[1]).deal,'broken');
  if(n===3)assert(state.messages.at(-1).content.includes('PATCH_NO_MATCH'));
  if(n===4)assert(state.messages.at(-1).content.includes('FILE_EXISTS'));
  if(n===5)assert.equal(JSON.parse(state.messages.at(-1).content).source,'broken');
 });assert.equal(r.calls,6);assert.equal(r.checks.length,2);assert(!r.patches[0].accepted);assert(r.patches[1].accepted);
 r=await scenario(['not native tools',msg(call('check_sources',{})),msg(call('write_file',{file:'../secret',source:'bad'})),msg(call('write_file',{file:'deal',source:'valid'}),call('write_file',{file:'dealui',source:'UI'},'d'))],(n,args,state)=>{if(n===1)assert.equal(state.messages[0].role,'user');if(n===2)assert(state.messages.at(-1).content.includes('INCOMPLETE_PAIR'));if(n===3)assert(state.messages.at(-1).content.includes('TOOL_SHAPE'));});assert.equal(r.calls,4);assert.equal(r.checks.length,1);
 // Paired writes create one complete checked pair in either order and one model turn.
 for(const files of [[['deal','valid'],['dealui','UI']],[['dealui','UI'],['deal','valid']]]){
  r=await scenario([msg(...files.map(([file,source],i)=>call('write_file',{file,source},'batch'+i)))],(n,args,state)=>assert.equal(state.initialWrites,true));
  assert.equal(r.calls,1);assert.equal(r.checks.length,1);assert.deepEqual(r.checks[0],['valid','UI']);
 }
 // Preflight the entire initial batch: malformed/duplicate targets cannot partially stage.
 for(const bad of [
  msg(call('write_file',{file:'deal',source:'bad'}),call('write_file',{file:'deal',source:'again'},'d')),
  msg(call('write_file',{file:'deal',source:'bad'}),call('write_file',{file:'../secret',source:'bad'},'d')),
  msg(call('write_file',{file:'deal',source:'bad'}),call('write_file',{file:'dealui',source:' '},'d')),
  msg(call('write_file',{file:'deal',source:'bad'}),call('read_file',{file:'deal'},'d')),
  msg(call('write_file',{file:'deal',source:'bad'}),call('write_file',{file:'dealui',source:'UI'}))
 ]){
  r=await scenario([bad,msg(call('write_file',{file:'deal',source:'valid'}),call('write_file',{file:'dealui',source:'UI'},'d'))],(n,args,state)=>{assert.equal(state.initialWrites,true);if(n===1)assert.equal(state.messages.length,1);});
  assert.equal(r.checks.length,1);
 }
 r=await scenario([msg(call('write_file',{file:'deal',source:'valid'})),msg(call('write_file',{file:'dealui',source:'UI'}),call('write_file',{file:'deal',source:'bad'},'d')),msg(call('write_file',{file:'dealui',source:'UI'}))],(n,args,state)=>{assert.equal(state.initialWrites,n===0);if(n===2)assert.equal(state.messages.at(-1).role,'user');});
 assert.equal(r.checks.length,1);
 // Inclusive 1-based ranges; literal lines (including CR/Unicode/blank final line), no writes.
 const source='first\r\n€ 😀 "quote" \\\n\nlast\n';
 const reads=[
  [{file:'deal'},v=>assert.equal(v.source,source)],
  [{file:'deal',start_line:null,end_line:null},v=>assert.equal(v.source,source)],
  [{file:'deal',start_line:1,end_line:2},v=>{assert.deepEqual(v.lines,[{number:1,text:'first\r'},{number:2,text:'€ 😀 "quote" \\'}]);assert(!('source' in v));assert.equal(v.total_lines,5);}],
  [{file:'deal',start_line:3,end_line:5},v=>assert.deepEqual(v.lines,[{number:3,text:''},{number:4,text:'last'},{number:5,text:''}])],
  [{file:'dealui',start_line:1,end_line:1},v=>assert.deepEqual(v.lines,[{number:1,text:'UI'}])],
  ...[{start_line:0,end_line:1},{start_line:2,end_line:1},{start_line:1,end_line:6},{start_line:null,end_line:1},{start_line:1.5,end_line:2},{start_line:'1',end_line:2},{start_line:1},{start_line:1,end_line:2,extra:true},{a:null,b:null}].map(bounds=>[{file:'deal',...bounds},v=>assert.equal(v.accepted,false)]),
  [{file:'../secret',start_line:1,end_line:1},v=>assert.equal(v.accepted,false)]
 ];
 r=await scenario([msg(call('write_file',{file:'deal',source}),call('write_file',{file:'dealui',source:'UI'},'d')),...reads.map(([args])=>msg(call('read_file',args))),msg(call('edit_sources',{edits:[{file:'deal',search:source,replacement:'valid'}]}))],(n,args,state)=>{
  if(n>1)reads[n-2][1](JSON.parse(state.messages.at(-1).content));
  if(n>0){assert.equal(state.initialWrites,false);assert.equal(JSON.parse(args[1]).deal,source);}
 });assert.equal(r.checks.length,2);assert(r.patches[0].accepted);
 r=await scenario([msg(call('read_file',{file:'deal',start_line:null,end_line:null})),msg(call('read_file',{file:'deal',start_line:1,end_line:1})),msg(call('write_file',{file:'deal',source:'valid'}),call('write_file',{file:'dealui',source:'UI'},'d'))],(n,args,state)=>{
  if(n===1){const v=JSON.parse(state.messages.at(-1).content);assert.equal(v.source,'');assert.equal(v.total_lines,0);}
  if(n===2)assert.equal(JSON.parse(state.messages.at(-1).content).accepted,false);
 });assert.equal(r.checks.length,1);
 console.log('PASS packaged native agent: paired/single writes, atomic batch rejection, staging-only batching, exact edits, literal numbered ranges/full reads, invalid ranges and restricted paths');
})().catch(e=>{console.error(e);process.exitCode=1});
