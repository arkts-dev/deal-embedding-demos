package dev.deal.connectors.tests

import android.app.Instrumentation
import android.os.Bundle
import dev.deal.embedding.*
import dev.deal.embedding.capabilities.*
import org.json.*

/** Deterministic device regression of the Deal UI JS runtime; no inference or providers. */
class UiLifecycleInstrumentation : Instrumentation() {
    override fun onCreate(arguments: Bundle?) { super.onCreate(arguments); start() }
    override fun onStart() {
        val result = Bundle()
        var runtime: ExperienceRuntime? = null
        try {
            val contract = CapabilityContract("host/work", "Controlled lifecycle fixture", listOf(CapabilityFunction("run", "Deferred integer", listOf(CapabilityParameter("value", CapabilityType.IntType)), CapabilityType.IntType)))
            class Stream : CapabilitySession {
                override val identity = java.util.UUID.randomUUID().toString()
                val requests = mutableListOf<JSONObject>()
                val replies = mutableListOf<JSONObject>()
                var closed = false
                override fun receive(request: JSONObject, now: Long) { check(!closed); requests.add(request) }
                override fun drain(now: Long): List<JSONObject> = replies.toList().also { replies.clear() }
                fun reply(id: Int, value: Int? = null) {
                    replies.add(JSONObject().put("id", id).put("ok", value != null).also {
                        if (value != null) it.put("value", value)
                        else it.put("error", JSONObject().put("code", "FAILED").put("message", "Injected effect failure"))
                    })
                }
                override fun close() { closed = true; replies.clear() }
            }
            val streams = mutableListOf<Stream>()
            val broker = object : CapabilityHost { override fun openSession(log: dev.deal.embedding.EmbeddingLog): CapabilitySession = Stream().also { streams.add(it) } }
            val source = ExperienceSource(context.assets.open("ui-lifecycle/app.deal").bufferedReader().use { it.readText() }, context.assets.open("ui-lifecycle/app.dealui").bufferedReader().use { it.readText() })
            runtime = ExperienceRuntime(targetContext, broker, EmbeddingConfig("app", listOf(contract), "ui-lifecycle-test"))
            val host = runtime
            host.activate(source)
            val stream = streams.single()
            fun snapshot() = host.poll()!!
            fun dispatch(label: String) {
                val children = snapshot().getJSONObject("tree").getJSONArray("children")
                val button = (0 until children.length()).map { children.getJSONObject(it) }.first { node ->
                    val props = node.getJSONArray("props")
                    (0 until props.length()).any { props.getJSONObject(it).optString("stringValue") == label }
                }
                val props = button.getJSONArray("props")
                host.dispatch((0 until props.length()).map { props.getJSONObject(it) }.first { it.getString("name") == "onClick" }.getInt("actionSlot"), null)
            }
            fun value(snapshot: JSONObject): Int = snapshot.getJSONObject("tree").getJSONArray("children").getJSONObject(0).getJSONArray("props").getJSONObject(0).getInt("intValue")
            fun waitVersion(version: Int): JSONObject {
                repeat(100) { val snap = snapshot(); if (snap.getInt("version") >= version) return snap; Thread.sleep(10) }
                error("Completion did not publish version $version")
            }
            dispatch("Start 1"); dispatch("Start 2")
            check(stream.requests.size == 2)
            val replacement = runCatching { host.activate(source) }.exceptionOrNull()
            check(replacement?.message?.contains("Wait for current work") == true)
            check(streams.size == 1 && !stream.closed)
            stream.reply(stream.requests[1].getInt("id"), 2); check(value(waitVersion(4)) == 2)
            stream.reply(stream.requests[0].getInt("id"), 1); check(value(waitVersion(5)) == 21)
            val before = snapshot(); dispatch("Bad"); val bad = snapshot()
            check(value(bad) == 21 && bad.getInt("version") == before.getInt("version") && bad.getString("fault").isNotEmpty())
            dispatch("Start 1"); stream.reply(stream.requests.last().getInt("id")); val failed = waitVersion(7)
            check(failed.getString("fault") == "Injected effect failure" && value(failed) == 21)
            host.activate(source); check(stream.closed && value(snapshot()) == 21)
            dispatch("Start 1"); val last = streams.last(); host.close(); check(last.closed)
            result.putString("uiLifecycleVerification", "overlapping effects, arrival-order commits, replacement blocked until physical exit, atomic update failure, effect fault/version, state restoration and disposal; no inference")
            finish(0, result)
        } catch (error: Throwable) { result.putString("failure", android.util.Log.getStackTraceString(error)); finish(1, result) }
        finally { runtime?.close() }
    }
}
