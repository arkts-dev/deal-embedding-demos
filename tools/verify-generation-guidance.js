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
 ctx.args=[previous,diagnostics];vm.runInContext("globalThis.done=false;globalThis.problem=null;feedback.patchInstruction.$f(...args).then(v=>{globalThis.result=v;done=true}).catch(e=>{problem=e;done=true});",ctx);
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
 assert(text.includes('RESERVED IDENTIFIERS (derived from the compiler lexer)'));assert(text.includes('from, function'));assert(!text.includes('number_literal'));
 assert(text.includes('required props: label, minorUnits'));assert(text.includes('requires children of types Money|Metric'));assert(text.includes('CHECKED LOCAL COMPARISON EXAMPLE'));assert(text.includes('backend never calculates a winner'));
 assert(text.includes('native write_file'));assert(text.includes('two independent native write_file calls in one initial response'));assert(text.includes('start_line/end_line'));assert(text.includes('both bounds to null'));assert(text.includes('Tool strings must decode to literal source'));assert(text.includes('never the renderer'));assert(text.includes('actual returned records after explicit loading'));assert(text.includes('A required provider result must come from that provider'));
 const workflow=vm.runInContext('guidance.workflow.$f()',ctx);assert(workflow.includes('never emulate a provider formula'));assert(workflow.includes('unless manual selection was explicitly requested'));assert(workflow.includes('Use write_file'));
 assert(text.includes('CHECKED ASYNC EXAMPLE'));assert(text.includes('host/sample'));assert(text.includes('not an available capability'));
 const previous=JSON.stringify({deal:'export class State {}',dealui:'ui.Column() {\nui.Toggle(value: true, checked: false)\n}'});
 const diagnostic=JSON.stringify([{code:'UI2051',file:'experience.dealui',line:2}]);
 const result=await instruction(previous,diagnostic);
 assert(result.includes('immutable'));assert(result.includes('ui.Toggle(value: true'));assert(result.includes('Exact renderer component contract'));assert(result.includes('Toggle'));assert(result.includes('string'));
 assert(result.includes('Call edit_sources'));assert(!result.includes('digest-bound exact patch'));
 const keyword=await instruction(JSON.stringify({deal:'    from: string = "";',dealui:''}),JSON.stringify([{code:'E1007',file:'experience.deal',line:1,column:5}]));assert(keyword.includes("Offending token 'from' is a reserved keyword"));assert(keyword.includes('do not rename wire fields'));
 const longer=await instruction(JSON.stringify({deal:'    fromInstant: string = "";',dealui:''}),JSON.stringify([{code:'E1007',file:'experience.deal',line:1,column:5}]));assert(!longer.includes('Offending token'));
 assert(result.includes('smallest rejected source region'));assert(!result.includes('both complete source strings'));assert(!vm.runInContext('feedback.instruction',ctx));
 const call=await instruction(previous,JSON.stringify([{code:'UI1016',file:'experience.dealui',line:2}]));assert(call.includes('cannot call functions'));assert(!call.includes('DEAL equality'));
 assert(result.includes('Diagnostic file: experience.dealui; patch file: dealui; line: 2'));
 assert(result.includes('2 | ui.Toggle(value: true'));
 const contaminated=JSON.stringify({deal:'export function main(): null { return null; }\n","dealui":"import * as app from \\"./experience\\";',dealui:'import * as app from "./experience";'});
 const escape=await instruction(contaminated,JSON.stringify([{code:'E1001',file:'experience.deal',line:2}]));
 assert(escape.includes('patch file: deal; line: 2'));assert(escape.includes('remove only the misplaced fragment from this file'));assert(escape.includes('do not edit the separate dealui file'));assert(!escape.includes('Array length is read-only'));
 const length=await instruction(previous,JSON.stringify({diagnostics:[{code:'E3017',range:{file:'src/experience.deal',startLine:1}}]}));assert(length.includes('array[array.length] = value'));assert(length.includes('patch file: deal; line: 1'));
 const unicode=await instruction(previous,JSON.stringify([{code:'E1052',file:'experience.deal',line:1}]));assert(unicode.includes('intended literal Unicode character'));assert(unicode.includes('do not unescape other valid source strings'));
 const typed=await instruction(previous,JSON.stringify([{code:'E3003',file:'experience.deal',line:1}]));assert(typed.includes('let cents: int = record.cents'));assert(!typed.includes('invalid backslash'));
 const generic=await instruction(previous,JSON.stringify([{code:'UI1009',file:'experience.dealui',line:2,message:"Expected ')'"}]));assert(!generic.includes('Use ==='));assert(!generic.includes('namespace imports'));assert(!generic.includes('misplaced fragment'));assert(!generic.includes('read-only'));
 assert(!(await instruction('not JSON',diagnostic)).includes('Exact reported source line'));
 let rejected=false;try{await instruction(previous,diagnostic,true)}catch(e){rejected=e.code==='STORAGE_FAILED'}assert(rejected,'operational parse failure must propagate');
 console.log('PASS compiled generation guidance: authored example; bounded source/pack feedback; invalid JSON safe; operational parser failure propagates');
})().catch(e=>{console.error(e);process.exitCode=1});
