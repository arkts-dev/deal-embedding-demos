"use strict";
const fs = require('fs'), path = require('path'), vm = require('vm'), assert = require('assert');
const base = path.resolve('build/checked-experience/js');
const factories = Object.create(null);
function walk(dir) {
  for (const name of fs.readdirSync(dir)) {
    const file = path.join(dir, name);
    if (fs.statSync(file).isDirectory()) walk(file);
    else if (name.endsWith('.js')) factories[path.relative(base, file).split(path.sep).join('/')] = new Function('require','module','exports','process','console',fs.readFileSync(file, 'utf8'));
  }
}
walk(base);
vm.runInThisContext(fs.readFileSync('dependencies/deal-embedding/android/assets/embedding/bindings/sandbox.js', 'utf8'));
vm.runInThisContext(fs.readFileSync('dependencies/deal-embedding/android/assets/embedding/bindings/ui-session.js', 'utf8'));
configureDealCapabilities(JSON.parse(fs.readFileSync('build/discovered-contracts.json', 'utf8')));
const entry = 'experience.js';
const initial = JSON.parse(mountDealUi(factories, entry));
function props(node, component) {
  if (node.component.endsWith('.' + component)) return node.props;
  for (const child of node.children) { const result = props(child, component); if (result) return result; }
}
const add = props(initial.tree, 'Button').find(p => p.name === 'onClick').actionSlot;
dealUi.dispatch(add);
const changed = JSON.parse(dealUi.snapshot());
assert(changed.version > initial.version);
assert.equal(props(changed.tree, 'IntText').find(p => p.name === 'value').intValue, 10);
function buttons(node, out = []) { if (node.component.endsWith('.Button')) out.push(node); for (const child of node.children) buttons(child,out); return out; }
const calc = buttons(changed.tree).find(n => n.props.some(p => p.stringValue === 'Calculate departure')).props.find(p => p.name === 'onClick').actionSlot;
dealUi.dispatch(calc);
(async () => {
  for (let i = 0; i < 20; i++) {
    for (const req of JSON.parse(dealCapabilities.take())) {
      const value = req.module === 'host/calendar' ? (req.function === 'start' ? 600 : null) : req.module === 'host/todo' ? [{id:'a',title:'Pack',done:false, minutes:15},{id:'b',title:'Prepare',done:false, minutes:10}] : 29;
      dealCapabilities.deliver([{id:req.id,ok:true,value}]);
    }
    await new Promise(resolve => setImmediate(resolve));
  }
  const finished = JSON.parse(dealUi.snapshot());
  assert.equal(finished.fault, '');
  assert.equal(props(finished.tree, 'Time').find(p => p.name === 'value').intValue, 536);
  const toggle = [...(function* walk(node) { yield node; for (const child of node.children) yield* walk(child); })(finished.tree)].find(n => n.component === 'ui.Toggle');
  dealUi.dispatch(propValue(toggle, 'onChange', 'actionSlot'), propValue(toggle, 'value', 'stringValue'));
  const selected = JSON.parse(dealUi.state());
  assert.equal(selected.tasks[0].selected, false);
  const saved = selected;
  function propValue(node, name, field) { return node.props.find(p => p.name === name)[field]; }
  dealUi.dispose();
  const restored = JSON.parse(mountDealUi(factories, entry, saved));
  assert.equal(props(restored.tree, 'IntText').find(p => p.name === 'value').intValue, 10);
  assert.equal(JSON.parse(dealUi.state()).tasks[0].selected, false);
  assert.throws(() => dealUi.dispatch(99999), /Invalid or stale/);
  dealUi.dispose();
  console.log('Checked generated UI: mounted, state changed, contract-driven capabilities completed, disposed');
})().catch(error => { console.error(error); process.exitCode = 1; });
