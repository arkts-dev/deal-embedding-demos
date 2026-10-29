'use strict';
// Execute exact packaged framework policy and checked UI; providers alone are external boundaries.
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const assets = process.argv[2] || 'build/embedding-library/aar/assets';
function load(root, ui = false) {
  const context = vm.createContext({});
  for (const file of ['dependencies/deal-embedding/android/assets/embedding/bindings/sandbox.js',
    ...(ui ? ['dependencies/deal-ui/runtime-js/session.js', 'dependencies/deal-embedding/android/assets/embedding/bindings/ui-session.js'] : [])]) {
    vm.runInContext(fs.readFileSync(file, 'utf8'), context);
  }
  context.factories = Object.create(null);
  function walk(directory, prefix = '') {
    for (const name of fs.readdirSync(directory)) {
      const file = path.join(directory, name), key = prefix + name;
      if (fs.statSync(file).isDirectory()) walk(file, key + '/');
      else if (name.endsWith('.js')) context.factories[key] = new Function('require', 'module', 'exports', 'process', 'console', fs.readFileSync(file, 'utf8'));
    }
  }
  walk(path.join(assets, root));
  return context;
}
const policy = load('discovery');
vm.runInContext("globalThis.policy=dealLoad(factories,'discovery.js').entry", policy);
function call(name, ...args) {
  policy.arguments = args;
  return vm.runInContext(`policy[${JSON.stringify(name)}].$f(...arguments)`, policy);
}
const capture = JSON.stringify({ facts: [{ id: 'task', text: 'Compare supplied budgets; unfinished.' }], capabilityIds: ['host/opaque.read'], catalogue: '[]' });
const proposal = { title: 'Budget comparison', brief: 'Editable amounts and calculated difference.', supportingFactIds: ['task'], capabilityIds: [] };
assert.match(call('prompt', capture), /supportingFactIds/);
assert.match(call('prompt', capture), /Never copy provider implementation formulas/);
assert.equal(JSON.parse(call('validate', 'null', capture)).suggestion, null);
assert.deepEqual(JSON.parse(call('validate', JSON.stringify(proposal), capture)).suggestion, proposal);
for (const reply of ['{}', '[]', 'true', JSON.stringify(proposal) + ' trailing',
  JSON.stringify({ ...proposal, title: null }), JSON.stringify({ ...proposal, extra: true }),
  JSON.stringify({ ...proposal, supportingFactIds: ['missing'] }),
  JSON.stringify({ ...proposal, supportingFactIds: ['task', 'task'] }),
  JSON.stringify({ ...proposal, capabilityIds: ['host/forged.read'] }),
  JSON.stringify(proposal).replace('"title":', '"title":"duplicate","title":')]) {
  assert.equal(JSON.parse(call('validate', reply, capture)).accepted, false, reply);
}
assert.equal(call('admit', 'one', 'one', 'same', 'same', 'true'), '');
for (const args of [['one', 'two', 'same', 'same', 'true'], ['one', 'one', 'same', 'different', 'true'], ['one', 'one', 'same', 'same', 'false']]) {
  assert.throws(() => call('admit', ...args));
}
assert.doesNotMatch(call('intent'), /Editable amounts and calculated difference/);
function nodes(tree) { return [tree, ...tree.children.flatMap(nodes)]; }
function prop(node, name) { return node.props.find(value => value.name === name); }
async function card(action) {
  const ui = load('assistance', true);
  vm.runInContext("configureDealCapabilities([{module:'embedding/assistance',functions:[{name:'open',parameters:[],result:{kind:'null'}},{name:'dismiss',parameters:[],result:{kind:'null'}}]}]);mountDealUi(factories,'experience_entry.js',{title:'A real suggestion',busy:false})", ui);
  let snapshot = JSON.parse(vm.runInContext('dealUi.snapshot()', ui));
  assert.equal(prop(nodes(snapshot.tree).find(node => node.component === 'ui.Heading'), 'value').stringValue, 'A real suggestion');
  const button = nodes(snapshot.tree).find(node => prop(node, 'text')?.stringValue === action);
  const slot = prop(button, 'onClick').actionSlot;
  vm.runInContext(`dealUi.dispatch(${slot},null)`, ui);
  await new Promise(resolve => setImmediate(resolve));
  snapshot = JSON.parse(vm.runInContext('dealUi.snapshot()', ui));
  for (const node of nodes(snapshot.tree).filter(node => prop(node, 'onClick'))) assert.equal(prop(node, 'enabled').booleanValue, false);
  const requests = JSON.parse(vm.runInContext('dealCapabilities.take()', ui));
  assert.equal(requests.length, 1);
  assert.deepEqual(requests[0], { id: 1, module: 'embedding/assistance', function: action.toLowerCase(), args: [] });
  vm.runInContext('dealCapabilities.deliver([{id:1,ok:true,value:null}])', ui);
  await new Promise(resolve => setImmediate(resolve));
  snapshot = JSON.parse(vm.runInContext('dealUi.snapshot()', ui));
  assert.equal(snapshot.fault, '');
  for (const node of nodes(snapshot.tree).filter(node => prop(node, 'onClick'))) assert.equal(prop(node, 'enabled').booleanValue, true);
  vm.runInContext("dealUi.dispose();configureDealCapabilities([])", ui);
  // The separately mounted generated isolate never receives this private module inventory.
  assert.throws(() => vm.runInContext("dealLoad(factories,'embedding/assistance').entry", ui));
}
(async () => {
  await card('Open'); await card('Dismiss');
  console.log('PASS packaged portable discovery: closed replies, references, abstention and stale/issued admission; checked suggestion Open/Dismiss, pending/completion and isolated action inventory.');
})().catch(error => { console.error(error); process.exitCode = 1; });
