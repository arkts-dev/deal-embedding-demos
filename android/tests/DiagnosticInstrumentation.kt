package dev.deal.connectors.tests

import android.app.Instrumentation
import android.os.Bundle
import dev.deal.embedding.*
import dev.deal.embedding.capabilities.*
import java.io.File
import java.util.UUID
import org.json.*

/** Real write and rename failures around compiled activation and subsequent action. */
class DiagnosticInstrumentation:Instrumentation(){
 override fun onCreate(arguments:Bundle?){super.onCreate(arguments);start()}
 override fun onStart(){val result=Bundle();val ctx=targetContext
  val directory=File(ctx.cacheDir,"diagnostic-${UUID.randomUUID()}").apply{mkdirs()}
  try{
   val model=object:ModelClient{override fun complete(input:String,previous:String,diagnostics:String,cancellation:GenerationCancellation,log:EmbeddingLog):String=error("Unexpected inference")}
   val registry=CapabilityRegistry(ctx,CapabilityDiscovery(ctx){false},emptyList())
   ApplicationProtocol(ctx,Embedding(ctx,registry,model,RuntimeConfig("experience",emptyList(),"diagnostic-${UUID.randomUUID()}"))).use{api->
    // Nonempty directory as temporary file makes write fail. As destination it makes rename fail.
    File(directory,"generation-status.json.tmp").mkdirs()
    DiagnosticFiles.write(directory,"generation-status.json","running")
    val source=Source("""export class State {value:number;}
export class Increment {}
export function initialState():State {return {value: 0};}
// @ui-update
export function increment(state:State,action:Increment):State {return {value:state.value+1};}
""","""import * as app from "./experience";
import * as ui from "./platform.dealui-pack";
// @ui-root
export view Main(state:app.State):View {ui.Column(){ui.IntText(value:state.value) ui.Button(text:"Increment",onClick:action app.Increment {})}}
""")
    val prepared=api.load("Diagnostic failure proof",source)
    val session=api.run(prepared)
    DiagnosticFiles.write(directory,"generation-status.json","activated")
    val initial=api.next(session,"")
    File(directory,"ui-snapshot.json").mkdirs();File(directory,"ui-snapshot.json/keep").writeText("occupied")
    DiagnosticFiles.write(directory,"ui-snapshot.json",initial.snapshot.json().toString())
    fun button(node:JSONObject):Int?{
     val props=node.getJSONArray("props")
     for(i in 0 until props.length()){val p=props.getJSONObject(i);if(p.optString("kind")=="action")return p.getInt("actionSlot")}
     val children=node.getJSONArray("children");for(i in 0 until children.length()){button(children.getJSONObject(i))?.let{return it}}
     return null
    }
    api.dispatch(session,button(initial.snapshot.tree)?:error("No action"),null)
    val updated=api.next(session,initial.cursor)
    check(updated.snapshot.version>initial.snapshot.version)
    DiagnosticFiles.write(directory,"ui-snapshot.json",updated.snapshot.json().toString())
    check(api.saved().any{it.id==prepared.id});api.close(session);api.forget(prepared)
   }
   result.putString("diagnostics","write/rename failures do not stop activation or subsequent compiled action");finish(0,result)
  }catch(error:Throwable){result.putString("failure",android.util.Log.getStackTraceString(error));finish(1,result)}finally{directory.deleteRecursively()}
 }
}
