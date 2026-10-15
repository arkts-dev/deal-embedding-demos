package dev.deal.connectors.tests

import android.app.Instrumentation
import android.os.Bundle
import dev.deal.embedding.*
import dev.deal.embedding.discovery.*
import org.json.*
import java.util.concurrent.*

/** Deterministic boundary and race tests: no model or application provider calls. */
class DiscoveryInstrumentation : Instrumentation() {
    override fun onCreate(arguments: Bundle?) { super.onCreate(arguments); start() }
    override fun onStart() {
        val result = Bundle()
        try {
            val input = DiscoveryInput(listOf(DiscoveryFact("event", "Compare approved 12000 EUR with revised 13750 EUR before deciding.")), emptyList())
            val yes = """{"title":"Compare budgets","brief":"Compare approved 12000 EUR and revised 13750 EUR with editable amounts and calculated difference.","factIds":["event"],"capabilityIds":[]}"""
            check(Discovery.parse("null", input) == null)
            val parsed = Discovery.parse(yes, input)!!
            check(parsed.brief.contains("12000 EUR") && parsed.factIds == listOf("event"))
            runCatching { (parsed.factIds as MutableList<String>).clear() }; check(parsed.factIds == listOf("event"))
            for (bad in listOf(yes + " trailing", yes.replace("[\"event\"]", "[\"forged\"]"), yes.replace("\"capabilityIds\":[]", "\"capabilityIds\":[\"host/forged.call\"]"), yes.replace("\"Compare budgets\"", "null"), yes.replace("\"title\":", "\"title\":\"duplicate\",\"title\":"), yes.replace("\"title\"", "'title'"), "{}")) {
                check(runCatching { Discovery.parse(bad, input) }.isFailure) { bad }
            }
            check(runCatching { DiscoveryInput(listOf(DiscoveryFact("event", "x".repeat(2049))), emptyList()) }.isFailure)
            val entered = CountDownLatch(1); val release = CountDownLatch(1)
            Discovery { entered.countDown(); release.await(5, TimeUnit.SECONDS); yes }.use { discovery ->
                val future = discovery.discover(input); check(entered.await(5, TimeUnit.SECONDS)); discovery.cancel(); release.countDown()
                check(runCatching { future.get(5, TimeUnit.SECONDS) }.isFailure)
            }
            Discovery { error("inference unavailable") }.use { discovery ->
                check(runCatching { discovery.discover(input).get(5, TimeUnit.SECONDS) }.exceptionOrNull()?.cause?.message == "inference unavailable")
            }
            Discovery { yes }.use { discovery -> check(discovery.discover(input).get(5, TimeUnit.SECONDS)?.brief == parsed.brief) }
            var reads = 0
            val module = dev.deal.embedding.capabilities.NativeCapabilities(
                dev.deal.embedding.capabilities.CapabilityContract("host/probe", "Explicit read", listOf(
                    dev.deal.embedding.capabilities.CapabilityFunction("read", "Read on demand", emptyList(), dev.deal.embedding.capabilities.CapabilityType("int"))
                )), mapOf("read" to { _: JSONArray -> reads++; 7 }))
            val model = object : ModelClient { override fun complete(input: String, previous: String, diagnostics: String, cancellation: GenerationCancellation, log: EmbeddingLog): String = error("No generation expected") }
            val registry = dev.deal.embedding.capabilities.CapabilityRegistry(targetContext, dev.deal.embedding.capabilities.CapabilityDiscovery(targetContext) { false }, listOf(module))
            ApplicationProtocol(targetContext, Embedding(targetContext, registry, model, EmbeddingConfig("experience", emptyList(), "discovery-admission-test"))).use { api ->
                val captured = DiscoveryInput(listOf(DiscoveryFact("event", "Exact captured evidence")), api.refresh())
                Discovery { yes }.use { d ->
                    val tip = d.discover(captured).get(5, TimeUnit.SECONDS)!!
                    val changed = DiscoveryInput(listOf(DiscoveryFact("event", "Changed evidence")), api.refresh())
                    check(runCatching { d.request(tip, changed, api) }.isFailure)
                    val request = d.request(tip, captured, api)
                    check(api.requestTrace(request).isNotBlank())
                    check(runCatching { d.request(tip, captured, api) }.isFailure)
                    api.cancel(request)
                    val old = d.discover(captured).get(5, TimeUnit.SECONDS)!!
                    d.cancel(); check(runCatching { d.request(old, captured, api) }.isFailure)
                    val beforeChange = d.discover(captured).get(5, TimeUnit.SECONDS)!!
                    val different = dev.deal.embedding.capabilities.NativeCapabilities(module.contract.copy(description = "Changed executable contract"), module.handlers)
                    val changedRegistry = dev.deal.embedding.capabilities.CapabilityRegistry(targetContext, dev.deal.embedding.capabilities.CapabilityDiscovery(targetContext) { false }, listOf(different))
                    ApplicationProtocol(targetContext, Embedding(targetContext, changedRegistry, model, EmbeddingConfig("experience", emptyList(), "discovery-changed-test"))).use { changedApi ->
                        check(runCatching { d.request(beforeChange, captured, changedApi) }.isFailure)
                    }
                }
                check(reads == 0) { "Discovery/admission executed a provider" }
            }
            result.putString("discovery", "PASS reply validation, immutable references, bounds, cancellation/stale fencing, explicit failure, single-use exact-input admission, catalogue invalidation, zero provider reads")
            finish(0, result)
        } catch (error: Throwable) { result.putString("failure", android.util.Log.getStackTraceString(error)); finish(1, result) }
    }
}
