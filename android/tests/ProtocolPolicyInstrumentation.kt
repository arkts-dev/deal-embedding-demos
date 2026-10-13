package dev.deal.connectors.tests

import android.app.Instrumentation
import android.os.Bundle
import dev.deal.embedding.*
import dev.deal.embedding.capabilities.*
import org.json.*

/** Real compiled consumer/core/app policy/checker/session. Deterministic inference fixtures,
 * not live provider reliability evidence. No application capability call during preparation.
 */
class ProtocolPolicyInstrumentation:Instrumentation(){
 override fun onCreate(arguments:Bundle?){super.onCreate(arguments);start()}
 override fun onStart(){val result=Bundle()
 try {
  val context=targetContext
  val detail=JSONObject().put("requirement","Boom stand").put("specification","Adjustable boom").put("quantity",1)
   .put("from","2026-10-16T14:15:00Z").put("until","2026-10-16T20:30:00Z").put("searchTerms",JSONArray().put("Boom").put("stand")).toString()
  val deal="""export class State { value:int=0; }
export class Increment {}
export function initialState():State {return {};}
// @ui-update
export function increment(state:State,action:Increment):State {return {value:state.value+1};}
"""
  val view="""import * as app from "./experience";
import * as ui from "./platform.dealui-pack";
// @ui-root
export view Main(state:app.State):View {ui.Column(){ui.Text(value:"Fixture source") ui.Button(text:"Increment",onClick:action app.Increment {})}}
"""
  var applicationCalls=0
  val read=NativeCapabilities(CapabilityContract("host/probe","Equipment fixture",listOf(CapabilityFunction("equipment","Explicit equipment read",listOf(CapabilityParameter("from",CapabilityType("string")),CapabilityParameter("until",CapabilityType("string"))),CapabilityType("array",maximum=32,element=CapabilityType("record",fields=mapOf("id" to CapabilityType("string"),"name" to CapabilityType("string"),"specification" to CapabilityType("string"),"cents" to CapabilityType.IntType,"available" to CapabilityType.IntType,"included" to CapabilityType("string"),"category" to CapabilityType("string"))))))),mapOf("equipment" to {_:JSONArray->applicationCalls++;JSONArray()}))
  val prepare=NativeCapabilities(CapabilityContract("host/organizer","Native preparation fixture",listOf(CapabilityFunction("stage","Prepare for review",listOf("kind","provider","title","detail","price","deadline").map{CapabilityParameter(it,CapabilityType("string"))},CapabilityType("record",fields=mapOf("staged" to CapabilityType("boolean"),"reference" to CapabilityType("string")))))),mapOf("stage" to {_:JSONArray->applicationCalls++;JSONObject().put("staged",true).put("reference","fixture")}))
  fun choices(unsupported:Boolean=false,invalid:Boolean=false):String {
   val selected=JSONObject().put("purpose",if(unsupported)"unavailable" else "compare").put("headline",if(invalid)"forged" else "text").put("notice","none").put("read","read0")
   return JSONObject().put("answers",selected).toString()
  }
  fun scenario(name:String,answer:String?,failure:Boolean=false):Pair<Int,Int> {
   var choices=0;var sources=0
   val model=object:ModelClient {override fun complete(input:String,previous:String,diagnostics:String,cancellation:GenerationCancellation,log:EmbeddingLog):String {
    cancellation.check()
    if(input.contains("\n\nISSUED OPTIONS\n")){choices++;if(failure)throw ProtocolFailure("PROVIDER_UNREACHABLE","Fixture provider unavailable");return answer!!}
    sources++;return JSONObject().put("deal",deal).put("dealui",view).toString()
   }}
   val host=Embedding(context,CapabilityRegistry(context,CapabilityDiscovery(context){false},listOf(read,prepare)),model,EmbeddingConfig("experience",emptyList(),"protocol-policy-$name",policyAssets=if(answer==null)"" else "organizer-policy"))
   ProtocolController(context,host).use{controller->
    controller.call("prepareCapabilities")
    val outcome=runCatching{controller.call("equipment",JSONArray().put("Compare suitable equipment").put(detail))}
    if(failure){check(outcome.exceptionOrNull().let{it is ProtocolFailure&&it.code=="PROVIDER_UNREACHABLE"});check(sources==0)}
    else {
     val e=outcome.getOrThrow() as JSONObject
     val catalogue=answer!=null&&sources==0
     check(e.getString("origin")==if(catalogue)"catalogue" else "generated")
     check(applicationCalls==0){"Policy/generation read a provider"}
     check((controller.call("saved") as JSONArray).length()==0){"Generation saved prematurely"}
     val source=controller.call("exactSource",JSONArray().put(e.getString("id"))) as JSONObject
     if(catalogue){check(source.getString("deal").contains("2026-10-16T14:15:00Z"));check(source.getString("deal").contains("host/probe"))}
     val s=controller.call("run",JSONArray().put(e.getString("id"))) as JSONObject
     controller.call("poll",JSONArray().put(s.getString("id")))
     check(applicationCalls==0){"Mount read a provider"}
     controller.call("close",JSONArray().put(s.getString("id")))
     controller.call("forget",JSONArray().put(e.getString("id")))
    }
   }
   return choices to sources
  }
  check(scenario("catalogue",choices())==1 to 0)
  check(scenario("unsupported",choices(unsupported=true))==1 to 1)
  check(scenario("invalid",choices(invalid=true))==1 to 1)
  check(scenario("absent",null)==0 to 1)
  check(scenario("provider-failure",choices(),true)==1 to 0)
  var invalidChoices=0;var invalidSources=0
  val invalidModel=object:ModelClient{override fun complete(input:String,previous:String,diagnostics:String,cancellation:GenerationCancellation,log:EmbeddingLog):String {if(input.contains("ISSUED OPTIONS")){invalidChoices++;return "{\"answers\":{\"choose\":\"yes\"}}"};invalidSources++;error("Invalid catalogue source must never reach source inference")}}
  val invalidHost=Embedding(context,CapabilityRegistry(context,CapabilityDiscovery(context){false},emptyList()),invalidModel,EmbeddingConfig("experience",emptyList(),"protocol-invalid-catalogue",policyAssets="protocol-invalid-policy"))
  ProtocolController(context,invalidHost).use {c->val error=runCatching{c.call("generated")}.exceptionOrNull();check(error is ProtocolFailure&&error.code=="GENERATION_REJECTED");check(invalidChoices==1&&invalidSources==0)}
  // Typed-answer coercion must reject, never gain authority from confidence.
  val questions=JSONArray().put(JSONObject().put("name","purpose").put("instructions","Choose").put("options",JSONArray().put(JSONObject().put("name","1").put("description","One"))))
  val malformed=JSONObject().put("answers",JSONObject().put("purpose","1")).put("typedAnswers",JSONArray().put(JSONObject().put("question","purpose").put("choice",1).put("probabilities",JSONArray()).put("confidence",1)))
  check(!ChoiceProtocol.answers(malformed.toString(),questions).getBoolean("accepted"))
  val lowConfidence=JSONObject(malformed.toString());lowConfidence.getJSONArray("typedAnswers").getJSONObject(0).put("choice","1").put("confidence",0)
  check(ChoiceProtocol.answers(lowConfidence.toString(),questions).getBoolean("accepted"))
  result.putString("protocolPolicyProof","compiled DEAL public consumer + Organizer policy: catalogue 1/0, unsupported 1/1, invalid choice 1/1, absent 0/1, provider failure 1/0; zero application calls during generation/mount; typed coercion rejected; zero confidence not authority; fixture inference, no network")
  finish(0,result)
 }catch(e:Throwable){result.putString("failure",android.util.Log.getStackTraceString(e));finish(1,result)}
 }
}
