package dev.deal.connectors.tests

import android.app.Instrumentation
import android.content.Intent
import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import dev.deal.embedding.*
import dev.deal.embedding.capabilities.*

/** Actual Compose surface and accessibility clicks, with authored source and no inference. */
class SurfaceTestActivity:ComponentActivity(){
    companion object {lateinit var surface:ExperienceSurface}
    var attached by mutableStateOf(true)
    override fun onCreate(state:Bundle?){super.onCreate(state);setContent{MaterialTheme{if(attached)ExperienceSurface(surface)else Text("Detached")}}}
}
class SurfaceInstrumentation:Instrumentation(){
    override fun onCreate(arguments:Bundle?){super.onCreate(arguments);start()}
    override fun onStart(){val result=Bundle();var protocol:ApplicationProtocol?=null;var surface:ExperienceSurface?=null;var activity:SurfaceTestActivity?=null
        try{
            val context=targetContext
            val model=object:ModelClient{override fun complete(input:String,previous:String,diagnostics:String,cancellation:GenerationCancellation,log:EmbeddingLog):String=error("Unexpected inference")}
            protocol=ApplicationProtocol(context,Embedding(context,CapabilityRegistry(context,CapabilityDiscovery(context){false},emptyList()),model,EmbeddingConfig("experience",emptyList(),"surface-${java.util.UUID.randomUUID()}")))
            val source=Source("""export class State { value:int=0; }
export class Increment {}
export function initialState():State {return {};}
// @ui-update
export function increment(state:State,action:Increment):State {return {value:state.value+1};}
""","""import * as app from "./experience";
import * as ui from "./platform.dealui-pack";
// @ui-root
export view Main(state:app.State):View {ui.Column(){ui.IntText(value:state.value) ui.Button(text:"Surface increment",accessibilityLabel:"Surface increment",onClick:action app.Increment {})}}
""")
            val experience=protocol.load("Surface proof",source)
            val errors=java.util.concurrent.CopyOnWriteArrayList<Throwable>()
            surface=ExperienceSurface(protocol,experience);surface.activate{errors.add(it)}
            SurfaceTestActivity.surface=surface
            activity=startActivitySync(Intent(context,SurfaceTestActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as SurfaceTestActivity
            fun until(check:()->Boolean){val end=System.currentTimeMillis()+10000;while(!check()){check(System.currentTimeMillis()<end){"Surface condition timed out"};Thread.sleep(20)}}
            fun click(node:AccessibilityNodeInfo?):Boolean {
                if(node==null)return false
                if(node.text?.toString()=="Surface increment"||node.contentDescription?.toString()=="Surface increment"){
                    var target:AccessibilityNodeInfo?=node
                    while(target!=null){if(target.isClickable)return target.performAction(AccessibilityNodeInfo.ACTION_CLICK);target=target.parent}
                }
                for(i in 0 until node.childCount)if(click(node.getChild(i)))return true
                return false
            }
            until{click(uiAutomation.rootInActiveWindow)}
            until{surface.snapshot?.tree?.toString()?.contains("\"intValue\":1")==true}
            EmbeddingLog.flush(context)
            until{protocol.events("","",200).events.any{it.optString("outcome")=="published"&&it.optInt("version")==1}}
            runOnMainSync{activity.attached=false};waitForIdleSync()
            check(protocol.saved().any{it.id==experience.id})
            runOnMainSync{activity.attached=true};waitForIdleSync()
            until{click(uiAutomation.rootInActiveWindow)}
            until{surface.snapshot?.tree?.toString()?.contains("\"intValue\":2")==true}
            check(errors.isEmpty())
            val forged=ExperienceSurface(protocol,experience.copy(id="forged"))
            check(runCatching{forged.activate{errors.add(it)}}.isFailure);forged.close()
            check(surface.snapshot!!.tree.toString().contains("\"intValue\":2"))
            surface.close();check(protocol.saved().any{it.id==experience.id})
            val cold=ExperienceSurface(protocol,experience);cold.activate{errors.add(it)}
            until{cold.snapshot!=null};check(cold.snapshot!!.tree.toString().contains("\"intValue\":0"));cold.close();protocol.forget(experience)
            result.putString("surface","actual Compose rendering/accessibility widget events, automatic publication, hide/show retains state, failed activation preserves existing surface, explicit close retains source, cold activation resets without inference")
            finish(0,result)
        }catch(error:Throwable){result.putString("failure",android.util.Log.getStackTraceString(error));finish(1,result)}finally{activity?.let{runOnMainSync{it.finish()}};surface?.close();protocol?.close()}
    }
}
