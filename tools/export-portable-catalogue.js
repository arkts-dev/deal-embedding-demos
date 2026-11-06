#!/usr/bin/env node
'use strict';
// Copy pinned catalogue/prompt data, never decisions, into the standalone library.
const fs = require('fs'), path = require('path'), cp = require('child_process');
const reference = path.resolve(process.argv[2]), destination = path.resolve(process.argv[3]);
const ts = require(process.argv[4]);
const revision = '7ee06af1941729b00a110493ad9d1d9b5b127d17';
const root = path.resolve('build/portable-public/pinned-export');
fs.mkdirSync(root, {recursive:true});
const archive = cp.execFileSync('git', ['-C', reference, 'archive', revision,
  'entry/src/main/ets/vera', 'tools/vera-harness', 'entry/src/main/resources/rawfile/vera-skill.txt',
  'entry/src/main/resources/rawfile/vera-surface-skill.txt'], {maxBuffer:32*1024*1024});
cp.execFileSync('tar', ['-x', '-C', root], {input:archive});
process.env.VERA_TYPESCRIPT_PATH = process.argv[4];
const {loadVera} = require(path.join(root,'tools/vera-harness/source-loader.js'));
function constants(file, names) {
  const text = fs.readFileSync(path.join(root,'entry/src/main/ets/vera',file+'.ets'),'utf8');
  const ast = ts.createSourceFile(file,text,ts.ScriptTarget.Latest,true);
  const declarations = ast.statements.filter(s => (s.name && names.includes(s.name.text)) ||
    (s.declarationList && s.declarationList.declarations.some(d => names.includes(d.name.text))));
  if (declarations.length !== names.length) throw Error('Missing pinned data declaration in '+file);
  const source = declarations.map(s=>s.getText(ast).replace(/^export /,'')).join('\n');
  return new Function(ts.transpileModule(source,{compilerOptions:{target:ts.ScriptTarget.ES2020}}).outputText+
    '\nreturn {'+names.join(',')+'};')();
}
const templates = loadVera('VeraTemplates'), specs=templates.fragments();
const compose=constants('VeraCompose',['READY_KINDS','EffectParam','Effect','EFFECTS']);
const real=templates.vetoes().find(v=>v.key==='needs_real_data');
const entries = map => [...map].map(([key,value])=>({key,value}));
const scene=constants('agent/AgentScene',['SCENE_EXAMPLE','SCENE_GUIDE']);
const data={
  planLanguage:constants('NeedsPlan',['NEEDS_PROMPT']).NEEDS_PROMPT,
  catalogue:specs.map(f=>({fragment:f,slots:f.slots.map(s=>({...s,criteria:entries(s.criteria)})),
    contentFields:f.contentFields,listFields:f.listFields,covers:f.covers,coversIf:f.coversIf})),
  choices:{sections:specs.map(f=>({key:f.kind,description:f.criteria})),realDataInstructions:real.instructions,
    realDataCriteria:[...real.criteria].map(([key,description])=>({key,description}))},
  sections:specs.map(f=>({question:'',kind:f.kind,owns:f.owns})),
  // Device effects are supplied only by an installed platform contract binding.
  // OHOS targets remain in referenceEffects as data, not Android authority.
  effects:[], readyKinds:compose.READY_KINDS,layouts:loadVera('VeraLayouts').layouts(),
  banner:templates.bannerSectionView(),sceneGuide:scene.SCENE_GUIDE,sceneTokens:6000,
  custom:true,planEnabled:true,alignEnabled:true,
  alignment:{wide:true,custom:true,readyKinds:compose.READY_KINDS,realData:{instructions:real.instructions,criteria:entries(real.criteria)}}
};
const rawfile = name=>fs.readFileSync(path.join(root,'entry/src/main/resources/rawfile',name),'utf8');
const result={referenceRevision:revision,data,referenceEffects:compose.EFFECTS,
  language:rawfile('vera-skill.txt'),guide:rawfile('vera-surface-skill.txt'),surfaceCatalogue:''};
fs.writeFileSync(destination,JSON.stringify(result,null,2)+'\n');
console.log(`Exported ${specs.length} pinned catalogue fragments to ${process.argv[3]}`);
