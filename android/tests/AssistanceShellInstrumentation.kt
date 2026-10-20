package dev.deal.connectors.tests

import android.app.Instrumentation
import android.content.Context
import android.content.ContextWrapper
import android.os.Bundle
import dev.deal.embedding.*
import dev.deal.embedding.capabilities.*
import dev.deal.shell.*
import java.util.UUID

/** Isolated real saved source/state reset; never flush installed users' assistance. */
class AssistanceShellInstrumentation : Instrumentation() {
    override fun onCreate(arguments: Bundle?) { super.onCreate(arguments); start() }
    override fun onStart() {
        val result = Bundle()
        val prefix = "shell-${UUID.randomUUID()}-"
        val created = mutableSetOf<String>()
        try {
            for (app in listOf("calendar", "organizer", "todo", "rental", "pizza", "vehicle")) {
                val context = object : ContextWrapper(targetContext) {
                    override fun getPackageName() = "dev.deal.apps.$app"
                    override fun getSharedPreferences(name: String, mode: Int): android.content.SharedPreferences {
                        val isolated = prefix + app + name; created.add(isolated)
                        return super.getSharedPreferences(isolated, mode)
                    }
                }
                val store = AssistanceShell.storage(context)
                check(AssistanceShell.settings(context).component!!.className == AssistanceSettingsActivity::class.java.name)
                check(AssistanceShell.inspect(context).component!!.className == AssistanceInspectActivity::class.java.name)
                val hive = context.getSharedPreferences("hive", 0)
                check(hive.edit().putBoolean("generation", false).putBoolean("discovery", false)
                    .putBoolean("grant.host/probe", true).putString("saved.fact", "old").putBoolean("dismissed.fact", true).commit())
                val provider = context.getSharedPreferences("provider", 0)
                check(provider.edit().putBoolean("allowed", true).putString("records", "personal records").commit())
                val records = context.getSharedPreferences("preparation-records", 0)
                check(records.edit().putString("events", "failed evidence").putString("opportunity.1", "old").commit())
                val dismissals = context.getSharedPreferences("preparation-discovery-dismissals", 0)
                check(dismissals.edit().putBoolean("fact", true).commit())
                val model = object : ModelClient {
                    override fun complete(input: String, previous: String, diagnostics: String, cancellation: GenerationCancellation, log: EmbeddingLog): String = error("No inference")
                }
                fun api(name: String) = ApplicationProtocol(context, Embedding(context,
                    CapabilityRegistry(context, CapabilityDiscovery(context) { false }, emptyList()), model,
                    EmbeddingConfig("experience", emptyList(), name, retainState = true)))
                val source = Source(targetContext.assets.open("ui/comparison.deal").bufferedReader().use { it.readText() },
                    targetContext.assets.open("ui/comparison.dealui").bufferedReader().use { it.readText() })
                check(AssistanceShell.stores(context) == listOf(store))
                val staged = context.getSharedPreferences("prepared", 0)
                check(staged.edit().putString("operations", "generated proposal").commit())
                val ownerApi = api(store)
                val experience = ownerApi.load("Reset fixture", source)
                val owner = ExperienceSurface(ownerApi, experience)
                owner.activate { throw AssertionError(it) }
                check(ownerApi.saved().isNotEmpty())
                val live = ownerApi.run(experience)
                val snapshot = ownerApi.next(live, "").snapshot
                val input = ToolkitNodes.all(snapshot.tree).first { it.optString("component").endsWith(".IntField") }
                ownerApi.dispatch(live, ToolkitNodes.slot(snapshot.tree, ToolkitNodes.string(input, "label")), "18100")
                check(ownerApi.next(live, "").snapshot.tree.toString().contains("18100"))
                val captured = ownerApi.capturesContent()
                val retainedEvents = ownerApi.retainedEvents().events.map { it.toString() }.toSet()
                var stopped = false
                val token = Any()
                AssistanceShell.register(token) { owner.close(); ownerApi.close(); stopped = true }
                val before = provider.all.toMap()
                try { AssistanceShell.reset(context, open = ::api) } finally { AssistanceShell.unregister(token) }
                check(stopped)
                api(store).use {
                    check(it.saved().isEmpty())
                    check(it.capturesContent() == captured)
                    check(it.retainedEvents().events.map { event -> event.toString() }.toSet().containsAll(retainedEvents))
                }
                if (app == "organizer") check(staged.all.isEmpty())
                check(provider.all == before)
                check(!hive.getBoolean("generation", true) && !hive.getBoolean("discovery", true))
                check(hive.getBoolean("grant.host/probe", false))
                check(!hive.contains("saved.fact") && !hive.contains("dismissed.fact"))
                check(records.getString("events", "") == "failed evidence")
                if (app == "calendar") { check(!records.contains("opportunity.1")); check(dismissals.all.isEmpty()) }
                AssistanceShell.reset(context, listOf(store), ::api) // Empty reset is safe and idempotent.
                val fail = Any()
                AssistanceShell.register(fail) { error("Injected owner close failure") }
                check(hive.edit().putString("saved.keep", "retain on failure").commit())
                try { check(runCatching { AssistanceShell.reset(context, listOf(store), ::api) }.isFailure) }
                finally { AssistanceShell.unregister(fail) }
                check(hive.contains("saved.keep") && !AssistanceShell.resetting)
                check(runCatching { AssistanceShell.reset(context, listOf(store)) { error("Injected store-open failure") } }.isFailure)
                check(hive.contains("saved.keep") && !AssistanceShell.resetting)
            }
            result.putString("shell", "PASS six app routes, active-owner close, real saved source/state deletion and cold empty reopen, idempotence, preserved authority/domain/failure records, failed cleanup retains links; no inference")
            finish(0, result)
        } catch (error: Throwable) { result.putString("failure", android.util.Log.getStackTraceString(error)); finish(1, result) }
        finally { created.forEach { targetContext.deleteSharedPreferences(it) } }
    }
}
