package dev.deal.connectors.tests

import android.app.Instrumentation
import android.os.Bundle
import dev.deal.embedding.*
import dev.deal.embedding.capabilities.*
import org.json.*
import java.util.UUID

/** Real compiler/session actions; read-only fixture capability, no inference or provider writes. */
class PreparationInstrumentation : Instrumentation() {
    override fun onCreate(arguments: Bundle?) { super.onCreate(arguments); start() }
    override fun onStart() {
        val receipt = Bundle()
        try {
            val ctx = targetContext
            val string = CapabilityType("string", maximum = 120)
            val task = CapabilityType("record", fields = mapOf("id" to string, "title" to string))
            val local = NativeCapabilities(CapabilityContract("host/preparation-plan", "Read-only accepted facts", listOf(CapabilityFunction("snapshot", "Read facts", emptyList(),
                CapabilityType("record", fields = mapOf("tasks" to CapabilityType("array", element = task, maximum = 8), "start" to CapabilityType.IntType, "suggested" to string))))),
                mapOf("snapshot" to { _ -> JSONObject().put("tasks", JSONArray().put(JSONObject().put("id", "one").put("title", "Get supporting evidence")).put(JSONObject().put("id", "two").put("title", "Water plants"))).put("start", 900).put("suggested", "one") }))
            val model = object : ModelClient { override fun complete(input: String, previous: String, diagnostics: String, cancellation: GenerationCancellation, log: EmbeddingLog): String = error("Unexpected inference") }
            ApplicationProtocol(ctx, Embedding(ctx, CapabilityRegistry(ctx, CapabilityDiscovery(ctx) { false }, listOf(local)), model, EmbeddingConfig("preparation", emptyList(), "prep-test-${UUID.randomUUID()}"))).use { api ->
                api.grant("host/preparation-plan")
                val e = api.load("Preparation", Source(ctx.assets.open("preparation/preparation.deal").bufferedReader().use { it.readText() }, ctx.assets.open("preparation/preparation.dealui").bufferedReader().use { it.readText() }))
                val session = api.run(e)
                fun tree() = api.next(session, "").snapshot.tree
                fun slot(node: JSONObject, label: String): Int? {
                    val ps = node.getJSONArray("props")
                    if ((0 until ps.length()).any { ps.getJSONObject(it).optString("stringValue") == label }) {
                        for (i in 0 until ps.length()) if (ps.getJSONObject(i).optString("kind") == "action") return ps.getJSONObject(i).getInt("actionSlot")
                    }
                    val children = node.getJSONArray("children")
                    for (i in 0 until children.length()) slot(children.getJSONObject(i), label)?.let { return it }
                    return null
                }
                fun tap(label: String, payload: String? = null) { api.dispatch(session, slot(tree(), label) ?: error("Missing $label"), payload) }
                tap("Load preparation tasks")
                val deadline = System.currentTimeMillis() + 10000
                while (!tree().toString().contains("Get supporting evidence")) { check(System.currentTimeMillis() < deadline); Thread.sleep(20) }
                tap("Calculate preparation and departure")
                check(tree().toString().contains("\"intValue\":855")) // 14:15 preparation
                check(tree().toString().contains("\"intValue\":870")) // 14:30 departure
                tap("Edit time for Get supporting evidence", "one")
                tap("Minutes: Get supporting evidence", "30")
                tap("Calculate preparation and departure")
                check(tree().toString().contains("\"intValue\":840"))
                tap("Water plants", "two")
                tap("Calculate preparation and departure")
                check(tree().toString().contains("\"intValue\":825"))
                tap("Travel minutes (estimate)", "-1")
                check(tree().toString().contains("\"intValue\":25"))
                api.close(session); api.forget(e)
            }
            receipt.putString("preparation", "compiled load/select/durations/estimates/departure; no inference or writes"); finish(0, receipt)
        } catch (error: Throwable) { receipt.putString("failure", android.util.Log.getStackTraceString(error)); finish(1, receipt) }
    }
}
