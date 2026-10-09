"use strict";
// Compiled-core regressions. Baseline is the frozen pre-migration template, test-only.
const fs=require('fs'),path=require('path'),vm=require('vm'),assert=require('assert'),crypto=require('crypto');
const base=path.resolve(process.env.CORE_ASSETS||'build/embedding-consumer/assets/generation');
const factories={};
function walk(dir){for(const name of fs.readdirSync(dir)){const file=path.join(dir,name);if(fs.statSync(file).isDirectory())walk(file);else if(name.endsWith('.js'))factories[path.relative(base,file).split(path.sep).join('/')]=new Function('require','module','exports','process','console',fs.readFileSync(file,'utf8'));}}
walk(base);
vm.runInThisContext(fs.readFileSync('dependencies/deal-embedding/android/assets/embedding/bindings/sandbox.js','utf8'));
let calls=[],failure=null,dataFailure=null;
configureDealCapabilities([
 {module:'embedding/policy-data',functions:[{name:'parse',parameters:[{name:'input',type:{kind:'string'}}],result:{kind:'json-object'}},{name:'lowercase',parameters:[{name:'input',type:{kind:'string'}}],result:{kind:'string'}},{name:'utf16Length',parameters:[{name:'input',type:{kind:'string'}}],result:{kind:'int'}},{name:'sha256',parameters:[{name:'input',type:{kind:'string'}}],result:{kind:'string'}}]},
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
    if(dataFailure && req.function===dataFailure.function){dealCapabilities.deliver([{id:req.id,ok:false,error:{code:'DATA_FAILED',message:'Injected data mechanism failure'}}]);continue;}
    try{reply=req.function==='sha256'?crypto.createHash('sha256').update(req.args[0]).digest('hex'):req.function==='utf16Length'?req.args[0].length:req.function==='lowercase'?req.args[0].toLowerCase():JSON.parse(req.args[0]);if(req.function==='parse'&&(reply===null||Array.isArray(reply)||typeof reply!=='object'))throw Error('object required');}
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
 const warning=chosen.notice==='warning'?'ui.Notice(text: '+quote('Check suitability: '+spec+'. Not booked.')+', tone: "warning")':'';
 const values={'quote(readModule)':quote(read.module),'quote(prepareModule)':quote(prepare.module),readFunction:read.contractFunction.name,prepareFunction:prepare.contractFunction.name,condition,'Int.MAX_VALUE / quantity':String(Math.floor(2147483647/quantity)),quantity:String(quantity),'quote("Required: $spec. Period: $from to $deadline. ")':quote(`Required: ${spec}. Period: ${from} to ${deadline}. `),'quote(deadline)':quote(deadline),sourceName,header,'quote("$purpose $title")':quote(`${purpose} ${title}`),'quote("$quantity required · $from to $deadline")':quote(`${quantity} required · ${from} to ${deadline}`),warning,argumentSource:read.arguments.map(quote).join(', ')};
 // Explicit, reviewed UX revision against the unchanged pre-migration baseline.
 const revised={deal:baseline.deal
  .replace('Load available options for the requested period.','Find available equipment for your requirement.')
  .replace('Loading catalogue…','Loading equipment…')
  .replace('return { rows: state.rows, status: error.code + ": " + error.message, failed: true };','let message: string = "Couldn’t load equipment. Please try again."; if (error.code === "PROVIDER_DENIED" || error.code === "DENIED") { message = "Allow sharing in the Rental app, then try again."; } return { rows: state.rows, status: message, failed: true };')
  .replace('Review the selected specification, then prepare for native review.','Check the selected specification, then prepare your selection.')
  .replace('Prepared for native review. No reservation has been made.','Selection ready for review. Not booked.')
  .replace('return { status: error.code + ": " + error.message, failed: true };','return { status: "Couldn’t prepare your selection. Please try again.", failed: true };'),
 dealui:baseline.dealui
  .replace('        When(state.loading) { ui.Progress(label: "Waiting for connected app") } Else {','        When(state.loading) { ui.Progress(label: "Getting your options ready") }\n        ui.Button(text: "Load options", enabled: !state.loading, accessibilityLabel: "Load options", onClick: action app.Load {})\n        When(!state.loading) {')
  .replace('            ui.Button(text: "Load options", accessibilityLabel: "Load options", onClick: action app.Load {})\n','')
  .replaceAll('Prepare for review','Prepare selection')};
 return Object.fromEntries(Object.entries(revised).map(([key,text])=>[key,text.replace('${(0 until arguments.length()).joinToString(", ") { quote(arguments.getString(it)) }}','$argumentSource').replace(/\$\{(.*?)\}|\$(\w+)/g,(_,a,b)=>{assert(Object.hasOwn(values,a||b),a||b);return values[a||b];})]));
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
 const emptyInventory=await invoke('choice-policy','inventory','[]','{}','{\"entries\":[]}');
 dataFailure={function:'parse'};
 await assert.rejects(invoke('choice-policy','validate','{}',emptyInventory),error=>error.code==='DATA_FAILED');
 await assert.rejects(invoke('choice-policy','inventory','[]','{}','{\"entries\":[]}'),error=>error.code==='DATA_FAILED');
 dataFailure={function:'utf16Length'};
 const ctx={requirement:'Stand',specification:'Boom',quantity:1,from:'start',until:'end',searchTerms:['stand']};
 await assert.rejects(invoke('choice-policy','inventory',JSON.stringify(catalog),JSON.stringify(ctx),JSON.stringify({entries:Object.entries(ctx).filter(([k,v])=>typeof v==='string').map(([key,value])=>({key,value}))})),error=>error.code==='DATA_FAILED');
 dataFailure=null;
 assert.equal(await invoke('source-literals','decimal',-2147483648),'-2147483648');
 const pair={deal:'ab😀cd',dealui:'view'}; const pairRaw=JSON.stringify(pair);
 const digest=crypto.createHash('sha256').update(Array.from(pair.deal).length+':'+pair.deal+Array.from(pair.dealui).length+':'+pair.dealui).digest('hex');
 const patch=edits=>JSON.stringify({baseDigest:digest,edits});
 const edit=(file,search,replacement)=>({file,search,replacement});
 assert.deepStrictEqual(JSON.parse((await invoke('source-repair','apply',pairRaw,patch([edit('deal','😀','X'),edit('deal','ab','AB')]))).get('source')),{deal:'ABXcd',dealui:'view'});
 for (const [raw,code] of [[JSON.stringify({baseDigest:'stale',edits:[]}), 'PATCH_BASE'],[patch([]),'PATCH_LIMIT'],[patch([edit('deal','missing','x')]),'PATCH_AMBIGUOUS'],[patch([edit('deal','ab😀','x'),edit('deal','😀','y')]),'PATCH_OVERLAP'],[patch([edit('other','ab','x')]),'PATCH_SHAPE'],['invalid','PATCH_JSON']]) {
  const result=await invoke('source-repair','apply',pairRaw,raw); assert.equal(result.get('accepted'),false);assert.equal(result.get('diagnostics'),code);
 }
 const overlappingBase=JSON.stringify({deal:'aaa',dealui:'v'});
 const overlappingDigest=crypto.createHash('sha256').update('3:aaa1:v').digest('hex');
 assert.equal((await invoke('source-repair','apply',overlappingBase,JSON.stringify({baseDigest:overlappingDigest,edits:[edit('deal','aa','b')]}))).get('diagnostics'),'PATCH_AMBIGUOUS');
 assert((await invoke('source-repair','instruction',pairRaw)).includes(digest));
 const guidance=await invoke('generation-guidance','guidance');
 // Intentional guidance revision after real-source failures; retain exact serialization regression.
 assert.equal(crypto.createHash('sha256').update(guidance).digest('hex'),'5e98d7307d6b05afed7e5b2e832ac58d0d8241fb12de4680ba3ec10c6f5b3828','Generation guidance changed unexpectedly');
 assert(guidance.includes('When(state.loading)') && guidance.includes('./platform.dealui-pack'));
 assert(guidance.includes('enabled: !state.loading'));
 assert(guidance.includes('euro-cents') && guidance.includes('integer event payload'));
 assert(guidance.includes('Do not show module paths') && guidance.includes('Native Inspect'));
 assert(guidance.includes('onSelect: action app.Action { value: payload }'));
 assert(guidance.includes('onClick: action app.Action {}'));
 console.log(`Core migration: ${combinations} exact source-pair parity cases; 7 invalid envelopes; checker/JSON/UTF-16 host failure propagation; escaping/Unicode/quantity bounds; syntax and payload guidance regression`);
})().catch(e=>{console.error(e);process.exitCode=1});
