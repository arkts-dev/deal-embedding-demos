#!/usr/bin/env node
// Real compiled advisory policy; no model, native compiler or application authority.
const fs=require('fs'),path=require('path'),vm=require('vm'),assert=require('assert');
const root=path.resolve(__dirname,'..'),assets=path.join(root,'build/embedding-library/aar/assets');
const ctx=vm.createContext({});
for(const name of ['sandbox.js','generation-contracts.js','protocol-contracts.js','protocol-host.js'])vm.runInContext(fs.readFileSync(path.join(assets,'embedding/bindings',name),'utf8'),ctx);
let source='const factories=Object.create(null);\n';
function visit(dir){for(const name of fs.readdirSync(dir)){const file=path.join(dir,name);if(fs.statSync(file).isDirectory())visit(file);else if(name.endsWith('.js'))source+=`factories[${JSON.stringify(path.relative(path.join(assets,'generation'),file))}]=function(require,module,exports,process,console){${fs.readFileSync(file,'utf8')}\n};\n`;}}
visit(path.join(assets,'generation'));
vm.runInContext(`configureDealCapabilities(Object.values(dealHostContracts));${source}globalThis.feedback=dealLoad(factories,'repair-guidance.js').entry;globalThis.guidance=dealLoad(factories,'generation-guidance.js').entry;`,ctx);
async function instruction(previous,diagnostics,operational=false){
 ctx.args=[previous,diagnostics];vm.runInContext("globalThis.done=false;globalThis.problem=null;feedback.instruction.$f(...args).then(v=>{globalThis.result=v;done=true}).catch(e=>{problem=e;done=true});",ctx);
 for(let i=0;i<500&&!ctx.done;i++){
  await new Promise(r=>setImmediate(r));const requests=JSON.parse(vm.runInContext('dealCapabilities.take()',ctx));
  for(const r of requests){assert.equal(r.module,'embedding/policy-data');assert.equal(r.function,'parse');
   let reply;
   try{if(operational)throw {code:'STORAGE_FAILED',message:'operational fixture'};reply={id:r.id,ok:true,value:JSON.parse(r.args[0])};}
   catch(e){reply={id:r.id,ok:false,error:{code:e.code||'INVALID_JSON',message:'fixture'}};}
   ctx.reply=reply;vm.runInContext('dealCapabilities.deliver([reply])',ctx);
  }
 }
 assert(ctx.done,'compiled policy stuck');if(ctx.problem)throw ctx.problem;return ctx.result;
}
(async()=>{
 const text=vm.runInContext('guidance.guidance.$f()',ctx);
 assert(text.includes('literal Unicode character'));assert(text.includes('DEAL does not support Unicode escape sequences'));assert(text.includes('record["from"]'));assert(!text.includes('String-key indexing like'));
 assert(text.includes('required props: label, minorUnits'));assert(text.includes('requires children of types Money|Metric'));assert(text.includes('CHECKED LOCAL COMPARISON EXAMPLE'));assert(text.includes('backend never calculates a winner'));
 assert(text.includes('CHECKED ASYNC EXAMPLE'));assert(text.includes('host/sample'));assert(text.includes('not an available capability'));
 const previous=JSON.stringify({deal:'export class State {}',dealui:'ui.Column() {\nui.Toggle(value: true, checked: false)\n}'});
 const diagnostic=JSON.stringify([{code:'UI2051',file:'experience.dealui',line:2}]);
 const result=await instruction(previous,diagnostic);
 assert(result.includes('immutable'));assert(result.includes('ui.Toggle(value: true'));assert(result.includes('Exact renderer component contract'));assert(result.includes('Toggle'));assert(result.includes('string'));
 const call=await instruction(previous,JSON.stringify([{code:'UI1016',file:'experience.dealui',line:2}]));assert(call.includes('cannot call functions'));assert(!call.includes('DEAL equality'));
 const generic=await instruction(previous,JSON.stringify([{code:'UI1009',file:'experience.dealui',line:2,message:"Expected ')'"}]));assert(!generic.includes('Use ==='));assert(!generic.includes('namespace imports'));
 assert(!(await instruction('not JSON',diagnostic)).includes('Exact reported source line'));
 let rejected=false;try{await instruction(previous,diagnostic,true)}catch(e){rejected=e.code==='STORAGE_FAILED'}assert(rejected,'operational parse failure must propagate');
 console.log('PASS compiled generation guidance: authored example; bounded source/pack feedback; invalid JSON safe; operational parser failure propagates');
})().catch(e=>{console.error(e);process.exitCode=1});
