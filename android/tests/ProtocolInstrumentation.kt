package dev.deal.connectors.tests
import android.app.Instrumentation
import android.os.Bundle
import dev.deal.embedding.*
import dev.deal.embedding.capabilities.*
import org.json.*

/** Actual compiler/session protocol tests. Fixture inference, never claimed as live model evidence. */
class ProtocolInstrumentation:Instrumentation(){
 override fun onCreate(arguments:Bundle?){super.onCreate(arguments);start()}
 override fun onStart(){val result=Bundle();var embedding:Embedding?=null
 try{
  val context=targetContext
  // DEAL has one numeric lane; integral constraints belong to native consumers.
  CapabilityType.NumberType.validate(1.5)
  WireType("number").validate(1.5)
  for (invalid in listOf(Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, "1.5")) {
   check(runCatching { CapabilityType.NumberType.validate(invalid) }.isFailure)
   check(runCatching { WireType("number").validate(invalid) }.isFailure)
  }
  check(runCatching { CapabilityType.parse(CapabilityType.NumberType.json().put("maximum", 1.5)) }.isFailure)
  check(runCatching { WireType.parse(WireType("number").json().put("maximum", 1.5)) }.isFailure)
  check(runCatching { CapabilityContract.parse(CapabilityContract("host/test","Numeric metadata",listOf(CapabilityFunction("read","Read",emptyList(),CapabilityType.NumberType))).json().put("version",1.5).toString()) }.isFailure)
  var proposedMinute = -1
  val calendar = dev.deal.apps.calendar.calendarCapabilities({ 960 }, { proposedMinute = it })
  calendar.invoke("propose", JSONArray().put(916.0));check(proposedMinute == 916)
  val vehicle = dev.deal.apps.vehicle.vehicleCapabilities()
  check(vehicle.invoke("estimate", JSONArray().put(12.0)) == 29)
  for (invalid in listOf(-1.0, 1.5, 1440.0, Double.MAX_VALUE)) {
   check(runCatching { calendar.invoke("propose", JSONArray().put(invalid)) }.isFailure)
  }
  check(proposedMinute == 916)
  for (invalid in listOf(0.0, 12.5, 1001.0, Double.MAX_VALUE)) {
   check(runCatching { vehicle.invoke("estimate", JSONArray().put(invalid)) }.isFailure)
  }
  val probe=NativeCapabilities(CapabilityContract("host/probe","Protocol binding probe",listOf(CapabilityFunction("ping","Explicit probe",emptyList(),CapabilityType.NumberType))),mapOf("ping" to {_:JSONArray->1}))
  val registry=CapabilityRegistry(context,CapabilityDiscovery(context){false},listOf(probe))
  val model=object:ModelClient{override fun complete(input:String,previous:String,diagnostics:String,cancellation:GenerationCancellation,log:EmbeddingLog):String=error("Unexpected inference")}
  embedding=Embedding(context,registry,model,EmbeddingConfig("experience",emptyList(),"protocol-instrumentation"))
  val host=embedding
  check(host.refresh().any{it.module=="host/probe"})
  val schema=WireType("record",fields=listOf(WireField("name",WireType("string",maximum=16)),WireField("count",WireType("number"),true)))
  val facts=host.snapshot(schema,"{\"name\":\"Ada\"}")
  check(host.contextJson(facts).contains("Ada"));check(host.contextSchema(facts).fields.size==2)
  check(runCatching{host.snapshot(schema,"{\"name\":\"Ada\",\"extra\":1}")}.isFailure)
  check(runCatching{host.contextJson(GenerationContext("forged"))}.isFailure)
  check(runCatching{host.snapshot(WireType("record",fields=listOf(WireField("bad",WireType(""),true))),"{}")}.isFailure)
  check(runCatching{host.snapshot(schema,"{\"name\":\"Ada\"} trailing")}.isFailure)
  check(runCatching{host.snapshot(WireType("string",maximum=1),"\"😀\"")}.isFailure)
  val optional=CapabilityType("record",fields=mapOf("value" to CapabilityType.NumberType),optionalFields=setOf("value"))
  optional.validate(JSONObject());check(CapabilityType.parse(optional.json()).optionalFields==setOf("value"))
  val deal="""import * as probe from "host/probe";
export class State { value: number ; }
export class Increment {}
export function initialState(): State { return {value: 0}; }
// @ui-update
export function increment(state: State, action: Increment): State { return { value: state.value + 1 }; }
"""
  val view="""import * as app from "./experience";
import * as ui from "./platform.dealui-pack";
// @ui-root
export view Main(state: app.State): View { ui.Column() { ui.Text(value: "Protocol ready") ui.Button(text: "Increment", onClick: action app.Increment {}) } }
"""
  val e=host.load("Protocol",Source(deal,view));check(host.saved().none{it.id==e.id});check(host.source(e).deal==deal)
  val session=host.run(e);check(host.run(e)==session);val snapshot=host.next(session,"").snapshot;check(snapshot.tree.toString().contains("Protocol ready"));host.published(session,snapshot.version);check(runCatching{host.published(session,snapshot.version+1)}.isFailure)
  check(host.saved().any{it.id==e.id});check(runCatching{host.run(e.copy(id="forged"))}.isFailure)
  host.close(session);check(runCatching{host.next(session,"")}.isFailure)
  val cold=host.run(e);check(cold!=session);host.close(cold)
  host.forget(e);check(host.saved().none{it.id==e.id})
  val request=host.request("Request","Unsupported",facts);check(host.requestTrace(request).isNotEmpty());host.cancel(request)
  check(runCatching{host.generate(request)}.exceptionOrNull() is java.util.concurrent.CancellationException)
  check(runCatching{host.generate(request)}.isFailure)
  val page=host.events("","",20);check(page.events.size<=20)
  var fixtureCalls=0
  val fixtureModel=object:ModelClient{override fun complete(input:String,previous:String,diagnostics:String,cancellation:GenerationCancellation,log:EmbeddingLog):String{if(JSONObject(input.substringAfterLast("\nNATIVE TOOL TURN\n")).optBoolean("compact"))return nativeFallbackFixture();fixtureCalls++;return nativeSourceFixture(deal,view)}}
  val fallback=Embedding(context,CapabilityRegistry(context,CapabilityDiscovery(context){false},listOf(probe)),fixtureModel,EmbeddingConfig("experience",emptyList(),"protocol-fallback"))
  try{fallback.refresh();val facts=fallback.snapshot(WireType("record"),"{}");val prepared=fallback.generate(fallback.request("Fallback","An authored test workflow",facts));check(prepared.origin=="generated"&&fixtureCalls==1);fallback.release(prepared)}finally{fallback.shutdown()}
  // No network: source fixture validates absent-policy fallback through real core.
  var badCalls=0
  val badModel=object:ModelClient{override fun complete(input:String,previous:String,diagnostics:String,cancellation:GenerationCancellation,log:EmbeddingLog):String {if(JSONObject(input.substringAfterLast("\nNATIVE TOOL TURN\n")).optBoolean("compact"))return nativeFallbackFixture();badCalls++;return if(badCalls<4)"not source" else nativeSourceFixture(deal,view)}}
  val repeated=Embedding(context,CapabilityRegistry(context,CapabilityDiscovery(context){false},listOf(probe)),badModel,EmbeddingConfig("experience",emptyList(),"protocol-repeated"))
  try{repeated.refresh();val c=repeated.snapshot(WireType("record"),"{}");val recovered=repeated.generate(repeated.request("Repeated","Fixture invalid response",c));check(recovered.attempts==5&&badCalls==4);repeated.release(recovered)}finally{repeated.shutdown()}
  // Invoke actual compiled DEAL declarations through the public native linkage.
  ProtocolController(context,host).use { controller ->
   check(controller.call("prepareCapabilities") as Int==1)
   check(controller.call("construction")=="construction");check(controller.call("invalidGrant")=="grant error")
   check(runCatching { controller.call("fractionalEvents") }.exceptionOrNull().let { it is ProtocolFailure && it.code == "INVALID_ARGUMENT" })
   val cancelled=controller.call("contexts") as String
   check(controller.call("cancelled",JSONArray().put(cancelled))=="cancelled")
   check((controller.call("lifecycle",JSONArray().put(deal).put(view)) as String).contains("cold"))
   check((controller.call("observation") as String).contains("artifact"))
  }
  val generatedHost=Embedding(context,CapabilityRegistry(context,CapabilityDiscovery(context){false},listOf(probe)),fixtureModel,EmbeddingConfig("experience",emptyList(),"protocol-controller-generated"))
  ProtocolController(context,generatedHost).use { controller ->
   val generated=controller.call("generated") as JSONObject
   val s=controller.call("run",JSONArray().put(generated.getString("id"))) as JSONObject
   check(controller.update(Session(s.getString("id"))).snapshot.tree.toString().contains("Protocol ready"))
   controller.call("close",JSONArray().put(s.getString("id")));controller.call("forget",JSONArray().put(generated.getString("id")))
   controller.call("shutdown")
  }
  val restartConfig=EmbeddingConfig("experience",emptyList(),"protocol-controller-restart")
  val restartHost=Embedding(context,CapabilityRegistry(context,CapabilityDiscovery(context){false},listOf(probe)),model,restartConfig)
  ProtocolController(context,restartHost).use{c->c.call("prepareCapabilities");val e=c.call("load",JSONArray().put("Across bootstrap").put(deal).put(view)) as JSONObject;val s=c.call("run",JSONArray().put(e.getString("id"))) as JSONObject;c.call("close",JSONArray().put(s.getString("id")))}
  val restarted=Embedding(context,CapabilityRegistry(context,CapabilityDiscovery(context){false},listOf(probe)),model,restartConfig)
  ProtocolController(context,restarted).use{c->c.call("prepareCapabilities");check(c.call("resumeSaved")==1)}
  // A valid generation may exceed the adapter's ordinary 120-second operation deadline.
  val longModel=object:ModelClient{override fun complete(input:String,previous:String,diagnostics:String,cancellation:GenerationCancellation,log:EmbeddingLog):String{
   if(JSONObject(input.substringAfterLast("\nNATIVE TOOL TURN\n")).optBoolean("compact"))return nativeFallbackFixture();Thread.sleep(121000);cancellation.check();return nativeSourceFixture(deal,view)
  }}
  ApplicationProtocol(context,Embedding(context,CapabilityRegistry(context,CapabilityDiscovery(context){false},listOf(probe)),longModel,EmbeddingConfig("experience",emptyList(),"protocol-long-generation"))).use{api->
   val request=api.request("Long generation","Authored source fixture",api.snapshot(WireType("record"),"{}"))
   val prepared=api.generate(request);check(prepared.origin=="generated");api.release(prepared);api.flushReclamation()
  }
  val entered=java.util.concurrent.CountDownLatch(1)
  val waitingModel=object:ModelClient {override fun complete(input:String,previous:String,diagnostics:String,cancellation:GenerationCancellation,log:EmbeddingLog):String {entered.countDown();while(true){cancellation.check();Thread.sleep(5)}}}
  val waitingHost=Embedding(context,CapabilityRegistry(context,CapabilityDiscovery(context){false},emptyList()),waitingModel,EmbeddingConfig("experience",emptyList(),"protocol-controller-cancel"))
  ProtocolController(context,waitingHost).use {controller ->
   val request=controller.call("waitingRequest") as String
   val operation=controller.start("generateWaiting",JSONArray().put(request))
   val deadline=System.currentTimeMillis()+10000
   while(entered.count>0){check(System.currentTimeMillis()<deadline);Thread.sleep(5)}
   controller.call("cancelWaiting",JSONArray().put(request))
   check(runCatching{controller.await(operation)}.exceptionOrNull().let{it is ProtocolFailure&&it.code=="CANCELLED"})
   check((controller.call("saved") as JSONArray).length()==0)
  }
  val slowEntered=java.util.concurrent.CountDownLatch(1);val slowRelease=java.util.concurrent.CountDownLatch(1)
  val noncooperative=object:ModelClient{override fun complete(input:String,previous:String,diagnostics:String,cancellation:GenerationCancellation,log:EmbeddingLog):String{slowEntered.countDown();slowRelease.await(10,java.util.concurrent.TimeUnit.SECONDS);return nativeSourceFixture(deal,view,input)}}
  val slowHost=Embedding(context,CapabilityRegistry(context,CapabilityDiscovery(context){false},listOf(probe)),noncooperative,EmbeddingConfig("experience",emptyList(),"protocol-controller-noncooperative"))
  ProtocolController(context,slowHost).use {c->
   try{val request=c.call("waitingRequest") as String;val operation=c.start("generateWaiting",JSONArray().put(request));val deadline=System.currentTimeMillis()+10000
    while(slowEntered.count>0){check(System.currentTimeMillis()<deadline);Thread.sleep(5)}
    c.call("cancelWaiting",JSONArray().put(request));check(runCatching{c.await(operation,2)}.exceptionOrNull().let{it is ProtocolFailure&&it.code=="CANCELLED"});check(slowRelease.count==1L)
   }finally{slowRelease.countDown()}
  }
  val capabilityDeal="""import * as probe from "host/probe";
export class State {value:number;}
export class Start {}
export class Done {value:number;}
export function initialState():State {return {value: 0};}
// @ui-update
export function start(state:State,action:Start):State {return state;}
// @ui-effect
export async function execute(state:State,action:Start):Done {let value:number=await probe.ping();return {value:value};}
// @ui-update
export function done(state:State,action:Done):State {return {value:action.value};}
"""
  val capabilityView="""import * as app from "./experience";
import * as ui from "./platform.dealui-pack";
// @ui-root
export view Main(state:app.State):View {ui.Column(){ui.IntText(value:state.value) ui.Button(text:"Probe",onClick:action app.Start {})}}
"""
  var capabilityCalls=0
  val nativeProbe=NativeCapabilities(probe.contract,mapOf("ping" to {_:JSONArray->capabilityCalls++;1}))
  val capabilityHost=Embedding(context,CapabilityRegistry(context,CapabilityDiscovery(context){false},listOf(nativeProbe)),model,EmbeddingConfig("experience",emptyList(),"protocol-controller-capability"))
  ProtocolController(context,capabilityHost).use{c->
   c.call("prepareCapabilities");val e=c.call("load",JSONArray().put("Explicit capability").put(capabilityDeal).put(capabilityView)) as JSONObject;val s=c.call("run",JSONArray().put(e.getString("id"))) as JSONObject
   check(capabilityCalls==0)
   val admission=capabilityHost.registry.openSession(EmbeddingLog(context))
   c.synchronous("revoke",JSONArray().put("host/probe"))
   admission.receive(JSONObject().put("id",1).put("module","host/probe").put("function","ping").put("args",JSONArray()),android.os.SystemClock.elapsedRealtime())
   check(admission.drain(android.os.SystemClock.elapsedRealtime()).single().getJSONObject("error").getString("code")=="DENIED");check(capabilityCalls==0)
   c.synchronous("grant",JSONArray().put("host/probe"))
   admission.receive(JSONObject().put("id",2).put("module","host/probe").put("function","ping").put("args",JSONArray()),android.os.SystemClock.elapsedRealtime())
   check(admission.drain(android.os.SystemClock.elapsedRealtime()).single().getBoolean("ok"));check(capabilityCalls==1);admission.close();capabilityCalls=0
   check(c.call("dispatch",JSONArray().put(s.getString("id")).put(0))===JSONObject.NULL)
   // No next call or application pump: the effect must complete autonomously.
   val completedDeadline=System.currentTimeMillis()+10000
   while(capabilityCalls==0){check(System.currentTimeMillis()<completedDeadline);Thread.sleep(10)}
   Thread.sleep(100);check(capabilityCalls==1)
   check(c.update(Session(s.getString("id"))).snapshot.tree.toString().contains("\"numberValue\":1")){"Effect completion did not progress autonomously"}
   c.synchronous("revoke",JSONArray().put("host/probe"));c.call("dispatch",JSONArray().put(s.getString("id")).put(0));Thread.sleep(150);check(capabilityCalls==1)
   val observed=c.update(Session(s.getString("id")))
   check(c.call("invalidCursor",JSONArray().put(s.getString("id")).put("forged"))=="invalid cursor")
   val waiting=c.start("next",JSONArray().put(s.getString("id")).put(observed.cursor));Thread.sleep(80)
   c.call("close",JSONArray().put(s.getString("id")))
   check(runCatching{c.await(waiting,2)}.exceptionOrNull().let{it is ProtocolFailure&&it.code=="CLOSED"})
   val reopened=c.call("run",JSONArray().put(e.getString("id"))) as JSONObject
   val initial=c.update(Session(reopened.getString("id")))
   val shutdownWait=c.start("next",JSONArray().put(reopened.getString("id")).put(initial.cursor));Thread.sleep(80)
   c.call("shutdown");check(runCatching{c.await(shutdownWait,2)}.isFailure)
  }
  // Faulted native isolate invalidates handles; explicit run cold-rechecks durable source.
  val faultHost=Embedding(context,CapabilityRegistry(context,CapabilityDiscovery(context){false},listOf(probe)),model,EmbeddingConfig("experience",emptyList(),"protocol-controller-fault"))
  ProtocolController(context,faultHost).use{c->
   c.call("prepareCapabilities");val e=c.call("load",JSONArray().put("Fault recovery").put(deal).put(view)) as JSONObject;val s=c.call("run",JSONArray().put(e.getString("id"))) as JSONObject
   val runtimeField=Embedding::class.java.getDeclaredField("runtime").apply{isAccessible=true};val runtime=runtimeField.get(faultHost) as ExperienceRuntime
   val ui=runtime.workspace(e.getString("id"))!!.session
   val programField=UiSession::class.java.getDeclaredField("program").apply{isAccessible=true};val sandboxProgram=programField.get(ui) as SandboxProgram
   val isolateField=SandboxProgram::class.java.getDeclaredField("isolate").apply{isAccessible=true};(isolateField.get(sandboxProgram) as androidx.javascriptengine.JavaScriptIsolate).close()
   Thread.sleep(100);check(c.update(Session(s.getString("id"))).snapshot.fault.isNotEmpty())
   val next=c.call("run",JSONArray().put(e.getString("id"))) as JSONObject
   check(next.getString("id")!=s.getString("id"));c.update(Session(next.getString("id")));c.call("close",JSONArray().put(next.getString("id")));c.call("forget",JSONArray().put(e.getString("id")))
  }
  result.putString("protocolVerification","compiled DEAL consumer, no injected turns: frozen contexts/copies/forged handles, generate/load/run/dispatch/next/publication/live/cold/forget/release, controls, observation, shutdown; cancellation before/during generation including noncooperative mechanism; fixture inference, no network")
  finish(0,result)
 }catch(e:Throwable){result.putString("failure",android.util.Log.getStackTraceString(e));finish(1,result)}finally{embedding?.shutdown()}
 }
}
