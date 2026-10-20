package dev.deal.connectors.tests

import android.app.Instrumentation
import android.content.Context
import android.content.ContextWrapper
import android.os.Bundle
import dev.deal.embedding.discovery.DiscoveryFact
import dev.deal.shell.Connections
import dev.deal.shell.TaskHelp
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

/** Deterministic native policy checks; no model calls or changes to app consent. */
class CleanlinessInstrumentation : Instrumentation() {
    override fun onCreate(arguments: Bundle?) { super.onCreate(arguments); start() }
    override fun onStart() {
        val result = Bundle()
        val prefix = "cleanliness-${UUID.randomUUID()}-"
        val ctx = object : ContextWrapper(targetContext) {
            override fun getPackageName() = "dev.deal.apps.calendar"
            override fun getSharedPreferences(name: String, mode: Int) = super.getSharedPreferences(prefix + name, mode)
        }
        var help: TaskHelp? = null
        try {
            val shared = Connections.settings(ctx)
            check(shared.edit().putBoolean("generation", false).putBoolean("discovery", false).commit())
            check(!Connections.settings(ctx).getBoolean("generation", true))
            check(!Connections.settings(ctx).getBoolean("discovery", true))
            val attempts = AtomicInteger()
            runOnMainSync {
                help = TaskHelp(targetContext, { listOf(DiscoveryFact("fixture", "Synthetic policy fixture")) }, {
                    attempts.incrementAndGet(); error("Injected bootstrap failure")
                })
                help!!.resume()
            }
            fun awaitFailure() {
                val end = System.currentTimeMillis() + 10000
                while (true) {
                    var failed = false
                    runOnMainSync { failed = help!!.failed && !help!!.busy }
                    if (failed) return
                    check(System.currentTimeMillis() < end); Thread.sleep(20)
                }
            }
            awaitFailure(); check(attempts.get() == 1)
            runOnMainSync { help!!.pause(); help!!.resume() }
            awaitFailure(); check(attempts.get() == 1) { "Returning from Inspect silently retried failure" }
            runOnMainSync { help!!.retry() }
            awaitFailure(); check(attempts.get() == 2) { "Explicit retry did not run exactly once" }
            dev.deal.shell.AssistanceShell.reset(ctx, emptyList())
            runOnMainSync { check(!help!!.failed && !help!!.busy); help!!.resume() }
            awaitFailure(); check(attempts.get() == 3) { "Confirmed reset did not release failed in-memory attempt" }
            result.putString("cleanliness", "PASS shared settings preserve explicit OFF; failed assistance survives pause/resume without retry; explicit retry runs once; explicit shell reset releases in-memory failure")
            finish(0, result)
        } catch (error: Throwable) { result.putString("failure", android.util.Log.getStackTraceString(error)); finish(1, result) }
        finally {
            runOnMainSync { help?.close() }
            listOf("hive").forEach { targetContext.deleteSharedPreferences(prefix + it) }
        }
    }
}
