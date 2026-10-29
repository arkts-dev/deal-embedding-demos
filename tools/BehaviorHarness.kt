package dev.deal.embedding

import dev.deal.embedding.capabilities.*
import org.json.*
import java.io.File

/** Host-only authoring check using the production maintained emitter. */
fun main(args:Array<String>) {
    val input=File(args[0]);val output=File(args[1]).apply{mkdirs()}
    val contracts=JSONArray(File(args[2]).readText()).let{a->(0 until a.length()).map{CapabilityContract.parse(a.getJSONObject(it).toString())}}
    val original=contracts.first()
    val functions=original.functions.toMutableList()
    val registration=NativeCapabilities(original.copy(functions=functions),functions.associate { it.name to { _:JSONArray -> JSONObject.NULL as Any } })
    functions.clear()
    check(registration.contract.functions==original.functions) { "Registration must freeze app-authored contracts" }
    (registration.contract.functions as? MutableList<*>)?.clear()
    check(registration.contract.functions==original.functions) { "Returned metadata must not mutate registration authority" }
    val plan=JSONObject(input.readText())
    val emitter=BehaviorCandidate(contracts,plan.has("custom"))
    fun assemble(value:JSONObject)=emitter.assemble(if(value.has("kind")||value.has("behaviors"))LegacyCompositionFixture(contracts).expand(value)else value)
    val pair=assemble(plan)
    check(pair.toString()==assemble(plan).toString()) { "Nondeterministic maintained emission" }
    if(input.nameWithoutExtension=="composed-food")check(pair.getString("deal").contains("allocation.allocate(")) { "Allocation must use the authored portable algorithm" }
    fun rejected(bad:JSONObject) { check(runCatching{assemble(bad)}.exceptionOrNull() is BehaviorCandidate.Rejected) }
    if(plan.has("fragments")) {
        rejected(JSONObject(plan.toString()).put("unexpected",true))
        rejected(JSONObject(plan.toString()).apply { getJSONArray("fragments").put(getJSONArray("fragments").getJSONObject(0)) })
        rejected(JSONObject(plan.toString()).apply { getJSONArray("fragments").getJSONObject(0).getJSONObject("config").put("extra",true) })
        listOf("fragments","commands","display").forEach { key ->
            val error=runCatching { emitter.assemble(JSONObject(plan.toString()).put(key,plan.getJSONArray(key).toString())) }.exceptionOrNull()
            check(error is BehaviorCandidate.Rejected && error.message.orEmpty().contains("actual JSON array")) { "Encoded $key must reject with native-array advice: $error" }
        }
        rejected(JSONObject(plan.toString()).apply {
            getJSONArray("fragments").put(JSONObject().put("id","cycle").put("kind","result").put("config",JSONObject().put("label","Cycle").put("format","number").put("value",JSONObject().put("expr","cycle.value"))))
        })
        rejected(JSONObject(plan.toString()).apply {
            getJSONArray("fragments").put(JSONObject().put("id","missing").put("kind","result").put("config",JSONObject().put("label","Missing").put("format","number").put("value",JSONObject().put("expr","absent.value"))))
        })
        File(output,"experience.deal").writeText(pair.getString("deal"));File(output,"experience.dealui").writeText(pair.getString("dealui"))
        val externals=JSONObject()
        contracts.forEachIndexed{i,c->val f="host$i.d.deal";File(output,f).writeText(CapabilityBindings.declaration(c));externals.put(c.module,JSONObject().put("declaration",f))}
        File(output,"deal.json").writeText(JSONObject().put("languageVersion","1.4").put("backend","js").put("moduleRoots",JSONArray().put("src")).put("externals",externals).toString())
        return
    }
    if(plan.has("kind")) {
        val behavior=JSONObject(plan.toString()).apply{remove("title")}
        val wrapped=JSONObject().put("title",plan.getString("title")).put("behaviors",JSONArray().put(behavior))
        check(pair.toString()==assemble(wrapped).toString())
        rejected(JSONObject(plan.toString()).put("functions","return 42;"))
        rejected(JSONObject(wrapped.toString()).put("behaviors",wrapped.getJSONArray("behaviors").toString()))
        rejected(JSONObject(plan.toString()).put("unexpected",true))
        rejected(JSONObject(wrapped.toString()).apply{getJSONArray("behaviors").put(getJSONArray("behaviors").getJSONObject(0))})
        val kind=plan.getString("kind")
        if(kind=="quotedAlternatives") {
            rejected(JSONObject(plan.toString()).put("groups",plan.getString("groups")+" false"))
            rejected(JSONObject(plan.toString()).put("groups","{}"))
            rejected(JSONObject(plan.toString()).put("groups","not data"))
            rejected(JSONObject(plan.toString()).put("groups",JSONArray(plan.getString("groups"))))
        }
        if(kind!="comparison")rejected(JSONObject(plan.toString()).apply{
            val binding=getJSONObject(when(kind){"providerSchedule"->"estimate";"allocation"->"menu";else->"catalogue"})
            binding.put("module","host/undisclosed")
        })
        File(output,"experience.deal").writeText(pair.getString("deal"));File(output,"experience.dealui").writeText(pair.getString("dealui"))
        val externals=JSONObject()
        contracts.forEachIndexed{i,c->val f="host$i.d.deal";File(output,f).writeText(CapabilityBindings.declaration(c));externals.put(c.module,JSONObject().put("declaration",f))}
        File(output,"deal.json").writeText(JSONObject().put("languageVersion","1.4").put("backend","js").put("moduleRoots",JSONArray().put("src")).put("externals",externals).toString())
        return
    }
    if(input.nameWithoutExtension=="food") {
        val allocation=plan.getJSONArray("nodes").let { a -> (0 until a.length()).map(a::getJSONObject).single { it.getString("op")=="allocate" } }
        fun helpers(change:(JSONObject)->Unit):Int {
            val variant=JSONObject(plan.toString())
            val node=JSONObject(allocation.toString()).put("id","additionalAllocation");change(node)
            variant.getJSONArray("nodes").put(node)
            variant.getJSONArray("actions").put(JSONObject().put("id","AdditionalAllocate").put("fields",JSONArray()).put("steps",JSONArray()).put("updates",JSONArray().put(JSONObject().put("field","rows").put("node","additionalAllocation"))))
            val source=emitter.assemble(variant).getString("deal")
            check(!Regex("function allocate[0-9]+\\(").containsMatchIn(source)) { "Search must not be generated in Kotlin" }
            return Regex("allocation\\.allocate\\(").findAll(source).count()
        }
        check(helpers { it.getJSONObject("params").getJSONArray("quotas").getJSONObject(0).put("minimum","zero") }>=2) { "Quota minima must remain call arguments" }
        check(helpers { it.getJSONObject("params").put("cost","diameterMm") }>=2) { "Distinct cost projections must use the portable search" }
        check(helpers { it.getJSONObject("params").getJSONArray("quotas").put(JSONObject().put("field","vegan").put("minimum","zero")) }>=2) { "Quota arity must remain data" }
        check(pair.toString()==emitter.assemble(plan).toString()) { "Assembly must be deterministic between plans" }
    }
    if(input.nameWithoutExtension=="independent") {
        rejected(JSONObject(plan.toString()).apply {
            getJSONArray("actions").getJSONObject(1).getJSONArray("steps").getJSONObject(0).put("bind","leftResult")
        })
        check(pair.getString("deal").contains("m_workflowReadleftEpoch"))
        check(pair.getString("deal").contains("m_workflowReadrightEpoch"))
    }
    rejected(JSONObject(plan.toString()).put("functions","return 42;"))
    rejected(JSONObject(plan.toString()).apply { getJSONArray("state").getJSONObject(0).put("unexpected",true) })
    rejected(JSONObject(plan.toString()).apply { getJSONArray("nodes").getJSONObject(0).put("op","unsupported") })
    val cyclic=JSONObject(plan.toString());cyclic.getJSONArray("nodes").put(JSONObject().put("id","cycle").put("op","abs").put("args",JSONArray().put("cycle")).put("params",JSONObject()));rejected(cyclic)
    rejected(JSONObject(plan.toString()).apply { getJSONArray("state").getJSONObject(0).put("initial",getJSONArray("state").getJSONObject(0).getString("initial")+" trailing") })
    if(plan.getJSONArray("actions").length()>0) rejected(JSONObject(plan.toString()).apply { getJSONArray("actions").getJSONObject(0).put("fields",JSONArray().put(JSONObject().put("name","same").put("type","number")).put(JSONObject().put("name","same").put("type","number"))) })
    val bindingErrors=JSONObject(plan.toString()).apply {
        getJSONArray("derived").put(JSONObject().put("field","missingComputed").put("node",getJSONArray("nodes").getJSONObject(0).getString("id")))
        if(getJSONArray("controls").length()>0)getJSONArray("controls").getJSONObject(0).put("invalidate",JSONArray().put("missingValidity"))
        else getJSONArray("controls").put(JSONObject().put("field",getJSONArray("state").getJSONObject(0).getString("name")).put("label","Test").put("widget","integer").put("minimum",0).put("maximum",100).put("step",1).put("invalidate",JSONArray().put("missingValidity")))
    }
    val diagnostic=runCatching{emitter.assemble(bindingErrors)}.exceptionOrNull()
    check(diagnostic is BehaviorCandidate.Rejected && diagnostic.message.orEmpty().contains("missingComputed") && diagnostic.message.orEmpty().contains("missingValidity")) { "Binding feedback must report independent errors together: $diagnostic" }
    File(output,"experience.deal").writeText(pair.getString("deal"));File(output,"experience.dealui").writeText(pair.getString("dealui"))
    val externals=JSONObject()
    contracts.forEachIndexed{i,c->val f="host$i.d.deal";File(output,f).writeText(CapabilityBindings.declaration(c));externals.put(c.module,JSONObject().put("declaration",f))}
    File(output,"deal.json").writeText(JSONObject().put("languageVersion","1.4").put("backend","js").put("moduleRoots",JSONArray().put("src")).put("externals",externals).toString())
}
