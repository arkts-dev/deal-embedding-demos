package dev.deal.connectors.tests

import android.app.Instrumentation
import android.os.Bundle
import dev.deal.embedding.*
import dev.deal.embedding.capabilities.*
import dev.deal.shell.AssistanceBuild
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** Opt-in repeated fresh generation. Expectations never enter the model request. */
class ToolkitLiveInstrumentation : Instrumentation() {
    private var enabled=false
    override fun onCreate(arguments:Bundle?) { super.onCreate(arguments);enabled=arguments?.getString("live")=="true";start() }
    override fun onStart() {
        val receipt=Bundle()
        if(!enabled) {receipt.putString("toolkitLive","SKIPPED: requires -e live true");finish(0,receipt);return}
        val results=JSONArray()
        val ctx=targetContext
        val oldCapture=EmbeddingLog(ctx).capturesContent
        try {
            EmbeddingLog.capture(ctx,true)
            val cases=listOf(24600 to 25950,31000 to 28750,22400 to 22400)
            repeat(2) { trial ->
                for((approved,committed) in cases) {
                    val row=JSONObject().put("trial",trial).put("approved",approved).put("committed",committed)
                    val started=System.currentTimeMillis()
                    var cleanup:ApplicationProtocol?=null
                    try {
                        val p=ApplicationProtocol(ctx,Embedding(ctx,CapabilityRegistry(ctx,CapabilityDiscovery(ctx){false},emptyList()),GatewayModelClient(AssistanceBuild.gateway()),
                            RuntimeConfig("experience",emptyList(),"toolkit-live-${UUID.randomUUID()}",retainState=true)))
                        cleanup=p
                        val facts=JSONObject().put("approvedCents",approved).put("committedCents",committed).put("currency","EUR")
                        val schema=WireType("record",fields=listOf(WireField("approvedCents",WireType("number")),WireField("committedCents",WireType("number")),WireField("currency",WireType("string",maximum=3))))
                        val request=p.request("Budget","Compare my approved budget with commitments in euros. Keep both amounts editable and show a clear over-budget, under-budget or on-budget result with the difference. No external reads are needed.",p.snapshot(schema,facts.toString()))
                        row.put("trace",p.requestTrace(request))
                        val e=p.generate(request);row.put("experience",e.id).put("attempts",e.attempts)
                        val s=p.run(e)
                        fun tree()=p.next(s,"").snapshot.tree
                        fun verify(a:Int,c:Int):Boolean {
                            val nodes=ToolkitNodes.all(tree())
                            val status=nodes.firstOrNull { it.optString("component").endsWith(".Comparison") }?.let { ToolkitNodes.string(it,"status").lowercase() } ?: ""
                            val statusOk=if(c>a) status.contains("over") else if(c<a) status.contains("under") else status.contains("on budget") || status.contains("equal") || status.contains("balanced")
                            return statusOk && nodes.any { it.optString("component").endsWith(".Money") && ToolkitNodes.integer(it,"minorUnits")==kotlin.math.abs(c-a) }
                        }
                        row.put("initialBehavior",verify(approved,committed))
                        val fields=ToolkitNodes.all(tree()).filter { it.optString("component").endsWith(".IntField") }
                        row.put("twoInputs",fields.size==2)
                        val a=fields.firstOrNull { ToolkitNodes.integer(it,"value")==approved }
                        val c=fields.firstOrNull { it!==a && ToolkitNodes.integer(it,"value")==committed }
                        if(a!=null && c!=null) {
                            val label=ToolkitNodes.string(a,"label")
                            val max=ToolkitNodes.integer(a,"maximum")
                            row.put("boundsCoverValues",ToolkitNodes.integer(a,"minimum")<=approved && max>=approved+100 && ToolkitNodes.integer(c,"minimum")<=committed && ToolkitNodes.integer(c,"maximum")>=committed+100)
                            row.put("factsPreserved",ToolkitNodes.integer(a,"value")==approved && ToolkitNodes.integer(c,"value")==committed)
                            p.dispatch(s,ToolkitNodes.slot(tree(),label),(approved+100).toString())
                            row.put("editedBehavior",verify(approved+100,committed))
                            p.dispatch(s,ToolkitNodes.slot(tree(),ToolkitNodes.string(c,"label")),(committed+200).toString())
                            row.put("secondInputBehavior",verify(approved+100,committed+200))
                        } else row.put("editedBehavior",false)
                        p.close(s)
                        val reopened=p.run(e)
                        val restored=ToolkitNodes.all(p.next(reopened,"").snapshot.tree)
                        row.put("settledReopen",restored.any { it.optString("component").endsWith(".Money") && ToolkitNodes.integer(it,"minorUnits")==kotlin.math.abs(committed-approved+100) } &&
                            restored.count { it.optString("component").endsWith(".IntField") && ToolkitNodes.integer(it,"value") in listOf(approved+100,committed+200) }==2)
                        p.close(reopened)
                        row.put("status",if(listOf("initialBehavior","twoInputs","boundsCoverValues","factsPreserved","editedBehavior","secondInputBehavior","settledReopen").all {row.optBoolean(it)}) "passed" else "failed")
                    } catch(error:Throwable) {row.put("status","failed").put("error",error.toString())}
                    finally {runCatching {cleanup?.close()}.onFailure {row.put("cleanup",it.toString()).put("status","failed")}}
                    row.put("durationMs",System.currentTimeMillis()-started);results.put(row)
                    // Preserve every attempt; no automatic new request or accepted-source patch.
                    java.io.File(ctx.filesDir,"toolkit-live-results.json").writeText(results.toString(2))
                }
            }
            receipt.putString("toolkitLive",results.toString())
            finish(if((0 until results.length()).all {results.getJSONObject(it).optString("status")=="passed"}) 0 else 1,receipt)
        } catch(error:Throwable) {receipt.putString("failure",android.util.Log.getStackTraceString(error));finish(1,receipt)}
        finally { EmbeddingLog.capture(ctx,oldCapture) }
    }
}
