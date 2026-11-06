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
            receipt.putString("portable", "PASS fixed needs → portable buildScreen → real Android compilation/shared smoke → sandbox input recomputation/restoration; source-only regressions pass; no inference; production unchanged")
            finish(0, receipt)
        } catch (error: Throwable) { receipt.putString("failure", android.util.Log.getStackTraceString(error)); finish(1, receipt) }
    }
}
