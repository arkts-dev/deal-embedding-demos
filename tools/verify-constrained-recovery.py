#!/usr/bin/env python3
"""Real portable composition and compiler checks for local recovery; no inference/device mutation."""
import json
import pathlib
import shutil
import subprocess
import time

ROOT = pathlib.Path(__file__).resolve().parents[1]
LIB = ROOT / 'dependencies/deal-embedding'
OUT = ROOT / 'build/constrained-generation' / ('recovery-check-' + str(time.time_ns()))
OUT.mkdir(parents=True)
CP = ROOT / 'build/embedding-library/classes'

def run(args, name):
    result = subprocess.run([str(x) for x in args], cwd=ROOT, text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    (OUT / (name + '.log')).write_text(result.stdout)
    if result.returncode:
        raise RuntimeError(name + ': ' + result.stdout[-3000:])
    return result.stdout

shutil.copytree(LIB / 'core/composition', OUT / 'core/composition')
for name in ['decoded','section-contract']:
    shutil.copy(LIB / f'core/{name}.deal', OUT / f'core/{name}.deal')
shutil.copytree(LIB / 'ui/fragments', OUT / 'ui/fragments')
(OUT / 'ui/fragments/support/formula.deal').write_text((LIB / 'core/composition/formula.deal').read_text().replace('"../decoded"','"../../../core/decoded"'))
(OUT / 'deal.json').write_text(json.dumps({'languageVersion':'1.5','backend':'jvm','moduleRoots':['core','ui'],'externals':{'embedding/syntax':{'declaration':'core/composition/lowering-syntax.d.deal'}}}))
run(['python3', LIB / 'tools/composition-schema.py', LIB / 'ui/fragment-contract.json', OUT / 'core/composition/validation.generated.deal'], 'schema')
(OUT / 'host').mkdir()
for entry, name in [('compose','Compose'), ('fragments','Fragments')]:
    run(['java','-Ddeal.home='+str(ROOT / 'dependencies/deal'),'-cp',CP,'deal.Main','compile',OUT / f'core/composition/{entry}.deal','--backend','jvm','--output',OUT / entry], entry + '-compile')
    source = OUT / entry / ('Composition' + name + '.java')
    source.write_text('package dev.deal.embedding.composition.' + entry + '; ' + source.read_text())
    run(['javac','--release','21','-cp',CP,'-d',OUT / 'host',source], entry + '-javac')

# Compilation fixtures below have their own manifests, never a second ancestor manifest.
(OUT / 'deal.json').unlink()
# The production Kotlin composer and lowerer link the newly compiled portable implementations.
(OUT / 'RecoveryHarness.kt').write_text('''package dev.deal.embedding
import org.json.*
import java.io.File
fun main(args: Array<String>) {
    val plan = JSONObject(File(args[0]).readText())
    val contracts=listOf(CapabilityContract("host/probe","Recovery fixture",listOf(CapabilityFunction("read","Read fixture",listOf(Parameter("amount",WireType("number"))),WireType("number")),CapabilityFunction("many","Read collection",emptyList(),WireType("array",element=WireType("number"))))))
    val emitter = BehaviorCandidate(contracts,plan.has("custom"))
    if (args.size > 2) {
        check(runCatching { emitter.assemble(plan) }.exceptionOrNull() is BehaviorCandidate.Rejected)
        return
    }
    val pair = emitter.assemble(plan)
    check(pair.toString() == emitter.assemble(plan).toString())
    val output = File(args[1]).apply { mkdirs() }
    File(output,"experience.deal").writeText(pair.getString("deal"))
    File(output,"experience.dealui").writeText(pair.getString("dealui"))
    File(output,"deal.json").writeText("{\\"languageVersion\\":\\"1.5\\",\\"backend\\":\\"js\\",\\"moduleRoots\\":[\\"src\\"]}")
    val bad = JSONObject(plan.toString()).put("notices", JSONArray().put(JSONObject().put("source","not text")))
    check(runCatching { emitter.assemble(bad) }.exceptionOrNull() is BehaviorCandidate.Rejected)
}
''')
cp = str(OUT / 'host') + ':' + str(ROOT / 'build/embedding-library/aar/classes.jar') + ':' + str(ROOT / 'build/behavior-four/json.jar')
run(['/snap/kotlin/current/bin/kotlinc','-jvm-target','21','-Xfriend-paths='+str(ROOT / 'build/embedding-library/aar/classes.jar'),'-classpath',cp,'-d',OUT / 'host',LIB / 'android/src/compiler/BehaviorCandidate.kt',OUT / 'RecoveryHarness.kt'], 'harness')
run(['javac','--release','21','-cp',CP,'-d',OUT / 'host',LIB / 'tools/ToolkitHarness.java'], 'toolkit-harness')
base = {'title':'Independent retained work','fragments':[{'id':'amount','kind':'input','config':{'label':'Amount','format':'integer','value':7,'minimum':0,'maximum':100}}], 'commands':[], 'display':['amount']}
section = {'id':'recovered','kind':'section','config':{
    'imports':[{'name':'upstream','type':'number','value':{'expr':'amount.value'}}],
    'exports':[{'name':'value','type':'number','value':{'expr':'metric.value'}}], 'capabilities':[],
    'plan':{'title':'Local recovery','fragments':[
        {'id':'factor','kind':'input','config':{'label':'Multiplier','format':'integer','value':2,'minimum':1,'maximum':5}},
        {'id':'metric','kind':'result','config':{'label':'Recovered metric','format':'integer','value':{'expr':'upstream.value * factor.value'}}}], 'commands':[], 'display':['factor','metric']}}}
recovered = json.loads(json.dumps(base)); recovered['fragments'] += [section,{'id':'downstream','kind':'result','config':{'label':'Dependent metric','format':'integer','value':{'expr':'recovered.value + amount.value'}}}]; recovered['display'] += ['recovered','downstream']
plans = {'retained':base, 'partial':dict(base,notices=['Unavailable: dependent calculation']), 'recovered':recovered,'notice-only':{'title':'Unavailable','fragments':[],'commands':[],'display':[],'notices':['Unavailable: required behavior']}}
provider=json.loads(json.dumps(base));provider['fragments'].append({'id':'local','kind':'section','config':{
    'imports':[{'name':'request','type':'number','value':{'expr':'amount.value'}}], 'exports':[{'name':'value','type':'number','value':{'expr':'metric.value'}}],
    'capabilities':[{'module':'host/probe','function':'read'}], 'plan':{'title':'Provider section','fragments':[
        {'id':'reading','kind':'read','config':{'binding':{'function':'read','module':'host/probe'},'arguments':[{'expr':'request.value'}]}},
        {'id':'metric','kind':'result','config':{'label':'Provider result','format':'integer','value':{'expr':'reading.result'}}}],
        'commands':[{'id':'Read','label':'Read local','targets':['reading']}], 'display':['metric']}}});provider['display'].append('local');plans['provider']=provider
collection=json.loads(json.dumps(base));collection['fragments'] += [
    {'id':'many','kind':'read','config':{'binding':{'module':'host/probe','function':'many'},'arguments':[]}},
    {'id':'collection','kind':'section','config':{'imports':[{'name':'letters','type':'array','value':{'expr':'many.result'}}],'exports':[{'name':'value','type':'number','value':{'expr':'metric.value'}}],'capabilities':[],'plan':{'title':'Collection import','fragments':[{'id':'metric','kind':'result','config':{'label':'Imported count','format':'integer','value':{'expr':'length(letters.value)'}}}], 'commands':[],'display':['metric']}}}]
collection['commands']=[{'id':'Load','label':'Load collection','targets':['many']}];collection['display'].append('collection');plans['collection']=collection
exported=json.loads(json.dumps(collection));exported['fragments'][1]={'id':'many','kind':'section','config':{'imports':[],'exports':[{'name':'result','type':'array','value':{'expr':'reading.result'}}],'capabilities':[{'module':'host/probe','function':'many'}],'plan':{'title':'Recovered collection read','fragments':[{'id':'reading','kind':'read','config':{'binding':{'module':'host/probe','function':'many'},'arguments':[]}}],'commands':[{'id':'Load','label':'Load exported collection','targets':['reading']}],'display':[]}}};exported['commands']=[];exported['display'].append('many');plans['collection-export']=exported
pure=json.loads(json.dumps(base));pure['custom']=[{'id':'scale','parameters':[{'name':'value','type':'number'}],'result':'number','body':'return value * 2;'}];pure['reason']='Issued pure computation';pure['fragments'].append({'id':'scaled','kind':'custom','config':{'label':'Scaled','format':'integer','section':'scale','arguments':[{'expr':'amount.value'}],'result':'number'}});pure['display'].append('scaled');plans['pure']=pure
pending=json.loads(json.dumps(collection));pending['fragments'][-1]['config']['plan']['fragments'][0]['config']['value']['expr']='sum(letters.value,row) / length(letters.value)';plans['pending-result']=pending
# Empty raw-helper inventories must not accidentally request custom-code admission.
recovered['reason']='Local UI recovery'
for name, plan in plans.items():
    (OUT / (name + '.json')).write_text(json.dumps(plan))
    run(['java','-cp',cp+':/snap/kotlin/current/lib/kotlin-stdlib.jar','dev.deal.embedding.RecoveryHarnessKt',OUT / (name+'.json'),OUT / name / 'input'],name+'-assemble')
    (OUT/name/'input/probe.d.deal').write_text('export function read(amount: number): Promise<number>;\\nexport function many(): Promise<number[]>;\\n'.replace('\\n','\n'))
    (OUT/name/'input/deal.json').write_text(json.dumps({'languageVersion':'1.5','backend':'js','moduleRoots':['src'],'externals':{'host/probe':{'declaration':'probe.d.deal'}}}))
    run(['java','-Ddeal.home='+str(ROOT / 'dependencies/deal'),'-cp',str(OUT / 'host')+':'+str(CP),'ToolkitHarness',LIB / 'ui/platform.dealui-pack',OUT / name / 'input',OUT / name / 'check',ROOT / 'build/embedding-consumer/assets/distribution'],name+'-check')
for name, mutate in [
    ('neighbor-reference',lambda p:p['fragments'][1]['config']['plan']['fragments'][1]['config']['value'].update(expr='amount.value')),
    ('neighbor-command',lambda p:p['fragments'][1]['config']['plan']['commands'].append({'id':'attack','label':'Attack','targets':['amount']})),
    ('unissued-call',lambda p:p['fragments'][1]['config']['plan']['fragments'].append({'id':'attack','kind':'read','config':{'binding':{'module':'host/private','function':'erase'},'arguments':[]}})),
    ('export-type',lambda p:p['fragments'][1]['config']['exports'][0].update(type='boolean')),
    ('nested-section',lambda p:p['fragments'][1]['config']['plan']['fragments'].append(section)),
]:
    bad=json.loads(json.dumps(recovered)); mutate(bad)
    (OUT/(name+'.json')).write_text(json.dumps(bad))
    run(['java','-cp',cp+':/snap/kotlin/current/lib/kotlin-stdlib.jar','dev.deal.embedding.RecoveryHarnessKt',OUT/(name+'.json'),OUT/name/'input','reject'],name+'-reject')
(OUT / 'plans').mkdir()
wire={'kind':'number','fields':[],'element':None}
(OUT / 'plans/contracts.json').write_text(json.dumps([{'module':'host/probe','description':'Recovery fixture','version':1,'functions':[{'name':'read','description':'Read fixture','parameters':[{'name':'amount','type':wire}],'result':wire},{'name':'many','description':'Read collection','parameters':[],'result':{'kind':'array','fields':[],'element':wire}}]}]))
print(run(['node','tools/test-constrained-recovery-session.js',OUT],'runtime').strip())
partial = (OUT / 'partial/input/experience.dealui').read_text()
assert 'Unavailable: dependent calculation' in (OUT / 'partial/input/experience.deal').read_text() or 'Unavailable: dependent calculation' in partial
assert 'Notice(' in partial
(OUT / 'assessment.json').write_text(json.dumps({'passed':True,'scope':'Real production composer/lowerer/compiler/session: retained input, honest gap notice, edit/bounds/restoration; malformed notices reject. Typed UI-section runtime and isolation covered; not live authoring or device runtime acceptance.'},indent=2)+'\n')
print('PASS real composition/compiler: independent retained input, partial gap notice, closed notice shape; evidence '+str(OUT.relative_to(ROOT)))
