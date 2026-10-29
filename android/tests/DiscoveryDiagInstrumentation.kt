package dev.deal.connectors.tests
import android.app.Instrumentation
import android.os.Bundle
import dev.deal.embedding.discovery.*
import org.json.JSONArray
import org.json.JSONObject

import dev.deal.embedding.*
/** Reads the retained validation failure detail for a real reply; diagnosis only. */
class DiscoveryDiagInstrumentation : Instrumentation() {
    private var args: Bundle? = null
    override fun onCreate(bundle: Bundle?) { super.onCreate(bundle); args = bundle; start() }
    override fun onStart() {
        val out = Bundle()
        try {
            val text = args?.getString("reply") ?: error("Missing -e reply")
            val facts = args?.getString("facts") ?: "Synthetic demo task hive-collect: Collect the vocal kit."
            val catalogue = args?.getString("catalogue") ?: "[]"
            val input = DiscoveryFacts(listOf(DiscoveryFact("task", facts)))
            val contracts = (0 until JSONArray(catalogue).length()).map { index ->
                val o = JSONArray(catalogue).getJSONObject(index)
                dev.deal.embedding.CapabilityContract(o.getString("module"), o.getString("description"), functions = listOf(
                    dev.deal.embedding.CapabilityFunction(o.getString("function"), o.getString("description"), result = dev.deal.embedding.WireType("number"))))
            }
            val captured = CapturedDiscoveryFacts(input, contracts)
            out.putString("result", runCatching { DiscoveryPolicy(targetContext).use { it.parse(text, captured) } }.fold({ if (it == null) "NULL" else "PARSED ${it.title}" }, { "${it.javaClass.simpleName}: ${it.message}" }))
            finish(0, out)
        } catch (error: Throwable) { out.putString("failure", android.util.Log.getStackTraceString(error)); finish(1, out) }
    }
}
