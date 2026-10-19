package dev.deal.connectors.tests

import android.app.Instrumentation
import android.os.Bundle
import dev.deal.embedding.*
import dev.deal.embedding.capabilities.*
import org.json.*
import java.util.UUID

/** Deterministic compiled protocol observation regression; no live inference or native file reader. */
class InspectionInstrumentation : Instrumentation() {
    override fun onCreate(arguments: Bundle?) { super.onCreate(arguments); start() }
    override fun onStart() {
        val result = Bundle()
        val ctx = targetContext
        val model = object : ModelClient { override fun complete(input:String, previous:String, diagnostics:String, cancellation:GenerationCancellation, log:EmbeddingLog):String = error("Unexpected inference") }
        try {
            ApplicationProtocol(ctx, Embedding(ctx, CapabilityRegistry(ctx, CapabilityDiscovery(ctx){false}, emptyList()), model, EmbeddingConfig("experience", emptyList(), "inspection-${UUID.randomUUID()}"))).use { api ->
                run {
                    fun event(trace:String, experience:String="", stage:String="source", outcome:String="started", target:String="", time:Long=1) = JSONObject().put("trace",trace).put("experience",experience).put("stage",stage).put("outcome",outcome).put("target",target).put("time",time.toString())
                    // Reversed links require fixed-point expansion; similarly named targets do not link.
                    val entries = listOf(event("d2", stage="opportunity-handoff", outcome="admitted", target="d1"), event("d1", stage="opportunity-handoff", outcome="admitted", target="g"), event("d1"), event("g"), event("g","e",outcome="completed"), event("r","e",stage="snapshot"), event("other","unrelated",target="g"), event("bad",stage="opportunity-handoff",outcome="failed",target="g"))
                    val page = EventPage(entries,"",false)
                    val selected = page.inspection(experiences=setOf("e"))
                    check(selected.events.map{it.getString("trace")}.toSet()==setOf("d2","d1","g","r"))
                    check(page.inspection(traces=setOf("g")).events.size==selected.events.size)
                    check(page.inspection().events.size==entries.size)
                    check(page.inspection(experiences=setOf("missing")).incomplete)
                    check(EventPage(entries,"more",false).inspection().incomplete)
                    check(EventPage(entries,"",true).inspection(experiences=setOf("e")).incomplete)
                    check(EventPage(listOf(event("late",time=10),event("late",time=2),event("old",time=1)),"",false).inspection(since=10).events.size==2)
                    selected.events.first().put("trace","mutated"); check(entries.first().getString("trace")=="d2")
                    val source = Source("""export class State {value:int=7;}
export class Increment {}
export function initialState():State {return {};}
// @ui-update
export function increment(state:State,action:Increment):State {return {value:state.value+1};}
""", """import * as app from "./experience";
import * as ui from "./platform.dealui-pack";
// @ui-root
export view Main(state:app.State):View {ui.Column(){ui.IntText(value:state.value) ui.Button(text:"Increment",onClick:action app.Increment {})}}
""")
                    val oldCapture = api.capturesContent()
                    try {
                        fun inspect(): ApplicationProtocol = ApplicationProtocol(ctx, Embedding(ctx,
                            CapabilityRegistry(ctx, CapabilityDiscovery(ctx){false}, emptyList()), model,
                            EmbeddingConfig("experience", emptyList(), "inspect-reader-${UUID.randomUUID()}")))
                        inspect().use { reader ->
                            reader.capture(true)
                            check(api.capturesContent()) { "Inspect capture did not reach active protocol" }
                        }
                        check(api.capturesContent()) { "Closing Inspect reset capture" }
                        val experience = api.load("Inspection fixture",source)
                        val session = api.run(experience)
                        val initial = api.next(session,"")
                        fun action(node:JSONObject):Int? {
                            val props=node.getJSONArray("props")
                            for(i in 0 until props.length())if(props.getJSONObject(i).optString("kind")=="action")return props.getJSONObject(i).getInt("actionSlot")
                            val children=node.getJSONArray("children")
                            for(i in 0 until children.length())action(children.getJSONObject(i))?.let{return it}
                            return null
                        }
                        api.dispatch(session,action(initial.snapshot.tree)!!,null)
                        val changed=api.next(session,initial.cursor)
                        check(changed.snapshot.tree.toString().contains("\"intValue\":8"))
                        EmbeddingLog.flush(ctx)
                        val scoped=api.retainedEvents().inspection(experiences=setOf(experience.id))
                        val snapshots=scoped.events.filter{it.optString("stage")=="snapshot" && it.optString("outcome")=="produced"}
                        check(snapshots.map{it.getInt("version")}.toSet().containsAll(listOf(initial.snapshot.version,changed.snapshot.version)))
                        for(update in listOf(initial,changed)) {
                            val retained=snapshots.first{it.getInt("version")==update.snapshot.version}
                            check(retained.getString("experience")==experience.id && retained.getString("session").isNotEmpty())
                            val artifact=api.artifact(retained.getString("artifact"));check(artifact.status=="captured")
                            fun canonical(value:Any):String = when(value) {
                                is JSONObject -> value.keys().asSequence().filterNot{it.startsWith("$")}.toList().sorted().joinToString(prefix="{",postfix="}") { JSONObject.quote(it)+":"+canonical(value.get(it)) }
                                is JSONArray -> (0 until value.length()).joinToString(prefix="[",postfix="]") { canonical(value.get(it)) }
                                else -> value.toString()
                            }
                            check(canonical(JSONObject(artifact.text))==canonical(update.snapshot.json())) { "Captured declared snapshot fields differ from public next result" }
                        }
                        check(scoped.events.none{it.optString("stage")=="snapshot" && it.optString("outcome")=="published"})
                        api.published(session,changed.snapshot.version)
                        Thread.sleep(100);EmbeddingLog.flush(ctx)
                        val after=api.retainedEvents().inspection(experiences=setOf(experience.id)).events
                        check(after.count{it.optString("stage")=="snapshot" && it.optString("outcome")=="produced"}==snapshots.size) { "Idle turns duplicated capture" }
                        check(after.any{it.optString("stage")=="snapshot" && it.optString("outcome")=="published" && it.optString("artifact").isEmpty()})
                        inspect().use { reader -> reader.capture(false); check(!api.capturesContent()) }
                        check(!api.capturesContent())
                        api.dispatch(session,action(changed.snapshot.tree)!!,null)
                        val uncaptured=api.next(session,changed.cursor)
                        EmbeddingLog.flush(ctx)
                        val omitted=api.retainedEvents().inspection(experiences=setOf(experience.id)).events.last{it.optString("stage")=="snapshot" && it.optString("outcome")=="produced"}
                        check(omitted.getInt("version")==uncaptured.snapshot.version && omitted.optString("artifact").isEmpty())
                        api.capture(true);Thread.sleep(100);EmbeddingLog.flush(ctx)
                        check(api.retainedEvents().inspection(experiences=setOf(experience.id)).events.last{it.optString("stage")=="snapshot" && it.optString("outcome")=="produced"}.optString("artifact").isEmpty()) { "Retroactive capture" }
                        api.close(session)
                        api.forget(experience)
                    } finally { api.capture(oldCapture) }
                    result.putString("inspection","PASS API-only correlation, transitive handoffs, unrelated isolation, incomplete/missing scopes, initial/edited snapshot artifacts, capture-off/nonretroactive, no idle duplicates, distinct publication")
                }
            }
            finish(0,result)
        } catch(error:Throwable) {result.putString("failure",android.util.Log.getStackTraceString(error));finish(1,result)}
    }
}

