package dev.deal.connectors.tests

import android.app.Instrumentation
import android.os.Bundle
import dev.deal.embedding.*
import dev.deal.embedding.capabilities.*
import org.json.*
import java.time.Instant

/** Deterministic generation through trusted core, typed reads, selection and native preparation. */
class CatalogueInstrumentation : Instrumentation() {
    override fun onCreate(arguments: Bundle?) { super.onCreate(arguments); start() }
    override fun onStart() {
        val result = Bundle()
        var runtime: ExperienceRuntime? = null
        var registry: CapabilityRegistry? = null
        try {
            val context = targetContext
            val from = Instant.now().plusSeconds(86400).toString()
            val until = Instant.now().plusSeconds(108000).toString()
            val disclosed = JSONObject().put("requirement", "Boom stand").put("specification", "Adjustable boom with clip adapter")
                .put("quantity", 1).put("from", from).put("until", until).put("searchTerms", JSONArray(listOf("Boom", "stand"))).toString()
            val str = CapabilityType("string", maximum = 512)
            val catalogue = CapabilityContract("host/rental", "Hire equipment with availability", listOf(CapabilityFunction("equipment", "Read catalogue for period", listOf(
                CapabilityParameter("from", str), CapabilityParameter("until", str)), CapabilityType("array", element = CapabilityType("record", fields = linkedMapOf(
                "id" to str, "name" to str, "specification" to str, "cents" to CapabilityType.IntType, "available" to CapabilityType.IntType))))))
            val stage = CapabilityContract("host/organizer", "Native review only", listOf(CapabilityFunction("stage", "Stage descriptor", listOf("kind", "provider", "title", "detail", "price", "deadline").map { CapabilityParameter(it, str) },
                CapabilityType("record", fields = mapOf("staged" to CapabilityType("boolean"), "reference" to str)))))
            var reads = 0; var stages = 0; var prepared: JSONArray? = null
            val rental = NativeCapabilities(catalogue, mapOf("equipment" to { args ->
                check(args.getString(0) == from && args.getString(1) == until) { "Period arguments were not preserved" }
                reads++
                JSONArray(listOf(
                    JSONObject().put("id", "stand").put("name", "Boom microphone stand").put("specification", "Adjustable boom; clip adapter").put("cents", 800).put("available", 2),
                    JSONObject().put("id", "package").put("name", "Vocal package").put("specification", "One boom stand plus cable").put("cents", 1200).put("available", 0),
                    JSONObject().put("id", "cable").put("name", "Cable").put("specification", "10 m XLR").put("cents", 500).put("available", 3)))
            }))
            val organizer = NativeCapabilities(stage, mapOf("stage" to { args -> stages++; prepared = args; JSONObject().put("staged", true).put("reference", "fixture-review") }))
            registry = CapabilityRegistry(context, CapabilityDiscovery(context) { false }, listOf(organizer, rental)); registry.grantAll()
            val config = EmbeddingConfig("experience", registry.contracts(), "catalogue-test")
            runtime = ExperienceRuntime(context, registry, config)
            val host = runtime
            var source: ExperienceSource? = null
            // Every issued non-diagnostic combination is checked, not merely assumed valid.
            for (purpose in listOf("compare", "choose")) for (headline in listOf("hero", "text")) for (notice in listOf("warning", "none")) {
                var calls = 0
                val model = object : ModelClient {
                    override fun complete(input: String, previous: String, diagnostics: String, cancellation: GenerationCancellation): String {
                        calls++
                        check(input.contains("ISSUED OPTIONS") && previous.isEmpty() && diagnostics.isEmpty()) { "Unexpected source repair: $diagnostics" }
                        return JSONObject().put("answers", JSONObject().put("purpose", purpose).put("headline", headline).put("notice", notice).put("read", "read0")).toString()
                    }
                }
                host.generate("Compare boom stands for native review", disclosed, model, GenerationCancellation()) {}.use { candidate ->
                    check(calls == 1 && candidate.attempts == 1) { "Template needed repair" }
                    check(reads == 0 && stages == 0) { "Generation invoked provider data" }
                    // Source replay receipt stays app-private and contains no provider data.
                    val prefs = context.getSharedPreferences("organizer-experience", 0)
                    // Capture exact accepted source from candidate-private staging via saved workspace storage.
                    val (workspace, _) = host.open("Catalogue verification", candidate)
                    val stored = context.getSharedPreferences("catalogue-test", 0)
                    source = ExperienceSource(stored.getString("workspace.${workspace.id}.deal", null)!!, stored.getString("workspace.${workspace.id}.dealui", null)!!)
                    prefs.edit().putString("workspace.catalogue-verification.deal", source!!.deal).putString("workspace.catalogue-verification.dealui", source!!.ui).commit()
                    host.closeWorkspace(workspace.id)
                }
            }
            // Literal escaping and Unicode term normalization compile and mount on the real backend.
            val escapedContext = JSONObject(disclosed).put("requirement", "Quote \" slash/ back\\ tab\t newline\n😀")
                .put("specification", "Specs \"x\" / \\ \n \t").put("quantity", 128).put("searchTerms", JSONArray(listOf("ÄBC", "İ", "BOOM"))).toString()
            var escapedCalls = 0
            val escapedModel = object : ModelClient {
                override fun complete(input: String, previous: String, diagnostics: String, cancellation: GenerationCancellation): String {
                    escapedCalls++
                    return "{\"answers\":{\"purpose\":\"compare\",\"headline\":\"text\",\"notice\":\"warning\",\"read\":\"read0\"}}"
                }
            }
            host.generate("Escaping fixture", escapedContext, escapedModel, GenerationCancellation()) {}.use { candidate ->
                check(escapedCalls == 1)
                val (workspace, _) = host.open("Escaping fixture", candidate); host.closeWorkspace(workspace.id)
            }
            // Unsupported aliases/context use the source escape hatch, with the reason preserved.
            for (unsupported in listOf("unavailable", "read999")) {
                var calls = 0
                val model = object : ModelClient {
                    override fun complete(input: String, previous: String, diagnostics: String, cancellation: GenerationCancellation): String {
                        calls++
                        if (calls == 1) return JSONObject().put("answers", JSONObject().put("purpose", "compare").put("headline", "text").put("notice", "none").put("read", unsupported)).toString()
                        check(calls == 2 && diagnostics.contains("CHOICE_") && previous.isEmpty())
                        return JSONObject().put("deal", source!!.deal).put("dealui", source!!.ui).toString()
                    }
                }
                host.generate("Unsupported choice test", disclosed, model, GenerationCancellation()) {}.use { check(calls == 2) }
            }
            // Negative policy fixtures: context/schema/ambiguity must not issue callable aliases.
            val policyCases = listOf(
                "missing context" to Pair(registry.contracts(), "{}"),
                "wrong quantity type" to Pair(registry.contracts(), JSONObject(disclosed).put("quantity", "1").toString()),
                "empty search term" to Pair(registry.contracts(), JSONObject(disclosed).put("searchTerms", JSONArray(listOf(" "))).toString()),
                "missing result field" to Pair(listOf(stage, catalogue.copy(functions = catalogue.functions.map { it.copy(result = it.result.copy(element = it.result.element!!.copy(fields = it.result.element!!.fields - "available"))) })), disclosed),
                "ambiguous preparation" to Pair(listOf(stage, stage.copy(module = "host/other"), catalogue), disclosed),
                "argument wire bound" to Pair(listOf(stage, catalogue.copy(functions = catalogue.functions.map { it.copy(parameters = it.parameters.map { p -> p.copy(type = p.type.copy(maximum = 1)) }) })), disclosed)
            )
            for ((label, fixture) in policyCases) {
                config.contracts = fixture.first
                try {
                    var calls = 0
                    val model = object : ModelClient {
                        override fun complete(input: String, previous: String, diagnostics: String, cancellation: GenerationCancellation): String {
                            calls++
                            if (calls == 1) {
                                val issued = JSONObject(input.substringAfter("ISSUED OPTIONS\n"))
                                check(issued.getJSONArray("reads").length() == 0) { "Policy issued unsupported $label" }
                                return JSONObject().put("answers", JSONObject().put("purpose", "compare").put("headline", "text").put("notice", "none").put("read", "read0")).toString()
                            }
                            check(calls == 2 && diagnostics.contains("CHOICE_ALIAS"))
                            cancellation.cancel(); cancellation.check(); error("unreachable")
                        }
                    }
                    val failure = runCatching { host.generate("Policy fixture", fixture.second, model, GenerationCancellation()) {} }.exceptionOrNull()
                    check(failure is java.util.concurrent.CancellationException) { "Policy fixture $label failed: $failure" }
                    check(calls == 2)
                } finally { config.contracts = registry.contracts() }
            }
            // Slow-path repair is deterministic: reject malformed source, then check and mount the saved valid pair.
            var sourceCalls = 0
            val repairing = object : ModelClient {
                override fun complete(input: String, previous: String, diagnostics: String, cancellation: GenerationCancellation): String {
                    sourceCalls++
                    if (sourceCalls == 1) return "invalid choice"
                    if (sourceCalls == 2) { check(previous.isEmpty() && diagnostics.contains("CHOICE_JSON")); return JSONObject().put("deal", "broken").put("dealui", "broken").toString() }
                    check(sourceCalls == 3 && previous.contains("broken") && diagnostics.contains("code"))
                    return JSONObject().put("deal", source!!.deal).put("dealui", source!!.ui).toString()
                }
            }
            host.generate("Repair fixture", disclosed, repairing, GenerationCancellation()) {}.use { candidate ->
                check(sourceCalls == 3 && candidate.attempts == 2)
                val (mounted, _) = host.open("Slow path", candidate); host.closeWorkspace(mounted.id)
            }
            // Envelope shape is rejected by DEAL before invoking the compiler, then repaired.
            val invalidEnvelopes = listOf("[]", "null", "{}", "{\"deal\":\"x\"}", "{\"deal\":1,\"dealui\":\"x\"}", "{\"deal\":\"x\",\"dealui\":\"y\",\"extra\":true}")
            for (invalid in invalidEnvelopes) {
                var calls = 0
                val model = object : ModelClient {
                    override fun complete(input: String, previous: String, diagnostics: String, cancellation: GenerationCancellation): String {
                        calls++
                        if (calls == 1) return "invalid choice"
                        if (calls == 2) return invalid
                        check(calls == 3 && previous == invalid && diagnostics.contains("INVALID_CANDIDATE"))
                        return JSONObject().put("deal", source!!.deal).put("dealui", source!!.ui).toString()
                    }
                }
                host.generate("Envelope repair fixture", disclosed, model, GenerationCancellation()) {}.use { check(calls == 3 && it.attempts == 2) }
            }
            val (retained, retainedTree) = host.check(source!!).use { host.open("Retained during generation failure", it) }
            var exhaustedCalls = 0
            val exhausted = object : ModelClient {
                override fun complete(input: String, previous: String, diagnostics: String, cancellation: GenerationCancellation): String { exhaustedCalls++; return "invalid" }
            }
            check(runCatching { host.generate("Exhaustion fixture", disclosed, exhausted, GenerationCancellation()) {} }.isFailure)
            check(exhaustedCalls == 4 && host.poll(retained.id)!!.toString() == retainedTree.toString()) { "Source exhaustion changed the healthy workspace" }
            var failureCalls = 0
            val transportFailure = object : ModelClient {
                override fun complete(input: String, previous: String, diagnostics: String, cancellation: GenerationCancellation): String {
                    failureCalls++; error("Injected inference transport failure")
                }
            }
            check(runCatching { host.generate("Transport test", disclosed, transportFailure, GenerationCancellation()) {} }.exceptionOrNull()?.message == "Injected inference transport failure")
            check(failureCalls == 1) { "Transport failure was retried as source repair" }
            check(host.poll(retained.id)!!.toString() == retainedTree.toString())
            // Valid envelope, oversized source: native resource enforcement must stop, not repair.
            var resourceCalls = 0
            val oversized = object : ModelClient {
                override fun complete(input: String, previous: String, diagnostics: String, cancellation: GenerationCancellation): String {
                    resourceCalls++
                    if (resourceCalls == 1) return "invalid choice"
                    return JSONObject().put("deal", "x".repeat(128 * 1024 + 1)).put("dealui", "x").toString()
                }
            }
            check(runCatching { host.generate("Resource enforcement fixture", disclosed, oversized, GenerationCancellation()) {} }.isFailure)
            check(resourceCalls == 2 && host.poll(retained.id)!!.toString() == retainedTree.toString())
            host.closeWorkspace(retained.id)
            val defectiveStage = stage.copy(functions = stage.functions.map { it.copy(name = "from") })
            config.contracts = listOf(catalogue, defectiveStage)
            try {
                var calls = 0
                val model = object : ModelClient {
                    override fun complete(input: String, previous: String, diagnostics: String, cancellation: GenerationCancellation): String {
                        calls++
                        return JSONObject().put("answers", JSONObject().put("purpose", "compare").put("headline", "text").put("notice", "none").put("read", "read0")).toString()
                    }
                }
                val failure = runCatching { host.generate("Template fault test", disclosed, model, GenerationCancellation()) {} }.exceptionOrNull()
                check(calls == 1 && failure?.message?.contains("TEMPLATE_CHECK_FAILED") == true) { "Template defect caused inference repair: $failure" }
            } finally { config.contracts = registry.contracts() }
            val (workspace, initial) = host.check(source!!).use { host.open("Interaction verification", it) }
            fun nodes(n: JSONObject): List<JSONObject> = listOf(n) + (0 until n.getJSONArray("children").length()).flatMap { nodes(n.getJSONArray("children").getJSONObject(it)) }
            fun prop(n: JSONObject, name: String): JSONObject = (0 until n.getJSONArray("props").length()).map { n.getJSONArray("props").getJSONObject(it) }.first { it.getString("name") == name }
            fun click(snapshot: JSONObject, label: String): JSONObject {
                val button = nodes(snapshot.getJSONObject("tree")).first { it.getString("component") == "ui.Button" && prop(it, "text").getString("stringValue") == label }
                return host.dispatch(workspace.id, prop(button, "onClick").getInt("actionSlot"), null)!!
            }
            fun waitAfter(version: Int): JSONObject {
                repeat(200) {
                    val snapshot = host.poll(workspace.id)!!
                    check(snapshot.optString("fault").isEmpty()) { snapshot.optString("fault") }
                    if (snapshot.getInt("version") >= version + 2) return snapshot
                    Thread.sleep(20)
                }
                error("Effect timed out after snapshot $version")
            }
            click(initial, "Load options")
            val loaded = waitAfter(initial.getInt("version"))
            val rows = nodes(loaded.getJSONObject("tree")).filter { it.getString("component") == "ui.Option" }
            check(rows.size == 2 && reads == 1 && stages == 0) { "Unexpected rows/read count" }
            val stand = rows.first { prop(it, "value").getString("stringValue") == "stand" }
            check(prop(stand, "price").getString("stringValue") == "€8.00")
            val unavailable = rows.first { prop(it, "value").getString("stringValue") == "package" }
            check(!prop(unavailable, "enabled").getBoolean("booleanValue"))
            host.dispatch(workspace.id, prop(stand, "onSelect").getInt("actionSlot"), "stand")
            val picked = host.poll(workspace.id)!!
            check(prop(nodes(picked.getJSONObject("tree")).first { it.getString("component") == "ui.Option" }, "selected").getBoolean("booleanValue"))
            click(picked, "Prepare for review")
            waitAfter(picked.getInt("version"))
            check(stages == 1 && prepared!!.getString(1) == "host/rental" && prepared!!.getString(2) == "Boom microphone stand")
            check(prepared!!.getString(3).contains("stand:1") && prepared!!.getString(3).contains(from) && prepared!!.getString(4) == "€8.00" && prepared!!.getString(5) == until)
            host.published(workspace.id, host.poll(workspace.id)!!.getInt("version"))
            val receiptsFile = java.io.File(context.filesDir, "embedding-receipts.jsonl")
            // A run can straddle the bounded rotation; inspect both retained pages.
            val previousReceipts = java.io.File(context.filesDir, "embedding-receipts.previous.jsonl")
            val receipts = (if (previousReceipts.exists()) previousReceipts.readLines() else emptyList())
                .plus(receiptsFile.readLines()).map { JSONObject(it) }
            val mount = receipts.last { it.optString("workspace") == workspace.id && it.getString("stage") == "mount" }
            val run = mount.getString("run")
            val correlated = receipts.filter { it.getString("run") == run }
            check(correlated.any { it.getString("stage") == "check" } && correlated.any { it.getString("stage") == "action" } && correlated.any { it.getString("stage") == "capability" } && correlated.any { it.getString("outcome") == "published" })
            val allowed = setOf("run", "stage", "outcome", "candidate", "workspace", "operation", "code", "durationMs", "version", "parentOperation")
            check(receipts.all { it.keys().asSequence().all { key -> key in allowed } })
            check(correlated.filter { it.getString("stage") == "capability" && it.getString("outcome") == "started" }.all { it.optString("parentOperation").isNotEmpty() })
            check(receipts.none { it.toString().contains("Adjustable boom") || it.toString().contains("clip adapter") })
            result.putString("catalogueVerification", "8 DEAL-owned choices checked; Unicode/escaping/quantity-bound template mounted; 6 negative policy fixtures; 6 DEAL envelope repair fixtures; native resource failure stops; source repair/exhaustion/cancellation and template/transport failure ownership; correlated shape-only receipts; matching rows, prices, availability and review descriptor; no provider reads during generation")
            finish(0, result)
        } catch (error: Throwable) { result.putString("failure", android.util.Log.getStackTraceString(error)); finish(1, result) }
        finally { runtime?.close(); registry?.close() }
    }
}
