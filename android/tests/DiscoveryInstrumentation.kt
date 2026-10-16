package dev.deal.connectors.tests

import android.app.Instrumentation
import android.os.Bundle
import dev.deal.embedding.*
import dev.deal.embedding.discovery.*
import org.json.*
import java.util.concurrent.*

/** Deterministic protocol-bound discovery; injected inference is not live Nano evidence. */
class DiscoveryInstrumentation : Instrumentation() {
    override fun onCreate(arguments: Bundle?) { super.onCreate(arguments); start() }
    override fun onStart() {
        val result = Bundle()
        try {
            val input = DiscoveryInput(listOf(DiscoveryFact("event", "Compare approved 12000 EUR with revised 13750 EUR before deciding.")))
            val capturedInput = CapturedDiscoveryInput(input, emptyList())
            val yes = """{"title":"Compare budgets","brief":"Compare approved 12000 EUR and revised 13750 EUR with editable amounts and calculated difference.","factIds":["event"],"capabilityIds":[]}"""
            check(Discovery.parse("null", capturedInput) == null)
            val parsed = Discovery.parse(yes, capturedInput)!!
            check(parsed.brief.contains("12000 EUR") && parsed.factIds == listOf("event"))
            runCatching { (parsed.factIds as MutableList<String>).clear() }; check(parsed.factIds == listOf("event"))
            for (bad in listOf(yes + " trailing", yes.replace("[\"event\"]", "[\"forged\"]"), yes.replace("\"capabilityIds\":[]", "\"capabilityIds\":[\"host/forged.call\"]"), yes.replace("\"Compare budgets\"", "null"), yes.replace("\"title\":", "\"title\":\"duplicate\",\"title\":"), yes.replace("\"title\"", "'title'"), "{}")) {
                check(runCatching { Discovery.parse(bad, capturedInput) }.isFailure) { bad }
            }
            check(runCatching { DiscoveryInput(listOf(DiscoveryFact("event", "x".repeat(2049)))) }.isFailure)
            var reads = 0
            val module = dev.deal.embedding.capabilities.NativeCapabilities(
                dev.deal.embedding.capabilities.CapabilityContract("host/probe", "Explicit read", listOf(
                    dev.deal.embedding.capabilities.CapabilityFunction("read", "Read on demand", emptyList(), dev.deal.embedding.capabilities.CapabilityType("int"))
                )), mapOf("read" to { _: JSONArray -> reads++; 7 }))
            val model = object : ModelClient { override fun complete(input: String, previous: String, diagnostics: String, cancellation: GenerationCancellation, log: EmbeddingLog): String = error("No generation expected") }
            val registry = dev.deal.embedding.capabilities.CapabilityRegistry(targetContext, dev.deal.embedding.capabilities.CapabilityDiscovery(targetContext) { false }, listOf(module))
            ApplicationProtocol(targetContext, Embedding(targetContext, registry, model, EmbeddingConfig("experience", emptyList(), "discovery-admission-test"))).use { api ->
                api.capture(true)
                val entered = CountDownLatch(1); val release = CountDownLatch(1)
                Discovery(targetContext, api) { entered.countDown(); release.await(5, TimeUnit.SECONDS); yes }.use { d ->
                    val future = d.discover(input); check(entered.await(10, TimeUnit.SECONDS)); d.cancel(); release.countDown()
                    check(runCatching { future.get(5, TimeUnit.SECONDS) }.isFailure)
                }
                Discovery(targetContext, api) { error("inference unavailable") }.use { d ->
                    check(runCatching { d.discover(input).get(10, TimeUnit.SECONDS) }.exceptionOrNull()?.cause?.message == "inference unavailable")
                }
                Discovery(targetContext, api) { "null" }.use { d -> check(d.discover(input).get(10, TimeUnit.SECONDS) == null) }
                val captured = DiscoveryInput(listOf(DiscoveryFact("event", "Exact captured evidence")))
                Discovery(targetContext, api) { prompt -> check(prompt.contains("host\\/probe") || prompt.contains("host/probe")); yes }.use { d ->
                    val tip = d.discover(captured).get(10, TimeUnit.SECONDS)!!
                    val changed = DiscoveryInput(listOf(DiscoveryFact("event", "Changed evidence")))
                    check(runCatching { d.request(tip, changed) }.isFailure)
                    val request = d.request(tip, captured)
                    check(api.requestTrace(request).isNotBlank())
                    check(runCatching { d.request(tip, captured) }.isFailure)
                    api.cancel(request)
                    val old = d.discover(captured).get(10, TimeUnit.SECONDS)!!
                    d.cancel(); check(runCatching { d.request(old, captured) }.isFailure)
                }
                // Internal catalogue seam simulates a changed bound registry, not another public API.
                var contracts = api.refresh()
                Discovery(targetContext, api, { contracts }) { yes }.use { d ->
                    val tip = d.discover(captured).get(10, TimeUnit.SECONDS)!!
                    contracts = contracts.map { it.copy(description = "Changed executable contract") }
                    check(runCatching { d.request(tip, captured) }.isFailure)
                }
                val admissionEntered = CountDownLatch(1); val admissionRelease = CountDownLatch(1)
                var acquisitions = 0
                Discovery(targetContext, api, {
                    acquisitions++
                    if (acquisitions > 1) { admissionEntered.countDown(); admissionRelease.await(5, TimeUnit.SECONDS) }
                    api.refresh()
                }) { yes }.use { d ->
                    val tip = d.discover(captured).get(10, TimeUnit.SECONDS)!!
                    val admission = CompletableFuture.supplyAsync { d.request(tip, captured) }
                    check(admissionEntered.await(5, TimeUnit.SECONDS)); d.cancel(); admissionRelease.countDown()
                    check(runCatching { admission.get(10, TimeUnit.SECONDS) }.isFailure)
                }
                // Cancellation remains callable while catalogue acquisition is blocked.
                val catalogueEntered = CountDownLatch(1); val catalogueRelease = CountDownLatch(1)
                Discovery(targetContext, api, { catalogueEntered.countDown(); catalogueRelease.await(5, TimeUnit.SECONDS); contracts }) { yes }.use { d ->
                    val future = d.discover(captured); check(catalogueEntered.await(5, TimeUnit.SECONDS))
                    d.cancel(); catalogueRelease.countDown(); check(runCatching { future.get(5, TimeUnit.SECONDS) }.isFailure)
                }
                check(reads == 0) { "Discovery/admission executed a provider" }
                EmbeddingLog.flush(targetContext)
                val events = api.retainedEvents().events
                check(events.any { it.optString("stage") == "opportunity" && it.optString("outcome") == "suggested" })
                check(events.any { it.optString("stage") == "opportunity" && it.optString("outcome") == "failed" })
                check(events.any { it.optString("stage") == "opportunity" && it.optString("outcome") == "none" })
                val handoff = events.last { it.optString("stage") == "opportunity-handoff" }
                check(handoff.optString("target").isNotBlank())
                val scoped = EventPage(events, "", false).inspection(traces = setOf(handoff.getString("target")))
                check(scoped.events.any { it.optString("stage") == "opportunity-inference" && it.optString("trace") == handoff.getString("trace") })
                val proposal = events.last { it.optString("stage") == "opportunity" && it.optString("outcome") == "suggested" }
                check(api.artifact(proposal.getString("artifact")).text.contains(parsed.brief))
                val exchanges = dialogueExchanges(events)
                check(exchanges.any { exchange -> exchange.stage == "opportunity-inference" && exchange.events.any { it.optString("outcome") == "started" } && exchange.events.any { it.optString("outcome") == "response" } })
                api.capture(false)
                Discovery(targetContext, api) { yes }.use { d -> d.discover(captured).get(10, TimeUnit.SECONDS) }
                EmbeddingLog.flush(targetContext)
                val uncaptured = api.retainedEvents().events.last { it.optString("stage") == "opportunity" && it.optString("outcome") == "suggested" }
                check(uncaptured.optString("content") == "omitted" && uncaptured.optString("artifact").isEmpty())
            }
            result.putString("discovery", "PASS canonical bound catalogue, strict replies, immutable evidence, cancellation, failure, exact-input single-use admission, changed catalogue, zero provider reads, unified observation and handoff correlation")
            finish(0, result)
        } catch (error: Throwable) { result.putString("failure", android.util.Log.getStackTraceString(error)); finish(1, result) }
    }
}
