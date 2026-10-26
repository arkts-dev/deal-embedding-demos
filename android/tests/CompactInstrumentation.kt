package dev.deal.connectors.tests

import android.app.Instrumentation
import android.os.Bundle
import dev.deal.embedding.*
import dev.deal.embedding.capabilities.*
import org.json.JSONObject
import java.util.UUID

/** Deterministic compact construction through the real generated-source checker/runtime. */
class CompactInstrumentation : Instrumentation() {
    override fun onCreate(arguments: Bundle?) { super.onCreate(arguments); start() }
    override fun onStart() {
        val receipt = Bundle()
        try {
            fun plan() = JSONObject("""{
              "imports":[{"alias":"sample","module":"host/sample"}],"records":[],
              "state":[{"name":"amount","type":"number","value":"10"},{"name":"reading","type":"number","value":"0"},{"name":"loaded","type":"boolean","value":"false"}],
              "derived":[{"name":"total","type":"number","value":"0","depends":["amount","reading"],"expression":"next.amount + next.reading"}],
              "controls":[{"field":"amount","label":"Amount","format":"integer","minimum":0,"maximum":1000,"step":1,"invalidate":[]}],
              "actions":[],"effects":[{"name":"Load","label":"Read value","body":"let value: number = await sample.value(); next.reading = value; next.loaded = true;"}],
              "functions":"","view":"ui.IntText(value: state.total)"
            }""")
            val modules = setOf("host/sample")
            val pair = CompactCandidate.assemble(plan(), modules)
            check(pair.toString() == CompactCandidate.assemble(plan(), modules).toString())
            fun rejected(p: JSONObject) { check(runCatching { CompactCandidate.assemble(p, modules) }.exceptionOrNull() is CompactCandidate.Rejected) }
            rejected(plan().put("extra", true))
            rejected(plan().put("view", "import * as app from \"./experience\";"))
            rejected(plan().put("functions", "function recompute(state:State):State { return state; }"))
            val large = plan().apply { getJSONArray("controls").getJSONObject(0).put("maximum", 99999999) }
            check(CompactCandidate.assemble(large, modules).getString("dealui").contains("maximum: 99999999,"))
            rejected(plan().apply { getJSONArray("imports").getJSONObject(0).put("module", "host/undisclosed") })
            rejected(plan().apply { getJSONArray("derived").getJSONObject(0).put("depends", org.json.JSONArray("[\"total\"]")) })
            rejected(plan().apply { getJSONArray("controls").getJSONObject(0).put("field", "missing") })
            rejected(plan().apply { getJSONArray("state").getJSONObject(0).put("name", "from") })
            var reads = 0; var fail = false
            val sample = NativeCapabilities(CapabilityContract("host/sample", "Isolated compact test", listOf(CapabilityFunction("value", "Read isolated value", emptyList(), CapabilityType.NumberType))), mapOf("value" to { _ -> reads++; if (fail) throw ProtocolFailure("UNAVAILABLE", "fixture"); 42 }))
            var modelCalls = 0
            val model = object : ModelClient { override fun complete(input: String, previous: String, diagnostics: String, cancellation: GenerationCancellation, log: EmbeddingLog): String {
                modelCalls++
                val body = JSONObject().put("role", "assistant").put("content", "")
                val call = JSONObject().put("id", "compact-$modelCalls").put("type", "function")
                val fn = JSONObject()
                if (modelCalls == 1) {
                    check(previous.isEmpty())
                    fn.put("name", "submit_compact").put("arguments", plan().apply { getJSONArray("derived").getJSONObject(0).put("expression", "next.amount + next.reading +") }.toString())
                } else {
                    check(modelCalls == 2 && diagnostics.isNotEmpty())
                    check(JSONObject(previous).getString("deal").contains("next.amount + next.reading +"))
                    check(input.contains("source"))
                    fn.put("name", "edit_sources").put("arguments", JSONObject().put("edits", org.json.JSONArray().put(JSONObject().put("file", "deal").put("search", "next.amount + next.reading +;").put("replacement", "next.amount + next.reading;"))).toString())
                }
                return body.put("tool_calls", org.json.JSONArray().put(call.put("function", fn))).toString()
            } }
            ApplicationProtocol(targetContext, Embedding(targetContext, CapabilityRegistry(targetContext, CapabilityDiscovery(targetContext) { false }, listOf(sample)), model,
                EmbeddingConfig("experience", emptyList(), "compact-${UUID.randomUUID()}"))).use { api ->
                api.refresh(); api.grant("host/sample")
                val exp = api.generate(api.request("Compact", "Isolated compact fixture", api.snapshot(WireType("record"), "{}")))
                check(modelCalls == 2)
                check(api.source(exp) == Source(pair.getString("deal"), pair.getString("dealui")))
                val session = api.run(exp)
                fun tree() = api.next(session, "").snapshot.tree
                fun nodes(n: JSONObject): List<JSONObject> = listOf(n) + (0 until n.getJSONArray("children").length()).flatMap { nodes(n.getJSONArray("children").getJSONObject(it)) }
                fun slot(component: String): Int = nodes(tree()).first { it.getString("component").endsWith(component) }.getJSONArray("props").let { p -> (0 until p.length()).map { p.getJSONObject(it) }.first { it.optString("kind") == "action" }.getInt("actionSlot") }
                fun await(value: String) { val end = System.currentTimeMillis() + 10000; while (!tree().toString().contains(value)) { check(System.currentTimeMillis() < end) { tree().toString() }; Thread.sleep(20) } }
                check(reads == 0)
                api.dispatch(session, slot("PrimaryAction"), null); await("\"numberValue\":52"); check(reads == 1)
                api.dispatch(session, slot("IntField"), "20"); await("\"numberValue\":62"); check(reads == 1)
                fail = true; api.dispatch(session, slot("PrimaryAction"), null); await("Check access and try again"); check(reads == 2)
                fail = false; api.dispatch(session, slot("PrimaryAction"), null)
                val end = System.currentTimeMillis() + 10000
                while (reads != 3 || tree().toString().contains("Check access and try again")) { check(System.currentTimeMillis() < end); Thread.sleep(20) }
                api.close(session); api.forget(exp)
            }
            receipt.putString("result", "PASS deterministic compact emission, malformed/reference/cycle/import rejection, real compile, explicit async success/failure/retry and local recomputation")
            finish(android.app.Activity.RESULT_OK, receipt)
        } catch (e: Throwable) { receipt.putString("failure", android.util.Log.getStackTraceString(e)); finish(android.app.Activity.RESULT_CANCELED, receipt) }
    }
}
