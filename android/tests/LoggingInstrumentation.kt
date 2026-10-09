package dev.deal.connectors.tests

import android.app.Instrumentation
import android.os.Bundle
import dev.deal.embedding.EmbeddingLog
import org.json.JSONObject
import java.io.File

/** Exercises compiled DEAL logger on its own isolate, outside application lifetime. */
class LoggingInstrumentation : Instrumentation() {
    override fun onCreate(arguments: Bundle?) { super.onCreate(arguments); start() }
    override fun onStart() {
        val result = Bundle()
        try {
            val context = targetContext
            val log = EmbeddingLog(context)
            EmbeddingLog.capture(context, false)
            log.event("host", "started", detail = "PRIVATE_SENTINEL")
            EmbeddingLog.flush(context)
            val directory = File(context.filesDir, "embedding-log")
            fun events() = listOf("previous.jsonl", "events.jsonl").flatMap { name -> File(directory, name).takeIf { it.exists() }?.readLines().orEmpty() }.map { JSONObject(it) }
            check(events().last { it.getString("trace") == log.run }.getString("content") == "omitted")
            EmbeddingLog.capture(context, true)
            log.operation("host", detail = "PRIVATE_SENTINEL") { child -> child.event("compiler-ui", "completed", detail = "payload") }
            log.event("host", "response", detail = "x".repeat(300000))
            EmbeddingLog.flush(context)
            val own = events().filter { it.getString("trace") == log.run }
            check(own.none { it.toString().contains("PRIVATE_SENTINEL") })
            val captured = own.first { it.getString("content") == "captured" }
            check(File(directory, captured.getString("artifact") + ".json").readText() == "PRIVATE_SENTINEL")
            val started = own.last { it.getString("stage") == "host" && it.getString("outcome") == "started" }
            check(own.any { it.getString("parent") == started.getString("span") })
            check(own.any { it.getString("content") == "truncated" })
            EmbeddingLog.capture(context, false)
            val threads = (1..4).map { n -> Thread { repeat(10) { log.event("host", "completed", operation = "$n:$it") } }.apply { start() } }
            threads.forEach { it.join() }; EmbeddingLog.flush(context)
            check(events().count { it.getString("trace") == log.run && it.getString("operation").contains(":") } == 40)
            // Freeze only the writer through test reflection; no production fault-injection API.
            val instanceField = EmbeddingLog::class.java.getDeclaredField("instance").apply { isAccessible = true }
            val sink = instanceField.get(null)
            val writer = sink.javaClass.getDeclaredField("executor").apply { isAccessible = true }.get(sink) as java.util.concurrent.ExecutorService
            val entered = java.util.concurrent.CountDownLatch(1); val resume = java.util.concurrent.CountDownLatch(1)
            writer.execute { entered.countDown(); resume.await(10, java.util.concurrent.TimeUnit.SECONDS) }
            check(entered.await(5, java.util.concurrent.TimeUnit.SECONDS))
            repeat(400) { log.event("host", "completed", operation = "overflow-$it") }
            resume.countDown(); EmbeddingLog.flush(context)
            check(events().any { it.optString("stage") == "logger" && it.optString("outcome") == "dropped" && it.getInt("bytes") >= 144 })
            // Physical filesystem failure, not a mocked success: logger failure must preserve block outcome.
            val eventFile = File(directory, "events.jsonl")
            val backup = File(directory, "test-events-backup")
            check(eventFile.renameTo(backup)); check(eventFile.mkdir())
            val value = log.operation("host") { 42 }
            check(value == 42)
            val original = IllegalStateException("original application failure")
            try { log.operation("host") { throw original } } catch (error: Throwable) { check(error === original) }
            EmbeddingLog.flush(context)
            var health = JSONObject(File(directory, "health.json").readText())
            check(health.getBoolean("active") && health.getString("code") == "LOGGER_FAILED")
            check(eventFile.delete()); check(backup.renameTo(eventFile))
            log.event("host", "completed", operation = "after-storage-recovery")
            EmbeddingLog.flush(context)
            health = JSONObject(File(directory, "health.json").readText())
            check(!health.getBoolean("active") && health.getBoolean("incomplete") && health.has("recoveredAt"))
            check(events().any { it.optString("operation") == "after-storage-recovery" })
            result.putString("logging", "DEAL schema; private artifacts; concurrent admission; real storage failure preserves results/errors; recovery retains incomplete history")
            finish(0, result)
        } catch (error: Throwable) { result.putString("failure", android.util.Log.getStackTraceString(error)); finish(1, result) }
        finally { EmbeddingLog.capture(targetContext, false) }
    }
}
