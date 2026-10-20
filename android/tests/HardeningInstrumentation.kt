package dev.deal.connectors.tests

import android.app.Instrumentation
import android.os.Bundle
import dev.deal.embedding.*
import dev.deal.embedding.capabilities.*
import org.json.*
import java.util.concurrent.*

/** Adversarial authored-source regressions. No network inference. */
class HardeningInstrumentation:Instrumentation(){
    override fun onCreate(arguments:Bundle?){super.onCreate(arguments);start()}
    override fun onStart(){val result=Bundle();var protocol:ApplicationProtocol?=null
        try{
            val context=targetContext
            fun until(check:()->Boolean){val end=System.currentTimeMillis()+10000;while(!check()){check(System.currentTimeMillis()<end){"Hardening condition timed out"};Thread.sleep(10)}}
            fun field(value:Any,name:String):Any?=value.javaClass.getDeclaredField(name).apply{isAccessible=true}.get(value)
            fun count(value:Any,name:String)=(field(value,name) as Map<*,*>).size
            val cursors=UpdateCursor();val ancient=cursors.issue(0)
            repeat(10000){check(cursors.revision(cursors.issue(it.toLong()),10000)==it.toLong())}
            check(cursors.revision(ancient,10000)==0L)
            check(UpdateCursor::class.java.declaredFields.none{Map::class.java.isAssignableFrom(it.type)})
            for(invalid in listOf("forged",cursors.issue(10001),ancient+"x",UpdateCursor().issue(0))){check(runCatching{cursors.revision(invalid,10000)}.exceptionOrNull().let{it is ProtocolFailure&&it.code=="INVALID_CURSOR"})}
            var calls=0
            val probe=NativeCapabilities(CapabilityContract("host/probe","probe",listOf(CapabilityFunction("ping","ping",emptyList(),CapabilityType.IntType))),mapOf("ping" to {_:JSONArray->++calls}))
            val registry=CapabilityRegistry(context,CapabilityDiscovery(context){false},listOf(probe))
            val model=object:ModelClient{override fun complete(input:String,previous:String,diagnostics:String,cancellation:GenerationCancellation,log:EmbeddingLog):String=throw ProtocolFailure("AUDIT_LIMIT","Fixture provider refusal")}
            val config=EmbeddingConfig("experience",emptyList(),"hardening-${java.util.UUID.randomUUID()}")
            val mechanisms=Embedding(context,registry,model,config)
            protocol=ApplicationProtocol(context,mechanisms)
            val api=protocol
            api.grant("host/probe")
            val admission=registry.openSession(EmbeddingLog(context))
            repeat(300){i->admission.receive(JSONObject().put("id",i+1).put("module","host/probe").put("function","ping").put("args",JSONArray()),android.os.SystemClock.elapsedRealtime());check(admission.drain(android.os.SystemClock.elapsedRealtime()).single().getBoolean("ok"))}
            check(calls==300)
            repeat(8){i->admission.receive(JSONObject().put("id",301+i).put("module","host/probe").put("function","ping").put("args",JSONArray()),0)}
            check(runCatching{admission.receive(JSONObject().put("id",309).put("module","host/probe").put("function","ping").put("args",JSONArray()),0)}.isFailure)
            check(admission.drain(0).size==8)
            admission.receive(JSONObject().put("id",309).put("module","host/probe").put("function","ping").put("args",JSONArray()),0);check(admission.drain(0).single().getBoolean("ok"));admission.close()
            val source=Source("""export class State {value:int=0;}
export class Increment {}
export function initialState():State {return {};}
// @ui-update
export function increment(state:State,action:Increment):State {return {value:state.value+1};}
""","""import * as app from "./experience";
import * as ui from "./platform.dealui-pack";
// @ui-root
export view Main(state:app.State):View {ui.Column(){ui.IntText(value:state.value) ui.Button(text:"Increment",onClick:action app.Increment {})}}
""")
            val experience=api.load("Hardening",source);val session=api.run(experience)
            val first=api.next(session,"")
            val slot=first.snapshot.tree.getJSONArray("children").getJSONObject(1).getJSONArray("props").let{props->(0 until props.length()).map{props.getJSONObject(it)}.first{it.optString("name")=="onClick"}.getInt("actionSlot")}
            val controller=field(api,"controller")!!
            repeat(50){
                val callbacks=java.util.concurrent.atomic.AtomicInteger()
                val errors=java.util.concurrent.atomic.AtomicInteger()
                val attached=api.observe(session,{callbacks.incrementAndGet()},{errors.incrementAndGet()})
                until{callbacks.get()>0};Thread.sleep(15);attached.close()
                val before=callbacks.get();api.dispatch(session,slot,null);Thread.sleep(20)
                check(callbacks.get()==before&&errors.get()==0){"Detached observer delivered a callback"}
                until{count(controller,"nextWaits")==0 && count(controller,"futures")==0}
                val deliveries=field(mechanisms,"deliveries") as Map<*,*>
                check((field(deliveries[session.id]!!,"waiters") as List<*>).isEmpty())
            }
            val latest=api.next(session,first.cursor);check(latest.snapshot.tree.toString().contains("\"intValue\":50"))
            check(runCatching{api.next(session,UpdateCursor().issue(0))}.exceptionOrNull().let{it is ProtocolFailure&&it.code=="INVALID_CURSOR"})
            api.close(session)
            // A received update can precede recomposition. A callback from the last
            // published (still visible) frame must not lose a rapid second edit.
            val typing=ExperienceSurface(api,experience)
            typing.activate{throw AssertionError("Surface failed",it)}
            until{typing.snapshot!=null}
            val rendered=checkNotNull(typing.snapshot)
            typing.accepted(rendered)
            api.dispatch(api.run(experience),slot,null)
            typing.activate{throw AssertionError("Surface failed",it)}
            until{typing.snapshot!==rendered}
            val beforeRapid=api.next(api.run(experience),"").snapshot.version
            typing.event(rendered,slot,null)
            until{api.next(api.run(experience),"").snapshot.version>beforeRapid}
            typing.close()
            var staleAccepted=false
            typing.event(rendered,slot,null){staleAccepted=true}
            check(!staleAccepted){"Closed surface accepted stale callback"}
            val labelTree=JSONObject().put("component","Text").put("props",JSONArray().put(JSONObject().put("name","value").put("stringValue","original"))).put("children",JSONArray())
            check(displayText(labelTree){"shown"}.toString().contains("shown"));check(labelTree.toString().contains("original"))
            val inputTree=JSONObject(labelTree.toString()).put("component","TextInput")
            check(displayText(inputTree){"changed"}.toString().contains("original"))
            val healthy=api.run(experience)
            val healthySnapshot=api.next(healthy,"").snapshot
            // Rejecting changed app context releases preparation and never touches the healthy saved source.
            val rejected=api.load("Rejected context",source)
            check(runCatching{api.withPreparation(rejected){error("Selected event changed")}}.isFailure)
            check(count(mechanisms,"candidates")==0)
            check(runCatching{api.source(rejected)}.isFailure)
            // Real checked candidate, forced catalogue revision mismatch at mount: cleanup survives failed activation.
            val failed=api.load("Failed mount",source)
            val frozen=field(mechanisms,"config") as EmbeddingConfig
            val old=frozen.contracts;frozen.contracts=emptyList()
            val candidateSurface=ExperienceSurface(api,failed)
            check(runCatching{api.withPreparation(failed){candidateSurface.activate{error("Unexpected callback")}}}.isFailure)
            frozen.contracts=old;candidateSurface.close()
            check(count(mechanisms,"candidates")==0)
            check(api.saved().any{it.id==experience.id})
            check(api.run(experience)==healthy)
            check(api.next(healthy,"").snapshot.json().toString()==healthySnapshot.json().toString())
            // Deliberate physical deletion failure: logical release retains native candidate and durable journal.
            val cleanup=api.load("Cleanup failure",source)
            val original=StagingReclamation.delete
            try{
                StagingReclamation.delete={false}
                api.release(cleanup)
                check(runCatching{api.flushReclamation()}.exceptionOrNull().let{it is ProtocolFailure&&it.code=="CLEANUP_FAILED"})
                check(StagingReclamation.pending(context)>0&&count(mechanisms,"candidates")==1)
                check(runCatching{api.source(cleanup)}.isFailure)
            }finally{StagingReclamation.delete=original}
            api.flushReclamation();check(StagingReclamation.pending(context)==0&&count(mechanisms,"candidates")==0)
            val closeFailure=api.load("Close cleanup failure",source);val closeSession=api.run(closeFailure)
            try{
                StagingReclamation.delete={false}
                check(runCatching{api.close(closeSession)}.exceptionOrNull().let{it is ProtocolFailure&&it.code=="CLEANUP_FAILED"})
                check(api.saved().any{it.id==closeFailure.id}&&StagingReclamation.pending(context)>0)
            }finally{StagingReclamation.delete=original}
            api.close(closeSession);check(StagingReclamation.pending(context)==0);api.forget(closeFailure)
            val request=api.request("Provider failure","Fixture request",api.snapshot(WireType("record"),"{}"))
            check(runCatching{api.generate(request)}.exceptionOrNull().let{it is ProtocolFailure&&it.code=="AUDIT_LIMIT"})
            val repeatedModel=object:ModelClient{override fun complete(input:String,previous:String,diagnostics:String,cancellation:GenerationCancellation,log:EmbeddingLog):String="invalid fixture"}
            ApplicationProtocol(context,Embedding(context,CapabilityRegistry(context,CapabilityDiscovery(context){false},emptyList()),repeatedModel,EmbeddingConfig("experience",emptyList(),"repeated-${java.util.UUID.randomUUID()}"))).use{other->
                val r=other.request("Repeated","Fixture request",other.snapshot(WireType("record"),"{}"))
                check(runCatching{other.generate(r)}.exceptionOrNull().let{it is ProtocolFailure&&it.code=="GENERATION_REPEATED_RESPONSE"})
            }
            val distinct=java.util.concurrent.atomic.AtomicInteger()
            val exhaustedModel=object:ModelClient{override fun complete(input:String,previous:String,diagnostics:String,cancellation:GenerationCancellation,log:EmbeddingLog):String="invalid fixture ${distinct.incrementAndGet()}"}
            ApplicationProtocol(context,Embedding(context,CapabilityRegistry(context,CapabilityDiscovery(context){false},emptyList()),exhaustedModel,EmbeddingConfig("experience",emptyList(),"exhausted-${java.util.UUID.randomUUID()}"))).use{other->
                val r=other.request("Exhausted","Fixture request",other.snapshot(WireType("record"),"{}"))
                check(runCatching{other.generate(r)}.exceptionOrNull().let{it is ProtocolFailure&&it.code=="GENERATION_ATTEMPT_LIMIT"});check(distinct.get()==3)
            }
            for((thrown,expected) in listOf(java.net.ConnectException("fixture") to "GATEWAY_UNREACHABLE",java.net.SocketTimeoutException("fixture") to "MODEL_TIMEOUT")){
                val failureModel=object:ModelClient{override fun complete(input:String,previous:String,diagnostics:String,cancellation:GenerationCancellation,log:EmbeddingLog):String=throw thrown}
                ApplicationProtocol(context,Embedding(context,CapabilityRegistry(context,CapabilityDiscovery(context){false},emptyList()),failureModel,EmbeddingConfig("experience",emptyList(),"transport-${java.util.UUID.randomUUID()}"))).use{other->
                    val r=other.request("Transport","Fixture request",other.snapshot(WireType("record"),"{}"))
                    check(runCatching{other.generate(r)}.exceptionOrNull().let{it is ProtocolFailure&&it.code==expected})
                }
            }
            check(dev.deal.embedding.activitySummary(JSONObject().put("stage","source").put("outcome","started"))=="source · started ")
            check(dev.deal.embedding.activitySummary(JSONObject().put("summary","retained exact summary"))=="retained exact summary")
            val events=listOf(JSONObject().put("trace","t").put("span","s").put("stage","source").put("outcome","started").put("time","1"),JSONObject().put("trace","t").put("span","r").put("parent","s").put("stage","source").put("outcome","response").put("time","2"))
            check(dialogueExchanges(events).single().events.size==2)
            check(dialogueExchanges(listOf(events[1])).single().events.size==1)
            check(api.retainedEvents(maximum=1).incomplete)
            for((code,title) in listOf("GENERATION_ATTEMPT_LIMIT" to "Couldn’t prepare options","GENERATION_REPEATED_RESPONSE" to "Couldn’t prepare options","GENERATION_TEMPLATE_CHECK" to "Options unavailable","AUDIT_LIMIT" to "Demo connection needs attention","MODEL_TIMEOUT" to "AI took too long","GATEWAY_UNREACHABLE" to "AI connection unavailable")){
                check(dev.deal.apps.organizer.generationProblem(ProtocolFailure(code,"fixture")).title==title)
            }
            api.forget(experience)
            result.putString("hardening","10000 authenticated revisions/constant retained cursor state; 309 direct admissions/buffer limit; 50 detach cycles/no callbacks or pending waits; real mount/context rejection preserves healthy session; retained cleanup failure/reclamation; provider and generation codes; shared observation")
            finish(0,result)
        }catch(error:Throwable){result.putString("failure",android.util.Log.getStackTraceString(error));finish(1,result)}finally{protocol?.close()}
    }
}
