package dev.deal.connectors.tests

import android.app.Instrumentation
import android.os.Bundle
import dev.deal.embedding.*
import dev.deal.embedding.capabilities.*
import org.json.*
import java.util.UUID

/** Real maintained graph admission/runtime; no live inference. */
internal fun counterBehaviorPlan() = JSONObject("""{
 "records":[],"state":[{"name":"value","type":"number","initial":"7"}],
 "nodes":[{"id":"one","op":"literal","args":[],"params":{"type":"number","value":"1"}},
          {"id":"increment","op":"add","args":["${'$'}state.value","one"],"params":{}},
          {"id":"label","op":"literal","args":[],"params":{"type":"string","value":"\"Increment\""}}],
 "derived":[],"controls":[],
 "actions":[{"id":"Increment","fields":[],"updates":[{"field":"value","node":"increment"}],"steps":[]}],
 "views":[{"id":"value","component":"IntText","props":[{"name":"value","node":"${'$'}state.value"}],"events":[],"children":[],"each":"","key":"","when":""},
          {"id":"button","component":"Button","props":[{"name":"text","node":"label"}],"events":[{"name":"onClick","action":"Increment","payload":[]}],"children":[],"each":"","key":"","when":""},
          {"id":"root","component":"Column","props":[],"events":[],"children":["value","button"],"each":"","key":"","when":""}],"root":"root"
}""")

