package dev.deal.connectors.tests

import android.app.Instrumentation
import android.os.Bundle
import dev.deal.embedding.*
import dev.deal.embedding.capabilities.*
import dev.deal.shell.Connections
import org.json.JSONArray
import org.json.JSONObject

/** All six installed app identities discover five real Binder providers. No inference or writes. */
open class HiveInstrumentation : Instrumentation() {
    override fun onCreate(arguments: Bundle?) { super.onCreate(arguments); start() }
    override fun onStart() {
        val receipt = Bundle()
        try {
            val ctx = targetContext
            val ownModule = mapOf("calendar" to "host/agenda", "organizer" to "host/show", "rental" to "host/rental", "pizza" to "host/pizza", "todo" to "host/todo", "vehicle" to "host/trip").getValue(ctx.packageName.substringAfterLast('.'))
            val expected = setOf("host/agenda", "host/show", "host/rental", "host/pizza", "host/todo", "host/trip") - ownModule
            val registry = CapabilityRegistry(ctx, CapabilityDiscovery(ctx) { uid -> uid != ctx.applicationInfo.uid && Connections.trusted(ctx, uid) }, emptyList())
            val noInference = object : ModelClient { override fun complete(input: String, previous: String, diagnostics: String, cancellation: GenerationCancellation, log: EmbeddingLog): String = error("Unexpected inference") }
            ApplicationProtocol(ctx, Embedding(ctx, registry, noInference, EmbeddingConfig("experience", emptyList(), "hive-test-${java.util.UUID.randomUUID()}"))).use { api ->
                val contracts = api.refresh()
                check(contracts.map { it.module }.toSet() == expected) { "${ctx.packageName}: ${contracts.map { it.module }} != $expected" }
                // Discovery's exact bounded full catalogue remains admissible, not a filtered shortlist.
                val bytes = JSONArray(contracts.map { it.json() }).toString().toByteArray().size
                check(bytes <= 32768) { "Catalogue exceeds discovery bound: $bytes" }
                fun probe(module: String, function: String, args: JSONArray): JSONObject {
                    registry.openSession(EmbeddingLog(ctx)).use { session ->
                        session.receive(JSONObject().put("id", 1).put("module", module).put("function", function).put("args", args), android.os.SystemClock.elapsedRealtime())
                        val end = System.currentTimeMillis() + 10000
                        while (true) {
                            session.drain(android.os.SystemClock.elapsedRealtime()).singleOrNull()?.let { return it }
                            check(System.currentTimeMillis() < end) { "Provider timeout: $module" }; Thread.sleep(10)
                        }
                    }
                }
                for (contract in contracts) {
                    val (name, args) = when (contract.module) {
                        "host/agenda" -> "events" to JSONArray().put("2026-10-23")
                        "host/show" -> "requirements" to JSONArray()
                        "host/rental" -> "equipment" to JSONArray().put("2026-10-23T14:15:00Z").put("2026-10-23T20:30:00Z")
                        "host/pizza" -> "menu" to JSONArray()
                        "host/todo" -> "unfinished" to JSONArray()
                        else -> "estimate" to JSONArray().put(12)
                    }
                    val denied = probe(contract.module, name, args)
                    check(!denied.getBoolean("ok")) { "Catalogue visibility granted execution: ${contract.module}" }
                    api.grant(contract.module)
                    val shared = probe(contract.module, name, args)
                    // Sharing is a user decision, never enabled by this test.
                    if (shared.getBoolean("ok")) contract.native().functions.single { it.name == name }.result.validate(shared.get("value"))
                    else check(shared.getJSONObject("error").getString("code") == "PROVIDER_DENIED") { shared.toString() }
                    api.revoke(contract.module)
                    check(!probe(contract.module, name, args).getBoolean("ok")) { "Revoked module executed" }
                }
                receipt.putString("hive", "${ctx.packageName}: five canonical remote providers, ${bytes}B full catalogue; no implicit grant, provider consent separate, revocation enforced; no inference/writes")
            }
            finish(0, receipt)
        } catch (error: Throwable) { receipt.putString("failure", android.util.Log.getStackTraceString(error)); finish(1, receipt) }
    }
}
class HiveCalendarInstrumentation : HiveInstrumentation()
class HiveOrganizerInstrumentation : HiveInstrumentation()
class HiveRentalInstrumentation : HiveInstrumentation()
class HivePizzaInstrumentation : HiveInstrumentation()
class HiveTodoInstrumentation : HiveInstrumentation()
class HiveVehicleInstrumentation : HiveInstrumentation()
