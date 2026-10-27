package dev.deal.connectors.tests

import android.app.Instrumentation
import android.os.Bundle
import android.os.SystemClock
import android.content.Intent
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.deal.apps.organizer.OrganizerActivity
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import dev.deal.embedding.*
import dev.deal.embedding.capabilities.*
import dev.deal.embedding.discovery.*
import dev.deal.shell.*
import org.json.*
import java.io.File

/** Opt-in fresh generation benchmark: discovery start -> first committed runnable frame.
 * Semantic assessment happens AFTER the immutable stop timestamp, with no model feedback.
 */
class FrameworkE2eInstrumentation: Instrumentation() {
    private var enabled=false
    private var selftest=false
    private var reassess=""
    private var additionalOnly=false
    private var savedStorage=""
    private var savedExperience=""
    override fun onCreate(arguments:Bundle?){super.onCreate(arguments);enabled=arguments?.getString("live")=="true";selftest=arguments?.getString("selftest")=="true";reassess=arguments?.getString("reassess").orEmpty();savedStorage=arguments?.getString("storage").orEmpty();savedExperience=arguments?.getString("experience").orEmpty();additionalOnly=arguments?.getString("additional")=="true";start()}
    private fun nodes(t:JSONObject)=ToolkitNodes.all(t)
    private fun text(n:JSONObject)=n.getJSONArray("props").let { p -> (0 until p.length()).map { p.getJSONObject(it).optString("stringValue") }.filter { it.isNotEmpty() }.joinToString(" ") }
    private fun showsTime(t:JSONObject,minutes:Int):Boolean = nodes(t).any { n ->
        n.optString("component").endsWith(".Time") && ToolkitNodes.integer(n,"value")==minutes
    } || nodes(t).any { text(it).contains("%02d:%02d".format(java.util.Locale.US,minutes/60,minutes%60)) }
    private fun slot(n:JSONObject):Int?=n.getJSONArray("props").let { p -> (0 until p.length()).map { p.getJSONObject(it) }.firstOrNull { it.optString("kind")=="action" }?.getInt("actionSlot") }
    private fun amount(t:JSONObject,cents:Int)=nodes(t).any { it.optString("component").endsWith(".Money") && ToolkitNodes.integer(it,"minorUnits")==cents } || nodes(t).any { text(it).contains("%.2f".format(java.util.Locale.US,cents/100.0)) }
    private class AssessmentIncomplete(message:String):Exception(message)
    private class RenderedSession(private val surface: ExperienceSurface) {
        var dispatchedVersion:Int?=null
        fun snapshot() = checkNotNull(surface.snapshot).json()
        fun dispatch(slot:Int,payload:String?) {
            check(dispatchedVersion==null){"Previous assessment action has not settled"}
            val value=checkNotNull(surface.snapshot)
            dispatchedVersion=value.version
            surface.event(value,slot,payload)
        }
        fun pause() { Thread.sleep(10) }
    }
    // A quiet OLD frame cannot acknowledge a queued action. First require a committed
    // newer snapshot, then wait through its pending UI and subsequent completion.
    private fun settledSnapshot(after:Int?,snapshot:()->JSONObject,pause:()->Unit,clock:()->Long):JSONObject {
        var same=0;var version=-1;val start=clock()
        while(true){
            val next=snapshot()
            if(next.optString("fault").isNotEmpty())error("Runtime fault: ${next.optString("fault")}")
            val pending=nodes(next.getJSONObject("tree")).any { it.optString("component").endsWith(".Progress") || it.optString("component").endsWith(".Spinner") }
            val acknowledged=after==null || next.getInt("version")>after
            if(acknowledged && next.getInt("version")==version && !pending)same++ else same=0
            version=next.getInt("version")
            if(same>=20)return next.getJSONObject("tree")
            // Assessment watchdog only: never report a product failure or feed generation.
            if(clock()-start>60000)throw AssessmentIncomplete("Action did not reach a newer, non-pending rendered snapshot within assessment watchdog")
            pause()
        }
    }
    private fun settle(s:RenderedSession):JSONObject {
        val tree=settledSnapshot(s.dispatchedVersion,s::snapshot,s::pause,SystemClock::elapsedRealtime)
        s.dispatchedVersion=null
        return tree
    }
    private fun verifyAssessmentWaiting(){
        fun frame(version:Int,pending:Boolean)=JSONObject().put("version",version).put("fault","").put("tree",JSONObject().put("component",if(pending)"ui.Progress" else "ui.Column").put("props",JSONArray()).put("children",JSONArray()))
        var time=0L
        settledSnapshot(1,{when{time<350->frame(1,false);time<1750->frame(2,true);else->frame(3,false)}},{time+=10},{time})
        check(time>=1950){"Evaluator accepted old or pending frame"}
        time=0
        // A fast action can skip its loading frame; the newer completed frame is enough.
        settledSnapshot(1,{if(time<350)frame(1,false) else frame(3,false)},{time+=10},{time})
        check(time>=550)
        time=0
        check(runCatching{settledSnapshot(1,{frame(1,false)},{time+=1000},{time})}.exceptionOrNull() is AssessmentIncomplete)
    }
    private fun evaluate(name:String,s:RenderedSession):String = try {
        var t=settle(s)
        var initialReadSlot:Int?=null
        // These are runtime actions, never capability reads during discovery/source construction.
        if(name!="budget") {
            val actions=nodes(t).filter { slot(it)!=null && it.optString("component").let { c -> c.endsWith(".Button")||c.endsWith(".PrimaryAction")||c.endsWith(".SecondaryAction") } }
            fun isReadAction(n:JSONObject)=text(n).lowercase().let { v -> listOf("read","load","fetch","check","estimate","find","compare","refresh","compute","calculate").any(v::contains) }
            // A visible local recalculation may precede the explicit provider command.
            // Prefer its primary action; do not mistake a secondary local edit for loading.
            val load=actions.firstOrNull { it.optString("component").endsWith(".PrimaryAction") && isReadAction(it) }
                ?: actions.firstOrNull(::isReadAction)
            if(load==null)error("No explicit action to read provider data. Observed actionable labels: ${actions.joinToString { text(it) }}")
            initialReadSlot=slot(load)!!
            s.dispatch(initialReadSlot,null);t=settle(s)
        }
        when(name) {
            "budget" -> {
                check(amount(t,1750)){"Expected EUR17.50 difference for180/197.50"}
                val fields=nodes(t).filter { it.optString("component").endsWith(".IntField") }
                check(fields.size>=2){"Both budget amounts must be editable"}
                val a=fields.firstOrNull { ToolkitNodes.integer(it,"value")==18000 } ?: error("Approved18000 cents input missing")
                s.dispatch(slot(a)!!,"18100");t=settle(s);check(amount(t,1650)){"181/197.50 must yield EUR16.50"}
                val b=nodes(t).first { it.optString("component").endsWith(".IntField") && ToolkitNodes.integer(it,"value")==19750 }
                s.dispatch(slot(b)!!,"19850");t=settle(s);check(amount(t,1750)){"181/198.50 must yield EUR17.50"}
                fun approved(cents:Int){val field=nodes(t).first { it.optString("component").endsWith(".IntField") && text(it).lowercase().let { v -> v.contains("approved")||v.contains("budget") } };s.dispatch(slot(field)!!,cents.toString());t=settle(s)}
                approved(19850);check(amount(t,0)){"Equal amounts must show zero difference"}
                approved(20000);check((amount(t,150)||amount(t,-150)) && nodes(t).joinToString(" "){text(it)}.contains("under",true)){"200/198.50 must show signed or magnitude EUR1.50 with under-budget status"}
                approved(18100)
            }
            "departure" -> {
                val rendered=nodes(t).joinToString(" "){text(it)}
                check(showsTime(t,916) && showsTime(t,906)){"Actual Vehicle29min:16:00 minus15 unload minus29 travel gives15:16; inspection10 earlier15:06. Observed:$rendered"}
                val fields=nodes(t).filter { it.optString("component").let { c -> c.endsWith(".IntField") || c.endsWith(".TimeField") } }
                val start=fields.firstOrNull { ToolkitNodes.integer(it,"value")==960 }
                if(start!=null){s.dispatch(slot(start)!!,"985");t=settle(s)}
                else {
                    val hour=fields.firstOrNull { text(it).contains("hour",true) && ToolkitNodes.integer(it,"value")==16 }
                    val minute=fields.firstOrNull { text(it).contains("minute",true) && ToolkitNodes.integer(it,"value")==0 }
                    if(hour==null || minute==null)throw AssessmentIncomplete("Unsupported event-start control representation; no minute-of-day or hour/minute pair")
                    s.dispatch(slot(minute)!!,"25");t=settle(s)
                }
                // Some correct workflows clear results on edits and require an explicit refresh.
                if(!showsTime(t,941)) {
                    val refresh=nodes(t).firstOrNull { slot(it)==initialReadSlot }
                    if(refresh!=null){s.dispatch(slot(refresh)!!,null);t=settle(s)}
                }
                val changed=nodes(t).joinToString(" "){text(it)}
                check(showsTime(t,941)&&showsTime(t,931)){"Editing start to16:25 must recompute departure15:41/inspection15:31; observed:$changed"}
            }
            "food" -> {
                val loadedText=nodes(t).joinToString(" "){text(it)}
                check(loadedText.contains("6 of 6") || loadedText.contains("6 / 6") || nodes(t).any { ToolkitNodes.string(it,"label").contains("pizza",true) && ToolkitNodes.string(it,"value")=="6" }){"Initial allocation must contain six pizzas and at least two vegan, not require manual fixing. Observed:$loadedText"}
                check(!loadedText.contains("Needs 2") && !loadedText.contains("Needs 6")){"Initial allocation must satisfy six/at-least-two-vegan constraints: $loadedText"}
                // Edit actual keyed rows through their issued action slots; no injected state/source.
                fun editRow(word:String,value:Int){
                    val all=nodes(t)
                    val group=all.filter { n -> nodes(n).any { text(it).contains(word,true) } && nodes(n).any { it.optString("component").endsWith(".IntField") } }.minByOrNull { nodes(it).size } ?: error("Editable $word quantity row missing")
                    val field=nodes(group).first { it.optString("component").endsWith(".IntField") }
                    s.dispatch(slot(field)!!,value.toString());t=settle(s)
                }
                editRow("Margherita",3);editRow("Garden",3);editRow("Pepperoni",0)
                check(amount(t,7100)){"3Margherita+3GardenVegan+0Pepperoni at actual30 cm size plus timely delivery500 must total7100 cents. Observed:${nodes(t).joinToString(" "){text(it)}}"}
                editRow("Margherita",4)
                check(amount(t,8100)){"Row edit4Margherita+3Garden must total8100 cents"}
            }
            "equipment" -> {
                // A two-stage catalogue -> quote workflow needs one explicit follow-up.
                // Do not dispatch the same quote action again under package/stand/cable labels.
                if(!(amount(t,1200)&&amount(t,1300)&&amount(t,5000))) {
                    val compare=nodes(t).firstOrNull { n -> slot(n)!=null && slot(n)!=initialReadSlot && n.optString("component").let { it.endsWith(".Button")||it.endsWith(".PrimaryAction")||it.endsWith(".SecondaryAction") } && text(n).contains("quote",true) }
                    if(compare!=null){s.dispatch(slot(compare)!!,null);t=settle(s)}
                }
                check(amount(t,1200)&&amount(t,1300)&&amount(t,5000)){"Comparison must show actual package1200,separate stand+cable1300 and separate refundable deposit5000 cents, using actual catalogue/quotes, not unavailable or assumed adapter. Observed:${nodes(t).joinToString(" "){text(it)}}"}
                val budget=nodes(t).firstOrNull { it.optString("component").endsWith(".IntField") && ToolkitNodes.integer(it,"value")==2000 } ?: error("Editable EUR20 rental budget missing")
                s.dispatch(slot(budget)!!,"1100");t=settle(s)
                check(nodes(t).joinToString(" "){text(it)}.contains("over",true)){"Rental hire1200/1300 must be over edited1100 budget; refundable deposit is separate"}
                val edited=nodes(t).first { it.optString("component").endsWith(".IntField") && ToolkitNodes.integer(it,"value")==1100 };s.dispatch(slot(edited)!!,"2000");t=settle(s)
            }
        }
        ""
    } catch(e:ProtocolFailure){throw e} catch(e:AssessmentIncomplete){"INCONCLUSIVE: ${e.message}"} catch(e:Exception){e.message ?: e.javaClass.simpleName}
    // Extra untimed assertions are reported separately to preserve historical comparability.
    // Exercise issued controls only; never alter provider records or generated state/source.
    private fun evaluateAdditional(name:String,s:RenderedSession):String = try {
        if(name=="food") {
            var t=settle(s)
            fun editRow(word:String,value:Int) {
                val group=nodes(t).filter { n -> nodes(n).any { text(it).contains(word,true) } && nodes(n).any { it.optString("component").endsWith(".IntField") } }.minByOrNull { nodes(it).size } ?: throw AssessmentIncomplete("Additional food check: editable $word row missing")
                val field=nodes(group).first { it.optString("component").endsWith(".IntField") }
                s.dispatch(slot(field)!!,value.toString());t=settle(s)
            }
            editRow("Margherita",3);editRow("Garden",3);editRow("Pepperoni",0)
            val late=nodes(t).firstOrNull { n -> n.optString("component").endsWith(".Option") && slot(n)!=null && text(n).contains("18:45") } ?: throw AssessmentIncomplete("Additional food check: no supported late-window Option control")
            val value=ToolkitNodes.string(late,"value")
            if(value.isEmpty())throw AssessmentIncomplete("Additional food check: late-window payload unavailable")
            s.dispatch(slot(late)!!,value);t=settle(s)
            val comparison=nodes(t).filter { it.optString("component").endsWith(".Comparison") }.joinToString(" "){text(it)}
            if(comparison.isEmpty())throw AssessmentIncomplete("Additional food check: unsupported feasibility presentation")
            check(listOf("not feasible","infeasible","invalid","not ready","cannot","late","misses").any { comparison.contains(it,true) }) { "Selecting an actual late delivery window must invalidate feasibility with otherwise valid quantities/budget. Observed status: $comparison" }
        }
        ""
    } catch(e:ProtocolFailure){throw e} catch(e:AssessmentIncomplete){"INCONCLUSIVE: ${e.message}"} catch(e:Exception){e.message ?: e.javaClass.simpleName}
    private fun assessSaved():String {
        check(reassess in setOf("budget","departure","food","equipment") && savedStorage.startsWith("framework-e2e-") && savedExperience.isNotEmpty())
        val ctx=targetContext
        val activity=startActivitySync(Intent(ctx,OrganizerActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as OrganizerActivity
        val registry=CapabilityRegistry(ctx,CapabilityDiscovery(ctx){uid->Connections.trusted(ctx,uid)},emptyList())
        // No inference in saved assessment, even if an accidental generation call is added.
        val noModel=object:ModelClient {override fun complete(input:String,previous:String,diagnostics:String,cancellation:GenerationCancellation,log:EmbeddingLog):String=error("Saved assessment must not invoke inference")}
        try {
            ApplicationProtocol(ctx,Embedding(ctx,registry,noModel,EmbeddingConfig("experience",emptyList(),savedStorage,retainState=true))).use { api ->
                api.capture(true);Connections.apply(ctx,api)
                val module=when(reassess){"departure"->"host/trip";"food"->"host/pizza";"equipment"->"host/rental";else->""}
                if(module.isNotEmpty())api.grant(module)
                val experience=api.saved().first { it.id==savedExperience }
                ExperienceSurface(api,experience).use { surface ->
                    val failure=AtomicReference<Throwable?>()
                    surface.activate {failure.set(it)}
                    runOnMainSync {activity.setContent {AppTheme(Accent.Organizer){ExperienceSurface(surface)}}}
                    while(surface.snapshot==null){failure.get()?.let{throw it};Thread.sleep(10)}
                    val feedback=if(additionalOnly)evaluateAdditional(reassess,RenderedSession(surface)) else evaluate(reassess,RenderedSession(surface))
                    File(ctx.filesDir,"framework-e2e-$reassess-reassessed-tree.json").writeText(checkNotNull(surface.snapshot).json().toString(2))
                    runOnMainSync {activity.setContent {Text("Assessment complete")}}
                    return feedback
                }
            }
        }finally{runOnMainSync{activity.finish()}}
    }
    override fun onStart(){val result=Bundle();val receipts=JSONArray();val ctx=targetContext
        try {
            verifyAssessmentWaiting()
            if(selftest){result.putString("assessmentWaiting","PASS delayed dispatch > old stability window, delayed provider, skipped loading frame, no-ack watchdog");finish(0,result);return}
            if(reassess.isNotEmpty()){
                val feedback=assessSaved()
                result.putString("reassessment",JSONObject().put("scenario",reassess).put("experience",savedExperience).put("semantic",if(feedback.isEmpty())"passed_supplied_assertions" else if(feedback.startsWith("INCONCLUSIVE:"))"inconclusive" else "failed_supplied_assertions").put("semanticFailure",feedback).toString())
                finish(0,result);return
            }
            check(enabled){"Requires -e live true"}
            val cases=listOf(
                "equipment" to "Vocal kit hire at The Foundry,stage centre,23October2026 16:15–22:30 Europe/Berlin (14:15Z–20:30Z). Need one adjustable boom stand including clip adapter and balanced XLR female-to-male cable minimum10m. Compare package and separate hire via Rental catalogue/quotes. Editable EUR20 budget; refundable deposit separate. Read-only,no booking.",
                "food" to "Crew dinner,23October2026,The Foundry stage entrance. Plan six30 cm pizzas,at least two vegan,EUR80 including delivery before18:30. Load Pizza menu/delivery,editable quantities and budget,recompute feasibility. No ordering.",
                "departure" to "Equipment collection,The Foundry,23October2026 16:00–17:00 Europe/Berlin. Distance12km; use actual Vehicle travel estimate. Need15min unloading before start and10min inspection before departure. Editable start,distance,buffers,recompute departure and inspection. No booking.",
                "budget" to "Budget reconciliation,23October2026. Compare approved18000 EUR cents with committed19750 EUR cents. Editable amounts,show over/under/equal and difference. Local arithmetic,no external reads."
            )
            for((name,fact) in cases.sortedBy { listOf("budget","departure","food","equipment").indexOf(it.first) }){
                val row=JSONObject().put("scenario",name);receipts.put(row);val started=SystemClock.elapsedRealtime()
                val storage="framework-e2e-$name-${java.util.UUID.randomUUID()}"
                val activity=startActivitySync(Intent(ctx,OrganizerActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as OrganizerActivity
                val registry=CapabilityRegistry(ctx,CapabilityDiscovery(ctx){uid->Connections.trusted(ctx,uid)},emptyList())
                // NO candidateTest: semantic failures cannot trigger generation/repair in this benchmark.
                val config=EmbeddingConfig("experience",emptyList(),storage,retainState=true)
                ApplicationProtocol(ctx,Embedding(ctx,registry,AssistanceBuild.gateway(),config)).use { api ->
                    api.capture(true);Connections.apply(ctx,api)
                    val module=when(name){"food"->"host/pizza";"departure"->"host/trip";"equipment"->"host/rental";else->""}
                    if(module.isNotEmpty())api.grant(module)
                    val facts=DiscoveryInput(listOf(DiscoveryFact("task",fact)))
                    val discoveryStarted=SystemClock.elapsedRealtime()
                    val request=Discovery(ctx,api,AssistanceBuild.discoveryGateway()).use { finder ->
                        val tip=finder.discover(facts).get()
                        row.put("discoveryOnlyMs",SystemClock.elapsedRealtime()-discoveryStarted)
                        row.put("discoveryMs",SystemClock.elapsedRealtime()-started)
                        checkNotNull(tip){"Discovery returned no suggestion; no automatic new request"}
                        finder.request(tip,facts)
                    }
                    row.put("trace",api.requestTrace(request))
                    val generationStarted=SystemClock.elapsedRealtime()
                    val e=api.generate(request)
                    row.put("experience",e.id).put("attempts",e.attempts).put("checkedMs",SystemClock.elapsedRealtime()-started)
                    val surface=ExperienceSurface(api,e)
                    try {
                        val frame=CountDownLatch(1);val renderedAt=AtomicLong(0);val renderError=AtomicReference<Throwable?>()
                        surface.activate { error -> renderError.set(error);frame.countDown() }
                        runOnMainSync {
                            activity.setContent {
                                AppTheme(Accent.Organizer) {
                                    Surface(Modifier.fillMaxSize()) {
                                        Column(Modifier.statusBarsPadding().navigationBarsPadding().padding(16.dp).verticalScroll(rememberScrollState())) {
                                            ExperienceSurface(surface,onSnapshot={snapshot ->
                                                if(renderedAt.get()==0L) {
                                                    if(snapshot.fault.isNotEmpty()){renderError.set(IllegalStateException(snapshot.fault));frame.countDown()}
                                                    else activity.window.decorView.viewTreeObserver.registerFrameCommitCallback {
                                                        renderedAt.compareAndSet(0,SystemClock.elapsedRealtime());frame.countDown()
                                                    }
                                                    activity.window.decorView.invalidate()
                                                }
                                            })
                                        }
                                    }
                                }
                            }
                        }
                        frame.await();renderError.get()?.let{throw it}
                        val stop=renderedAt.get();check(stop>0)
                        row.put("status","rendered").put("runMs",stop-started).put("generationToRenderMs",stop-generationStarted)
                        row.put("stopDefinition","first generated UI frame committed to swap chain; not physical pixel scanout")
                        // Freeze and persist timed outcome BEFORE any semantic action, screenshot or cleanup.
                        File(ctx.filesDir,"framework-e2e-results.json").writeText(receipts.toString(2))
                        sendStatus(2,Bundle().apply{putString("timed",row.toString())})
                        uiAutomation.takeScreenshot()?.let { bitmap -> File(ctx.filesDir,"framework-e2e-$name.png").outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) };bitmap.recycle() }
                        val feedback=evaluate(name,RenderedSession(surface))
                        row.put("semantic",if(feedback.isEmpty())"passed_supplied_assertions" else if(feedback.startsWith("INCONCLUSIVE:"))"inconclusive" else "failed_supplied_assertions").put("semanticFailure",feedback)
                        File(ctx.filesDir,"framework-e2e-$name-final-tree.json").writeText(checkNotNull(surface.snapshot).json().toString(2))
                        if(feedback.isEmpty() && name=="food") {
                            val additional=evaluateAdditional(name,RenderedSession(surface))
                            row.put("additionalSemantic",if(additional.isEmpty())"passed_additional_assertions" else if(additional.startsWith("INCONCLUSIVE:"))"inconclusive" else "failed_additional_assertions").put("additionalSemanticFailure",additional)
                            File(ctx.filesDir,"framework-e2e-$name-additional-tree.json").writeText(checkNotNull(surface.snapshot).json().toString(2))
                        }
                    } finally {
                        runOnMainSync { activity.setContent { Text("Assessment complete") } }
                        surface.close()
                    }
                }
                runOnMainSync { activity.finish() }
                File(ctx.filesDir,"framework-e2e-results.json").writeText(receipts.toString(2));sendStatus(2,Bundle().apply{putString("scenario",row.toString())})
            }
            result.putString("e2e",receipts.toString());finish(0,result)
        }catch(e:Throwable){result.putString("failure",android.util.Log.getStackTraceString(e));result.putString("receipts",receipts.toString());File(ctx.filesDir,"framework-e2e-results.json").writeText(receipts.toString(2));finish(1,result)}
    }
}
