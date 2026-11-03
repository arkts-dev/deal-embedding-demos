#!/usr/bin/env node
'use strict';
// Read-only historical accepted-source replay; a bounded observer stops the replay,
// never changes the source or supplies semantic feedback to generation.
const fs=require('fs'),vm=require('vm'),assert=require('assert');
let harness=fs.readFileSync('tools/test-behavior-session.js','utf8');harness=harness.slice(0,harness.indexOf('(async()=>'));
globalThis.require=require;vm.runInThisContext(harness);
(async()=>{
 const s=mount('departure');s.dispatch(slot(s,'Load and recalculate'));
 const first=requests();assert.equal(first.length,1);assert.equal(first[0].function,'estimate');
 dealCapabilities.deliver([{id:first[0].id,ok:true,value:29}]);await tick();await tick();
 const next=requests();assert.equal(next.length,1,'A completion incorrectly schedules another estimate');assert.equal(next[0].function,'estimate');
 assert.equal(JSON.parse(s.checkpoint()??'null'),null,'Self-rescheduling completion keeps replacement unsettled');
 const visible=JSON.parse(s.snapshot());assert.equal(visible.fault,'');
 assert(nodes(visible.tree).some(n=>String(value(n,'value')).includes('15:16')),'Arithmetic frame appears even while effects never settle');
 s.dispose();
 console.log(JSON.stringify({reproduced:true,firstRequests:first.length,completionRequests:next.length,settled:false,rootCause:'Generated refresh completion has the same effect-bearing action type as its initiating action; effect unconditionally calls estimate for every refresh, scheduling another completion indefinitely.',scope:'Exact historical source, real compiler and production session; one reply only, no inference or source changes.'},null,2));
})().catch(e=>{console.error(e);process.exitCode=1});
