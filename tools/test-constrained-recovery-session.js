#!/usr/bin/env node
'use strict';
// Real compiler output and production session; no model or provider calls.
const fs=require('fs'),vm=require('vm'),assert=require('assert');
let harness=fs.readFileSync('tools/test-behavior-session.js','utf8');
harness=harness.slice(0,harness.indexOf('(async()=>'));
globalThis.require=require;vm.runInThisContext(harness);
let s=mount('retained');const baseline=state(s);s.dispose();
s=mount('partial');assert.deepEqual(state(s),baseline,'A gap notice must not change maintained state');
assert(nodes(snapshot(s).tree).some(n=>n.component.endsWith('.Notice')&&value(n,'text')==='Unavailable: dependent calculation'));
assert.equal(requests().length,0,'Gap notices never invoke providers');
s.dispatch(slot(s,'Amount'),'8');assert.equal(state(s).m_amountValue,8);
s.dispatch(slot(s,'Amount'),'101');assert.equal(state(s).m_amountValue,8,'Retained bounds stay authoritative');
const saved=JSON.parse(s.checkpoint());s.dispose();s=mount('partial',saved);
assert.equal(state(s).m_amountValue,8);assert(nodes(snapshot(s).tree).some(n=>value(n,'text')==='Unavailable: dependent calculation'));
assert.equal(snapshot(s).fault,'');s.dispose();
s=mount('recovered');assert.equal(state(s).m_amountValue,7);assert.equal(state(s).m_recovered_value,14);assert.equal(state(s).m_downstreamValue,21);
s.dispatch(slot(s,'Amount'),'9');assert.equal(state(s).m_recovered_value,18);assert.equal(state(s).m_downstreamValue,27);
s.dispatch(slot(s,'Multiplier'),'3');assert.equal(state(s).m_recovered_value,27);assert.equal(state(s).m_amountValue,9,'Owned actions cannot mutate maintained neighbor');
assert.equal(requests().length,0);assert.equal(snapshot(s).fault,'');s.dispose();
s=mount('pure');assert.equal(state(s).m_scaledValue,14);s.dispatch(slot(s,'Amount'),'8');assert.equal(state(s).m_scaledValue,16);assert.equal(snapshot(s).fault,'');s.dispose();
(async()=>{
 s=mount('provider');s.dispatch(slot(s,'Read local'));let pending=requests();assert.equal(pending.length,1);assert.deepEqual(pending[0].args,[7]);
 dealCapabilities.deliver([{id:pending[0].id,ok:true,value:29}]);await tick();await tick();assert.equal(requests().length,0,'A section completion must not reschedule the provider');
 assert.equal(state(s).m_local_value,29);assert(s.checkpoint()!==null,'Provider section settles after one completion');
 s.dispatch(slot(s,'Amount'),'8');assert.equal(state(s).m_amountValue,8);s.dispatch(slot(s,'Read local'));pending=requests();assert.deepEqual(pending[0].args,[8]);
 dealCapabilities.deliver([{id:pending[0].id,ok:true,value:31}]);await tick();await tick();assert.equal(state(s).m_local_value,31);assert.equal(requests().length,0);assert.equal(snapshot(s).fault,'');s.dispose();
 s=mount('collection');s.dispatch(slot(s,'Load collection'));pending=requests();assert.equal(pending.length,1);assert.equal(pending[0].function,'many');
 dealCapabilities.deliver([{id:pending[0].id,ok:true,value:[2,4,8]}]);await tick();await tick();assert.equal(state(s).m_collection_value,3);assert.equal(requests().length,0);assert.equal(snapshot(s).fault,'');s.dispose();
 s=mount('pending-result');assert.equal(snapshot(s).fault,'');s.dispatch(slot(s,'Load collection'));pending=requests();assert.equal(pending.length,1);
 dealCapabilities.deliver([{id:pending[0].id,ok:true,value:[2,4,6]}]);await tick();await tick();assert.equal(state(s).m_collection_value,4);assert.equal(snapshot(s).fault,'');s.dispose();
 s=mount('collection-export');s.dispatch(slot(s,'Load exported collection'));pending=requests();assert.equal(pending.length,1);
 dealCapabilities.deliver([{id:pending[0].id,ok:true,value:[1,3,5,7]}]);await tick();await tick();assert.equal(state(s).m_collection_value,4);assert.equal(requests().length,0);assert.equal(snapshot(s).fault,'');s.dispose();
 console.log('PASS real recovered session: owned state/actions/exports, provider completion settles, maintained edits/bounds, honest notices and restoration');
})().catch(e=>{console.error(e);process.exitCode=1});
