package dev.deal.connectors.tests

import android.app.Instrumentation
import android.content.*
import android.os.*
import dev.deal.embedding.capabilities.*
import org.json.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** A same-signer non-demo identity is not a member of the six-app trust set. */
open class CallerInstrumentation : Instrumentation() {
    protected open val provider = "todo"
    override fun onCreate(arguments: Bundle?) { super.onCreate(arguments); start() }
    override fun onStart() {
        val result = Bundle()
        val latch = CountDownLatch(1)
        var denied = false
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, binder: IBinder) {
                try { ICapabilityService.Stub.asInterface(binder).describe() }
                catch (_: SecurityException) { denied = true }
                finally { latch.countDown() }
            }
            override fun onServiceDisconnected(name: ComponentName) {}
        }
        try {
            val service = if (provider == "todo") "TodoService" else "VehicleService"
            check(context.bindService(Intent().setComponent(ComponentName("dev.deal.apps.$provider", "dev.deal.apps.$provider.$service")), connection, Context.BIND_AUTO_CREATE))
            check(latch.await(10, TimeUnit.SECONDS) && denied) { "Provider accepted non-demo test UID" }
            val type = CapabilityType("record", fields = mapOf("nested" to CapabilityType("array", element = CapabilityType.NumberType, maximum = 2)))
            type.validate(JSONObject().put("nested", JSONArray(listOf(1, 2))))
            check(runCatching { type.validate(JSONObject().put("nested", JSONArray(listOf("bad")))) }.isFailure)
            check(runCatching { type.validate(JSONObject().put("nested", JSONArray()).put("extra", true)) }.isFailure)
            check(runCatching { CapabilityType.parse(type.json().put("maximum", 999999)) }.isFailure)
            result.putString("callerAuthentication", "$provider rejected provider UID; typed records validated")
            finish(0, result)
        } catch (error: Throwable) { result.putString("failure", error.toString()); finish(1, result) }
        finally { try { context.unbindService(connection) } catch (_: IllegalArgumentException) {} }
    }
}
class VehicleCallerInstrumentation : CallerInstrumentation() { override val provider = "vehicle" }
