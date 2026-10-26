package dev.deal.connectors.tests

import android.app.Instrumentation
import android.os.Bundle
import dev.deal.embedding.*
import dev.deal.embedding.capabilities.*
import org.json.*

/** Real Binder boundary, separate provider process, no inference. */
class RemoteProbeService:CapabilityService(){
    override val capabilities=NativeCapabilities(CapabilityContract("host/remote-probe","Remote echo",listOf(
        CapabilityFunction("echo","Echo integer",listOf(CapabilityParameter("value",CapabilityType.NumberType)),CapabilityType.NumberType)
    )),mapOf("echo" to {args:JSONArray->args.getInt(0)}))
    override fun authorize(uid:Int)=uid==applicationInfo.uid
    override fun consent()=true
}
class RemoteCapabilityInstrumentation:Instrumentation(){
    override fun onCreate(arguments:Bundle?){super.onCreate(arguments);start()}
    override fun onStart(){val result=Bundle()
        try{
            val ctx=targetContext
            val registry=CapabilityRegistry(ctx,CapabilityDiscovery(ctx){it==ctx.applicationInfo.uid},emptyList())
            val model=object:ModelClient{override fun complete(input:String,previous:String,diagnostics:String,cancellation:GenerationCancellation,log:EmbeddingLog):String=error("Unexpected inference")}
            ApplicationProtocol(ctx,Embedding(ctx,registry,model,EmbeddingConfig("experience",emptyList(),"remote-${java.util.UUID.randomUUID()}"))).use{api->
                check(api.refresh().any{it.module=="host/remote-probe"});api.grant("host/remote-probe")
                val session=registry.openSession(EmbeddingLog(ctx))
                try{for(i in 1..1005){
                    session.receive(JSONObject().put("id",i).put("module","host/remote-probe").put("function","echo").put("args",JSONArray().put(i)),android.os.SystemClock.elapsedRealtime())
                    val deadline=System.currentTimeMillis()+10000
                    var reply:JSONObject?=null
                    while(reply==null){reply=session.drain(android.os.SystemClock.elapsedRealtime()).singleOrNull();check(System.currentTimeMillis()<deadline){"Remote request $i timed out"};Thread.sleep(2)}
                    check(reply.getBoolean("ok")&&reply.getInt("value")==i){"Remote request $i failed: $reply"}
                }}finally{session.close()}
            }
            result.putString("remote","1005 real Binder calls, exact replies, one session");finish(0,result)
        }catch(error:Throwable){result.putString("failure",android.util.Log.getStackTraceString(error));finish(1,result)}
    }
}
