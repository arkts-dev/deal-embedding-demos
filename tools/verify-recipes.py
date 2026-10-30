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
function rejected(slots: recipes.Slot[], blocks: recipes.Block[]): boolean {
    try { recipes.place(slots,blocks); }
    catch (value) { let error: Error = value as Error; return error.code === "INVALID_CONFIG"; }
    return false;
}
export function main(): null {
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
print('PASS portable recipes: required slots, accepted kinds, capacity, unique placement; authoritative theme projection')
