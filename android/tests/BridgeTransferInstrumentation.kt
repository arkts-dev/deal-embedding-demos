package dev.deal.connectors.tests

import android.app.Instrumentation
import android.os.Bundle
import dev.deal.embedding.*
import org.json.JSONObject

/** Transport regression independent of generation policy; no inference. */
class BridgeTransferInstrumentation : Instrumentation() {
 override fun onCreate(arguments:Bundle?){super.onCreate(arguments);start()}
 override fun onStart(){val receipt=Bundle();try{
  val engine=SandboxEngine.acquire(targetContext)
  try{SandboxProgram(engine,512*1024).use{p->
   val text="x".repeat(700000)+"🌍\r\n\u0000"+"z".repeat(700000)
   p.evaluate("globalThis.transferCalls=0; globalThis.largeText=${JSONObject.quote(text)}; ''")
   val actual=p.evaluateString("(transferCalls++,largeText)");check(actual==text);check(p.evaluate("String(transferCalls)")=="1")
   for(small in listOf("","🌍\\r\\n\\u0000", "x".repeat(16384), "y".repeat(16385))){
    val calls=JSONObject(p.metrics().toString()).getLong("calls")
    check(p.evaluateString("(transferCalls++,${JSONObject.quote(small)})")==small)
    val after=JSONObject(p.metrics().toString()).getLong("calls")
    if(small.length<=16384)check(after-calls==1L)
   }
   check(p.evaluate("String(transferCalls)")=="5")
  }}finally{SandboxEngine.release(engine)}
  receipt.putString("result","PASS lossless >512KiB single-evaluation bridge transfer, Unicode and paging boundaries")
  finish(0,receipt)
 }catch(e:Throwable){receipt.putString("failure",android.util.Log.getStackTraceString(e));finish(1,receipt)}}
}
