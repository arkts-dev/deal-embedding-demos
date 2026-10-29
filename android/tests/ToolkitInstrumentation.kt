package dev.deal.connectors.tests

import android.app.Instrumentation
import android.os.Bundle
import dev.deal.embedding.*
import dev.deal.embedding.capabilities.*
import org.json.JSONObject
import java.util.Locale
import java.util.UUID

/** Exact packaged sources, compiler/runtime actions, rejection and settled reconstruction. */
class ToolkitInstrumentation : Instrumentation() {
    override fun onCreate(arguments: Bundle?) { super.onCreate(arguments); start() }
    override fun onStart() {
        val result = Bundle()
        var phase = "bootstrap"
        try {
            val ctx = targetContext
            val model = object : ModelClient {
                override fun complete(input:String,previous:String,diagnostics:String,cancellation:GenerationCancellation,log:EmbeddingLog):String = error("No inference")
            }
            val storage = "toolkit-${UUID.randomUUID()}"
            fun api() = ApplicationProtocol(ctx, Embedding(ctx, CapabilityRegistry(ctx, CapabilityDiscovery(ctx) { false }, emptyList()), model,
                RuntimeConfig("experience", emptyList(), storage, retainState = true)))
            fun source(name:String) = Source(ctx.assets.open("ui/$name.deal").bufferedReader().use { it.readText() }, ctx.assets.open("ui/$name.dealui").bufferedReader().use { it.readText() })
            val comparison = source("comparison")
            var id = ""
            api().use { p ->
                phase = "load"
                val e = p.load("Budget", comparison); id = e.id
                phase = "run"
                val s = p.run(e)
                phase = "next"
                fun tree() = p.next(s, "").snapshot.tree
                fun checkResult(status:String, difference:Int) {
                    val t=tree()
                    check(ToolkitNodes.all(t).single { it.optString("component").endsWith(".Comparison") }.let { ToolkitNodes.string(it,"status") } == status)
                    val value=ToolkitNodes.all(t).single { it.optString("component").endsWith(".Money") && ToolkitNodes.string(it,"label")=="Difference" }
                    check(ToolkitNodes.integer(value,"minorUnits")==difference)
                }
                fun edit(label:String,value:Int) { p.dispatch(s,ToolkitNodes.slot(tree(),label),value.toString()) }
                checkResult("Over budget",1750)
                edit("Approved budget (€)",20000); checkResult("Under budget",250)
                edit("Commitments (€)",20000); checkResult("On budget",0)
                edit("Approved budget (€)",0); checkResult("Over budget",20000)
                edit("Approved budget (€)",-1); checkResult("Over budget",20000)
                edit("Approved budget (€)",10000001); checkResult("Over budget",20000)
                edit("Approved budget (€)",10000000); checkResult("Under budget",9980000)
                edit("Approved budget (€)",18300); checkResult("Over budget",1700)
                val original=tree()
                val copied=displayText(original) { "[$it]" }
                check(ToolkitNodes.all(copied).any { ToolkitNodes.string(it,"status")=="[Over budget]" })
                check(ToolkitNodes.all(copied).any { ToolkitNodes.integer(it,"minorUnits")==1700 })
                check(ToolkitNodes.all(copied).filter { it.optString("component").endsWith(".IntField") }.all { !ToolkitNodes.string(it,"label").startsWith("[") })
                check(!original.toString().contains("[Over budget]"))
                val version=p.next(s,"").snapshot.version
                p.published(s,version)
                // Observation is asynchronous; acceptance does not promise disk visibility.
                val deadline=android.os.SystemClock.elapsedRealtime()+10000
                var observed=false
                while(!observed && android.os.SystemClock.elapsedRealtime()<deadline) {
                    observed=p.events(p.sessionTrace(s),"",200).events.any {
                        it.optString("stage")=="snapshot" && it.optString("outcome")=="published" &&
                            it.optInt("version")==version && it.optString("session")==s.id
                    }
                    if(!observed) Thread.sleep(50)
                }
                check(observed) {"Published snapshot observation missing for version $version"}
                p.close(s)
            }
            api().use { p ->
                val e=p.saved().single { it.id==id }; check(p.source(e)==comparison)
                val s=p.run(e); val t=p.next(s,"").snapshot.tree
                check(ToolkitNodes.all(t).any { ToolkitNodes.integer(it,"minorUnits")==1700 })
                p.dispatch(s,ToolkitNodes.slot(t,"Reset amounts"),null)
                check(p.next(s,"").snapshot.tree.toString().contains("\"numberValue\":1750"))
                p.close(s);p.forget(e)
                val values=source("values"); val v=p.load("Hire options",values); val session=p.run(v)
                val nodes=ToolkitNodes.all(p.next(session,"").snapshot.tree)
                check(nodes.filter { it.optString("component").endsWith(".Money") }.map { ToolkitNodes.integer(it,"minorUnits") }==listOf(1200,1300,5000))
                p.close(session);p.forget(v)
                fun reject(ui:String, code:String) {
                    val failure=runCatching { p.load("Invalid composition",Source(values.deal,ui)) }.exceptionOrNull() ?: error("Invalid composition accepted")
                    check(failure.toString().contains(code)) { failure.toString() }
                }
                reject(values.dealui.replace("ui.Money(label: amount.label, minorUnits: amount.minorUnits)","ui.Text(value: amount.label)"),"UI2048")
                reject(values.dealui.replace("ui.Money(label: amount.label, minorUnits: amount.minorUnits)","When(true) { ui.Text(value: amount.label) }"),"UI2048")
                reject(values.dealui.replace("ui.Money(label: \"Refundable deposit\", minorUnits: 5000)","ui.Money(minorUnits: 5000)"),"UI2028")
                reject(values.dealui.replace("ui.Metric(label: \"Travel\", value: \"29\", unit: \"min\")","ui.Metric(label: \"Travel\", value: \"29\", format: \"money\")"),"UI2029")
            }
            api().use { p ->
                val e=p.load("Rows",source("rows")); val s=p.run(e)
                fun tree()=p.next(s,"").snapshot.tree
                fun quantities()=ToolkitNodes.all(tree()).filter { it.optString("component").endsWith(".IntField") }.map { ToolkitNodes.string(it,"label") to ToolkitNodes.integer(it,"value") }
                val a=ToolkitNodes.slot(tree(),"a"); val b=ToolkitNodes.slot(tree(),"b")
                check(a!=b)
                p.dispatch(s,a,"7");check(quantities()==listOf("a" to 7,"b" to 2))
                check(runCatching { p.dispatch(s,b,"9") }.isFailure)
                p.dispatch(s,ToolkitNodes.slot(tree(),"Reverse rows"),null);check(quantities()==listOf("b" to 2,"a" to 7))
                p.dispatch(s,ToolkitNodes.slot(tree(),"b"),"8");check(quantities()==listOf("b" to 8,"a" to 7))
                val removed=ToolkitNodes.slot(tree(),"a")
                p.dispatch(s,ToolkitNodes.slot(tree(),"Remove a"),null);check(quantities()==listOf("b" to 8))
                check(runCatching { p.dispatch(s,removed,"11") }.isFailure)
                p.close(s);p.forget(e)
            }
            api().use { p ->
                val e=p.load("Primitives",source("primitives"));val s=p.run(e)
                check(p.next(s,"").snapshot.tree.toString().contains("-2147483648|0|15:16||"))
                p.close(s);p.forget(e)
            }
            check(formatMoney(1750,"EUR",Locale.US)=="€17.50")
            check(formatMoney(-1750,"EUR",Locale.US)=="-€17.50")
            check(formatMoney(Int.MIN_VALUE,"EUR",Locale.US)=="-€21,474,836.48")
            check(formatMoney(1750,"JPY",Locale.US).contains("1,750"))
            check(formatMoney(1750,"KWD",Locale.US).contains("1.750"))
            check(formatMoney(1750,"EUR",Locale.GERMANY).contains("17,50"))
            check(runCatching { formatMoney(1750,"not-currency") }.isFailure)
            result.putString("toolkit","PASS exact sources, explicit over/under/equal, payload/bounds/reset, retained reconstruction, dynamic typed children, required props, publication, signed/localized money; no inference or provider reads")
            finish(0,result)
        } catch(error:Throwable) { result.putString("failure",phase + ": " + android.util.Log.getStackTraceString(error));finish(1,result) }
    }
}

internal object ToolkitNodes {
    fun all(node:JSONObject):List<JSONObject> = listOf(node)+(0 until node.getJSONArray("children").length()).flatMap { all(node.getJSONArray("children").getJSONObject(it)) }
    private fun prop(node:JSONObject,name:String):JSONObject? = node.optJSONArray("props")?.let { a -> (0 until a.length()).map { a.getJSONObject(it) }.firstOrNull { it.optString("name")==name } }
    fun string(node:JSONObject,name:String) = prop(node,name)?.optString("stringValue") ?: ""
    fun integer(node:JSONObject,name:String) = prop(node,name)?.optInt("numberValue") ?: Int.MIN_VALUE
    fun slot(tree:JSONObject,label:String):Int {
        val node=all(tree).first { string(it,"label")==label || string(it,"text")==label }
        val props=node.getJSONArray("props")
        return (0 until props.length()).map { props.getJSONObject(it) }.first { it.optString("kind")=="action" }.getInt("actionSlot")
    }
}
