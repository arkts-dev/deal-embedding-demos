#!/usr/bin/env node
// Compiled trusted policy: deterministic lifecycle/boundary checks, never AI-quality evidence.
const fs=require('fs'),vm=require('vm'),assert=require('assert'),path=require('path');
const root=path.resolve(__dirname,'..'),assets=path.join(root,'build/embedding-consumer/assets/embedding/bindings');
const ctx=vm.createContext({});
for(const name of ['sandbox.js','generation-contracts.js','protocol-contracts.js','protocol-host.js'])vm.runInContext(fs.readFileSync(path.join(assets,name),'utf8'),ctx);
let factories='const factories=Object.create(null);\n';
const base=path.join(root,'build/calendar-policy/discovery');
function visit(dir){for(const name of fs.readdirSync(dir)){const file=path.join(dir,name);if(fs.statSync(file).isDirectory())visit(file);else if(name.endsWith('.js'))factories+=`factories[${JSON.stringify(path.relative(base,file))}]=function(require,module,exports,process,console){${fs.readFileSync(file,'utf8')}\n};\n`;}}
visit(base);
vm.runInContext(`configureDealCapabilities([...Object.values(dealHostContracts),{module:'host/preparation-facts',functions:[{name:'current',parameters:[],result:{kind:'string'}},{name:'remember',parameters:[{name:'state',type:{kind:'string'}}],result:{kind:'null'}}]}]);${factories}globalThis.entry=dealLoad(factories,'preparation.js').entry;`,ctx);
// Shape/choice mechanics only. Domain logic runs in compiler-emitted DEAL.
let calls=0,selected='related',current='',fail=false;
async function invoke(name,...args){
 vm.runInContext(`globalThis.result=null;globalThis.done=false;globalThis.problem=null;entry[${JSON.stringify(name)}].$f(...${JSON.stringify(args)}).then(v=>{result=v;done=true}).catch(e=>{problem=e;done=true});`,ctx);
 for(let i=0;i<1000&&!ctx.done;i++){
  await new Promise(r=>setImmediate(r));const requests=JSON.parse(vm.runInContext('dealCapabilities.take()',ctx));
  for(const r of requests){let value;
   if(r.module==='embedding/policy-data')value=r.function==='sha256'?require('crypto').createHash('sha256').update(r.args[0]).digest('hex'):JSON.parse(r.args[0]);
   else if(r.module==='host/preparation-facts')value=r.function==='current'?current:null;
   else if(r.module==='embedding/chooser'){calls++;if(globalThis.live){const response=await fetch('http://127.0.0.1:8787/generate',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({input:r.args[0],previous:'',diagnostics:'',capture:false})});if(!response.ok)throw Error('Gateway HTTP '+response.status);value=(await response.json()).content;}else if(fail){ctx.reply={id:r.id,ok:false,error:{code:'UNAVAILABLE',message:'offline'}};vm.runInContext('dealCapabilities.deliver([reply])',ctx);continue;}else {const issued=JSON.parse(r.args[0].split('\n\nISSUED OPTIONS\n')[1]);value=JSON.stringify({answers:Object.fromEntries(Object.keys(issued.questions).map((name,i)=>[name,i===0?selected:'none']))});}}
   else if(r.function==='questions')value=JSON.stringify({questions:Object.fromEntries(r.args[0].map(q=>[q.name,q.options.map(o=>o.name)])),decisionQuestions:Object.fromEntries(r.args[0].map(q=>[q.name,{type:'choice',instructions:q.instructions,criteria:Object.fromEntries(q.options.map(o=>[o.name,o.description]))}]))});
   else {const answers=JSON.parse(r.args[0]).answers;const accepted=r.args[1].every(q=>q.options.some(o=>o.name===answers[q.name]));value={accepted,reason:'',answers:accepted?r.args[1].map(q=>({question:q.name,choice:answers[q.name],probabilities:[],confidence:null})):[]};}
   ctx.reply={id:r.id,ok:true,value};vm.runInContext('dealCapabilities.deliver([reply])',ctx);
  }
 }
 if(ctx.problem)throw ctx.problem;assert(ctx.done,'Policy stuck');return JSON.parse(ctx.result);
}
(async()=>{
 if(process.argv.includes('--live')) {
  const cases=[
   ['Apartment viewing','Bring proof of income.','Download last three payslips','related'],
   ['Project budget review','Discuss supplier costs.','Check revised vendor quotation','related'],
   ['Client onboarding','Verify business registration.','Fetch company incorporation certificate','related'],
   ['School enrolment','We need evidence of where you live.','Print the latest electricity statement','related'],
   ['Review meeting','Bring the documents, if relevant.','Download the report','ask'],
   ['Swimming lesson','Bring a towel.','Check revised vendor quotation','none'],
   ['Budget review','Discuss supplier costs.','Download last three payslips','none'],
   ['Review meeting','Bring the documents, if relevant.','Pack','none'],
  ];
  // Live transport, same compiled policy and issued questions; no preselected answer.
  globalThis.live=true;
  let failures=0;
  for(const [title,note,task,expected] of cases){
   current=JSON.stringify({day:'2026-10-15',events:[{alias:'e0',id:'event',title,notes:note}],tasks:[{alias:'t0',id:'task',title:task,complete:false},{alias:'t1',id:'unrelated',title:'Water balcony plants',complete:false}]});
   const result=await invoke('discover',current,JSON.stringify({input:'',attempted:[],dismissed:[]}));
   const actual=result.status==='suggestion'?(result.suggestion.uncertain?'ask':'related'):result.status==='no_relationship'?'none':result.status;
   const ok=actual===expected&&(actual==='none'||result.suggestion.taskId==='task');if(!ok)failures++;
   console.log(JSON.stringify({title,task,expected,actual,ok}));
  }
  console.log(JSON.stringify({attempts:cases.length,failures}));if(failures)process.exitCode=1;return;
 }
 let state={input:'',attempted:[],dismissed:[]};
 function input(title='Appointment'){return JSON.stringify({day:'2026-10-15',events:[{alias:'e0',id:'e1',title,notes:'Bring proof of income.'}],tasks:[{alias:'t0',id:'t1',title:'Download last three payslips',complete:false},{alias:'t1',id:'t2',title:'Water balcony plants',complete:false}]});}
 current=input();let result=await invoke('discover',current,JSON.stringify(state));assert.equal(result.status,'suggestion');assert.equal(result.suggestion.taskId,'t1');state=result.state;
 await invoke('accept',JSON.stringify(result.suggestion));
 let repeated=await invoke('discover',current,JSON.stringify(state));assert.equal(repeated.status,'suggestion');assert.equal(calls,1);
 state=await invoke('dismiss',JSON.stringify(state),JSON.stringify(result.suggestion));current=input('Updated appointment');result=await invoke('discover',current,JSON.stringify(state));assert.equal(result.status,'no_relationship');
 current=input('Another event');selected='forged';result=await invoke('discover',current,JSON.stringify({input:'',attempted:[],dismissed:[]}));assert.equal(result.status,'rejected');
 selected='none';result=await invoke('discover',current,JSON.stringify({input:'',attempted:[],dismissed:[]}));assert.equal(result.status,'no_relationship');
 selected='ask';result=await invoke('discover',current,JSON.stringify({input:'',attempted:[],dismissed:[]}));assert(result.suggestion.uncertain);
 current='{}';let rejected=false;try{await invoke('accept',JSON.stringify(result.suggestion))}catch(e){rejected=true}assert(rejected);
 result=await invoke('discover',input(),JSON.stringify({input:'',attempted:[],dismissed:[]}));assert.equal(result.status,'stale');
 fail=true;current=input();result=await invoke('discover',current,JSON.stringify({input:'',attempted:[],dismissed:[]}));assert.equal(result.status,'unavailable');const count=calls;
 await invoke('discover',current,JSON.stringify(result.state));assert.equal(calls,count);
 console.log('PASS compiled preparation policy: references, duplicate admission, dismissals, none, ambiguity, stale acceptance/results, unavailable/no retry');
})().catch(e=>{console.error(e);process.exitCode=1});
