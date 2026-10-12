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
  val probe=NativeCapabilities(CapabilityContract("host/probe","Protocol binding probe",listOf(CapabilityFunction("ping","Explicit probe",emptyList(),CapabilityType.IntType))),mapOf("ping" to {_:JSONArray->1}))
  val registry=CapabilityRegistry(context,CapabilityDiscovery(context){false},listOf(probe))
  val model=object:ModelClient{override fun complete(input:String,previous:String,diagnostics:String,cancellation:GenerationCancellation,log:EmbeddingLog):String=error("Unexpected inference")}
  embedding=Embedding(context,registry,model,EmbeddingConfig("experience",emptyList(),"protocol-instrumentation"))
  val host=embedding
  check(host.refresh().any{it.module=="host/probe"})
  val schema=WireType("record",fields=listOf(WireField("name",WireType("string",maximum=16)),WireField("count",WireType("int"),true)))
  val facts=host.snapshot(schema,"{\"name\":\"Ada\"}")
  check(host.contextJson(facts).contains("Ada"));check(host.contextSchema(facts).fields.size==2)
  check(runCatching{host.snapshot(schema,"{\"name\":\"Ada\",\"extra\":1}")}.isFailure)
  check(runCatching{host.contextJson(GenerationContext("forged"))}.isFailure)
  check(runCatching{host.snapshot(WireType("record",fields=listOf(WireField("bad",WireType(""),true))),"{}")}.isFailure)
  check(runCatching{host.snapshot(schema,"{\"name\":\"Ada\"} trailing")}.isFailure)
  check(runCatching{host.snapshot(WireType("string",maximum=1),"\"😀\"")}.isFailure)
  val optional=CapabilityType("record",fields=mapOf("value" to CapabilityType.IntType),optionalFields=setOf("value"))
  optional.validate(JSONObject());check(CapabilityType.parse(optional.json()).optionalFields==setOf("value"))
  val deal="""import * as probe from "host/probe";
export class State { value: int = 0; }
export class Increment {}
export function initialState(): State { return {}; }
// @ui-update
export function increment(state: State, action: Increment): State { return { value: state.value + 1 }; }
"""
  val view="""import * as app from "./experience";
import * as ui from "./platform.dealui-pack";
// @ui-root
export view Main(state: app.State): View { ui.Column() { ui.Text(value: "Protocol ready") ui.Button(text: "Increment", onClick: action app.Increment {}) } }
"""
  val e=host.load("Protocol",Source(deal,view));check(host.saved().none{it.id==e.id});check(host.source(e).deal==deal)
  val session=host.run(e);check(host.run(e)==session);val snapshot=host.poll(session);check(snapshot.tree.toString().contains("Protocol ready"));host.published(session,snapshot.version);check(runCatching{host.published(session,snapshot.version+1)}.isFailure)
  check(host.saved().any{it.id==e.id});check(runCatching{host.run(e.copy(id="forged"))}.isFailure)
  host.close(session);check(runCatching{host.poll(session)}.isFailure)
  val cold=host.run(e);check(cold!=session);host.close(cold)
  host.forget(e);check(host.saved().none{it.id==e.id})
  val request=host.request("Request","Unsupported",facts);check(host.requestTrace(request).isNotEmpty());host.cancel(request)
  check(runCatching{host.generate(request)}.exceptionOrNull() is java.util.concurrent.CancellationException)
  check(runCatching{host.generate(request)}.isFailure)
  val page=host.events("","",20);check(page.events.size<=20)
  var fixtureCalls=0
  val fixtureModel=object:ModelClient{override fun complete(input:String,previous:String,diagnostics:String,cancellation:GenerationCancellation,log:EmbeddingLog):String{fixtureCalls++;return JSONObject().put("deal",deal).put("dealui",view).toString()}}
  val fallback=Embedding(context,CapabilityRegistry(context,CapabilityDiscovery(context){false},listOf(probe)),fixtureModel,EmbeddingConfig("experience",emptyList(),"protocol-fallback"))
  try{fallback.refresh();val facts=fallback.snapshot(WireType("record"),"{}");val prepared=fallback.generate(fallback.request("Fallback","An authored test workflow",facts));check(prepared.origin=="generated"&&fixtureCalls==1);fallback.release(prepared)}finally{fallback.shutdown()}
  // No network: source fixture validates absent-policy fallback through real core.
  val badModel=object:ModelClient{override fun complete(input:String,previous:String,diagnostics:String,cancellation:GenerationCancellation,log:EmbeddingLog):String="not source"}
  val repeated=Embedding(context,CapabilityRegistry(context,CapabilityDiscovery(context){false},emptyList()),badModel,EmbeddingConfig("experience",emptyList(),"protocol-repeated"))
  try{repeated.refresh();val c=repeated.snapshot(WireType("record"),"{}");val error=runCatching{repeated.generate(repeated.request("Repeated","Fixture invalid response",c))}.exceptionOrNull();check(error is GenerationRejected&&error.reason==GenerationRejected.Reason.REPEATED_RESPONSE)}finally{repeated.shutdown()}
  // Invoke actual compiled DEAL declarations through the public native linkage.
  val engine=SandboxEngine.acquire(context);val program=SandboxProgram(engine,256*1024)
  try {
   val binding=PublicProtocolBinding(host)
   val bundle=SandboxBundle.build(context,listOf("protocol-contracts.js","protocol-host.js","public-protocol.js"),"","(globalThis.publicFactories=factories,'')",assetRoot="protocol-consumer")
   program.evaluate(bundle)
   fun call(name:String,args:JSONArray,turn:JSONObject?=null):Any {
    program.evaluate("protocolCall(publicFactories,'consumer.js',${JSONObject.quote(name)},$args,${turn?:"null"})")
    for(step in 0..100){
     val requests=JSONArray(program.evaluate("dealCapabilities.take()"))
     for(i in 0 until requests.length()){val r=requests.getJSONObject(i);val value=binding.invoke(r.getString("function"),r.getJSONArray("args"));program.evaluate("dealCapabilities.deliver(${JSONArray().put(JSONObject().put("id",r.getInt("id")).put("ok",true).put("value",value))})")}
     val result=JSONObject(program.evaluate("JSON.stringify(publicResult)"));if(result.getBoolean("done")){check(!result.has("error")){result.toString()};return result.get("value")};Thread.sleep(10)
    };error("Public DEAL call did not complete")
   }
   val compiled=call("load",JSONArray().put("Public ABI").put(deal).put(view)) as JSONObject
   val mounted=call("run",JSONArray().put(compiled.getString("id"))) as JSONObject
   val tree=call("poll",JSONArray().put(mounted.getString("id"))) as JSONObject;check(tree.getJSONObject("tree").toString().contains("Protocol ready"))
   val saved=binding.invoke("saved",JSONArray())
   val turn=JSONObject().put("name","saved").put("args",JSONArray()).put("value",saved)
   check((call("saved",JSONArray(),turn) as JSONArray).length()>0)
   call("events",JSONArray());call("close",JSONArray().put(mounted.getString("id")));call("forget",JSONArray().put(compiled.getString("id")))
  }finally{program.close();SandboxEngine.release(engine)}
  result.putString("protocolVerification","validated contexts, forged handles, preparation/save/run/close/cold/forget, request cancellation and observation exercised; zero inference")
  finish(0,result)
 }catch(e:Throwable){result.putString("failure",android.util.Log.getStackTraceString(e));finish(1,result)}finally{embedding?.shutdown()}
 }
}
