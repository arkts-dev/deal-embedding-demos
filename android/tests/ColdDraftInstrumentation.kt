package dev.deal.connectors.tests

import android.app.Instrumentation
import android.os.Bundle
import dev.deal.embedding.*
import dev.deal.embedding.capabilities.*

/** Two adb instrumentation invocations cross a real process boundary and XML reload. */
class ColdDraftInstrumentation : Instrumentation() {
    private var phase = ""
    override fun onCreate(arguments: Bundle?) { super.onCreate(arguments); phase = arguments?.getString("phase") ?: ""; start() }
    override fun onStart() {
        val result = Bundle()
        try {
            val model = object : ModelClient {
                override fun complete(input:String,previous:String,diagnostics:String,cancellation:GenerationCancellation,log:EmbeddingLog):String = error("No inference")
            }
            val source = Source("""export class State {value:number;}
export class Change {value:number;}
export function initialState():State {return {value: 18000};}
// @ui-update
export function change(state:State,action:Change):State {return {value:action.value};}
""" + "\n", """import * as app from "./experience";
import * as ui from "./platform.dealui-pack";
// @ui-root
export view Main(state:app.State):View {ui.Column(){ui.IntField(value:state.value,label:"Budget",onChange:action app.Change {value:payload})}}
""" + "\n")
            ApplicationProtocol(targetContext, Embedding(targetContext,
                CapabilityRegistry(targetContext, CapabilityDiscovery(targetContext) { false }, emptyList()),model,
                EmbeddingConfig("experience",emptyList(),"cold-process-draft",retainState=true))).use { api ->
                if (phase == "write") api.saved().forEach(api::forget)
                val e = if (phase == "write") api.load("Process draft",source) else api.saved().single()
                check(api.source(e) == source) { "Checked source bytes changed across process restart" }
                val session=api.run(e)
                val tree=api.next(session,"").snapshot.tree
                if (phase == "write") {
                    val props=tree.getJSONArray("children").getJSONObject(0).getJSONArray("props")
                    val slot=(0 until props.length()).map{props.getJSONObject(it)}.single{it.optString("kind")=="action"}.getInt("actionSlot")
                    api.dispatch(session,slot,"18100")
                } else {
                    check(phase == "read")
                    check(tree.toString().contains("\"numberValue\":18100")) { "Draft reset across process restart: $tree" }
                }
                api.close(session)
                if (phase == "read") api.forget(e)
            }
            result.putString("coldDraft","PASS $phase exact source bytes and retained value across process boundary")
            finish(0,result)
        } catch(error:Throwable) {result.putString("failure",android.util.Log.getStackTraceString(error));finish(1,result)}
    }
}
