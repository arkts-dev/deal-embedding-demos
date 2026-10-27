package dev.deal.embedding

import dev.deal.embedding.capabilities.*
import org.json.*
import java.io.File

/** Isolated prototype entry; deliberately not registered with generation or activation. */
fun main(args:Array<String>) {
    val plan=JSONObject(File(args[0]).readText());val out=File(args[1]).apply{mkdirs()}
    val cs=JSONArray(File(args[2]).readText()).let{a->(0 until a.length()).map{CapabilityContract.parse(a.getJSONObject(it).toString())}}
    val graph=SelectionComposition(cs).expand(plan)
    check(graph.toString()==SelectionComposition(cs).expand(plan).toString())
    File(out,"expanded.json").writeText(graph.toString(2))
    check(runCatching{BehaviorCandidate(cs).assemble(graph)}.exceptionOrNull() is BehaviorCandidate.Rejected) { "Prototype must not enter shipping admission" }
    val pair=BehaviorCandidate(cs,selectionPrototype=true).assemble(graph)
    File(out,"experience.deal").writeText(pair.getString("deal"));File(out,"experience.dealui").writeText(pair.getString("dealui"))
    val externals=JSONObject()
    cs.forEachIndexed{i,c->val f="host$i.d.deal";File(out,f).writeText(CapabilityBindings.declaration(c));externals.put(c.module,JSONObject().put("declaration",f))}
    File(out,"deal.json").writeText(JSONObject().put("languageVersion","1.4").put("backend","js").put("moduleRoots",JSONArray().put("src")).put("externals",externals).toString())
}
