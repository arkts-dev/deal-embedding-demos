package dev.deal.connectors.tests

import android.app.Instrumentation
import android.os.Bundle
import dev.deal.apps.development.GenerationGateway
import dev.deal.embedding.*
import dev.deal.embedding.capabilities.*
import org.json.*
import java.io.File
import java.security.MessageDigest

/** Two bounded real source experiments. Choice is bypassed only by this test ModelClient.
 * Defaults to retained test-APK fixtures; inference requires mode=real explicitly.
 * Private saved responses support no-inference replay; routine receipts remain shape-only.
 */
class SourceGenerationInstrumentation : Instrumentation() {
    private var mode = "fixtures"
    private var round = "initial"
    override fun onCreate(arguments: Bundle?) {
        mode = arguments?.getString("mode") ?: "fixtures"
        require(mode in setOf("fixtures", "real", "saved"))
        round = arguments?.getString("round") ?: "initial"
        require(round.matches(Regex("[a-z0-9-]+")))
        super.onCreate(arguments); start()
    }
    override fun onStart() {
        val result = Bundle()
        var registry: CapabilityRegistry? = null
        var runtime: ExperienceRuntime? = null
        try {
            val context = targetContext
            val dir = File(context.filesDir, "source-exploration/$round").apply { mkdirs() }
            val str = CapabilityType("string", maximum = 512)
            val rental = CapabilityContract("host/rental", "Equipment catalogue for a requested period", listOf(CapabilityFunction("equipment", "Read equipment for period", listOf(CapabilityParameter("from", str), CapabilityParameter("until", str)), CapabilityType("array", element = CapabilityType("record", fields = linkedMapOf("id" to str, "name" to str, "specification" to str, "cents" to CapabilityType.IntType, "available" to CapabilityType.IntType))))))
            val organizer = CapabilityContract("host/organizer", "Prepare a descriptor, never reserve", listOf(CapabilityFunction("stage", "Stage for native review", listOf("kind", "provider", "title", "detail", "price", "deadline").map { CapabilityParameter(it, str) }, CapabilityType("record", fields = mapOf("staged" to CapabilityType("boolean"), "reference" to str)))))
            var reads = 0; var stages = 0; var descriptor: JSONArray? = null
            val localRead = NativeCapabilities(rental, mapOf("equipment" to { args ->
                check(args.getString(0) == "2026-10-10T16:00:00Z" && args.getString(1) == "2026-10-10T22:00:00Z")
                reads++
                JSONArray(listOf(
                    JSONObject().put("id", "stand").put("name", "Boom microphone stand").put("specification", "Adjustable boom; clip adapter").put("cents", 800).put("available", 2),
                    JSONObject().put("id", "package").put("name", "Vocal package").put("specification", "One boom stand plus cable").put("cents", 1200).put("available", 1),
                    JSONObject().put("id", "cable").put("name", "Cable").put("specification", "10 m XLR").put("cents", 500).put("available", 3)))
            }))
            val localStage = NativeCapabilities(organizer, mapOf("stage" to { args -> stages++; descriptor=args; JSONObject().put("staged", true).put("reference", "fixture-review") }))
            registry = CapabilityRegistry(context, CapabilityDiscovery(context) { false }, listOf(localRead, localStage)); registry.grantAll()
            runtime = ExperienceRuntime(context, registry, EmbeddingConfig("experience", registry.contracts(), "source-exploration"))
            val host = runtime
            fun nodes(node: JSONObject): List<JSONObject> = listOf(node) + (0 until node.getJSONArray("children").length()).flatMap { nodes(node.getJSONArray("children").getJSONObject(it)) }
            fun prop(node: JSONObject, name: String): JSONObject = (0 until node.getJSONArray("props").length()).map { node.getJSONArray("props").getJSONObject(it) }.first { it.getString("name") == name }
            val summary = JSONArray()
            for ((name, quantity, budget) in listOf(Triple("quantity-budget", 2, 1800), Triple("single-budget", 1, 1000))) {
                val receipt = JSONObject().put("case", name).put("round", round).put("mode", mode).put("quantity", quantity).put("budgetCents", budget)
                var phase = "generation"
                var calls=0; var choiceCalls=0
                val started=android.os.SystemClock.elapsedRealtime()
                try {
                    val disclosed = JSONObject().put("requirement", "Boom stand").put("specification", "Adjustable boom with clip adapter").put("quantity", quantity).put("budgetCents", budget).put("from", "2026-10-10T16:00:00Z").put("until", "2026-10-10T22:00:00Z").put("searchTerms", JSONArray(listOf("boom", "stand"))).toString()
                    val intent = "Build a rental shortlist for the disclosed Boom stand requirement. This differs from the stock template: enforce budgetCents as the TOTAL price ceiling for quantity, as well as available >= quantity. Match boom and stand across lowercased name/specification. Show only qualifying options using ui.Option with stable equipment id as value, formatted TOTAL euro price, enabled and selected state. Load only on button Load options; no reads at initialization. Selecting an option retains it. Button Prepare for review stages the selection through host/organizer.stage with provider host/rental, original item title, detail containing Item <id>:<quantity> and both exact period strings, total price, and until as deadline. Show Prepared for native review on success; never claim a reservation. Preserve selection through staging. Provide loading/empty/error states. A retry Load options must recover after a denied read. No other capability calls. Application module filename is experience. Use explicit DEAL loops and typed field reads. Return ONLY JSON with deal and dealui strings, not markdown."
                    val real = GenerationGateway("http://127.0.0.1:8787/generate")
                    val model = object : ModelClient {
                        override fun complete(input: String, previous: String, diagnostics: String, cancellation: GenerationCancellation, log: dev.deal.embedding.EmbeddingLog): String {
                            if (input.contains("ISSUED OPTIONS\n")) {
                                choiceCalls++
                                return "{\"answers\":{\"purpose\":\"unavailable\",\"headline\":\"text\",\"notice\":\"none\",\"read\":\"unavailable\"}}"
                            }
                            calls++
                            val response=real.complete(input, previous, diagnostics, cancellation, log)
                            // Explicit experiment-only private evidence, including rejected attempts.
                            File(dir, "$name-attempt-$calls.json").writeText(JSONObject().put("response", response).put("diagnostics", diagnostics).put("repair", previous.isNotEmpty()).toString())
                            return response
                        }
                    }
                    val baselineReads=reads; val baselineStages=stages
                    val candidate = if (mode != "real") {
                        val raw = if (mode == "fixtures") this@SourceGenerationInstrumentation.context.assets.open("source-generation/$name.json").bufferedReader().use { it.readText() }
                            else File(dir, "$name-accepted.json").readText()
                        val stored=JSONObject(raw)
                        File(dir, "$name-accepted.json").writeText(stored.toString())
                        host.check(ExperienceSource(stored.getString("deal"), stored.getString("dealui")))
                    } else host.generate(intent, disclosed, model, GenerationCancellation()) {}
                    receipt.put("sourceCalls", calls).put("choiceBypassCalls", choiceCalls).put("firstPassAccepted", mode == "real" && calls == 1).put("attempts", candidate.attempts)
                    phase = "mount"
                    check(reads == baselineReads && stages == baselineStages)
                    val (workspace, initial) = candidate.use { host.open(name, it) }
                    try {
                        if (mode == "real") {
                            val prefs=context.getSharedPreferences("source-exploration", 0)
                            val source=JSONObject().put("deal", prefs.getString("workspace.${workspace.id}.deal", null)).put("dealui", prefs.getString("workspace.${workspace.id}.dealui", null))
                            File(dir, "$name-accepted.json").writeText(source.toString())
                        }
                        val stored=JSONObject(File(dir, "$name-accepted.json").readText())
                        val pairBytes=(stored.getString("deal") + "\u0000" + stored.getString("dealui")).toByteArray(Charsets.UTF_8)
                        receipt.put("sourcePairSha256", MessageDigest.getInstance("SHA-256").digest(pairBytes).joinToString("") { "%02x".format(it) })
                        phase = "load"
                        fun snap()=host.poll(workspace.id)!!.also { check(it.optString("fault").isEmpty()) { "RUNTIME_FAULT: ${it.optString("fault")}" } }
                        fun click(snapshot: JSONObject, label: String) {
                            val controls=nodes(snapshot.getJSONObject("tree"))
                            val button=controls.firstOrNull { it.getString("component") == "ui.Button" && prop(it,"text").getString("stringValue") == label }
                            if (button != null) host.dispatch(workspace.id, prop(button,"onClick").getInt("actionSlot"), null)
                            else {
                                val retry=controls.firstOrNull { it.getString("component") == "ui.Failure" && prop(it,"accessibilityLabel").getString("stringValue").contains(label) }
                                    ?: error("Missing action: $label")
                                host.dispatch(workspace.id, prop(retry,"onRetry").getInt("actionSlot"), null)
                            }
                        }
                        fun waitFor(test: (JSONObject)->Boolean): JSONObject {
                            repeat(150) { val s=snap(); if(test(s)) return s; Thread.sleep(20) }; error("BEHAVIOR_TIMEOUT")
                        }
                        click(initial,"Load options")
                        val loaded=waitFor { nodes(it.getJSONObject("tree")).any { n -> n.getString("component") == "ui.Option" } }
                        val rows=nodes(loaded.getJSONObject("tree")).filter { it.getString("component") == "ui.Option" }
                        check(rows.size == 1 && prop(rows.single(),"value").getString("stringValue") == "stand") { "Budget/quantity shortlist mismatch" }
                        val price=if(quantity==2) "€16.00" else "€8.00"
                        check(prop(rows.single(),"price").getString("stringValue") == price) { "Wrong total price" }
                        receipt.put("budgetQuantityFilter", true)
                        phase = "selection"
                        host.dispatch(workspace.id, prop(rows.single(),"onSelect").getInt("actionSlot"), "stand")
                        val picked=snap(); check(nodes(picked.getJSONObject("tree")).filter { it.getString("component") == "ui.Option" }.any { prop(it,"selected").getBoolean("booleanValue") })
                        phase = "prepare"
                        descriptor=null; click(picked,"Prepare for review")
                        val prepared=waitFor { descriptor != null && nodes(it.getJSONObject("tree")).flatMap { n -> (0 until n.getJSONArray("props").length()).map { p -> n.getJSONArray("props").getJSONObject(p).optString("stringValue") } }.any { text -> text.contains("Prepared for native review") } }
                        check(nodes(prepared.getJSONObject("tree")).any { it.getString("component") == "ui.Option" && prop(it,"value").getString("stringValue") == "stand" && prop(it,"selected").getBoolean("booleanValue") }) { "Preparation lost selection" }
                        val d=descriptor!!; check(d.getString(1)=="host/rental" && d.getString(2)=="Boom microphone stand" && d.getString(3).contains("Item stand:$quantity") && d.getString(3).contains("2026-10-10T16:00:00Z") && d.getString(3).contains("2026-10-10T22:00:00Z") && d.getString(4)==price && d.getString(5)=="2026-10-10T22:00:00Z")
                        receipt.put("selectionAndReviewDescriptor", true)
                        phase = "denial-recovery"
                        registry.revoke("host/rental"); click(snap(),"Load options")
                        val denied=waitFor { nodes(it.getJSONObject("tree")).any { n -> (0 until n.getJSONArray("props").length()).any { p -> n.getJSONArray("props").getJSONObject(p).optString("stringValue").contains("not granted") } } }
                        registry.grant("host/rental"); click(denied,"Load options")
                        waitFor { nodes(it.getJSONObject("tree")).any { n -> n.getString("component")=="ui.Option" } }
                        check(reads == baselineReads+2 && stages == baselineStages+1)
                        receipt.put("behaviorPassed", true).put("denialRecovery", true)
                        // Debug native replay uses the unchanged checked pair, with real discovered providers.
                        val pair=JSONObject(File(dir,"$name-accepted.json").readText())
                        context.getSharedPreferences("organizer-experience",0).edit()
                            .putString("workspace.source-$round-$name.deal",pair.getString("deal"))
                            .putString("workspace.source-$round-$name.dealui",pair.getString("dealui"))
                            .putString("workspace.source-$round-$name.title","Source experiment $name")
                            .putString("workspace.source-$round-$name.origin",WorkspaceOrigin.AI_SOURCE.name).commit()
                    } finally { host.closeWorkspace(workspace.id) }
                } catch(error: Throwable) {
                    receipt.put("behaviorPassed", false).put("failedPhase", phase).put("errorClass",error.javaClass.simpleName).put("sourceCalls",calls)
                    File(dir,"$name-failure.txt").writeText(android.util.Log.getStackTraceString(error))
                }
                receipt.put("durationMs",android.os.SystemClock.elapsedRealtime()-started); summary.put(receipt)
                File(dir,"$mode-summary.json").writeText(summary.toString(2))
            }
            result.putString("sourceExploration",summary.toString()); finish(if((0 until summary.length()).all { summary.getJSONObject(it).optBoolean("behaviorPassed") }) 0 else 1,result)
        } catch(error: Throwable) { result.putString("failure",android.util.Log.getStackTraceString(error)); finish(1,result) }
        finally { runtime?.close(); registry?.close() }
    }
}
