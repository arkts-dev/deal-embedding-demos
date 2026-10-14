package dev.deal.connectors.tests

import android.app.Instrumentation
import android.os.Bundle
import dev.deal.embedding.*
import dev.deal.embedding.capabilities.*
import org.json.*
import java.util.UUID

/** Actual authored departure source, compiler and effects; no live inference or Calendar writes. */
class CalendarInstrumentation:Instrumentation(){
 override fun onCreate(arguments:Bundle?){super.onCreate(arguments);start()}
 override fun onStart(){val result=Bundle()
 try{
  val ctx=targetContext
  var start=600;var reads=0;var proposals=0;var minute=-1
  val calendar=NativeCapabilities(CapabilityContract("host/calendar","Selected event",listOf(
   CapabilityFunction("start","Event start",emptyList(),CapabilityType("int",minimumInt=0,maximumInt=1439)),
   CapabilityFunction("propose","Native review",listOf(CapabilityParameter("minute",CapabilityType("int",minimumInt=0,maximumInt=1439))),CapabilityType.NullType)
  )),mapOf("start" to {_:JSONArray->reads++;start},"propose" to {args:JSONArray->proposals++;minute=args.getInt(0);JSONObject.NULL}))
  val trip=NativeCapabilities(CapabilityContract("host/trip","Simulation",listOf(CapabilityFunction("estimate","5 + 2*km",listOf(CapabilityParameter("distanceKm",CapabilityType("int",minimumInt=1,maximumInt=1000))),CapabilityType("int",minimumInt=7,maximumInt=2005)))),mapOf("estimate" to {args:JSONArray->reads++;5+2*args.getInt(0)}))
  val model=object:ModelClient{override fun complete(input:String,previous:String,diagnostics:String,cancellation:GenerationCancellation,log:EmbeddingLog):String=error("Unexpected inference")}
  ApplicationProtocol(ctx,Embedding(ctx,CapabilityRegistry(ctx,CapabilityDiscovery(ctx){false},listOf(calendar,trip)),model,EmbeddingConfig("departure",emptyList(),"calendar-test-${UUID.randomUUID()}"))).use{api->
   val source=Source(ctx.assets.open("experience/departure.deal").bufferedReader().use{it.readText()},ctx.assets.open("experience/departure.dealui").bufferedReader().use{it.readText()})
   val e=api.load("Departure",source);check(reads==0&&proposals==0)
   val session=api.run(e);var update=api.next(session,"");check(reads==0&&proposals==0)
   fun action(node:JSONObject,label:String):Int?{
    val props=node.getJSONArray("props");if((0 until props.length()).any{props.getJSONObject(it).optString("stringValue")==label}){
     return (0 until props.length()).map{props.getJSONObject(it)}.first{it.optString("kind")=="action"}.getInt("actionSlot")
    }
    val children=node.getJSONArray("children");for(i in 0 until children.length())action(children.getJSONObject(i),label)?.let{return it};return null
   }
   fun tap(label:String){api.dispatch(session,action(update.snapshot.tree,label)?:error("Missing $label"),null);update=api.next(session,"")}
   fun calculated(){val end=System.currentTimeMillis()+10000;while(!update.snapshot.tree.toString().contains("Review before saving.")&&!update.snapshot.tree.toString().contains("no reminder prepared.")&&!update.snapshot.tree.toString().contains("Let’s reconnect")){check(System.currentTimeMillis()<end){update.snapshot.json().toString()};Thread.sleep(10);update=api.next(session,"")}}
   api.grant("host/calendar");api.grant("host/trip")
   tap("Calculate departure");calculated();check(minute==566&&proposals==1&&reads==2)
   tap("Add 5 minutes");tap("Add 2 kilometres");tap("Calculate departure");calculated();check(minute==557&&proposals==2)
   start=10;tap("Calculate departure");calculated();check(proposals==2&&update.snapshot.tree.toString().contains("Earlier day required"))
   start=600;api.revoke("host/trip");tap("Calculate departure");calculated();check(proposals==2&&update.snapshot.tree.toString().contains("Let’s reconnect"));api.grant("host/trip")
   // Saturate existing increment controls, verify they cannot exceed provider bounds.
   repeat(510){tap("Add 2 kilometres")};repeat(300){tap("Add 5 minutes")}
   check(update.snapshot.tree.toString().contains("\"intValue\":1000")&&update.snapshot.tree.toString().contains("\"intValue\":1435"))
   tap("Calculate departure");calculated();check(proposals==2)
   api.close(session);api.forget(e)
  }
  result.putString("calendar","checked template; no preparation reads; exact calculation, edits, bounds, earlier-day rejection and revoke recovery");finish(0,result)
 }catch(error:Throwable){result.putString("failure",android.util.Log.getStackTraceString(error));finish(1,result)}
 }
}
