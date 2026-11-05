#!/usr/bin/env node
'use strict';
const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path');
const [reference,output]=process.argv.slice(2).map(p=>path.resolve(p));
const {loadVera}=require(path.join(reference,'tools/vera-harness/source-loader.js'));
const compose=loadVera('VeraCompose'),generated=require(path.join(output,'js/parity.js'));
// Expose private functions only in the test-loaded module, without changing
// their bodies. Runtime imports still resolve through the pinned loader.
const ts=require(process.env.VERA_TYPESCRIPT_PATH);
const source=fs.readFileSync(path.join(reference,'entry/src/main/ets/vera/VeraCompose.ets'),'utf8');
const instrumented=source.replace(/function (evalFormula|writtenMeasure|formulaVera|useWritten)\(/g,'export function $1(');
const compiled=ts.transpileModule(instrumented,{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2020}}).outputText;
const mod={exports:{}};
new Function('require','module','exports',compiled)(name=>{
    if(name.startsWith('./'))return loadVera(name.slice(2));
    if(name==='@ohos.net.http')return {};
    throw Error('Unexpected reference import '+name);
},mod,mod.exports);
const privateReference=mod.exports;
const cases=[];
function check(text,known=['a','b','c','remaining','entryTotal','math.custom']){
    const expected=JSON.parse(JSON.stringify(compose.parseFormula(text,known)));
    const lengths=Object.fromEntries([text,...text].map(s=>[s,s.length]));
    const reply=JSON.stringify({text,known,lengths});
    assert.deepEqual(JSON.parse(generated.inspectFormula.$f(reply)),expected,text);
    cases.push({name:'formula '+text,reply,expected});
}
for(const text of ['', ' ', '\t','\n','1','0001','.5','00.50','1.','1e3','1e-3','0x10','a','a+a+b','missing',
    'math.custom','a.b.c','1 + 2 * 3','(1+2)*3','--a','+-+b','a/b%c','a < b','a===b','a!==b',
    'a < b < c','(a < b) < c','a < (b < c)','min(a,b,c,1)','Math.max(a,b)','math.ceil(.2)',
    'sqrt(a)','pow(a,b)','round()','min(a)','max()','abs(a,b)','other(a)','MATH.abs(a)','a(1)',
    '(a','a)','min(a,b','min(a,)','a +','a b','a & b','a; call()','a\nb','a\tb','a\u00a0b','é','?','😃'.repeat(151),
    '1'.repeat(300),'1'.repeat(301),' '.repeat(299)+'1',' '.repeat(300)+'1'])check(text);
