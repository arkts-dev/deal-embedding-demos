package dev.deal.apps.calendar

import android.content.Context
import dev.deal.apps.development.GenerationGateway
import dev.deal.embedding.*
import dev.deal.embedding.capabilities.*
import org.json.*
import java.time.LocalDate

/** Native disclosure and data adapters only; relationship/lifecycle policy is compiled DEAL. */
class PreparationDiscovery(private val context: Context) : AutoCloseable {
    private val registry = CapabilityRegistry(context, CapabilityDiscovery(context) { uid ->
        context.packageManager.checkSignatures(uid, context.applicationInfo.uid) == android.content.pm.PackageManager.SIGNATURE_MATCH
    }, emptyList())
    private val protocol = ApplicationProtocol(context, Embedding(context, registry, GenerationGateway("http://127.0.0.1:8787/generate"), EmbeddingConfig("preparation", emptyList(), "preparation-discovery")))
    private val log = EmbeddingLog(context)
    private var date = LocalDate.now(AGENDA_ZONE).plusDays(1)
    @Volatile var foreground = true
    private val prefs = context.getSharedPreferences("preparation", 0)
    @Volatile private var state = prefs.getString("state", null) ?: "{\"input\":\"\",\"attempted\":[],\"dismissed\":[]}"
    private val facts = NativeCapabilities(CapabilityContract("host/preparation-facts", "Host-controlled disclosed input freshness", listOf(
        CapabilityFunction("current", "Re-read the opted-in foreground facts", emptyList(), CapabilityType("string", maximum = 8192)),
        CapabilityFunction("remember", "Persist policy-owned admission state before inference", listOf(CapabilityParameter("state", CapabilityType("string", maximum = 8192))), CapabilityType.NullType)
    )), mapOf("current" to { _ -> input(date) }, "remember" to { args -> check(prefs.edit().putString("state", args.getString(0)).commit()); state = args.getString(0); JSONObject.NULL }))
    private val policy = TrustedProgram(context, "calendar-discovery", "preparation.js", listOf(facts), GenerationGateway("http://127.0.0.1:8787/generate"))
    fun enabled() = prefs.getBoolean("disclose", false)
    fun allow(value: Boolean) { prefs.edit().putBoolean("disclose", value).commit(); if (!value) protocol.revoke("host/todo") }
    private fun input(day: LocalDate): String {
        check(foreground && enabled()) { "Preparation disclosure is off or Calendar is not foreground" }
        val events = ShowAgenda(context).events(day).take(4)
        val tasks = registry.read("host/todo", "unfinished", JSONArray(), log) as JSONArray
        val sortedTasks = (0 until tasks.length()).map { tasks.getJSONObject(it) }.sortedBy { it.getString("id") }
        val disclosed = JSONObject().put("day", day.toString()).put("events", JSONArray(events.sortedWith(compareBy<ShowEvent> { it.from }.thenBy { it.id }).mapIndexed { index, e ->
            JSONObject().put("alias", "e$index").put("id", "${e.id}:${e.from}").put("title", e.title.take(120)).put("notes", e.notes.take(384))
                .put("startMinute", java.time.Instant.ofEpochMilli(e.from).atZone(AGENDA_ZONE).let { it.hour * 60 + it.minute })
        })).put("tasks", JSONArray(sortedTasks.mapIndexed { index, task -> task.put("alias", "t$index") }))
        return disclosed.toString().also { require(it.toByteArray().size <= 5000) }
    }
    fun discover(day: LocalDate): JSONObject {
        date = day
        protocol.refresh(); protocol.grant("host/todo")
        val current = input(day)
        val result = JSONObject(policy.call("discover", current, state).get(90, java.util.concurrent.TimeUnit.SECONDS))
        state = result.getJSONObject("state").toString()
        check(prefs.edit().putString("state", state).commit())
        return result
    }
    fun dismiss(tip: JSONObject) {
        state = policy.call("dismiss", state, tip.toString()).get(10, java.util.concurrent.TimeUnit.SECONDS)
        check(prefs.edit().putString("state", state).commit())
    }
    fun accept(tip: JSONObject): String = policy.call("accept", tip.toString()).get(15, java.util.concurrent.TimeUnit.SECONDS)
    override fun close() { foreground = false; teardown({ policy.close() }, { protocol.close() }) }
}
