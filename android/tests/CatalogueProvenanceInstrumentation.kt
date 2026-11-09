package dev.deal.connectors.tests

import android.app.Instrumentation
import android.os.Bundle
import dev.deal.embedding.*
import dev.deal.embedding.capabilities.*
import org.json.*
import java.util.UUID

/** Public pinned catalogue plus isolated generic function-hole backend fixtures; no inference. */
class CatalogueProvenanceInstrumentation : Instrumentation() {
 override fun onCreate(arguments:Bundle?){super.onCreate(arguments);start()}
 override fun onStart(){val receipt=Bundle();try{
  var plans=0;var choices=0;var contents=0
  val model=object:ModelClient{override fun complete(input:String,previous:String,diagnostics:String,cancellation:GenerationCancellation,log:EmbeddingLog):String{
   cancellation.check();check(previous.isEmpty()&&diagnostics.isEmpty())
   if(input.contains("\nPORTABLE CHOICE\n")){choices++;return """{"answers":{"ready.0":{"choice":"key_facts","confidence":1},"whose.0":{"choice":"written","confidence":1},"ready.1":{"choice":"key_facts","confidence":1},"whose.1":{"choice":"written","confidence":1}}}"""}
   val operation=JSONObject(input.substringAfterLast("\nPORTABLE COMPLETION\n")).getString("operation")
   if(operation=="plan"){plans++;return """{"title":"Catalogue facts","needs":[{"type":"content","what":"Read-only fixture facts"},{"type":"content","what":"Read-only fixture facts"}]}"""}
   check(operation=="content"){"Unexpected model construction: $operation"};contents++
   return """{"section1_fact1_label":"First","section1_fact1_value":"Alpha","section1_fact2_label":"Second","section1_fact2_value":"Beta","section2_fact1_label":"Third","section2_fact1_value":"Gamma","section2_fact2_label":"Fourth","section2_fact2_value":"Delta"}"""
  }}
  val config=RuntimeConfig("experience",emptyList(),"catalogue-${UUID.randomUUID()}")
  ApplicationProtocol(targetContext,Embedding(targetContext,CapabilityRegistry(targetContext,CapabilityDiscovery(targetContext){false},emptyList()),model,config)).use{api->
   val e=api.generate(api.request("Catalogue facts","Show two read-only fact sections",api.snapshot(WireType("record"),"{}")))
   try{check(plans==1&&contents==1);val source=api.source(e);val s=api.run(e)
    try{val frame=api.next(s,"");check(frame.snapshot.fault.isEmpty());for(text in listOf("Alpha","Beta","Gamma","Delta"))check(frame.snapshot.tree.toString().contains(text)){frame.snapshot.tree.toString()}}
    finally{api.close(s)}
    val loaded=api.load("Catalogue reload",source);try{check(api.source(loaded)==source)}finally{api.forget(loaded)}
   }finally{api.forget(e)}
  }
  // Backend fixture uses a generic catalogue function, not a production template.
  val base=JSONObject(targetContext.assets.open("ui/portable-catalogue.json").bufferedReader().use{it.readText()})
  val fragment=JSONObject().put("kind","key_facts").put("criteria","Generic fixture").put("supportClass","")
   .put("stateFields",JSONArray()).put("initStatements","").put("initEntries","")
   .put("functions","function answerIDX(): number {\n__BODY__\n}")
   .put("viewBlock","{ sections.push(ui.Section(\"content\",\"\",\"\",[ui.MetricGroup(\"default\",1,[ui.NumberStat(\"default\",\"Fixture\",answerIDX(),\"\",\"\",1,\"\",\"chart\",0)])])); }")
   .put("imports",JSONArray()).put("ports",JSONArray()).put("indexed",true).put("owns","").put("tab","")
   .put("holes",JSONArray().put(JSONObject().put("key","answer").put("token","__BODY__").put("signature","answer(): number").put("purpose","Return the disclosed number")
    .put("defaultBody","return 0;").put("when","").put("byModel",true).put("usesSdk",false).put("slot","")))
  val entry=JSONObject().put("fragment",fragment).put("slots",JSONArray()).put("contentFields",JSONArray()).put("listFields",JSONArray()).put("covers",JSONArray()).put("coversIf",JSONArray())
  val data=base.getJSONObject("data");data.put("catalogue",JSONArray().put(entry)).put("alignEnabled",false)
  base.put("request",JSONObject().put("agent",JSONObject().put("prompt","Return 7").put("widget",false).put("scene","").put("sceneAllowed",false)).put("mode","deepseek").put("wholeEnabled",true).put("surfaceEnabled",false)).put("originals",JSONArray())
  val engine=SandboxEngine.acquire(targetContext)
  try{for(mode in listOf("repair","exhaust","partial","provider","cancelled","compiler")){
   val fixture=JSONObject(base.toString())
   val configured=fixture.getJSONObject("data").getJSONArray("catalogue").getJSONObject(0).getJSONObject("fragment")
   if(mode=="partial"){
    configured.put("functions",configured.getString("functions")+"\nfunction otherIDX(): number {\n__OTHER__\n}")
    configured.getJSONArray("holes").put(JSONObject(configured.getJSONArray("holes").getJSONObject(0).toString()).put("key","other").put("token","__OTHER__").put("signature","other(): number"))
   }
   if(mode=="compiler")configured.put("viewBlock",configured.getString("viewBlock").replace("ui.NumberStat(","ui.Unsupported("))
   var code=0;val thinking=mutableListOf<Boolean>()
   PortableRequestBackend(targetContext,config,engine){operation,payload->when(operation){
    "plan"->"""{"title":"Catalogue hole","needs":[{"type":"content","what":"Read-only fixture fact"}]}"""
    "jev"->"""{"answers":{"ready.0":{"choice":"key_facts","confidence":1},"whose.0":{"choice":"written","confidence":1}}}"""
    "content"->"{}"
    "code"->{code++;thinking+=payload.getBoolean("think");if(mode=="provider")throw ProtocolFailure("FIXTURE_OPERATIONAL","catalogue provider fixture")
     if(mode=="cancelled")throw java.util.concurrent.CancellationException("catalogue cancelled")
     val keys=Regex("\\\"((?:answer|other)[0-9]+)\\\": function").findAll(payload.getString("body")).map{it.groupValues[1]}.toList()
     val bodies=JSONObject();for(key in keys)bodies.put(key,if(mode=="partial") {if(key.startsWith("answer"))"return 7;" else "return 7 +;"} else if(mode=="exhaust"||code==1)"return 7 +;" else "return 7;")
     JSONObject().put("holes",bodies).toString()}
    "cancelled"->"{\"value\":false}"
    "status","preview"->""
    "toolCount","toolSpan"->"{\"value\":0}"
    else->error("Unexpected catalogue mechanism: $operation")
   }}.use{backend->
    if(mode in listOf("provider","cancelled","compiler")){
     val error=runCatching{backend.build(fixture.toString())}.exceptionOrNull();check(error!=null&&code<=1){"Operational error must propagate without repair: $error $code"}
     if(mode=="provider")check(error is ProtocolFailure&&error.code=="FIXTURE_OPERATIONAL")
     if(mode=="cancelled")check(error is java.util.concurrent.CancellationException)
     if(mode=="compiler")check(error is deal.codegen.jvm.JvmRuntime.DealError&&error.code=="PORTABLE_UNSUPPORTED")
    }else{
     val result=backend.build(fixture.toString());check(result.getString("kind")=="agent"){result.toString()}
     val agent=result.getJSONObject("agent");check(agent.getString("errors").isEmpty()){agent.toString()};check(agent.getString("executable").isNotEmpty());check(agent.isNull("whole"))
     check(code==if(mode=="repair")2 else 3){"code=$code $agent"};check(thinking==if(mode=="repair")listOf(false,false)else listOf(false,false,true))
     val fill=agent.getJSONObject("blockFill");check(fill.getString("holesNote").contains(if(mode=="repair")"repaired=1" else if(mode=="partial")"partial" else "default")){fill.toString()}
     if(mode=="partial"){val kept=fill.getJSONArray("holeBodies");check(kept.length()==1&&kept.getJSONObject(0).getString("key").startsWith("answer"));check(agent.getString("source").contains("return 7;"))}
     CapabilityRegistry(targetContext,CapabilityDiscovery(targetContext){false},emptyList()).use{registry->
      UiSession(targetContext,registry,engine,config).use{session->
       val mounted=session.mount(backend.output(agent.getString("executable")));check(mounted.getString("fault").isEmpty())
       val expected=if(mode=="exhaust")"0" else "7";check(mounted.getJSONObject("tree").toString().contains("\"stringValue\":\"$expected\"")){mounted.toString()}
      }
     }
    }
   }
  }}finally{SandboxEngine.release(engine)}
  receipt.putString("result","PASS public pinned duplicate catalogue sections/reload; generic catalogue backend repair/exhaustion/thinking/partial retention and provider/cancellation/compiler propagation; no live inference")
  finish(0,receipt)
 }catch(e:Throwable){receipt.putString("failure",android.util.Log.getStackTraceString(e));finish(1,receipt)}}
}
