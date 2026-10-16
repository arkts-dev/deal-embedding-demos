package dev.deal.connectors.tests

import android.app.Instrumentation
import android.os.Bundle
import android.content.Intent
import dev.deal.embedding.*
import dev.deal.embedding.capabilities.*
import dev.deal.embedding.discovery.*
import org.json.*
import java.util.concurrent.TimeUnit

/** Opt-in fresh on-device inference with synthetic facts only. No expected output is supplied. */
class DiscoveryLiveInstrumentation : Instrumentation() {
    private var live = false
    override fun onCreate(arguments: Bundle?) { super.onCreate(arguments); live = arguments?.getString("live") == "true"; start() }
    override fun onStart() {
        val result = Bundle(); val receipts = JSONArray()
        try {
            check(live) { "Requires -e live true" }
            startActivitySync(Intent().setClassName(targetContext.packageName, "dev.deal.apps.calendar.AgendaActivity").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            Thread.sleep(1500)
            var reads = 0
            val native = NativeCapabilities(CapabilityContract("host/calendar", "Selected Calendar event and native-reviewed departure proposal", listOf(
                CapabilityFunction("start", "Read selected event start minute and capture its revision", emptyList(), CapabilityType("int", minimumInt = 0, maximumInt = 1439)),
                CapabilityFunction("propose", "Stage a departure minute for native user confirmation; never writes", listOf(CapabilityParameter("minute", CapabilityType("int", minimumInt = 0, maximumInt = 1439))), CapabilityType.NullType)
            )), mapOf("start" to { _: JSONArray -> reads++; 900 }, "propose" to { _: JSONArray -> reads++; JSONObject.NULL }))
            val model = object : ModelClient { override fun complete(input: String, previous: String, diagnostics: String, cancellation: GenerationCancellation, log: EmbeddingLog): String = error("Unexpected heavy generation") }
            ApplicationProtocol(targetContext, Embedding(targetContext, CapabilityRegistry(targetContext, CapabilityDiscovery(targetContext) { false }, listOf(native)), model, EmbeddingConfig("preparation", emptyList(), "discovery-live-test"))).use { api ->
                val cases = listOf(
                    Triple("budget", true, "Gig Demo supplier review tomorrow at 15:00. Notes: Before deciding, compare the approved 12000 EUR budget with the revised 13750 EUR quotation. The comparison is not done."),
                    Triple("unrelated", false, "Gig Demo dentist appointment tomorrow at 15:00. No notes, preparation requests or unresolved decisions."),
                    Triple("resolved", false, "Gig Demo supplier review tomorrow at 15:00. Notes: The approved 12000 EUR and revised 13750 EUR have already been compared; the 1750 EUR increase is approved. Preparation complete; no further work needed."),
                    Triple("ambiguous", false, "Gig Demo Review tomorrow at 15:00. Notes: Bring the things. Sort it out."),
                    Triple("insufficient", false, "Gig Demo evening drive tomorrow at 18:00. Notes: Avoid running out of charge. No vehicle, battery, route, range or charger data is available.")
                )
                Discovery(targetContext, api).use { discovery ->
                    for ((id, expected, text) in cases) {
                        val start = android.os.SystemClock.elapsedRealtime()
                        val receipt = JSONObject().put("id", id).put("expectedSuggestion", expected)
                        try {
                            val tip = discovery.discover(DiscoveryInput(listOf(DiscoveryFact("event", text)))).get(120, TimeUnit.SECONDS)
                            receipt.put("suggested", tip != null).put("title", tip?.title).put("brief", tip?.brief).put("correctPresence", (tip != null) == expected)
                        } catch (error: Throwable) { receipt.put("error", error.toString()) }
                        receipt.put("elapsedMs", android.os.SystemClock.elapsedRealtime() - start); receipts.put(receipt)
                        val status = Bundle(); status.putString("case", receipt.toString()); sendStatus(2, status)
                        Thread.sleep(6000)
                    }
                }
                check(reads == 0) { "Discovery executed providers" }
            }
            targetContext.getFileStreamPath("discovery-live.json").writeText(receipts.toString(2))
            check((0 until receipts.length()).all { receipts.getJSONObject(it).optBoolean("correctPresence") }) { "Discovery presence failures: $receipts" }
            result.putString("discoveryLive", receipts.toString()); finish(0, result)
        } catch (error: Throwable) { result.putString("failure", android.util.Log.getStackTraceString(error)); result.putString("receipts", receipts.toString()); finish(1, result) }
    }
}