for(const op of ['+','-','*','/','%','<','<=','>','>=','==','!=','===','!==']){
    for(const left of ['a','1','-b','(a+b)'])for(const right of ['b','.5','max(c,1)'])check(left+op+right);
}
for(const name of ['floor','ceil','round','abs','sqrt','min','max','pow','math.min','Math.pow']){
    for(const args of ['', 'a','a,b','a,b,c','a<b,b','(a+b),c'])check(name+'('+args+')');
}
for(let i=0;i<100;i++)check(i%2?`max(0, (a - ${i}) / (b + .5))`:`a + min(b, c, ${i}) * -a`);
const observe=n=>Number.isNaN(n)?'#NaN':n===Infinity?'#Infinity':n===-Infinity?'#-Infinity':Object.is(n,-0)?'#-0':n;
for(const text of ['a+b','a-b','a*b','a/b','a%b','a<=b','a!=b','-a','floor(a)','ceil(a)','round(a)','abs(a)','sqrt(a)',
    'min(a,b,0)','max(a,b,0)','pow(a,b)','remaining+1','remaining/60','min(remaining,a)','min(a,b)','1']){
    for(const [a,b] of [[-0,0],[-0.5,2],[-0.1,3],[0.49999999999999994,0],[4,2],[-4,2]]){
        const known=['a','b','remaining'],parsed=compose.parseFormula(text,known);
        const numbers=[];function collect(n){if(n.kind==='num')numbers.push({value:n.value,text:String(n.value)});n.args.forEach(collect);}collect(parsed.node);
        const env={a,b,remaining:120},operands=[{id:'a',measure:'seconds'},{id:'b',measure:'minutes'},{id:'remaining',measure:'seconds'}];
        const powers=[];const originalPow=Math.pow;
        Math.pow=(a,b)=>{const value=originalPow(a,b);powers.push({a,b,value});return value;};
        let evaluated;try{evaluated=privateReference.evalFormula(parsed.node,new Map(Object.entries(env)));}finally{Math.pow=originalPow;}
        const expected={...JSON.parse(JSON.stringify(parsed)),evaluated:observe(evaluated),measure:privateReference.writtenMeasure(parsed.node,operands),
            emitted:privateReference.formulaVera(parsed.node)};
        const lengths=Object.fromEntries([text,...text].map(s=>[s,s.length]));
        const reply=JSON.stringify({text,known,lengths,numbers,env:Object.fromEntries(Object.entries(env).map(([key,n])=>[key,observe(n)])),operands,powers,remainder:{a:observe(a),b:observe(b),value:observe(a%b)}});
        assert.deepEqual(JSON.parse(generated.inspectFormula.$f(reply)),expected,text+' '+a+' '+b);
        cases.push({name:'evaluate '+text+' '+a+' '+b,reply,expected});
    }
}
fs.writeFileSync(path.join(output,'formula-cases.json'),JSON.stringify(cases,null,2)+'\n');
const written=[];
for(const text of ['', 'a+b','a/b','unknown+1','sqrt(a)','remaining','1','Math.max(a, b)']){
    for(const result of [undefined,0,3,3.01,3.03,120])for(const confidence of [0.49,0.5]){
        const raw={inputs:[{id:'a',kind:'count',measure:'seconds',value:1},{id:'b',kind:'count',measure:'seconds',value:2}],
            countdown:'Timer',values:[{id:'v',label:'Result',format:'minutes',formula:text,...(result===undefined?{}:{example:{result}})}]};
        const sketchText=JSON.stringify(raw),value=compose.parseSketch(sketchText),comp=new privateReference.Composition(value),binding=new privateReference.Binding();
        const answers={'written.v':{choice:'right',confidence}};
        const fault=privateReference.useWritten(comp,answers,0,binding);
        const expected={fault,binding:fault?null:{formula:binding.formula,expr:binding.expr,reads:binding.reads,scale:binding.scale},log:comp.answers};
        const parsed=compose.parseFormula(text,['a','b','remaining','elapsed','duration']);
        const numbers=[];function collect(n){if(!n)return;if(n.kind==='num')numbers.push({value:n.value,text:String(n.value)});n.args.forEach(collect);}collect(parsed.node);
        for(const n of [result,1,2,Math.round((text==='a/b'?0.5:text==='1'?1:text==='sqrt(a)'?1:3)*1000)/1000])if(typeof n==='number')numbers.push({value:n,text:String(n)});
        const env={a:1,b:2};
        const lengths=Object.fromEntries([text,...text].map(s=>[s,s.length]));
        const reply=JSON.stringify({sketch:sketchText,answers,env,lengths,numbers,fixed:confidence.toFixed(2)});
        assert.deepEqual(JSON.parse(generated.inspectWritten.$f(reply)),expected,'written '+text+' '+result);
        written.push({name:'written '+text+' '+result+' '+confidence,reply,expected});
    }
}
assert.ok(written.some(c=>c.expected.binding!==null),'exercise successful binding');
assert.ok(written.some(c=>c.expected.fault.includes('not 3.03')),'exercise worked-example mismatch');
assert.ok(written.some(c=>c.expected.fault==='Jev does not read its written formula as right'),'exercise confidence rejection');
assert.ok(written.some(c=>c.expected.binding?.scale===' / 60'),'exercise time conversion');
fs.writeFileSync(path.join(output,'written-cases.json'),JSON.stringify(written,null,2)+'\n');
console.log(`PASS ${cases.length} formula grammar/evaluation and ${written.length} written-validation cases`);
