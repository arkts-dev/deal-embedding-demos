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
            receipt.putString("portable", "PASS public ApplicationProtocol.generate → shared buildScreen → Android compilation/shared smoke → canonical publication/mount/edit/reopen; provider/cancellation/unsupported failures publish nothing and never fallback; fixed-route/source regressions pass; no live inference")
            finish(0, receipt)
        } catch (error: Throwable) { receipt.putString("failure", android.util.Log.getStackTraceString(error)); finish(1, receipt) }
    }
}
