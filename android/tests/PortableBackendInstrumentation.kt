package dev.deal.connectors.tests

import android.app.Instrumentation
import android.os.Bundle
import dev.deal.embedding.*
import dev.deal.embedding.capabilities.*
import org.json.JSONObject
import java.util.UUID

/** Inference-free emitted-source → shared lowering → Android compiler/session. */
class PortableBackendInstrumentation : Instrumentation() {
    override fun onCreate(arguments: Bundle?) { super.onCreate(arguments); start() }
    override fun onStart() {
        val receipt = Bundle()
        try {
            val context = targetContext
            val config = RuntimeConfig("experience", emptyList(), "portable-${UUID.randomUUID()}")
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
            receipt.putString("portable", "PASS shared JVM lowering, real Android DEAL/Deal UI compilation, sandbox input recomputation and restoration; no inference; production unchanged")
            finish(0, receipt)
        } catch (error: Throwable) { receipt.putString("failure", android.util.Log.getStackTraceString(error)); finish(1, receipt) }
    }
}
