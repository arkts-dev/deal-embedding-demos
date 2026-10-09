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
            val directory = EmbeddingLog.directory(context)
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
            result.putString("logging", "DEAL schema; independent Android sink; opt-in artifacts; truncation; parent spans; no raw content in events")
            finish(0, result)
        } catch (error: Throwable) { result.putString("failure", android.util.Log.getStackTraceString(error)); finish(1, result) }
        finally { EmbeddingLog.capture(targetContext, false) }
    }
}
