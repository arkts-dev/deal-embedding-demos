#!/usr/bin/env node
// Production-compiled portable needs, candidate and admission code. No substitute evaluator.
const fs=require('fs'),path=require('path'),vm=require('vm'),assert=require('assert');
const base=path.resolve(process.argv[2]||'build/constrained-generation/generation-js');
const assets=path.resolve('build/embedding-library/aar/assets');
const ctx=vm.createContext({});
for(const file of ['sandbox.js','generation-contracts.js','protocol-contracts.js','protocol-host.js'])vm.runInContext(fs.readFileSync(path.join(assets,'embedding/bindings',file),'utf8'),ctx);
const sources={};function walk(dir){for(const name of fs.readdirSync(dir)){const p=path.join(dir,name);if(fs.statSync(p).isDirectory())walk(p);else if(name.endsWith('.js'))sources[path.relative(base,p)]=fs.readFileSync(p,'utf8');}}walk(base);ctx.sources=sources;
vm.runInContext("const factories=Object.fromEntries(Object.entries(sources).map(([k,v])=>[k,new Function('require','module','exports','process','console',v)]));globalThis.load=name=>dealLoad(factories,name+'.js').entry;globalThis.decode=text=>{const rt=load('deal/runtime');function convert(v){if(Array.isArray(v))return v.map(convert);if(v&&typeof v==='object')return rt.makeTable(Object.fromEntries(Object.entries(v).map(([k,x])=>[k,convert(x)])));return v;}return convert(JSON.parse(text));};globalThis.encode=value=>{function plain(v){if(v instanceof Map)return Object.fromEntries([...v].map(([k,x])=>[k,plain(x)]));if(Array.isArray(v))return v.map(plain);if(v&&typeof v==='object')return Object.fromEntries(Object.entries(v).filter(([k])=>!k.startsWith('$')).map(([k,x])=>[k,plain(x)]));return v;}return JSON.stringify(plain(value));};",ctx);
const run=code=>JSON.parse(vm.runInContext(`encode(${code})`,ctx));
const term=(id,op,args=[],extra={})=>({id,op,args,source:'',path:'',value:null,evidence:'',dimension:'unknown',...extra});
const literal=(id,value,dimension='unknown')=>term(id,'literal',[],{value,dimension,evidence:String(value)});
const need=(id,kind,config)=>({id,kind,label:id,config,evidence:'task'});
const input=(id,value)=>need(id,'input',{value,format:'integer',minimum:0,maximum:10000});
const facts={title:'Synthetic relation',terms:[literal('a',15,'count'),literal('b',3,'count'),term('ap','port',[],{source:'x',path:'value'}),term('bp','port',[],{source:'y',path:'value'}),term('difference','subtract',['ap','bp'])],needs:[input('x','a'),input('y','b'),need('z','result',{value:'difference',format:'integer'})]};
ctx.doc=JSON.stringify(facts);ctx.intent='task 15 3';ctx.catalog=JSON.stringify({contracts:[]});
run(`load('needs').parse.$f(decode(doc),intent,'',catalog)`);
ctx.requirements=vm.runInContext(`load('needs').parse.$f(decode(doc),intent,'',catalog)`,ctx);
ctx.issued=vm.runInContext(`load('need-candidates').enumerate.$f(requirements,[])`,ctx);
assert.equal(run('issued').gaps.length,0);assert.equal(run('issued').candidates.length,3);
const plan=run(`load('need-selection').plan.$f(requirements,issued.candidates)`);assert.equal(plan.fragments[2].config.value.expr,'(x.value - y.value)');assert.equal(plan.fragments.length,3);
function rejected(code){assert.throws(()=>vm.runInContext(code,ctx));}
rejected(`load('need-selection').plan.$f(requirements,[issued.candidates[2]])`);
const partial=structuredClone(facts);partial.terms[0].value=1;ctx.bad=JSON.stringify(partial);rejected(`load('needs').parse.$f(decode(bad),intent,'',catalog)`);
const words=structuredClone(facts);words.terms[0].value=6;words.terms[0].evidence='six30';ctx.words=JSON.stringify(words);run(`load('needs').parse.$f(decode(words),'task six30 3','','')`);
words.terms[0].evidence='sixteen';ctx.words=JSON.stringify(words);rejected(`load('needs').parse.$f(decode(words),'task sixteen 3','','')`);
words.terms[0].evidence='SIX';ctx.words=JSON.stringify(words);run(`load('needs').parse.$f(decode(words),'task SIX 3','','')`);
words.terms[2].op='field';ctx.words=JSON.stringify(words);rejected(`load('needs').parse.$f(decode(words),'task SIX 3','','')`);
const altered=structuredClone(facts);altered.terms[0].value=999;ctx.bad=JSON.stringify(altered);rejected(`load('needs').parse.$f(decode(bad),intent,'',catalog)`);
const mismatch=structuredClone(facts);mismatch.terms[0].dimension='money';mismatch.terms[1].dimension='length';ctx.bad=JSON.stringify(mismatch);
ctx.mismatch=vm.runInContext(`load('needs').parse.$f(decode(bad),intent,'',catalog)`,ctx);assert.equal(run(`load('need-candidates').enumerate.$f(mismatch,[])`).gaps.length,1);
const wrongFormat=structuredClone(facts);wrongFormat.needs[0].config.format='number';ctx.wrongFormat=JSON.stringify(wrongFormat);assert(run(`load('need-candidates').enumerate.$f(load('needs').parse.$f(decode(wrongFormat),intent,'',''),[])`).gaps.some(x=>x.startsWith('x:')),'Unsupported input formats reject before candidate freezing');
const frozenEdit=structuredClone(facts);frozenEdit.terms[4].args=['a','bp'];ctx.frozenEdit=JSON.stringify(frozenEdit);assert(run(`load('need-candidates').enumerate.$f(load('needs').parse.$f(decode(frozenEdit),intent,'',''),[])`).gaps[0].includes('Editable assumption'));
const cycle=structuredClone(facts);cycle.terms[4].args=['difference','bp'];ctx.bad=JSON.stringify(cycle);ctx.cycle=vm.runInContext(`load('needs').parse.$f(decode(bad),intent,'',catalog)`,ctx);assert.equal(run(`load('need-candidates').enumerate.$f(cycle,[])`).gaps.length,1);
// Same structural types and values, different requested relation: framework must not pick a task answer.
const addition=structuredClone(facts);addition.terms[4].op='add';ctx.addition=JSON.stringify(addition);assert.equal(run(`load('need-candidates').enumerate.$f(load('needs').parse.$f(decode(addition),intent,'',catalog),[])`).candidates[2].fragment.config.value.expr,'(x.value + y.value)');
// Unrelated, renamed modules/functions/fields preserve the same generic admission behavior.
const wire=kind=>({kind,fields:[],element:null});
for(const [module,fn,field] of [['host/laboratory','sample','flux'],['unrelated/inventory','observe','temperature']]){
 const contracts=[{module,functions:[{name:fn,parameters:[],result:{...wire('record'),fields:[{name:field,type:wire('number'),optional:false}]}}]}];
 const d={title:'Unrelated read',terms:[term('projection','port',[],{source:'resource',path:'result.'+field})],needs:[need('resource','read',{module,function:fn,arguments:[]}),need('metric','result',{value:'projection',format:'number'})]};
 ctx.contracts=JSON.stringify({contracts});ctx.d=JSON.stringify(d);const issue=run(`load('need-candidates').enumerate.$f(load('needs').parse.$f(decode(d),'task','',''),decode(contracts).get('contracts'))`);assert.equal(issue.gaps.length,0);assert.equal(issue.candidates[1].fragment.config.value.expr,'resource.result.'+field);
 d.terms[0].path='result.fabricated';ctx.d=JSON.stringify(d);assert.equal(run(`load('need-candidates').enumerate.$f(load('needs').parse.$f(decode(d),'task','',''),decode(contracts).get('contracts'))`).gaps.length,1);
}
const collectionContracts=[{module:'unrelated/collection',functions:[{name:'observe',parameters:[{name:'key',type:wire('number')}],result:{...wire('array'),element:wire('number')}}]}];
const collectionNeeds={title:'Collection read',terms:[literal('key',3),term('rows','port',[],{source:'resource',path:'result'})],needs:[need('resource','read',{module:'unrelated/collection',function:'observe',arguments:['key']}),need('panel','section',{imports:[{name:'values',type:'array',term:'rows'}],exports:[{name:'count',type:'number'}],capabilities:[]})]};
ctx.collectionContracts=JSON.stringify({contracts:collectionContracts});ctx.collectionNeeds=JSON.stringify(collectionNeeds);ctx.emptyPlan=JSON.stringify({title:'Read recovery',fragments:[],commands:[],display:[]});
run(`load('need-holes').validate.$f(load('needs').parse.$f(decode(collectionNeeds),'task 3','',''),decode(collectionContracts).get('contracts'))`);
const readBoundary=run(`load('need-holes').issue.$f(load('needs').parse.$f(decode(collectionNeeds),'task 3','',''),decode(emptyPlan),'resource',decode(collectionContracts).get('contracts'))`);
assert.deepEqual(readBoundary.exports,[{name:'result',type:'array'}]);assert.deepEqual(readBoundary.imports,[{name:'argument0',type:'number',value:{expr:'3'}}]);assert.deepEqual(readBoundary.capabilities,[{module:'unrelated/collection',function:'observe'}]);
const rowContracts=[{module:'unrelated/rows',functions:[{name:'list',parameters:[],result:{...wire('array'),element:{...wire('record'),fields:[{name:'cost',optional:false,type:wire('number')}]}}}]}];
const rowNeeds={title:'Collection scope',terms:[term('rows','port',[],{source:'resource',path:'result'}),term('cost','field',[],{source:'row',path:'cost'}),term('sum','sum',['rows','cost'])],needs:[need('resource','read',{module:'unrelated/rows',function:'list',arguments:[]}),need('total','result',{value:'sum',format:'number'})]};
ctx.rowContracts=JSON.stringify({contracts:rowContracts});ctx.rowNeeds=JSON.stringify(rowNeeds);const rowsIssued=run(`load('need-candidates').enumerate.$f(load('needs').parse.$f(decode(rowNeeds),'task','',''),decode(rowContracts).get('contracts'))`);assert.deepEqual(rowsIssued.gaps,[]);assert.equal(rowsIssued.candidates[1].fragment.config.value.expr,'sum(resource.result,row.cost)');
rowNeeds.terms[1].path='invented';ctx.rowNeeds=JSON.stringify(rowNeeds);assert(run(`load('need-candidates').enumerate.$f(load('needs').parse.$f(decode(rowNeeds),'task','',''),decode(rowContracts).get('contracts'))`).gaps.some(x=>x.includes('Undisclosed field')));
ctx.planForInput=JSON.stringify({...plan,fragments:plan.fragments.filter(f=>f.id!=='x'),display:plan.display.filter(x=>x!=='x')});
assert.deepEqual(run(`load('need-holes').issue.$f(requirements,decode(planForInput),'x',[])`).exports,[{name:'value',type:'number'}]);
// Exact custom interfaces preserve all selected fragments and forbid arbitrary replacement graphs.
const custom=structuredClone(facts);custom.needs.push(need('hole','custom',{parameters:[{name:'x',type:'number'}],result:'number',arguments:['ap'],format:'integer'}));ctx.custom=JSON.stringify(custom);ctx.customNeeds=vm.runInContext(`load('needs').parse.$f(decode(custom),intent,'','')`,ctx);ctx.plan=JSON.stringify(plan);ctx.sections=JSON.stringify({reason:'Uncovered relation',sections:[{id:'hole',parameters:[{name:'x',type:'number'}],result:'number',arguments:['ap'],body:'return x;'}]});
const merged=run(`load('need-holes').merge.$f(customNeeds,decode(plan),['hole'],decode(sections),[])`);assert.deepEqual(merged.fragments.slice(0,3),plan.fragments);assert.equal(merged.fragments[3].kind,'custom');
ctx.sections=JSON.stringify({reason:'Ordered transport',sections:[{id:'hole',parameters:[{type:'number',name:'x'}],result:'number',arguments:['ap'],body:'return x;'}]});assert.equal(run(`load('need-holes').merge.$f(customNeeds,decode(plan),['hole'],decode(sections),[])`).fragments[3].kind,'custom');
ctx.sections=JSON.stringify({reason:'bad',sections:[{id:'x',parameters:[],result:'number',arguments:[],body:'return 0;'}]});rejected(`load('need-holes').merge.$f(customNeeds,decode(plan),['hole'],decode(sections),[])`);
// Recovery accounting is request-local and group charges cannot reset on split/re-entry.
ctx.budget=vm.runInContext(`load('need-recovery').budget.$f(['x','z'])`,ctx);
assert.equal(run(`load('need-recovery').charge.$f(budget,['x','z'])`),false);
assert.equal(run(`load('need-recovery').charge.$f(budget,['x'])`),false);
assert.equal(run(`load('need-recovery').charge.$f(budget,['x'])`),true);
rejected(`load('need-recovery').charge.$f(budget,['x','z'])`);
assert.equal(run(`load('need-recovery').count.$f(budget,'z')`),1,'Rejected group charge is atomic');
rejected(`load('need-recovery').charge.$f(budget,['renamed'])`);
assert.deepEqual(run(`load('need-recovery').remaining.$f(budget,['x','z'])`),['z']);
assert.deepEqual(run(`load('need-recovery').retain.$f([issued.candidates[0],issued.candidates[2]])`).map(x=>x.need),['x']);
// Nominal candidate arrays are obtained from real enumeration, not retagged decoded records.
const chain=structuredClone(facts);chain.needs.push(need('follow','result',{value:'zp',format:'number'}));chain.terms.push(term('zp','port',[],{source:'z',path:'value'}));ctx.chainDoc=JSON.stringify(chain);ctx.chainIssued=vm.runInContext(`load('need-candidates').enumerate.$f(load('needs').parse.$f(decode(chainDoc),intent,'',''),[])`,ctx);
assert.deepEqual(run(`load('need-recovery').retain.$f([chainIssued.candidates[0],chainIssued.candidates[2],chainIssued.candidates[3]])`).map(x=>x.need),['x']);
rejected(`load('need-recovery').authorize.$f(decode(plan),true,true)`);
rejected(`load('need-recovery').authorize.$f(decode(plan),false,false)`);
// Issued section interfaces are immutable; successful merges append owned subtrees only.
const sectionFacts=structuredClone(facts);sectionFacts.needs.push(need('panel','section',{imports:[{name:'incoming',type:'number',term:'ap'}],exports:[{name:'value',type:'number'}],capabilities:[]}));ctx.sectionFacts=JSON.stringify(sectionFacts);
ctx.sectionNeeds=vm.runInContext(`load('needs').parse.$f(decode(sectionFacts),intent,'','')`,ctx);ctx.plan=JSON.stringify(plan);
run(`load('need-holes').validate.$f(sectionNeeds,[])`);
const invalidBoundary=structuredClone(sectionFacts);invalidBoundary.needs.at(-1).config.imports[0].term='x';ctx.invalidBoundary=JSON.stringify(invalidBoundary);rejected(`load('need-holes').validate.$f(load('needs').parse.$f(decode(invalidBoundary),intent,'',''),[])`);
invalidBoundary.needs.at(-1).config.imports[0].term='ap';invalidBoundary.needs.at(-1).config.imports[0].type='string';ctx.invalidBoundary=JSON.stringify(invalidBoundary);rejected(`load('need-holes').validate.$f(load('needs').parse.$f(decode(invalidBoundary),intent,'',''),[])`);
const boundary=run(`load('need-holes').issue.$f(sectionNeeds,decode(plan),'panel',[])`);
const panel={...boundary,exports:[{name:'value',type:'number',value:{expr:'metric.value'}}],plan:{title:'Recovered panel',fragments:[{id:'metric',kind:'result',config:{label:'Metric',format:'integer',value:{expr:'incoming.value'}}}],commands:[],display:['metric']}};
ctx.panel=JSON.stringify({reason:'Local display',sections:[panel]});const sectionMerged=run(`load('need-holes').merge.$f(sectionNeeds,decode(plan),['panel'],decode(panel),[])`);assert.deepEqual(sectionMerged.fragments.slice(0,3),plan.fragments);assert.equal(sectionMerged.fragments[3].kind,'section');
ctx.reordered=JSON.stringify({sections:[{plan:panel.plan,capabilities:panel.capabilities,exports:panel.exports.map(x=>({value:x.value,type:x.type,name:x.name})),imports:panel.imports.map(x=>({value:x.value,type:x.type,name:x.name})),id:panel.id}],reason:'Order independent transport'});
assert.equal(run(`load('need-holes').merge.$f(sectionNeeds,decode(plan),['panel'],decode(reordered),[])`).fragments[3].kind,'section');
ctx.a=JSON.stringify({module:'unrelated/sensor',function:'observe'});ctx.b=JSON.stringify({function:'observe',module:'unrelated/sensor'});assert(run(`load('section-contract').same.$f(decode(a),decode(b))`));
const changed=structuredClone(panel);changed.imports[0].value.expr='y.value';ctx.panel=JSON.stringify({reason:'Changed import',sections:[changed]});rejected(`load('need-holes').merge.$f(sectionNeeds,decode(plan),['panel'],decode(panel),[])`);
ctx.panel=JSON.stringify({reason:'Changed export',sections:[{...panel,exports:[]}]});rejected(`load('need-holes').merge.$f(sectionNeeds,decode(plan),['panel'],decode(panel),[])`);
const closures=run(`load('need-recovery').subsets.$f(decode(plan))`);assert.equal(closures.length,3);assert.deepEqual(closures[2].fragments.map(f=>f.id),['x','y','z']);
const brokenPlan=structuredClone(plan);brokenPlan.fragments.shift();ctx.brokenPlan=JSON.stringify(brokenPlan);assert.deepEqual(run(`load('need-recovery').subsets.$f(decode(brokenPlan))`).map(p=>p.fragments.map(f=>f.id)),[['y']]);
const malformedPlan=structuredClone(plan);malformedPlan.fragments[0].config.format='number';ctx.malformedPlan=JSON.stringify(malformedPlan);assert.deepEqual(run(`load('need-recovery').subsets.$f(decode(malformedPlan))`).map(p=>p.fragments.map(f=>f.id)),[['y']],'A malformed component cannot abort unrelated subset enumeration');
// Exact repair authority is lexical containment plus byte-identical maintained neighbors.
const baseBody='function custom_local(): number {\n// DEAL-SECTION-BEGIN local\nreturn 1;\n// DEAL-SECTION-END local\n}\nfunction neighbor(): number { return 2; }';
function preserved(body,ui='UI'){ctx.original=baseBody;ctx.proposed=baseBody.replace('return 1;',body);ctx.ui=ui;return run(`load('source-preservation').preserves.$f(original,'UI',proposed,ui)`);}
assert(preserved('if (true) { return 3; } return 4;'));
assert(preserved('let text: string = "} /*"; /* } */ return 3;'));
assert(!preserved('} function neighborHijack(): number { return 3; } function extra(): number {'));
assert(!preserved('return 3; /*'));
assert(!preserved('return "unterminated;'));
assert(!preserved('// DEAL-SECTION-BEGIN forged\nreturn 3;'));
assert(!preserved('return 3;','changed'));
ctx.original=baseBody;ctx.proposed=baseBody.replace('return 2;','return 9;');assert(!run(`load('source-preservation').preserves.$f(original,'UI',proposed,'UI')`));
// Published maintained outputs must also exist in typed extraction admission.
ctx.outputs=JSON.stringify({title:'Output schema fixture',terms:[],needs:['allocation','quotes','alternatives'].map(kind=>({id:kind,kind,label:kind,evidence:'fixture',config:{}}))});
ctx.outputNeeds=vm.runInContext(`load('needs').parse.$f(decode(outputs),'fixture','','')`,ctx);
for(const [owner,port,kind,fields] of [['allocation','total','number',[]],['allocation','valid','boolean',[]],['allocation','rows','array',['id','name','cost','quantity']],['quotes','results','array',['key','label','cost','deposit','available','expiresAt']],['alternatives','baskets','array',['items','key','label']]]){ctx.owner=owner;ctx.port=port;const value=run(`load('need-candidates').output.$f(outputNeeds,[],owner,port)`);assert.equal(value.type.kind,kind);if(kind==='array')assert.deepEqual(value.type.element.fields.map(f=>f.name),fields);}
ctx.owner='quotes';ctx.port='results';const quoted=run(`load('need-candidates').output.$f(outputNeeds,[],owner,port)`);ctx.quoteType=JSON.stringify(quoted.type.element);ctx.badField='cents';rejected(`load('need-expressions').member.$f(decode(quoteType),badField)`);
ctx.owner='alternatives';ctx.port='baskets';const baskets=run(`load('need-candidates').output.$f(outputNeeds,[],owner,port)`);const item=baskets.type.element.fields.find(f=>f.name==='items').type.element;assert.deepEqual(item.fields.map(f=>f.name),['id','label','capacity','quantity','components']);
console.log('PASS exact section repair rejects lexical escape, swallowed/spoofed boundaries and neighbor/UI changes');
console.log('PASS dependency-closed subset isolation preserves independent work and prunes missing outputs');
console.log('PASS issued UI-section boundaries: immutable imports/exports and appended owned subtree');
console.log('PASS recovery accounting, atomic dispatch preflight, transitive pruning and conservative fallback authorization');
console.log('PASS portable constrained generation: typed relations, exact evidence, dimensions, cycles, prerequisites, field disclosure, domain substitution, same-type relation changes and preserved custom holes');
