package dev.deal.connectors.tests

import android.app.Instrumentation
import android.os.Bundle
import dev.deal.embedding.*
import dev.deal.embedding.capabilities.*
import org.json.*
import java.util.UUID

/** Injected needs/choice/section replies; production composer, checker, storage and runtime. */
class ConstrainedRecoveryInstrumentation : Instrumentation() {
 override fun onCreate(arguments:Bundle?){super.onCreate(arguments);start()}
 override fun onStart(){val receipt=Bundle();try{
  fun nodes(n:JSONObject):List<JSONObject> = listOf(n)+(0 until n.getJSONArray("children").length()).flatMap{nodes(n.getJSONArray("children").getJSONObject(it))}
  fun prop(n:JSONObject,key:String):JSONObject?=(0 until n.getJSONArray("props").length()).map{n.getJSONArray("props").getJSONObject(it)}.firstOrNull{it.optString("name")==key}
  for(partial in listOf(false,true)){
   var holes=0;val thinking=mutableListOf<Boolean>()
   val model=object:ModelClient{override fun complete(input:String,previous:String,diagnostics:String,cancellation:GenerationCancellation,log:EmbeddingLog):String{
    check(previous.isEmpty()){"Unexpected whole-source/exact-repair route"}
    if(input.contains("\nGENERATION OPERATION\n"))return """{"title":"Local recovery","terms":[{"id":"initial","op":"literal","args":[],"source":"","path":"","value":7,"evidence":"7","dimension":"unknown"},{"id":"source","op":"port","args":[],"source":"amount","path":"value","value":null,"evidence":"","dimension":"unknown"}],"needs":[{"id":"amount","kind":"input","label":"Amount","config":{"value":"initial","format":"integer","minimum":0,"maximum":100},"evidence":"task"},{"id":"metric","kind":"result","label":"Recovered metric","config":{"value":"source","format":"integer"},"evidence":"task"}]}"""
    if(input.contains("\n\nISSUED OPTIONS\n"))return """{"answers":{"Need0":"amount_maintained","Need1":"uncovered"}}"""
    val turn=JSONObject(input.substringAfterLast("\nNATIVE TOOL TURN\n"));check(turn.getBoolean("holes"));holes++;thinking+=turn.getBoolean("thinking")
    if(partial)return JSONObject().put("role","assistant").put("content","").put("tool_calls",JSONArray().put(JSONObject().put("id","refusal-$holes").put("type","function").put("function",JSONObject().put("name","source_fallback").put("arguments",JSONObject().put("reason","Cannot fill this local hole").toString())))).toString()
    val issued=JSONObject(input.substringAfter("\nUNCOVERED NEEDS\n").substringBefore("\nFill only")).getJSONArray("interfaces").getJSONObject(0)
    val section=JSONObject(issued.toString()).put("exports",JSONArray("""[{"name":"value","type":"number","value":{"expr":"display.value"}}]""")).put("plan",JSONObject("""{"title":"Owned display","fragments":[{"id":"display","kind":"result","config":{"label":"Recovered metric","format":"integer","value":{"expr":"source.value"}}}],"commands":[],"display":["display"]}"""))
    return JSONObject().put("role","assistant").put("content","").put("tool_calls",JSONArray().put(JSONObject().put("id","section-$holes").put("type","function").put("function",JSONObject().put("name","submit_section").put("arguments",JSONObject().put("reason","Local display recovery").put("sections",JSONArray().put(section)).toString())))).toString()
   }}
   ApplicationProtocol(targetContext,Embedding(targetContext,CapabilityRegistry(targetContext,CapabilityDiscovery(targetContext){false},emptyList()),model,RuntimeConfig("experience",emptyList(),"recovery-${UUID.randomUUID()}",retainState=true))).use{api->
    val e=api.generate(api.request("Recovery","task 7",api.snapshot(WireType("record"),"{}")))
    check(holes==if(partial)3 else 1);check(thinking==if(partial)listOf(false,false,true)else listOf(false))
    val source=api.source(e);check(source.deal.contains("m_amountValue"));check(source.dealui.contains(if(partial)"Unavailable: Recovered metric" else "Recovered metric")||source.deal.contains(if(partial)"Unavailable: Recovered metric" else "Recovered metric"))
    val s=api.run(e);var tree=api.next(s,"").snapshot.tree
    val control=nodes(tree).first{prop(it,"label")?.optString("stringValue")=="Amount"}
    val action=(0 until control.getJSONArray("props").length()).map{control.getJSONArray("props").getJSONObject(it)}.first{it.optString("kind")=="action"}.getInt("actionSlot")
    api.dispatch(s,action,"8")
    val end=System.currentTimeMillis()+10000
    do{Thread.sleep(20);tree=api.next(s,"").snapshot.tree}while(!tree.toString().contains("\"numberValue\":8")&&System.currentTimeMillis()<end)
    check(tree.toString().contains("\"numberValue\":8"));if(partial)check(nodes(tree).any{it.getString("component").endsWith(".Notice")})
    api.close(s);val restored=api.run(e);check(api.next(restored,"").snapshot.tree.toString().contains("\"numberValue\":8")){"Retained-state reopen lost the edited value (partial=$partial)"};api.close(restored);api.forget(e)
   }
  }
  val purePlan=JSONObject("""{"title":"Pure recovery","fragments":[{"id":"amount","kind":"input","config":{"label":"Amount","format":"integer","value":7,"minimum":0,"maximum":100}},{"id":"scaled","kind":"custom","config":{"label":"Scaled","format":"integer","section":"scale","arguments":[{"expr":"amount.value"}],"result":"number"}}],"custom":[{"id":"scale","parameters":[{"name":"value","type":"number"}],"result":"number","body":"return value * 2;"}],"reason":"Issued pure computation","commands":[],"display":["amount","scaled"]}""")
  val purePair=BehaviorCandidate(emptyList(),true).assemble(purePlan)
  val pureModel=object:ModelClient{override fun complete(input:String,previous:String,diagnostics:String,cancellation:GenerationCancellation,log:EmbeddingLog):String=error("No pure replay inference")}
  ApplicationProtocol(targetContext,Embedding(targetContext,CapabilityRegistry(targetContext,CapabilityDiscovery(targetContext){false},emptyList()),pureModel,RuntimeConfig("experience",emptyList(),"recovery-pure-${UUID.randomUUID()}"))).use{api->
   val e=api.load("Pure recovery",Source(purePair.getString("deal"),purePair.getString("dealui")));val s=api.run(e);var tree=api.next(s,"").snapshot.tree
   fun scaled(tree:JSONObject,value:String):Boolean=nodes(tree).any{prop(it,"label")?.optString("stringValue")=="Scaled"&&prop(it,"value")?.optString("stringValue")==value}
   check(scaled(tree,"14"));val control=nodes(tree).first{prop(it,"label")?.optString("stringValue")=="Amount"};val props=control.getJSONArray("props");val slot=(0 until props.length()).map{props.getJSONObject(it)}.first{it.optString("kind")=="action"}.getInt("actionSlot")
   api.dispatch(s,slot,"8");val deadline=System.currentTimeMillis()+10000;do{Thread.sleep(20);tree=api.next(s,"").snapshot.tree}while(!scaled(tree,"16")&&System.currentTimeMillis()<deadline);check(scaled(tree,"16"));api.close(s);api.forget(e)
  }
  // A typed collection import aliases the disclosed array, preserving its wire
  // descriptor and maintained provider/action ownership across the section boundary.
  val manyContract=CapabilityContract("host/collection","Recovery collection",listOf(CapabilityFunction("many","Read values",emptyList(),WireType("array",element=WireType("number")))))
  val manyProvider=NativeCapabilities(manyContract,mapOf("many" to {_:JSONArray->JSONArray().put(2).put(4).put(6)}))
  val manyPlan=JSONObject("""{"title":"Collection import","fragments":[{"id":"many","kind":"read","config":{"binding":{"module":"host/collection","function":"many"},"arguments":[]}},{"id":"count","kind":"section","config":{"imports":[{"name":"values","type":"array","value":{"expr":"many.result"}}],"exports":[{"name":"value","type":"number","value":{"expr":"metric.value"}}],"capabilities":[],"plan":{"title":"Count","fragments":[{"id":"metric","kind":"result","config":{"label":"Imported count","format":"integer","value":{"expr":"sum(values.value,row) / length(values.value)"}}}],"commands":[],"display":["metric"]}}}],"commands":[{"id":"Load","label":"Load collection","targets":["many"]}],"display":["count"]}""")
  val manyRead=manyPlan.getJSONArray("fragments").getJSONObject(0)
  manyPlan.getJSONArray("fragments").put(0,JSONObject().put("id","many").put("kind","section").put("config",JSONObject().put("imports",JSONArray()).put("exports",JSONArray("""[{"name":"result","type":"array","value":{"expr":"reading.result"}}]""")).put("capabilities",JSONArray("""[{"module":"host/collection","function":"many"}]""")).put("plan",JSONObject().put("title","Recovered collection read").put("fragments",JSONArray().put(JSONObject(manyRead.toString()).put("id","reading"))).put("commands",JSONArray("""[{"id":"Read","label":"Load exported collection","targets":["reading"]}]""")).put("display",JSONArray()))))
  manyPlan.put("commands",JSONArray());manyPlan.getJSONArray("display").put("many")
  val manyPair=BehaviorCandidate(listOf(manyContract)).assemble(manyPlan)
  val manyModel=object:ModelClient{override fun complete(input:String,previous:String,diagnostics:String,cancellation:GenerationCancellation,log:EmbeddingLog):String=error("No collection replay inference")}
  ApplicationProtocol(targetContext,Embedding(targetContext,CapabilityRegistry(targetContext,CapabilityDiscovery(targetContext){false},listOf(manyProvider)),manyModel,RuntimeConfig("experience",emptyList(),"recovery-collection-${UUID.randomUUID()}"))).use{api->
   api.refresh();api.grant(manyContract.module);val e=api.load("Collection recovery",Source(manyPair.getString("deal"),manyPair.getString("dealui")));val s=api.run(e);var snapshot=api.next(s,"").snapshot
   val button=nodes(snapshot.tree).first{prop(it,"text")?.optString("stringValue")=="Load exported collection"};val props=button.getJSONArray("props");val slot=(0 until props.length()).map{props.getJSONObject(it)}.first{it.optString("kind")=="action"}.getInt("actionSlot")
   api.dispatch(s,slot,null);val deadline=System.currentTimeMillis()+10000
   fun counted(tree:JSONObject):Boolean=nodes(tree).any{prop(it,"label")?.optString("stringValue")=="Imported count"&&prop(it,"value")?.optString("stringValue")=="4"}
   do{Thread.sleep(20);snapshot=api.next(s,"").snapshot}while(!counted(snapshot.tree)&&System.currentTimeMillis()<deadline)
   check(snapshot.fault.isEmpty());check(counted(snapshot.tree)){"Collection import did not preserve array length: ${snapshot.tree}"};api.close(s);api.forget(e)
  }
  // A provider completion in an owned section must settle, not trigger itself again.
  var reads=0
  val contract=CapabilityContract("host/probe","Recovery fixture",listOf(CapabilityFunction("read","Read fixture",listOf(Parameter("amount",WireType("number"))),WireType("number"))))
  val provider=NativeCapabilities(contract,mapOf("read" to {args:JSONArray->reads++;args.getDouble(0)+22}))
  val plan=JSONObject("""{"title":"Provider section","fragments":[{"id":"amount","kind":"input","config":{"label":"Amount","format":"integer","value":7,"minimum":0,"maximum":100}},{"id":"local","kind":"section","config":{"imports":[{"name":"request","type":"number","value":{"expr":"amount.value"}}],"exports":[{"name":"value","type":"number","value":{"expr":"metric.value"}}],"capabilities":[{"module":"host/probe","function":"read"}],"plan":{"title":"Owned provider","fragments":[{"id":"reading","kind":"read","config":{"binding":{"module":"host/probe","function":"read"},"arguments":[{"expr":"request.value"}]}},{"id":"metric","kind":"result","config":{"label":"Provider result","format":"integer","value":{"expr":"reading.result"}}}],"commands":[{"id":"Read","label":"Read local","targets":["reading"]}],"display":["metric"]}}}],"commands":[],"display":["amount","local"]}""")
  val pair=BehaviorCandidate(listOf(contract)).assemble(plan)
  val noModel=object:ModelClient{override fun complete(input:String,previous:String,diagnostics:String,cancellation:GenerationCancellation,log:EmbeddingLog):String=error("No inference in provider replay")}
  ApplicationProtocol(targetContext,Embedding(targetContext,CapabilityRegistry(targetContext,CapabilityDiscovery(targetContext){false},listOf(provider)),noModel,RuntimeConfig("experience",emptyList(),"recovery-provider-${UUID.randomUUID()}",retainState=true))).use{api->
   api.refresh();api.grant(contract.module);val e=api.load("Provider recovery",Source(pair.getString("deal"),pair.getString("dealui")));val s=api.run(e);val initial=api.next(s,"").snapshot.tree
   val button=nodes(initial).first{prop(it,"text")?.optString("stringValue")=="Read local"};val props=button.getJSONArray("props");val slot=(0 until props.length()).map{props.getJSONObject(it)}.first{it.optString("kind")=="action"}.getInt("actionSlot")
   api.dispatch(s,slot,null);val end=System.currentTimeMillis()+10000;var tree=initial
   do{Thread.sleep(20);tree=api.next(s,"").snapshot.tree}while(!tree.toString().contains("29")&&System.currentTimeMillis()<end)
   check(tree.toString().contains("29"));Thread.sleep(200);check(reads==1){"Section completion rescheduled provider: $reads"};api.close(s);api.forget(e)
  }
  receipt.putString("result","PASS production needs/choice/section recovery, three-call denial/partial notices, retained edits and restoration; no live inference")
  finish(android.app.Activity.RESULT_OK,receipt)
 }catch(e:Throwable){receipt.putString("failure",android.util.Log.getStackTraceString(e));finish(android.app.Activity.RESULT_CANCELED,receipt)}}
}
