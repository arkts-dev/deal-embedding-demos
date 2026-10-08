"use strict";
// Compiled-core regressions. Baseline is the frozen pre-migration template, test-only.
const fs=require('fs'),path=require('path'),vm=require('vm'),assert=require('assert'),crypto=require('crypto');
const base=path.resolve(process.env.CORE_ASSETS||'build/embedding-consumer/assets/generation');
const factories={};
function walk(dir){for(const name of fs.readdirSync(dir)){const file=path.join(dir,name);if(fs.statSync(file).isDirectory())walk(file);else if(name.endsWith('.js'))factories[path.relative(base,file).split(path.sep).join('/')]=new Function('require','module','exports','process','console',fs.readFileSync(file,'utf8'));}}
walk(base);
vm.runInThisContext(fs.readFileSync('dependencies/deal-embedding/android/assets/embedding/bindings/sandbox.js','utf8'));
let calls=[],failure=null;
configureDealCapabilities([
 {module:'embedding/policy-data',functions:[{name:'parse',parameters:[{name:'input',type:{kind:'string'}}],result:{kind:'json-object'}},{name:'lowercase',parameters:[{name:'input',type:{kind:'string'}}],result:{kind:'string'}}]},
 {module:'embedding/checker',functions:[{name:'check',parameters:['deal','dealui'].map(name=>({name,type:{kind:'string'}})),result:JSON.parse(fs.readFileSync('dependencies/deal-embedding/core/host/check-result.json','utf8'))}]},
 {module:'embedding/observer',functions:[{name:'record',parameters:['stage','outcome','code'].map(name=>({name,type:{kind:'string'}})),result:{kind:'null'}}]}
]);
const loaded={};
function moduleOf(name){return loaded[name] ||= dealLoad(factories,name+'.js').entry;}
async function invoke(module,name,...args){
 let done=false,value,error;
 Promise.resolve(moduleOf(module)[name].$f(...args)).then(v=>{done=true;value=v},e=>{done=true;error=e});
 for(let i=0;i<200&&!done;i++){
  for(const req of JSON.parse(dealCapabilities.take())){
   let reply;
   if(req.module==='embedding/policy-data'){
    try{reply=req.function==='lowercase'?req.args[0].toLowerCase():JSON.parse(req.args[0]);if(req.function==='parse'&&(reply===null||Array.isArray(reply)||typeof reply!=='object'))throw Error('object required');}
    catch(e){dealCapabilities.deliver([{id:req.id,ok:false,error:{code:'INVALID_JSON',message:'Expected JSON object'}}]);continue;}
   }else if(req.module==='embedding/checker'){
    calls.push(req.args);if(failure){dealCapabilities.deliver([{id:req.id,ok:false,error:failure}]);continue;}
    reply={accepted:true,candidateId:'checked',diagnostics:''};
   }else reply=null;
   dealCapabilities.deliver([{id:req.id,ok:true,value:reply}]);
  }
  await new Promise(resolve=>setImmediate(resolve));
 }
 assert(done,'Core call did not complete');if(error)throw error;return value;
}
const quote=s=>'"'+s.replace(/\\/g,'\\\\').replace(/"/g,'\\"').replace(/\n/g,'\\n').replace(/\t/g,'\\t')+'"';
const baseline=JSON.parse(fs.readFileSync('tools/fixtures/catalogue-source-baseline.json','utf8'));
function expected(sourceName,selection){
 const {answers:chosen,read,preparation:prepare,context}=selection;
 const quantity=context.quantity,from=context.from,deadline=context.until,spec=context.specification,title=context.requirement;
 const condition=context.searchTerms.map(t=>'strings.contains(search, '+quote(t.toLowerCase())+')').join(' && ');
 const purpose=chosen.purpose==='choose'?'Choose':'Compare',header=chosen.headline==='hero'?'Hero':'Text';
 const warning=chosen.notice==='warning'?'ui.Notice(text: '+quote('Requested: '+spec+'. Availability is not a reservation.')+', tone: "warning")':'';
 const values={'quote(readModule)':quote(read.module),'quote(prepareModule)':quote(prepare.module),readFunction:read.contractFunction.name,prepareFunction:prepare.contractFunction.name,condition,'Int.MAX_VALUE / quantity':String(Math.floor(2147483647/quantity)),quantity:String(quantity),'quote("Required: $spec. Period: $from to $deadline. ")':quote(`Required: ${spec}. Period: ${from} to ${deadline}. `),'quote(deadline)':quote(deadline),sourceName,header,'quote("$purpose $title")':quote(`${purpose} ${title}`),'quote("$quantity required · $from to $deadline")':quote(`${quantity} required · ${from} to ${deadline}`),warning,argumentSource:read.arguments.map(quote).join(', ')};
 return Object.fromEntries(Object.entries(baseline).map(([key,text])=>[key,text.replace('${(0 until arguments.length()).joinToString(", ") { quote(arguments.getString(it)) }}','$argumentSource').replace(/\$\{(.*?)\}|\$(\w+)/g,(_,a,b)=>{assert(Object.hasOwn(values,a||b),a||b);return values[a||b];})]));
}
(async()=>{
 const str={kind:'string',maximum:8192};
 const read={name:'equipment',parameters:[{name:'from',type:str},{name:'until',type:str}],result:{kind:'array',element:{kind:'record',fields:{id:str,name:str,specification:str,cents:{kind:'int'},available:{kind:'int'}}}}};
 const prepare={name:'stage',parameters:['kind','provider','title','detail','price','deadline'].map(name=>({name,type:str})),result:{kind:'record',fields:{staged:{kind:'boolean'}}}};
 const catalog=[{module:'host/rental',functions:[read]},{module:'host/organizer',functions:[prepare]}];
 let combinations=0;
 for(const ctx of [
  {requirement:'Boom stand',specification:'Adjustable boom',quantity:1,from:'2026-10-09T16:00:00Z',until:'2026-10-09T22:00:00Z',searchTerms:['Boom','stand']},
  {requirement:'"Slash/Back\\Tab\tLine\n😀"',specification:'Specs "x" / \\ \n \t',quantity:128,from:'start',until:'end',searchTerms:['ÄBC','İ','BOOM']}
 ]){
  const entries=Object.entries(ctx).filter(([k,v])=>typeof v==='string').map(([key,value])=>({key,value}));
  const inv=await invoke('choice-policy','inventory',JSON.stringify(catalog),JSON.stringify(ctx),JSON.stringify({entries}));
  for(const purpose of ['compare','choose'])for(const headline of ['hero','text'])for(const notice of ['warning','none']){
   const answers={purpose,headline,notice,read:'read0'};
   const decision=await invoke('choice-policy','validate',JSON.stringify({answers}),inv);assert.equal(decision.get('accepted'),true);
   const actual=JSON.parse(await invoke('catalogue-source','lower','experience',decision.get('selection')));
   const selection={answers,read:{module:'host/rental',contractFunction:read,arguments:[ctx.from,ctx.until]},preparation:{module:'host/organizer',contractFunction:prepare},context:ctx};
   assert.deepStrictEqual(actual,expected('experience',selection));combinations++;
  }
 }
 for(const raw of ['bad','[]','null','{}','{"deal":"x"}','{"deal":1,"dealui":"x"}','{"deal":"x","dealui":"y","extra":true}']){
  const count=calls.length;const result=await invoke('candidate-check','check',raw);assert.equal(result.get('accepted'),false);assert.equal(calls.length,count);assert(result.get('diagnostics').includes('INVALID_CANDIDATE'));
 }
 assert.equal((await invoke('candidate-check','check','{"deal":"x","dealui":"y"}')).get('candidateId'),'checked');assert.deepStrictEqual(calls.at(-1),['x','y']);
 failure={code:'RESOURCE_FAILURE',message:'injected'};await assert.rejects(invoke('candidate-check','check','{"deal":"x","dealui":"y"}'),error=>error.code==='RESOURCE_FAILURE'&&error.message==='injected');failure=null;
 assert.equal(await invoke('source-literals','decimal',-2147483648),'-2147483648');
 const guidance=await invoke('generation-guidance','guidance');
 assert.equal(crypto.createHash('sha256').update(guidance).digest('hex'),'907338fecdafa04ad37b73316f68a0cbb8a8a9e7352ef8de33f8f4e8ef554f60','Prompt changed during migration');
 console.log(`Core migration: ${combinations} exact source-pair parity cases; 7 invalid envelopes; fatal host propagation; escaping/Unicode/quantity bounds; exact prompt parity`);
})().catch(e=>{console.error(e);process.exitCode=1});
