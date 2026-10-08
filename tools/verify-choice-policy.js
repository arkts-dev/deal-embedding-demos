"use strict";
// Compare compiled DEAL policy against the frozen, previously verified Kotlin decisions.
// This test-only oracle is not shipped and is not an implementation fallback.
const fs = require('fs'), path = require('path'), vm = require('vm'), assert = require('assert');
const base = path.resolve('build/embedding-consumer/assets/generation');
const factories = Object.create(null);
function walk(dir) {
  for (const name of fs.readdirSync(dir)) {
    const file = path.join(dir, name);
    if (fs.statSync(file).isDirectory()) walk(file);
    else if (name.endsWith('.js')) factories[path.relative(base, file).split(path.sep).join('/')] = new Function('require','module','exports','process','console',fs.readFileSync(file,'utf8'));
  }
}
walk(base);
vm.runInThisContext(fs.readFileSync('dependencies/deal-embedding/android/assets/embedding/bindings/sandbox.js','utf8'));
configureDealCapabilities([{module:'embedding/policy-data',functions:[{name:'parse',parameters:[{name:'input',type:{kind:'string'}}],result:{kind:'json-object'}},{name:'lowercase',parameters:[{name:'input',type:{kind:'string'}}],result:{kind:'string'}},{name:'utf16Length',parameters:[{name:'input',type:{kind:'string'}}],result:{kind:'int'}}]}]);
const {rt,entry: policy} = dealLoad(factories,'choice-policy.js');
async function invoke(fn,...args) {
  let done=false,result,error;
  fn.$f(...args).then(v=>{done=true;result=v;},e=>{done=true;error=e;});
  for(let i=0;i<100&&!done;i++) {
    for(const req of JSON.parse(dealCapabilities.take())) {
      try { const v=req.function==='utf16Length'?req.args[0].length:req.function==='lowercase'?req.args[0].toLowerCase():JSON.parse(req.args[0]); if(req.function==='parse'&&(v===null||Array.isArray(v)||typeof v!=='object'))throw Error('object required'); dealCapabilities.deliver([{id:req.id,ok:true,value:v}]); }
      catch(e){dealCapabilities.deliver([{id:req.id,ok:false,error:{code:'INVALID_JSON',message:'Expected object'}}]);}
    }
    await new Promise(resolve=>setImmediate(resolve));
  }
  assert(done,'Portable policy did not complete'); if(error)throw error; return result;
}
const string=(maximum=128)=>({kind:'string',maximum});
const fields={id:string(),name:string(),specification:string(),cents:{kind:'int'},available:{kind:'int'}};
const read={name:'equipment',parameters:[{name:'from',type:string()},{name:'until',type:string()}],result:{kind:'array',element:{kind:'record',fields}}};
const prepare={name:'stage',parameters:['kind','provider','title','detail','price','deadline'].map(name=>({name,type:string()})),result:{kind:'record',fields:{staged:{kind:'boolean'}}}};
const catalog=[{module:'host/organizer',functions:[prepare]},{module:'host/rental',functions:[read]}];
const context={requirement:'Boom stand',specification:'Adjustable boom',quantity:1,from:'2026-10-09T16:00:00Z',until:'2026-10-09T22:00:00Z',searchTerms:['Boom','stand']};
const copy=v=>JSON.parse(JSON.stringify(v));
function oracle(catalog,context) {
  let reads=[],preparations=[];
  for(const contract of catalog)for(const fn of contract.functions){
    if(JSON.stringify(fn.parameters.map(p=>p.name))===JSON.stringify(['kind','provider','title','detail','price','deadline'])&&fn.parameters.every(p=>p.type.kind==='string')&&fn.result.kind==='record'&&fn.result.fields.staged?.kind==='boolean')preparations.push(fn);
    const fields=fn.result.element?.fields||{};
    if(fn.result.kind!=='array'||fn.result.element?.kind!=='record'||!['id','name','specification'].every(k=>fields[k]?.kind==='string')||!['cents','available'].every(k=>fields[k]?.kind==='int'))continue;
    if(fn.parameters.every(p=>p.type.kind==='string'&&typeof context[p.name]==='string'&&context[p.name].length<=p.type.maximum))reads.push({alias:'read'+reads.length,module:contract.module,arguments:fn.parameters.map(p=>context[p.name])});
  }
  const supported=preparations.length===1&&Number.isInteger(context.quantity)&&context.quantity>=1&&context.quantity<=128&&['requirement','specification','from','until'].every(k=>typeof context[k]==='string'&&context[k].trim())&&Array.isArray(context.searchTerms)&&context.searchTerms.length>=1&&context.searchTerms.length<=8&&context.searchTerms.every(t=>typeof t==='string'&&t.trim());
  return supported?reads:[];
}
(async()=>{
  const cases=[['supported',catalog,context],['missing context',catalog,{}],['wrong type',catalog,{...context,quantity:'1'}],['empty term',catalog,{...context,searchTerms:[' ']}],['ambiguous',catalog.concat([{module:'host/other',functions:[prepare]}]),context]];
  const missing=copy(catalog);delete missing[1].functions[0].result.element.fields.available;cases.push(['missing field',missing,context]);
  const bounded=copy(catalog);bounded[1].functions[0].parameters[0].type.maximum=1;cases.push(['bounds',bounded,context]);
  const unicode=copy(catalog);unicode[1].functions[0].parameters[0].type.maximum=1;cases.push(['UTF16 bound',unicode,{...context,from:'😀'}]);
  const extra=copy(catalog);extra[1].functions[0].parameters.push({name:'custom',type:string()});cases.push(['dynamic key',extra,{...context,custom:'provided'}]);
  const exact=copy(catalog);exact[1].functions[0].parameters[0].type.maximum=2;
  cases.push(['UTF16 exact supplementary',exact,{...context,from:'😀'}]);
  const combining=copy(catalog);combining[1].functions[0].parameters[0].type.maximum=1;
  cases.push(['UTF16 combining bound',combining,{...context,from:'e\u0301'}]);
  let validated=0;
  for(const [label,contracts,ctx]of cases){
    const entries={entries:Object.entries(ctx).filter(([k,v])=>typeof v==='string').map(([key,value])=>({key,value}))};
    const inv=await invoke(policy.inventory,JSON.stringify(contracts),JSON.stringify(ctx),JSON.stringify(entries));
    const actual=inv.get('reads').map(r=>({alias:r.get('alias'),module:r.get('module'),arguments:r.get('arguments')}));
    assert.deepStrictEqual(actual,oracle(contracts,ctx),label);
    for(const purpose of ['compare','choose'])for(const headline of ['hero','text'])for(const notice of ['warning','none']){
      const answer={answers:{purpose,headline,notice,read:'read0'}};
      const decision=await invoke(policy.validate,JSON.stringify(answer),inv);
      assert.equal(decision.get('accepted'),actual.length>0,label);validated++;
    }
    for(const raw of ['not json','{}','{"answers":{}}',JSON.stringify({answers:{purpose:'compare',headline:'text',notice:'none',read:'read999'}})]){
      assert.equal((await invoke(policy.validate,raw,inv)).get('accepted'),false);validated++;
    }
  }
  console.log(`Portable DEAL policy parity: ${cases.length} fixtures, ${validated} answer decisions; includes UTF-16 bounds and dynamic context keys`);
})().catch(error=>{console.error(error);process.exitCode=1;});
