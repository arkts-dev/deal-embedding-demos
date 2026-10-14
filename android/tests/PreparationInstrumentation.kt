package dev.deal.connectors.tests

import android.app.Instrumentation
import android.os.Bundle
import dev.deal.embedding.*
import dev.deal.embedding.capabilities.*
import org.json.*
import java.util.UUID

/** Real compiler/session actions; read-only fixture capability, no inference or provider writes. */
class PreparationInstrumentation : Instrumentation() {
    private var visual = false
    override fun onCreate(arguments: Bundle?) { visual = arguments?.getString("visual") == "true"; super.onCreate(arguments); start() }
    override fun onStart() {
        val receipt = Bundle()
        try {
            val ctx = targetContext
            val model = object : ModelClient { override fun complete(input: String, previous: String, diagnostics: String, cancellation: GenerationCancellation, log: EmbeddingLog): String = error("Unexpected inference") }
            ApplicationProtocol(ctx, Embedding(ctx, CapabilityRegistry(ctx, CapabilityDiscovery(ctx) { false }, emptyList()), model, EmbeddingConfig("preparation", emptyList(), "prep-test-${UUID.randomUUID()}"))).use { api ->
                val initial = "let tasks: Task[] = []; let one: Task = {id:\"one\",title:\"Get supporting evidence\",chosen:true}; let two: Task = {id:\"two\",title:\"Water plants\"}; tasks[0] = one; tasks[1] = two; return {tasks:tasks,start:900,loaded:true};"
                val deal = ctx.assets.open("preparation/preparation.deal").bufferedReader().use { it.readText() }.replace("function acceptedState(): State { return {}; }", "function acceptedState(): State { $initial }")
                val e = api.load("Preparation", Source(deal, ctx.assets.open("preparation/preparation.dealui").bufferedReader().use { it.readText() }))
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
                if (visual) {
                    java.io.File(ctx.filesDir, "preparation-visual-tree.json").writeText(tree().toString())
                    ctx.startActivity(android.content.Intent(ctx, WorkspaceClarityActivity::class.java).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        .putExtra("screen", "preparation").putExtra("tree", tree().toString()))
                }
                val deadline = System.currentTimeMillis() + 10000
                while (!tree().toString().contains("Get supporting evidence")) { check(System.currentTimeMillis() < deadline); Thread.sleep(20) }
                tap("Update times")
                check(tree().toString().contains("\"intValue\":855")) // 14:15 preparation
                check(tree().toString().contains("\"intValue\":870")) // 14:30 departure
                tap("Edit time for Get supporting evidence", "one")
                tap("Minutes: Get supporting evidence", "30")
                tap("Update times")
                check(tree().toString().contains("\"intValue\":840"))
                tap("Water plants", "two")
                tap("Update times")
                check(tree().toString().contains("\"intValue\":825"))
                tap("Travel minutes (estimate)", "-1")
                check(tree().toString().contains("\"intValue\":25"))
                api.close(session); api.forget(e)
            }
            receipt.putString("preparation", "compiled populated initial plan/select/durations/estimates/departure; no capabilities, inference or writes"); finish(0, receipt)
        } catch (error: Throwable) { receipt.putString("failure", android.util.Log.getStackTraceString(error)); finish(1, receipt) }
    }
}
