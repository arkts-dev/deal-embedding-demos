package dev.deal.connectors.tests

import android.app.Instrumentation
import android.os.Bundle
import dev.deal.embedding.*
import dev.deal.embedding.capabilities.*
import org.json.JSONArray
import java.util.UUID
import java.util.concurrent.*

/** Compound failures must preserve the primary error and never veto execution teardown. */
class CompoundFailureInstrumentation:Instrumentation(){
    override fun onCreate(arguments:Bundle?){super.onCreate(arguments);start()}
    override fun onStart(){
        val result=Bundle()
        val context=targetContext
        val delete=StagingReclamation.delete
        val cleanups=mutableListOf<AutoCloseable>()
        try{
            fun field(value:Any,name:String):Any?=value.javaClass.getDeclaredField(name).apply{isAccessible=true}.get(value)
            fun hasCleanup(error:Throwable):Boolean=error.suppressed.any{it is ProtocolFailure&&it.code=="CLEANUP_FAILED" || hasCleanup(it)}
            val source=Source("""export class State {value:number;}
export function initialState():State {return {value: 0};}
""","""import * as app from "./experience";
import * as ui from "./platform.dealui-pack";
// @ui-root
export view Main(state:app.State):View {ui.Text(value:"Compound failure proof")}
""")
            fun registry()=CapabilityRegistry(context,CapabilityDiscovery(context){false},emptyList())
            fun config()=EmbeddingConfig("experience",emptyList(),"compound-${UUID.randomUUID()}")
            val model=object:ModelClient{
                override fun complete(input:String,previous:String,diagnostics:String,cancellation:GenerationCancellation,log:EmbeddingLog):String=error("Unexpected inference")
            }
            // Actual isolate death combined with unsuccessful deletion, not two independent tests.
            val runtimeRegistry=registry();cleanups.add(runtimeRegistry)
            val runtime=ExperienceRuntime(context,runtimeRegistry,config());cleanups.add(runtime)
            val candidate=runtime.check(ExperienceSource(source.deal,source.dealui))
            val live=runtime.open("Isolate failure",candidate,candidate.identity,"session-${UUID.randomUUID()}").first
            val program=field(live.session,"program") as SandboxProgram
            (field(program,"isolate") as androidx.javascriptengine.JavaScriptIsolate).close()
            try{
                StagingReclamation.delete={false}
                val error=runCatching{runtime.advance(live.id)}.exceptionOrNull()?:error("Expected isolate failure")
                check(error !is ProtocolFailure || error.code!="CLEANUP_FAILED"){"Deletion replaced isolate failure"}
                check(generateSequence(error){it.cause}.any{it is ClosedSandboxProgram||it is androidx.javascriptengine.IsolateTerminatedException||it is IllegalStateException})
                check(hasCleanup(error)){"Isolate failure lost suppressed deletion failure"}
                check(runtime.workspace(live.id)==null && StagingReclamation.pending(context)>0)
                check(runtime.savedWorkspaces().any{it.id==live.id})
                check(runCatching{program.evaluate("1")}.isFailure)
            }finally{StagingReclamation.delete=delete}
            StagingReclamation.retry(context);runtime.forgetWorkspace(live.id);runtime.close()

            // A capability-session close fails after releasing its own resources.
            // closeWorkspace must retain that exact primary object and still attempt deletion.
            val primary=IllegalStateException("Injected capability session close failure")
            var capabilityClosed=false
            val host=object:CapabilityHost{
                override fun openSession(log:EmbeddingLog)=object:CapabilitySession{
                    override val identity="session-${UUID.randomUUID()}"
                    override fun receive(request:org.json.JSONObject,now:Long)=error("Unexpected capability")
                    override fun drain(now:Long)=emptyList<org.json.JSONObject>()
                    override fun close(){capabilityClosed=true;throw primary}
                }
            }
            val closeRuntime=ExperienceRuntime(context,host,config());cleanups.add(closeRuntime)
            val checked=closeRuntime.check(ExperienceSource(source.deal,source.dealui))
            val closing=closeRuntime.open("Close failure",checked,checked.identity,"session-${UUID.randomUUID()}").first
            try{
                StagingReclamation.delete={false}
                val error=runCatching{closeRuntime.closeWorkspace(closing.id)}.exceptionOrNull()
                check(error===primary&&capabilityClosed&&hasCleanup(primary))
                check(StagingReclamation.pending(context)>0)
                check(runCatching{(field(closing.session,"program") as SandboxProgram).evaluate("1")}.isFailure)
            }finally{StagingReclamation.delete=delete}
            StagingReclamation.retry(context);closeRuntime.forgetWorkspace(closing.id);closeRuntime.close()

            // UI diagnostic is translated to CandidateRejected; cleanup evidence must travel with it.
            try{
                StagingReclamation.delete={false}
                val error=runCatching{ExperienceCompiler(context,config()).compile(source.deal,"this is not Deal UI")}.exceptionOrNull()
                check(error is CandidateRejected && hasCleanup(error)){"Translated UI rejection lost cleanup failure: $error"}
                check(StagingReclamation.pending(context)>0)
            }finally{StagingReclamation.delete=delete}
            StagingReclamation.retry(context)

            // Public application load AND generation must retain compiler cleanup evidence.
            val invalidModel=object:ModelClient{
                override fun complete(input:String,previous:String,diagnostics:String,cancellation:GenerationCancellation,log:EmbeddingLog):String=
                    nativeSourceFixture(source.deal,"this is not Deal UI",input)
            }
            val evidenceApi=ApplicationProtocol(context,Embedding(context,registry(),invalidModel,config()));cleanups.add(evidenceApi)
            try{
                StagingReclamation.delete={false}
                val loading=runCatching{evidenceApi.load("Bad UI",Source(source.deal,"this is not Deal UI"))}.exceptionOrNull()?:error("Expected public load rejection")
                check(hasCleanup(loading)){"Public load lost suppressed cleanup: $loading"}
                StagingReclamation.delete=delete;StagingReclamation.retry(context);StagingReclamation.delete={false}
                val request=evidenceApi.request("Bad generated UI","Authored invalid fixture",evidenceApi.snapshot(WireType("record"),"{}"))
                val generated=runCatching{evidenceApi.generate(request)}.exceptionOrNull()?:error("Expected public generation rejection")
                check(hasCleanup(generated)){"Public generation lost suppressed cleanup: $generated"}
            }finally{StagingReclamation.delete=delete;evidenceApi.close()}
            StagingReclamation.retry(context)

            // Surface cleanup failure must stop its executor without vetoing adapter shutdown.
            val ownedMechanisms=Embedding(context,registry(),model,config())
            val ownedApi=ApplicationProtocol(context,ownedMechanisms);cleanups.add(ownedApi)
            val ownedExperience=ownedApi.load("Owned surface",source)
            val owned=ExperienceSurface(ownedApi,ownedExperience);cleanups.add(owned)
            owned.activate{}
            try{
                StagingReclamation.delete={false}
                check(runCatching{owned.close()}.exceptionOrNull().let{it is ProtocolFailure&&it.code=="CLEANUP_FAILED"})
                check((field(owned,"actions") as ExecutorService).awaitTermination(10,TimeUnit.SECONDS)){"Surface executor survived failed close"}
            }finally{StagingReclamation.delete=delete;runCatching{ownedApi.close()}}
            StagingReclamation.retry(context)

            // A dead consumer makes subscription detach throw; adapter shutdown still stops resources.
            val observerMechanisms=Embedding(context,registry(),model,config())
            val observerApi=ApplicationProtocol(context,observerMechanisms);cleanups.add(observerApi)
            val observerExperience=observerApi.load("Dead observer",source)
            val observerSession=observerApi.run(observerExperience)
            val received=CountDownLatch(1)
            observerApi.observe(observerSession,{received.countDown()},{})
            check(received.await(10,TimeUnit.SECONDS))
            val observerController=field(observerApi,"controller") as ProtocolController
            (field(observerController,"program") as SandboxProgram).close()
            check(runCatching{observerApi.close()}.isFailure)
            check((field(observerApi,"observers") as ExecutorService).awaitTermination(10,TimeUnit.SECONDS)){"Observer executor survived failed detach"}
            check((field(observerMechanisms,"driver") as ExecutorService).awaitTermination(10,TimeUnit.SECONDS)){"Embedding driver survived failed detach"}

            // Deletion stays unsuccessful for the ENTIRE shutdown, including live-session deletion.
            val r=registry();val cfg=config();val mechanisms=Embedding(context,r,model,cfg)
            val api=ApplicationProtocol(context,mechanisms);cleanups.add(api)
            val experience=api.load("Shutdown live session",source)
            val session=api.run(experience)
            val initial=api.next(session,"")
            val unused=api.load("Shutdown unused preparation",source)
            val controller=field(api,"controller") as ProtocolController
            val invocation=controller.start("next",JSONArray().put(session.id).put(initial.cursor))
            val future=(field(controller,"futures") as Map<*,*>)[invocation] as CompletableFuture<*>
            val end=System.currentTimeMillis()+10000
            while((field(controller,"nextWaits") as Map<*,*>).isEmpty()){
                check(System.currentTimeMillis()<end){"Next wait was not registered"};Thread.sleep(5)
            }
            try{
                StagingReclamation.delete={false}
                api.release(unused)
                val error=runCatching{api.close()}.exceptionOrNull()?:error("Shutdown must report reclamation failure")
                check(error is ProtocolFailure&&error.code=="CLEANUP_FAILED"&&error.suppressed.isNotEmpty())
                check(StagingReclamation.pending(context)>=2){"Shutdown lost physical reclamation ownership"}
                check(field(controller,"ended")==true)
                for(name in listOf("driving","workers","serial")){
                    val executor=field(controller,name) as ExecutorService
                    check(executor.isShutdown&&executor.isTerminated){"$name remained live after failed cleanup"}
                }
                check((field(mechanisms,"driver") as ExecutorService).awaitTermination(10,TimeUnit.SECONDS))
                check(field(mechanisms,"stopped")==true)
                check((field(mechanisms,"sessions") as Map<*,*>).isEmpty())
                check((field(mechanisms,"deliveries") as Map<*,*>).isEmpty())
                check((field(controller,"nextWaits") as Map<*,*>).isEmpty())
                check((field(controller,"futures") as Map<*,*>).isEmpty())
                check(future.isCompletedExceptionally)
                check(runCatching{(field(controller,"program") as SandboxProgram).evaluate("1")}.exceptionOrNull() is ClosedSandboxProgram)
                check(runCatching{r.openSession(EmbeddingLog(context))}.isFailure)
                check((field(mechanisms,"runtime") as ExperienceRuntime).workspaces().isEmpty())
                check((field(mechanisms,"runtime") as ExperienceRuntime).savedWorkspaces().any{it.id==experience.id})
            }finally{StagingReclamation.delete=delete}
            // The next bootstrap reclaims durable pending deletion, without deleting saved source.
            ApplicationProtocol(context,Embedding(context,registry(),model,cfg)).use{reopened->
                check(StagingReclamation.pending(context)==0)
                val saved=reopened.saved().single{it.id==experience.id}
                check(reopened.source(saved)==source);reopened.forget(saved)
            }
            result.putString("compoundFailures","permanent deletion failure cannot veto shutdown; drivers/executors/isolate/waits stopped; journal survives and next bootstrap reclaims; isolate and session-close primary errors retain suppressed cleanup; translated UI rejection retains cleanup")
            finish(0,result)
        }catch(error:Throwable){result.putString("failure",android.util.Log.getStackTraceString(error));finish(1,result)}finally{
            StagingReclamation.delete=delete
            cleanups.reversed().forEach{runCatching{it.close()}}
            runCatching{StagingReclamation.retry(context)}
        }
    }
}
