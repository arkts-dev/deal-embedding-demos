package dev.deal.connectors.tests

import android.app.Instrumentation
import android.os.Bundle
import dev.deal.embedding.*
import dev.deal.embedding.capabilities.*
import org.json.JSONObject
import java.util.UUID

/** Inference-free full build route plus emitted-source compiler regressions. */
class PortableBackendInstrumentation : Instrumentation() {
    override fun onCreate(arguments: Bundle?) { super.onCreate(arguments); start() }
    override fun onStart() {
        val receipt = Bundle()
        try {
            val context = targetContext
            val config = RuntimeConfig("experience", emptyList(), "portable-${UUID.randomUUID()}")
            val engineForRoute = SandboxEngine.acquire(context)
            try {
                val needs = """{"title":"Arithmetic","inputs":[{"id":"a","kind":"count","value":8},{"id":"b","kind":"count","value":2}],"needs":[{"type":"figure","id":"v","label":"Sum","formula":"a+b"}]}"""
                var plans = 0; var choices = 0
                PortableRequestBackend(context, config, engineForRoute) { operation, input ->
                    when (operation) {
                        "plan" -> { plans++; check(!input.getBoolean("think")); needs }
                        "jev" -> { choices++; """{"answers":{"expr.v":{"choice":"add:a:b","confidence":1}}}""" }
                        "cancelled" -> """{"value":false}"""
                        "status", "preview" -> ""
                        "toolCount", "toolSpan" -> """{"value":0}"""
                        else -> error("Unexpected route mechanism: $operation $input")
                    }
                }.use { backend ->
                    val request = """{"request":{"agent":{"prompt":"Add a and b and show their sum","widget":false,"scene":"","sceneAllowed":false},"mode":"deepseek","wholeEnabled":true,"surfaceEnabled":false},"originals":["sum"],"language":"","guide":"","surfaceCatalogue":"","data":{"planLanguage":"fixed needs fixture","catalogue":[],"choices":{"sections":[],"realDataInstructions":"","realDataCriteria":[]},"sections":[],"effects":[],"readyKinds":[],"layouts":[],"banner":"","sceneGuide":"","sceneTokens":6000,"custom":true,"planEnabled":true,"alignEnabled":false,"alignment":{"wide":false,"custom":true,"readyKinds":[],"realData":null}}}"""
                    val outcome = backend.build(request)
                    check(outcome.getString("kind") == "agent") { outcome.toString() }
                    val result = outcome.getJSONObject("agent")
                    check(result.getString("errors").isEmpty()) { result.toString() }
                    check(plans == 1 && choices == 1) { "plans=$plans choices=$choices" }
                    check(backend.compileCount > 0 && backend.smokeCount > 0 && backend.executionCount >= 10 && backend.closedMachines == backend.smokeCount)
                    CapabilityRegistry(context, CapabilityDiscovery(context) { false }, emptyList()).use { registry ->
                        val output = backend.output(result.getString("executable"))
                        fun find(node: JSONObject, component: String): JSONObject? {
                            if (node.getString("component") == component) return node
                            val children = node.getJSONArray("children")
                            for (i in 0 until children.length()) find(children.getJSONObject(i), component)?.let { return it }
                            return null
                        }
                        fun prop(node: JSONObject, name: String): JSONObject {
                            val props = node.getJSONArray("props")
                            return (0 until props.length()).map { props.getJSONObject(it) }.single { it.getString("name") == name }
                        }
                        fun sum(snapshot: JSONObject): String {
                            check(snapshot.getString("fault").isEmpty()) { snapshot.toString() }
                            return prop(checkNotNull(find(snapshot.getJSONObject("tree"), "ui.Metric")), "value").getString("stringValue")
                        }
                        var checkpoint: String? = null
                        UiSession(context, registry, engineForRoute, config).use { session ->
                            val initial = session.mount(output); check(sum(initial) == "10.0")
                            val field = checkNotNull(find(initial.getJSONObject("tree"), "ui.TextField"))
                            session.dispatch(prop(field, "onChange").getInt("actionSlot"), "12")
                            check(sum(session.advance()) == "14.0"); checkpoint = session.state(); check(checkpoint != null)
                        }
                        UiSession(context, registry, engineForRoute, config).use { restored -> check(sum(restored.mount(output, checkpoint)) == "14.0") }
                    }
                }
            } finally { SandboxEngine.release(engineForRoute) }
            val source = """import * as ui from "std/ui"
@jsonable
class AppState {aText:string; bText:string;}
function init():AppState {return {aText:"8",bText:"2"};}
function set_a(state:AppState,value:string):AppState {return {aText:value,bText:state.bText};}
function view(state:AppState):ui.View {
 let sections:ui.View[]=[];
 let a:number=strings.toNumber(state.aText,0);
 let b:number=strings.toNumber(state.bText,0);
 let v:number=a+b;
 sections.push(ui.Header("default","Arithmetic","","",""));
 sections.push(ui.Section("content","","",[ui.TextField("default","a",state.aText,"set_a","number")]));
 sections.push(ui.Section("summary","","",[ui.MetricGroup("default",1,[ui.NumberStat("default","Sum",v,"","",1,"","chart",1)])]));
 return ui.AppTheme("clean","",sections);
}"""
            val compiler = ExperienceCompiler(context, config)
            val unsupported = runCatching { compiler.compilePortable(source.replace("ui.NumberStat(", "ui.Unsupported(")) }.exceptionOrNull()
            check(unsupported is deal.codegen.jvm.JvmRuntime.DealError && unsupported.code == "PORTABLE_UNSUPPORTED") { "Missing lowering must propagate, not trigger candidate repair: $unsupported" }
            val malformed = runCatching { compiler.compilePortable(source + "\nfunction invalid(") }.exceptionOrNull()
            check(malformed is CandidateRejected) { "Expected syntax diagnostic data: $malformed" }
            val lowered = compiler.lowerPortable(source)
            // Recheck through ordinary admission, as saved canonical pairs do.
            val output = compiler.compile(lowered.deal, lowered.ui)
            try {
                val engine = SandboxEngine.acquire(context)
                try {
                    CapabilityRegistry(context, CapabilityDiscovery(context) { false }, emptyList()).use { registry ->
                        fun find(node: JSONObject, component: String): JSONObject? {
                            if (node.getString("component") == component) return node
                            val children = node.getJSONArray("children")
                            for (i in 0 until children.length()) find(children.getJSONObject(i), component)?.let { return it }
                            return null
                        }
                        fun prop(node: JSONObject, name: String): JSONObject {
                            val props = node.getJSONArray("props")
                            return (0 until props.length()).map { props.getJSONObject(it) }.single { it.getString("name") == name }
                        }
                        fun sum(snapshot: JSONObject): String {
                            check(snapshot.getString("fault").isEmpty()) { snapshot.toString() }
                            return prop(checkNotNull(find(snapshot.getJSONObject("tree"), "ui.Metric")), "value").getString("stringValue")
                        }
                        var checkpoint: String? = null
                        UiSession(context, registry, engine, config).use { session ->
                            val initial = session.mount(output)
                            check(sum(initial) == "10.0")
                            val input = checkNotNull(find(initial.getJSONObject("tree"), "ui.TextField"))
                            session.dispatch(prop(input, "onChange").getInt("actionSlot"), "12")
                            check(sum(session.advance()) == "14.0")
                            checkpoint = session.state()
                            check(checkpoint != null)
                        }
                        UiSession(context, registry, engine, config).use { restored -> check(sum(restored.mount(output, checkpoint)) == "14.0") }
                    }
                } finally { SandboxEngine.release(engine) }
            } finally { StagingReclamation.remove(context, output.parentFile) }
            var publicPlans=0; var publicChoices=0
            val publicModel=object:ModelClient {
                override fun complete(input:String,previous:String,diagnostics:String,cancellation:GenerationCancellation,log:EmbeddingLog):String {
                    cancellation.check(); check(previous.isEmpty() && diagnostics.isEmpty())
                    if(input.contains("\nPORTABLE COMPLETION\n")) {
                        publicPlans++
                        check(input.contains("frozen-facts-🌍") && input.contains("You turn a request for a small phone app"))
                        check(!JSONObject(input.substringAfterLast("\nPORTABLE COMPLETION\n")).getBoolean("thinking"))
                        return """{"title":"Arithmetic","inputs":[{"id":"a","kind":"count","value":8},{"id":"b","kind":"count","value":2}],"needs":[{"type":"figure","id":"v","label":"Sum","formula":"a+b"}]}"""
                    }
                    check(input.contains("\nPORTABLE CHOICE\n")) { input }
                    publicChoices++
                    return """{"answers":{"expr.v":{"choice":"add:a:b","confidence":1}}}"""
                }
            }
            CapabilityRegistry(context,CapabilityDiscovery(context){false},emptyList()).use { registry ->
                ApplicationProtocol(context,Embedding(context,registry,publicModel,config)).use { api ->
                    val snapshot=api.snapshot(WireType("record",fields=listOf(WireField("facts",WireType("string")))),"""{"facts":"frozen-facts-🌍"}""")
                    val experience=api.generate(api.request("Arithmetic","Add a and b and show their sum",snapshot))
                    try {
                        check(publicPlans==1 && publicChoices==1) { "plans=$publicPlans choices=$publicChoices" }
                        check(experience.attempts==2 && experience.origin=="generated") { experience.toString() }
                        val saved=api.source(experience); check(saved.deal.contains("PortableState"))
                        fun find(node:JSONObject,component:String):JSONObject? {
                            if(node.getString("component")==component)return node
                            val children=node.getJSONArray("children")
                            for(i in 0 until children.length())find(children.getJSONObject(i),component)?.let{return it}
                            return null
                        }
                        fun prop(node:JSONObject,name:String):JSONObject {
                            val props=node.getJSONArray("props")
                            return (0 until props.length()).map{props.getJSONObject(it)}.single{it.getString("name")==name}
                        }
                        val session=api.run(experience)
                        try {
                            val initial=api.next(session,"");check(initial.snapshot.fault.isEmpty())
                            check(prop(checkNotNull(find(initial.snapshot.tree,"ui.Metric")),"value").getString("stringValue")=="10.0")
                            val field=checkNotNull(find(initial.snapshot.tree,"ui.TextField"))
                            api.dispatch(session,prop(field,"onChange").getInt("actionSlot"),"12")
                            val changed=api.next(session,initial.cursor)
                            check(prop(checkNotNull(find(changed.snapshot.tree,"ui.Metric")),"value").getString("stringValue")=="14.0")
                            check(api.source(experience)==saved)
                        } finally {api.close(session)}
                        val reopened=api.run(experience)
                        try {check(api.next(reopened,"").snapshot.fault.isEmpty());check(publicPlans==1 && publicChoices==1)}finally{api.close(reopened)}
                        val reloaded=api.load("Canonical saved-source recheck",saved)
                        try {
                            check(api.source(reloaded)==saved)
                            val cold=api.run(reloaded)
                            try {check(api.next(cold,"").snapshot.fault.isEmpty())}finally{api.close(cold)}
                        } finally {api.forget(reloaded)}
                        val cancelled=api.request("Cancelled before dispatch","Add a and b",snapshot)
                        api.cancel(cancelled)
                        check(runCatching{api.generate(cancelled)}.isFailure)
                        check(publicPlans==1 && publicChoices==1)
                    } finally {api.forget(experience)}
                }
            }
            // Budget delivery path: disclosed amounts, editable signed difference
            // and an independently recomputed over-budget indicator. No source fixture.
            var budgetCalls=0
            val budgetModel=object:ModelClient {
                override fun complete(input:String,previous:String,diagnostics:String,cancellation:GenerationCancellation,log:EmbeddingLog):String {
                    cancellation.check();check(previous.isEmpty() && diagnostics.isEmpty());budgetCalls++
                    if(input.contains("\nPORTABLE COMPLETION\n")) {
                        check(input.contains("approvedCents") && input.contains("19750"))
                        return """{"title":"Budget reconciliation","inputs":[{"id":"approved","label":"Approved EUR","kind":"amount","measure":"money","unit":"EUR","value":180},{"id":"committed","label":"Committed EUR","kind":"amount","measure":"money","unit":"EUR","value":197.5}],"needs":[{"type":"figure","id":"balance","label":"Under (+), over (-)","means":"approved less committed, negative when over budget","format":"money","unit":"EUR","formula":"approved-committed"},{"type":"figure","id":"over","label":"Over budget","means":"1 when committed exceeds approved, otherwise 0","format":"count","formula":"committed>approved","example":{"given":{"approved":180,"committed":197.5},"result":1}}]}"""
                    }
                    check(input.contains("\nPORTABLE CHOICE\n")){input}
                    return """{"answers":{"expr.balance":{"choice":"subtract:approved:committed","confidence":1},"show.balance":{"choice":"stat","confidence":1},"written.over":{"choice":"right","confidence":1},"show.over":{"choice":"stat","confidence":1}}}"""
                }
            }
            CapabilityRegistry(context,CapabilityDiscovery(context){false},emptyList()).use { registry ->
                ApplicationProtocol(context,Embedding(context,registry,budgetModel,config.copy(retainState=true))).use { api ->
                    val facts=api.snapshot(WireType("record",fields=listOf(WireField("approvedCents",WireType("number")),WireField("committedCents",WireType("number")))),"""{"approvedCents":18000,"committedCents":19750}""")
                    val experience=api.generate(api.request("Budget","Compare approved18000 EUR cents with committed19750 EUR cents. Editable amounts,show over/under/equal and difference. Local arithmetic,no external reads.",facts))
                    try {
                        check(budgetCalls==2 && experience.attempts==2){"Budget dispatched repair/fallback: $budgetCalls $experience"}
                        val saved=api.source(experience)
                        fun nodes(node:JSONObject):List<JSONObject> {
                            val out=mutableListOf(node);val children=node.getJSONArray("children")
                            for(i in 0 until children.length())out.addAll(nodes(children.getJSONObject(i)))
                            return out
                        }
                        fun prop(node:JSONObject,name:String):JSONObject {
                            val props=node.getJSONArray("props")
                            return (0 until props.length()).map{props.getJSONObject(it)}.single{it.getString("name")==name}
                        }
                        fun values(tree:JSONObject):List<String> = nodes(tree).filter{it.getString("component")=="ui.Metric"}.map{prop(it,"value").getString("stringValue")}
                        val session=api.run(experience)
                        try {
                            var frame=api.next(session,"");check(frame.snapshot.fault.isEmpty())
                            check(values(frame.snapshot.tree)==listOf("€-17.50","1")){frame.snapshot.tree.toString()}
                            val fields=nodes(frame.snapshot.tree).filter{it.getString("component")=="ui.TextField"}
                            check(fields.size==2)
                            fun edit(index:Int,value:String,expected:List<String>) {
                                api.dispatch(session,prop(fields[index],"onChange").getInt("actionSlot"),value)
                                val changed=api.next(session,frame.cursor)
                                check(changed.cursor!=frame.cursor && changed.snapshot.version>frame.snapshot.version && changed.snapshot.fault.isEmpty())
                                check(values(changed.snapshot.tree)==expected){changed.snapshot.tree.toString()};frame=changed
                            }
                            edit(1,"198,50",listOf("€-18.50","1"))
                            edit(0,"200",listOf("€1.50","0"))
                            edit(0,"198.50",listOf("€0.00","0"))
                            check(api.source(experience)==saved && budgetCalls==2)
                        } finally {api.close(session)}
                        val reopened=api.run(experience)
                        try {check(values(api.next(reopened,"").snapshot.tree)==listOf("€0.00","0"))}finally{api.close(reopened)}
                        val reloaded=api.load("Budget canonical source",saved)
                        try {
                            check(api.source(reloaded)==saved)
                            val cold=api.run(reloaded)
                            try {check(values(api.next(cold,"").snapshot.tree)==listOf("€-17.50","1"))}finally{api.close(cold)}
                        } finally {api.forget(reloaded)}
                        check(budgetCalls==2)
                    } finally {api.forget(experience)}
                }
            }
            // Public departure construction uses disclosed targets and actual
            // provider execution; the framework contains no travel formula.
            val vehicle=dev.deal.apps.vehicle.vehicleCapabilities()
            var travelReads=0;var departureCalls=0;var departureCodeCalls=0;var failTravel=false
            val observedVehicle=NativeCapabilities(vehicle.contract,mapOf("estimate" to {args:org.json.JSONArray->travelReads++;if(failTravel)throw IllegalStateException("Travel fixture unavailable");vehicle.invoke("estimate",args)}))
            val departureModel=object:ModelClient {
                override fun complete(input:String,previous:String,diagnostics:String,cancellation:GenerationCancellation,log:EmbeddingLog):String {
                    cancellation.check();departureCalls++
                    if(input.contains("\nPORTABLE COMPLETION\n")) {
                        val operation=JSONObject(input.substringAfterLast("\nPORTABLE COMPLETION\n")).getString("operation")
                        if(operation=="code") {
                            departureCodeCalls++
                            check(departureCodeCalls==1){"Unexpected departure repair: " + input.substringAfterLast("It failed:").take(3000)}
                            check(previous.isEmpty()&&diagnostics.isEmpty())
                            check(input.contains("\"target\":\"host\\/trip.estimate\"") || input.contains("\"target\":\"host/trip.estimate\"")){"Actual disclosed call target missing"}
                            return """<classes></classes>
<fields>c_travel: number = 0;
c_start: number = 960;
c_distance: number = 12;
c_unload: number = 15;
c_inspection: number = 10;
c_message: string = "Read travel first";</fields>
<functions>
function c_read(state: AppState, value: number): AppState {
  sdk.call("host/trip.estimate", ["distanceKm", ui.numberToString(state.c_distance)], "c_answer");
  return {c_message: "Reading" __PASSTHROUGH__};
}
function c_distance(state: AppState, value: number): AppState {
  sdk.call("host/trip.estimate", ["distanceKm", ui.numberToString(value)], "c_answer");
  return {c_distance: value, c_message: "Reading" __PASSTHROUGH__};
}
function c_start(state: AppState, value: number): AppState { return {c_start: value __PASSTHROUGH__}; }
function c_unload(state: AppState, value: number): AppState { return {c_unload: value __PASSTHROUGH__}; }
function c_inspection(state: AppState, value: number): AppState { return {c_inspection: value __PASSTHROUGH__}; }
function c_answer(state: AppState, value: string): AppState {
  let travel: number = strings.toNumber(value, -1);
  if (travel < 0) { return {c_message: "Read failed; retry" __PASSTHROUGH__}; }
  return {c_travel: travel, c_message: "" __PASSTHROUGH__};
}
</functions>
<view>
sections.push(ui.Section("content", "Travel", "", [ui.TimeField("default", "Collection start", state.c_start, "c_start"), ui.NumberField("default", "Distance km", state.c_distance, 1, 1000, "c_distance"), ui.NumberField("default", "Unloading minutes", state.c_unload, 0, 120, "c_unload"), ui.NumberField("default", "Inspection minutes", state.c_inspection, 0, 120, "c_inspection"), ui.Button("default", "Read travel", "c_read", 0, true), ui.Text("default", state.c_message)]));
sections.push(ui.Section("summary", "Times", "", [ui.Stat("default", "Vehicle minutes", ui.numberToString(state.c_travel), "", "chart"), ui.Stat("default", "Departure", ui.clockToString(state.c_start - state.c_travel - state.c_unload - state.c_inspection), "", "clock"), ui.Stat("default", "Inspection", ui.clockToString(state.c_start - state.c_inspection), "", "clock")]));
</view>"""
                        }
                        check(operation=="plan"){"Unexpected departure operation: $operation"}
                        check(input.contains("Simulated vehicle travel") && input.contains("estimate")){"Departure contract not disclosed"}
                        return """{"title":"Departure","needs":[{"type":"other","what":"Read Vehicle travel estimate and recompute departure and inspection with editable start, distance, unloading and inspection buffers; retry failed reads","on":"other"}]}"""
                    }
                    check(input.contains("\nPORTABLE CHOICE\n")){"Unexpected departure transport: $input"}
                    val questions=JSONObject(input.substringAfterLast("\nPORTABLE CHOICE\n")).getJSONObject("questions")
                    val answers=JSONObject()
                    questions.keys().forEach { key ->
                        val question=questions.getJSONObject(key)
                        if(question.getString("type")=="choice") {
                            val criteria=question.getJSONObject("criteria")
                            val choice=when {criteria.has("none")->"none";criteria.has("own")->"own";criteria.has("no")->"no";criteria.has("false")->"false";else->null}
                            if(choice!=null)answers.put(key,JSONObject().put("choice",choice).put("confidence",1))
                        }
                    }
                    return JSONObject().put("answers",answers).toString()
                }
            }
            CapabilityRegistry(context,CapabilityDiscovery(context){false},listOf(observedVehicle)).use {registry->
                ApplicationProtocol(context,Embedding(context,registry,departureModel,config.copy(retainState=true))).use {api->
                    api.refresh();api.grant("host/trip")
                    val before=api.saved().map{it.id}.toSet()
                    val request=api.request("Departure","Equipment collection at16:00. Distance12km; use actual Vehicle travel estimate. Need15min unloading and10min inspection. Editable start,distance,buffers; recompute departure and inspection. No booking.",api.snapshot(WireType("record"),"{}"))
                    val experience=api.generate(request)
                    var session:Session?=null
                    try {
                        check(departureCodeCalls==1 && travelReads==0)
                        val source=api.source(experience);session=api.run(experience)
                        var update=api.next(session,"")
                        fun nodes(node:JSONObject):List<JSONObject> = listOf(node)+(0 until node.getJSONArray("children").length()).flatMap{nodes(node.getJSONArray("children").getJSONObject(it))}
                        fun prop(node:JSONObject,name:String):JSONObject {val values=node.getJSONArray("props");return (0 until values.length()).map{values.getJSONObject(it)}.single{it.getString("name")==name}}
                        fun metrics()=nodes(update.snapshot.tree).filter{it.getString("component")=="ui.Metric"}.map{prop(it,"value").getString("stringValue")}
                        fun dispatch(label:String,payload:String?,ready:()->Boolean) {
                            val node=nodes(update.snapshot.tree).single{n->val p=n.getJSONArray("props");(0 until p.length()).map{p.getJSONObject(it)}.any{it.optString("name") in listOf("label","text") && it.optString("stringValue")==label}}
                            val event=if(node.getString("component")=="ui.Button")"onClick" else "onChange"
                            val version=update.snapshot.version
                            api.dispatch(session!!,prop(node,event).getInt("actionSlot"),payload)
                            val deadline=System.nanoTime()+java.util.concurrent.TimeUnit.SECONDS.toNanos(20)
                            do {check(System.nanoTime()<deadline){"Departure update timed out"};update=api.next(session!!,update.cursor);check(update.snapshot.fault.isEmpty())}while(update.snapshot.version<=version || !ready())
                        }
                        dispatch("Read travel",null){metrics()==listOf("29","15:06","15:50")}
                        check(travelReads==1)
                        dispatch("Distance km","20"){metrics()==listOf("45","14:50","15:50")}
                        check(travelReads==2)
                        dispatch("Unloading minutes","20"){metrics()==listOf("45","14:45","15:50")}
                        dispatch("Inspection minutes","15"){metrics()==listOf("45","14:40","15:45")}
                        dispatch("Collection start","1020"){metrics()==listOf("45","15:40","16:45")}
                        check(travelReads==2 && departureCodeCalls==1 && api.source(experience)==source)
                        failTravel=true
                        dispatch("Read travel",null){nodes(update.snapshot.tree).any{n->val p=n.getJSONArray("props");(0 until p.length()).map{p.getJSONObject(it)}.any{it.optString("stringValue")=="Read failed; retry"}}}
                        check(travelReads==3 && metrics()==listOf("45","15:40","16:45"))
                        failTravel=false
                        dispatch("Read travel",null){travelReads==4 && nodes(update.snapshot.tree).none{n->val p=n.getJSONArray("props");(0 until p.length()).map{p.getJSONObject(it)}.any{it.optString("stringValue") in listOf("Reading","Read failed; retry")}}}
                        check(metrics()==listOf("45","15:40","16:45"))
                        api.revoke("host/trip")
                        dispatch("Read travel",null){nodes(update.snapshot.tree).any{n->val p=n.getJSONArray("props");(0 until p.length()).map{p.getJSONObject(it)}.any{it.optString("stringValue")=="Read failed; retry"}}}
                        check(travelReads==4)
                        api.grant("host/trip")
                        dispatch("Read travel",null){travelReads==5 && nodes(update.snapshot.tree).none{n->val p=n.getJSONArray("props");(0 until p.length()).map{p.getJSONObject(it)}.any{it.optString("stringValue") in listOf("Reading","Read failed; retry")}}}
                        api.close(session!!);session=api.run(experience);update=api.next(session!!,"")
                        check(metrics()==listOf("45","15:40","16:45") && travelReads==5 && departureCodeCalls==1 && api.source(experience)==source)
                        api.close(session!!);session=null
                        val cold=api.load("Departure exact reload",source)
                        try {session=api.run(cold);update=api.next(session!!,"");check(metrics()==listOf("0","15:35","15:50") && travelReads==5 && api.source(cold)==source)}finally{session?.let{api.close(it)};session=null;api.forget(cold)}
                    } finally {session?.let{api.close(it)};api.forget(experience)}
                    check(api.saved().map{it.id}.toSet()==before)
                }
            }
            // Failures and cancellation must not dispatch repair/whole fallback,
            // publish candidates, or poison the next request's runtime owner.
            for(failureMode in listOf("provider","cancel","unsupported")) {
                var calls=0
                val entered=java.util.concurrent.CountDownLatch(1)
                val failing=object:ModelClient {
                    override fun complete(input:String,previous:String,diagnostics:String,cancellation:GenerationCancellation,log:EmbeddingLog):String {
                        calls++;entered.countDown()
                        when(failureMode) {
                            "provider" -> throw ProtocolFailure("PROVIDER_HTTP","fixture transport failure")
                            "cancel" -> {val stopped=java.util.concurrent.CountDownLatch(1);cancellation.onCancel{stopped.countDown()}.use{check(stopped.await(10,java.util.concurrent.TimeUnit.SECONDS));cancellation.check()};error("Cancellation lost")}
                            else -> return """{"title":"Toggle","inputs":[{"id":"enabled","kind":"toggle","label":"Enabled"}],"needs":[{"type":"figure","id":"v","label":"Sum","formula":"1"}]}"""
                        }
                    }
                }
                CapabilityRegistry(context,CapabilityDiscovery(context){false},emptyList()).use { registry ->
                    ApplicationProtocol(context,Embedding(context,registry,failing,config)).use { api ->
                        val before=api.saved().map{it.id}.toSet()
                        val request=api.request("Failure fixture","Add a and b and show their sum",api.snapshot(WireType("record"),"{}"))
                        val executor=java.util.concurrent.Executors.newSingleThreadExecutor()
                        try {
                            val future=executor.submit<Throwable?>{runCatching{api.generate(request)}.exceptionOrNull()}
                            check(entered.await(10,java.util.concurrent.TimeUnit.SECONDS))
                            if(failureMode=="cancel")api.cancel(request)
                            val error=future.get(60,java.util.concurrent.TimeUnit.SECONDS)
                            check(error!=null){"Failure unexpectedly published"}
                            if(failureMode=="provider")check(error is ProtocolFailure && error.code=="PROVIDER_HTTP"){error.toString()}
                            if(failureMode=="cancel")check(error is java.util.concurrent.CancellationException || (error is ProtocolFailure && error.code=="CANCELLED")){error.toString()}
                            if(failureMode=="unsupported")check(error is ProtocolFailure && error.code=="PORTABLE_UNSUPPORTED"){error.toString()}
                            check(calls==1){"Operational failure dispatched fallback: $failureMode $calls $error"}
                            check(api.saved().map{it.id}.toSet()==before)
                        } finally {executor.shutdownNow();check(executor.awaitTermination(10,java.util.concurrent.TimeUnit.SECONDS))}
                    }
                }
            }
            receipt.putString("portable", "PASS public arithmetic and budget generation → shared buildScreen → Android compilation/shared smoke → canonical publication/mount/edit/reopen/reload; budget over/under/equal, decimal comma and two editable inputs; departure invokes actual Vehicle, recomputes edited distance/start/buffers, handles failure/retry/revocation, retains state and reloads exact source; provider/cancellation/unsupported failures never fallback; no live inference")
            finish(0, receipt)
        } catch (error: Throwable) { receipt.putString("failure", android.util.Log.getStackTraceString(error)); finish(1, receipt) }
    }
}
