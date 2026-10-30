#!/usr/bin/env python3
"""Execute portable slot rules and native token projection; no inference or device writes."""
import pathlib
import shutil
import subprocess
import tempfile

root = pathlib.Path(__file__).resolve().parents[1]
out = pathlib.Path(tempfile.mkdtemp(prefix='recipe-check-', dir=root / 'build'))
lib = root / 'dependencies/deal-embedding'
shutil.copytree(lib / 'core', out / 'core')
shutil.copytree(lib / 'ui', out / 'ui')
(out / 'deal.json').write_text('{"languageVersion":"1.5","backend":"js","moduleRoots":["core","ui","src"]}')
(out / 'src').mkdir()
(out / 'src/probe.deal').write_text('''import * as recipes from "../core/composition/recipes";
import * as model from "../core/composition/lowering-model";
import * as decoded from "../core/decoded";
import * as builder from "../ui/fragments/support/builder";
function rejected(slots: recipes.Slot[], blocks: recipes.Block[]): boolean {
    try { recipes.place(slots,blocks); }
    catch (value) { let error: Error = value as Error; return error.code === "INVALID_CONFIG"; }
    return false;
}
export function main(): null {
    let contracts: Record<string, unknown>[] = []; let operands: string[] = []; let nodes: Record<string, unknown>[] = [{id:"first",op:"literal",args:operands,params:{type:"number",value:"1"}}];
    let emptyNodes: Record<string, unknown>[] = [];
    let a: model.Model = model.create(contracts,false,false,nodes);
    let b: model.Model = model.create(contracts,false,false,emptyNodes);
    let fields: Record<string, unknown>[] = [{name:"value",type:"number"}];
    model.declare(a,"Example",fields); a.addHelper("helper-a"); b.addHelper("helper-b");
    if (!a.hasNode("first") || b.hasNode("first") || !a.hasRecord("Example") || b.hasRecord("Example") || a.helpers() !== "helper-a" || b.helpers() !== "helper-b" || b.declarations() !== "") {
        throw {code:"ISOLATION_TEST",message:"Composition stores escaped their request"};
    }
    let node: Record<string, unknown> = a.node("first"); if ((node.op as string) !== "literal") { throw {code:"LOOKUP_TEST",message:"Node lookup changed"}; }
    a.removeRecord("Example"); if (a.hasRecord("Example")) { throw {code:"LOOKUP_TEST",message:"Record removal failed"}; }
    let first: builder.Builder = builder.create(contracts); let second: builder.Builder = builder.create(contracts);
    builder.state(first,"answer","number","1"); first.setRoot("first");
    let left: Record<string, unknown> = first.plan(); let right: Record<string, unknown> = second.plan();
    let leftState: Record<string, unknown>[] = decoded.records(left.state); let rightState: Record<string, unknown>[] = decoded.records(right.state);
    if (leftState.length !== 1 || rightState.length !== 0 || (right.root as string) !== "root") { throw {code:"ISOLATION_TEST",message:"Builder stores escaped their request"}; }
    let raw: string = "a\\\\b\\\"c\\nd\\t€";
    if (model.quote(raw) !== "\\\"a\\\\\\\\b\\\\\\\"c\\\\nd\\\\t€\\\"") { throw {code:"QUOTE_TEST",message:"Literal escaping changed"}; }
    let slots: recipes.Slot[] = [{name:"hero",accepts:["result"],required:true,maximum:1}];
    let good: recipes.Block[] = [{id:"a",kind:"result",views:[]}];
    recipes.place(slots,good);
    let empty: recipes.Block[] = [];
    let wrong: recipes.Block[] = [{id:"a",kind:"input",views:[]}];
    let excess: recipes.Block[] = [{id:"a",kind:"result",views:[]},{id:"b",kind:"result",views:[]}];
    let duplicate: recipes.Slot[] = [{name:"a",accepts:["result"],required:true,maximum:1},{name:"b",accepts:["result"],required:true,maximum:1}];
    if (!rejected(slots,empty) || !rejected(slots,wrong) || !rejected(slots,excess) || !rejected(duplicate,good)) {
        throw {code:"RECIPE_TEST",message:"Invalid placement accepted"};
    }
    return null;
}
''')
subprocess.run(['java', '-Ddeal.home=' + str(root / 'dependencies/deal'), '-cp', str(root / 'build/embedding-library/classes'),
                'deal.Main', 'compile', str(out / 'src/probe.deal'), '--backend', 'js', '--output', str(out / 'js')], check=True)
subprocess.run(['node', '-e', 'require(process.argv[1]).main.$f()', str(out / 'js/probe.js')], check=True)
subprocess.run(['python3', str(lib / 'tools/theme-build.py'), str(lib / 'ui/theme.json'), str(out / 'PackTheme.kt')], check=True)
assert (out / 'PackTheme.kt').read_bytes() == (root / 'build/embedding-library/generated/PackTheme.kt').read_bytes()
print('PASS portable recipes, request-local lowering/builder isolation, selected lookups, literal escaping; authoritative theme projection')