class CompactInstrumentation : Instrumentation() {
 override fun onCreate(arguments:Bundle?){super.onCreate(arguments);start()}
 override fun onStart(){val receipt=Bundle();try{
  val plan=counterBehaviorPlan();val emitter=BehaviorCandidate(emptyList());val pair=emitter.assemble(plan)
  check(pair.toString()==emitter.assemble(plan).toString())
  fun rejected(p:JSONObject){check(runCatching{emitter.assemble(p)}.exceptionOrNull() is BehaviorCandidate.Rejected)}
  rejected(JSONObject(plan.toString()).put("functions","return 7;"))
  rejected(JSONObject(plan.toString()).apply{getJSONArray("nodes").getJSONObject(1).put("args",JSONArray("[\"increment\",\"one\"]"))})
  var calls=0
  val model=object:ModelClient{override fun complete(input:String,previous:String,diagnostics:String,cancellation:GenerationCancellation,log:EmbeddingLog):String{
   calls++;check(calls==1&&previous.isEmpty());return nativeCompactFixture(plan)
  }}
  ApplicationProtocol(targetContext,Embedding(targetContext,CapabilityRegistry(targetContext,CapabilityDiscovery(targetContext){false},emptyList()),model,EmbeddingConfig("experience",emptyList(),"behavior-${UUID.randomUUID()}"))).use{api->
   val e=api.generate(api.request("Counter","Editable counter",api.snapshot(WireType("record"),"{}")))
   check(api.source(e)==Source(pair.getString("deal"),pair.getString("dealui")))
   val s=api.run(e)
   fun nodes(n:JSONObject):List<JSONObject> = listOf(n)+(0 until n.getJSONArray("children").length()).flatMap{nodes(n.getJSONArray("children").getJSONObject(it))}
   val t=api.next(s,"").snapshot.tree
   val button=nodes(t).first{it.getString("component").endsWith(".Button")}
   val props=button.getJSONArray("props");val slot=(0 until props.length()).map{props.getJSONObject(it)}.first{it.optString("kind")=="action"}.getInt("actionSlot")
   api.dispatch(s,slot,null)
   val end=System.currentTimeMillis()+10000
   while(!api.next(s,"").snapshot.tree.toString().contains("\"numberValue\":8")){check(System.currentTimeMillis()<end);Thread.sleep(20)}
   api.close(s);api.forget(e)
  }
  // All four maintained plans are test assets, never shipped in the production library.
  val contracts=JSONArray(context.assets.open("behavior-fixtures/contracts.json").bufferedReader().use{it.readText()}).let{a->(0 until a.length()).map{CapabilityContract.parse(a.getJSONObject(it).toString())}}
  var providerReads=0
  fun attributes(kind:String)=JSONObject().put("kind",kind).put("adjustable",kind=="stand").put("clipAdapter",kind=="stand").put("balanced",kind=="cable").put("connectorFrom",if(kind=="cable")"XLR female" else "").put("connectorTo",if(kind=="cable")"XLR male" else "").put("lengthMm",if(kind=="cable")10000 else 0)
  val catalogue=JSONArray().put(JSONObject().put("id","opaque-stand").put("name","Fixture stand").put("cents",800).put("available",2).put("attributes",attributes("stand")).put("components",JSONArray()))
   .put(JSONObject().put("id","opaque-cable").put("name","Fixture cable").put("cents",500).put("available",2).put("attributes",attributes("cable")).put("components",JSONArray()))
   .put(JSONObject().put("id","opaque-bundle").put("name","Fixture bundle").put("cents",1200).put("available",1).put("attributes",attributes("package")).put("components",JSONArray().put(JSONObject().put("itemId","opaque-stand").put("quantity",1)).put(JSONObject().put("itemId","opaque-cable").put("quantity",1))))
  val providers=contracts.map{c->NativeCapabilities(c,c.functions.associate{f->f.name to {args:JSONArray->
   providerReads++
   when(c.module+"."+f.name){
    "host/trip.estimate"->29
    "host/pizza.menu"->JSONArray().put(JSONObject().put("id","opaque-normal").put("name","Standard").put("cents",1000).put("vegan",false).put("diameterMm",300)).put(JSONObject().put("id","opaque-plant").put("name","Plant").put("cents",1200).put("vegan",true).put("diameterMm",300))
    "host/pizza.delivery"->JSONArray().put(JSONObject().put("id","timely").put("from","17:45").put("until","18:15").put("meetsRequirement",true).put("deliveryCents",500))
    "host/rental.equipment"->catalogue
    "host/rental.quote"->{val items=args.getString(0);check(items=="opaque-bundle:1" || items=="opaque-stand:1,opaque-cable:1");JSONObject().put("cents",if(items=="opaque-bundle:1")1200 else 1300).put("depositCents",5000).put("expiresInMinutes",15).put("available",true)}
    "host/optional.read"->JSONObject()
    else->error("Unexpected fixture call")
   }
  }})}
  for(task in listOf("budget","departure","food","equipment","fragment-budget","fragment-departure","fragment-food","fragment-equipment")){
   val scenario=task.removePrefix("fragment-")
   val fixture=JSONObject(context.assets.open("behavior-fixtures/$task.json").bufferedReader().use{it.readText()})
   val emitted=BehaviorCandidate(contracts).assemble(fixture)
   ApplicationProtocol(targetContext,Embedding(targetContext,CapabilityRegistry(targetContext,CapabilityDiscovery(targetContext){false},providers),object:ModelClient{override fun complete(input:String,previous:String,diagnostics:String,cancellation:GenerationCancellation,log:EmbeddingLog):String=error("No inference in maintained device fixture")},EmbeddingConfig("experience",emptyList(),"behavior-$task-${UUID.randomUUID()}"))).use{api->
    api.refresh();contracts.forEach{api.grant(it.module)}
    val readsBefore=providerReads;val exp=api.load(task,Source(emitted.getString("deal"),emitted.getString("dealui")));val session=api.run(exp)
    fun nodes(n:JSONObject):List<JSONObject> = listOf(n)+(0 until n.getJSONArray("children").length()).flatMap{nodes(n.getJSONArray("children").getJSONObject(it))}
    var tree=api.next(session,"").snapshot.tree;check(providerReads==readsBefore)
    if(scenario!="budget"){
     val button=nodes(tree).first{it.getString("component").endsWith(".PrimaryAction")};val props=button.getJSONArray("props");val action=(0 until props.length()).map{props.getJSONObject(it)}.first{it.optString("kind")=="action"}.getInt("actionSlot");api.dispatch(session,action,null)
     val expected=when(scenario){"departure"->"15:16";"food"->"Ready";else->"Within budget"};val end=System.currentTimeMillis()+15000
     while(!tree.toString().contains(expected)){check(System.currentTimeMillis()<end){"$task: $tree"};Thread.sleep(20);tree=api.next(session,"").snapshot.tree}
    }else check(tree.toString().contains("1750"))
    api.close(session);api.forget(exp)
   }
  }
  val engine=SandboxEngine.acquire(targetContext)
  try{SandboxProgram(engine,512*1024).use{p->
   val text="x".repeat(700000)+"🌍\r\n\u0000"+"z".repeat(700000)
   p.evaluate("globalThis.transferCalls=0; globalThis.largeText=${JSONObject.quote(text)}; ''")
   val actual=p.evaluateString("(transferCalls++,largeText)");check(actual==text);check(p.evaluate("String(transferCalls)")=="1")
  }}finally{SandboxEngine.release(engine)}
  receipt.putString("result","PASS maintained admission, deterministic emission, closed-shape/cycle rejection, real compiled update, lossless >512KiB single-evaluation bridge transfer")
  finish(android.app.Activity.RESULT_OK,receipt)
 }catch(e:Throwable){receipt.putString("failure",android.util.Log.getStackTraceString(e));finish(android.app.Activity.RESULT_CANCELED,receipt)}}
}
