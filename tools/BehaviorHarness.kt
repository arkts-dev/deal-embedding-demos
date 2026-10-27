package dev.deal.embedding

import dev.deal.embedding.capabilities.*
import org.json.*
import java.io.File

/** Host-only authoring check using the production maintained emitter. */
fun main(args:Array<String>) {
    val input=File(args[0]);val output=File(args[1]).apply{mkdirs()}
    val contracts=JSONArray(File(args[2]).readText()).let{a->(0 until a.length()).map{CapabilityContract.parse(a.getJSONObject(it).toString())}}
    val plan=JSONObject(input.readText())
    val emitter=BehaviorCandidate(contracts,plan.has("custom"))
    val pair=emitter.assemble(plan)
    check(pair.toString()==emitter.assemble(plan).toString()) { "Nondeterministic maintained emission" }
    fun rejected(bad:JSONObject) { check(runCatching{emitter.assemble(bad)}.exceptionOrNull() is BehaviorCandidate.Rejected) }
    rejected(JSONObject(plan.toString()).put("functions","return 42;"))
    rejected(JSONObject(plan.toString()).apply { getJSONArray("state").getJSONObject(0).put("unexpected",true) })
    rejected(JSONObject(plan.toString()).apply { getJSONArray("nodes").getJSONObject(0).put("op","unsupported") })
    val cyclic=JSONObject(plan.toString());cyclic.getJSONArray("nodes").put(JSONObject().put("id","cycle").put("op","abs").put("args",JSONArray().put("cycle")).put("params",JSONObject()));rejected(cyclic)
    rejected(JSONObject(plan.toString()).apply { getJSONArray("state").getJSONObject(0).put("initial",getJSONArray("state").getJSONObject(0).getString("initial")+" trailing") })
    if(plan.getJSONArray("actions").length()>0) rejected(JSONObject(plan.toString()).apply { getJSONArray("actions").getJSONObject(0).put("fields",JSONArray().put(JSONObject().put("name","same").put("type","number")).put(JSONObject().put("name","same").put("type","number"))) })
    File(output,"experience.deal").writeText(pair.getString("deal"));File(output,"experience.dealui").writeText(pair.getString("dealui"))
    val externals=JSONObject()
    contracts.forEachIndexed{i,c->val f="host$i.d.deal";File(output,f).writeText(CapabilityBindings.declaration(c));externals.put(c.module,JSONObject().put("declaration",f))}
    File(output,"deal.json").writeText(JSONObject().put("languageVersion","1.4").put("backend","js").put("moduleRoots",JSONArray().put("src")).put("externals",externals).toString())
}
