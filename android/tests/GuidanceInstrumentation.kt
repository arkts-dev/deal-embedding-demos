package dev.deal.connectors.tests

import android.app.Instrumentation
import android.os.Bundle
import dev.deal.embedding.*
import dev.deal.embedding.capabilities.*
import org.json.JSONObject
import java.util.UUID

/** The exact packaged guidance example, with real compilation and async execution. */
class GuidanceInstrumentation : Instrumentation() {
    override fun onCreate(arguments: Bundle?) { super.onCreate(arguments); start() }
    override fun onStart() {
        val receipt = Bundle()
        try {
            val ctx = targetContext
            var calls = 0
            var fail = false
            val sample = NativeCapabilities(CapabilityContract("host/sample", "Example-only read fixture", listOf(CapabilityFunction("value", "Read fixture value", emptyList(), CapabilityType.IntType))), mapOf("value" to { _ -> calls++; if (fail) throw ProtocolFailure("UNAVAILABLE", "Fixture failure"); 42 }))
            val model = object : ModelClient { override fun complete(input: String, previous: String, diagnostics: String, cancellation: GenerationCancellation, log: EmbeddingLog): String = error("Unexpected inference") }
            ApplicationProtocol(ctx, Embedding(ctx, CapabilityRegistry(ctx, CapabilityDiscovery(ctx) { false }, listOf(sample)), model, EmbeddingConfig("experience", emptyList(), "guidance-${UUID.randomUUID()}"))).use { api ->
                api.refresh(); api.grant("host/sample")
                val source = Source(ctx.assets.open("guidance-example/generation-example.deal").bufferedReader().use { it.readText() }, ctx.assets.open("guidance-example/generation-example.dealui").bufferedReader().use { it.readText() })
                api.withPreparation(api.load("Guidance example", source)) { experience ->
                    val session = api.run(experience)
                    fun tree() = api.next(session, "").snapshot.tree
                    fun action(node: JSONObject): Int? {
                        val props = node.getJSONArray("props")
                        if ((0 until props.length()).any { props.getJSONObject(it).optString("stringValue") == "Read value" }) {
                            return (0 until props.length()).map { props.getJSONObject(it) }.first { it.optString("kind") == "action" }.getInt("actionSlot")
                        }
                        val children = node.getJSONArray("children")
                        for (i in 0 until children.length()) action(children.getJSONObject(i))?.let { return it }
                        return null
                    }
                    fun read() { api.dispatch(session, action(tree()) ?: error("Read control missing"), null) }
                    fun await(text: String) {
                        val end = System.currentTimeMillis() + 10000
                        while (!tree().toString().contains(text)) { check(System.currentTimeMillis() < end); Thread.sleep(20) }
                    }
                    check(calls == 0) { "Example read without explicit action" }
                    read(); await("\"intValue\":42"); check(calls == 1)
                    fail = true; read(); await("Check access and try again"); check(calls == 2)
                    fail = false; read()
                    val end = System.currentTimeMillis() + 10000
                    while (calls != 3 || tree().toString().contains("Check access and try again")) { check(System.currentTimeMillis() < end); Thread.sleep(20) }
                    check(tree().toString().contains("\"intValue\":42"))
                    api.close(session); api.forget(experience)
                }
            }
            // Exact checked source + source-bound draft reopen, not a rendered-text rewrite.
            ApplicationProtocol(ctx, Embedding(ctx, CapabilityRegistry(ctx, CapabilityDiscovery(ctx) { false }, emptyList()), model,
                EmbeddingConfig("experience", emptyList(), "currency-${UUID.randomUUID()}", retainState = true))).use { api ->
                val source = Source("""export class State {amount:string="€20.00";}
export function initialState():State {return {};}
""", """import * as app from "./experience";
import * as ui from "./platform.dealui-pack";
// @ui-root
export view Main(state:app.State):View {ui.Column(){ui.Text(value:state.amount)}}
""")
                val experience = api.load("Unicode currency fixture", source)
                repeat(2) {
                    val session = api.run(experience)
                    check(api.next(session, "").snapshot.tree.toString().contains("€20.00"))
                    api.close(session)
                }
                check(api.source(experience) == source)
                api.forget(experience)
            }
            receipt.putString("guidance", "exact authored example compiled; no init read; explicit success/failure/retry; no inference")
            finish(0, receipt)
        } catch (error: Throwable) { receipt.putString("failure", android.util.Log.getStackTraceString(error)); finish(1, receipt) }
    }
}
