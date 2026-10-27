#!/usr/bin/env python3
"""Isolated architecture falsification; current compiler/runtime, no device mutations."""
import hashlib,json,os,pathlib,subprocess,sys,time
ROOT=pathlib.Path(__file__).resolve().parents[1];os.chdir(ROOT)
OUT=ROOT/'build/selection-prototype'/str(time.time_ns());OUT.mkdir(parents=True,exist_ok=False)
print(str(OUT),flush=True)
def run(args,name):
 r=subprocess.run(list(map(str,args)),stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True)
 (OUT/(name+'.log')).write_text(r.stdout)
 if r.returncode:print(r.stdout);raise SystemExit(r.returncode)
 return r.stdout
cp='build/embedding-library/aar/classes.jar:build/behavior-four/json.jar';compiler=ROOT/'dependencies/deal-embedding/android/src/compiler'
run(['/snap/kotlin/current/bin/kotlinc','-jvm-target','21','-classpath',cp,'-d',OUT/'host',compiler/'BehaviorCandidate.kt',compiler/'MaintainedComposition.kt',compiler/'MaintainedFormula.kt',compiler/'FragmentComposition.kt',compiler/'SelectionComposition.kt','tools/SelectionHarness.kt'],'kotlin')
run(['javac','--release','21','-cp','build/embedding-library/classes','-d',OUT/'host','dependencies/deal-embedding/tools/ToolkitHarness.java'],'java')
run([sys.executable,'tools/selection-fixtures.py',OUT/'plans'],'fixtures')
for task in ['food','equipment','laboratory']:
 run(['java','-cp',str(OUT/'host')+':'+cp+':/snap/kotlin/current/lib/kotlin-stdlib.jar','dev.deal.embedding.SelectionHarnessKt',OUT/'plans'/f'{task}.json',OUT/task/'input',OUT/'plans/contracts.json'],task+'-assemble')
 run(['java','-Ddeal.home=dependencies/deal','-cp','build/embedding-library/classes:'+str(OUT/'host'),'ToolkitHarness','dependencies/deal-embedding/ui/platform.dealui-pack',OUT/task/'input',OUT/task/'check','build/embedding-consumer/assets/distribution'],task+'-compile')
print(run(['node','tools/test-selection-session.js',OUT],'runtime'))
(OUT/'receipt.json').write_text(json.dumps(dict(scope='Experimental semantic prototype only; not shipping integration or live delivery.',hashes={str(p.relative_to(ROOT)):hashlib.sha256(p.read_bytes()).hexdigest() for p in [compiler/'BehaviorCandidate.kt',compiler/'SelectionComposition.kt',ROOT/'tools/selection-fixtures.py',ROOT/'tools/test-selection-session.js']}),indent=2))
