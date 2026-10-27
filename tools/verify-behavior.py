#!/usr/bin/env python3
"""Production typed emitter -> real compiler -> production JS session. No model or device writes."""
import hashlib,json,os,pathlib,shutil,subprocess,sys
ROOT=pathlib.Path(__file__).resolve().parents[1]
OUT=ROOT/'build/behavior-verification'
if OUT.exists():shutil.rmtree(OUT)
OUT.mkdir(parents=True)
os.chdir(ROOT)

def run(args,name):
 r=subprocess.run([str(x) for x in args],text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT)
 (OUT/(name+'.log')).write_text(r.stdout)
 if r.returncode:print(r.stdout);raise SystemExit(r.returncode)
 return r.stdout

cp='build/embedding-library/aar/classes.jar:build/behavior-four/json.jar'
run(['/snap/kotlin/current/bin/kotlinc','-jvm-target','21','-classpath',cp,'-d',OUT/'host',ROOT/'dependencies/deal-embedding/android/src/compiler/BehaviorCandidate.kt',ROOT/'dependencies/deal-embedding/android/src/compiler/MaintainedComposition.kt',ROOT/'dependencies/deal-embedding/android/src/compiler/MaintainedFormula.kt',ROOT/'dependencies/deal-embedding/android/src/compiler/FragmentComposition.kt',ROOT/'tools/BehaviorHarness.kt',ROOT/'tools/FormulaHarness.kt'],'kotlin')
print(run(['java','-cp',str(OUT/'host')+':'+cp+':/snap/kotlin/current/lib/kotlin-stdlib.jar','dev.deal.embedding.FormulaHarnessKt'],'formulas').strip())
run(['javac','--release','21','-cp','build/embedding-library/classes','-d',OUT/'host','dependencies/deal-embedding/tools/ToolkitHarness.java'],'java')
run([sys.executable,'tools/behavior-fixtures.py',OUT/'plans'],'fixtures')
import jsonschema
schema=json.loads(pathlib.Path('dependencies/deal-embedding/ui/behavior-contract.json').read_text())
maintained_schema=json.loads(pathlib.Path('dependencies/deal-embedding/ui/maintained-contract.json').read_text())
fragment_schema=json.loads(pathlib.Path('dependencies/deal-embedding/ui/fragment-contract.json').read_text())
for plan in (OUT/'plans').glob('composed-*.json'):
 jsonschema.validate(json.loads(plan.read_text()),fragment_schema)
for task in ['budget','departure','food','equipment']:
 data=json.loads((OUT/'plans'/f'fragment-{task}.json').read_text()); title=data.pop('title');jsonschema.validate(dict(title=title,behaviors=[data]),maintained_schema)
for task in ['budget','departure','food','equipment','optional','join','independent']:
 jsonschema.validate(json.loads((OUT/'plans'/f'{task}.json').read_text()),schema)
for task in ['composed-budget','composed-departure','composed-food','composed-independent','composed-equipment','composed-optional','composed-transfer','fragment-food','fragment-equipment','fragment-budget','fragment-departure','budget','departure','food','equipment','custom-budget','optional','join','independent']:
 run(['java','-cp',str(OUT/'host')+':'+cp+':/snap/kotlin/current/lib/kotlin-stdlib.jar','dev.deal.embedding.BehaviorHarnessKt',OUT/'plans'/f'{task}.json',OUT/task/'input',OUT/'plans/contracts.json'],task+'-assemble')
 run(['java','-Ddeal.home=dependencies/deal','-cp','build/embedding-library/classes:'+str(OUT/'host'),'ToolkitHarness','dependencies/deal-embedding/ui/platform.dealui-pack',OUT/task/'input',OUT/task/'check','build/embedding-consumer/assets/distribution'],task+'-compile')
print(run(['node','tools/test-behavior-session.js',OUT],'runtime').strip())
print(run(['node','tools/test-maintained-session.js',OUT],'maintained-runtime').strip())
revisions={p:subprocess.check_output(['git','-C',p,'rev-parse','HEAD'],text=True).strip() for p in ['.','dependencies/deal-embedding','dependencies/deal-ui','dependencies/deal']}
inputs=['dependencies/deal-embedding/ui/fragment-contract.json','dependencies/deal-embedding/android/src/compiler/FragmentComposition.kt','dependencies/deal-embedding/android/src/compiler/MaintainedFormula.kt','tools/FormulaHarness.kt','dependencies/deal-embedding/android/src/compiler/MaintainedComposition.kt','dependencies/deal-embedding/ui/maintained-contract.json','tools/test-maintained-session.js','dependencies/deal-embedding/android/src/compiler/BehaviorCandidate.kt','dependencies/deal-embedding/ui/platform.dealui-pack','dependencies/deal-embedding/ui/behavior-contract.json','tools/behavior-fixtures.py','tools/test-behavior-session.js']
(OUT/'receipt.json').write_text(json.dumps(dict(scope='Deterministic supplied-plan coverage, not live interpretation, device delivery, visuals or latency.',revisions=revisions,hashes={p:hashlib.sha256(pathlib.Path(p).read_bytes()).hexdigest() for p in inputs}),indent=2))
