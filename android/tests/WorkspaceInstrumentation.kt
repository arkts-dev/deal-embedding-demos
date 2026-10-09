package dev.deal.connectors.tests

import android.app.Instrumentation
import android.os.Bundle
import dev.deal.embedding.*
import dev.deal.embedding.capabilities.*
import org.json.*

/** Deterministic saved-source replay and independent-session regression; never invokes inference. */
class WorkspaceInstrumentation : Instrumentation() {
    override fun onCreate(arguments: Bundle?) { super.onCreate(arguments); start() }
    override fun onStart() {
        val result = Bundle()
        var registry: CapabilityRegistry? = null
        var runtime: ExperienceRuntime? = null
        try {
            val context = targetContext
            val prefs = context.getSharedPreferences("organizer-experience", 0)
            val key = prefs.all.keys.firstOrNull { it.endsWith(".deal") && prefs.getString(it, "")!!.contains("host.staged()") }
                ?: error("No saved workspace reading staged operations")
            val source = ExperienceSource(prefs.getString(key, null)!!, prefs.getString(key.removeSuffix(".deal") + ".dealui", null)!!)
            val fields = listOf("kind", "provider", "title", "detail", "price", "deadline").associateWith { CapabilityType("string") }
            var calls = 0
            val local = NativeCapabilities(CapabilityContract("host/organizer", "Read-only session verification fixture", listOf(
                CapabilityFunction("staged", "Read empty fixture", emptyList(), CapabilityType("array", element = CapabilityType("record", fields = fields))))),
                mapOf("staged" to { _ -> calls++; JSONArray() }))
            registry = CapabilityRegistry(context, CapabilityDiscovery(context) { false }, listOf(local))
            registry.grantAll()
            // Independent broker streams both start at 1; draining one must not steal another reply.
            val a = registry.openSession(dev.deal.embedding.EmbeddingLog(context)); val b = registry.openSession(dev.deal.embedding.EmbeddingLog(context))
            fun request(id: Int) = JSONObject().put("id", id).put("module", "host/organizer").put("function", "staged").put("args", JSONArray())
            a.receive(request(1), 0); b.receive(request(1), 0)
            check(a.drain(0).single().getBoolean("ok")); check(b.drain(0).single().getBoolean("ok"))
            a.close()
            b.receive(request(2), 0); check(b.drain(0).single().getBoolean("ok"))
            registry.revokeAll()
            b.receive(request(3), 0); check(b.drain(0).single().getJSONObject("error").getString("code") == "DENIED")
            registry.grantAll(); b.close()
            check(runCatching { b.drain(0) }.exceptionOrNull()?.message == "Capability session is closed")
            runtime = ExperienceRuntime(context, registry, EmbeddingConfig("experience", registry.contracts(), "workspace-verification"))
            val host = runtime
            val (first, firstTree) = host.check(source).use { host.open("First replay", it) }
            val (second, secondTree) = host.check(source).use { host.open("Second replay", it) }
            fun nodes(n: JSONObject): List<JSONObject> = listOf(n) + (0 until n.getJSONArray("children").length()).flatMap { nodes(n.getJSONArray("children").getJSONObject(it)) }
            fun click(id: String, snapshot: JSONObject) {
                val button = nodes(snapshot.getJSONObject("tree")).first { it.getString("component") == "ui.Button" }
                val props = button.getJSONArray("props")
                val slot = (0 until props.length()).map { props.getJSONObject(it) }.first { it.getString("name") == "onClick" }.getInt("actionSlot")
                host.dispatch(id, slot, null)
            }
            fun complete(id: String, afterVersion: Int = 1): JSONObject {
                repeat(100) {
                    val snapshot = host.poll(id)!!
                    check(snapshot.optString("fault").isEmpty()) { snapshot.optString("fault") }
                    if (snapshot.getInt("version") >= afterVersion + 2) return snapshot
                    Thread.sleep(20)
                }
                error("Effect completion was not published")
            }
            val baseline = calls
            click(first.id, firstTree); click(second.id, secondTree)
            complete(first.id); complete(second.id)
            check(calls == baseline + 2)
            host.closeWorkspace(first.id)
            val before = host.poll(second.id)!!
            click(second.id, before); complete(second.id, before.getInt("version"))
            check(calls == baseline + 3)
            check(host.savedWorkspaces().any { it.id == first.id })
            val reopened = host.reopen(first.id)
            check(reopened.first.id == first.id && calls == baseline + 3)
            val retainedId = reopened.first.id
            runtime.close()
            runtime = ExperienceRuntime(context, registry, EmbeddingConfig("experience", registry.contracts(), "workspace-verification"))
            check(runtime.savedWorkspaces().any { it.id == retainedId })
            val restored = runtime.reopen(retainedId)
            check(restored.first.id == retainedId && calls == baseline + 3)
            val button = nodes(restored.second.getJSONObject("tree")).first { it.getString("component") == "ui.Button" }
            val props = button.getJSONArray("props")
            val slot = (0 until props.length()).map { props.getJSONObject(it) }.first { it.getString("name") == "onClick" }.getInt("actionSlot")
            runtime.dispatch(retainedId, slot, null)
            // Opening starts fresh local UI state, but never a model or provider operation.
            repeat(100) { if (runtime.poll(retainedId)!!.getInt("version") < 3) Thread.sleep(20) }
            check(calls == baseline + 4)
            runtime.forgetWorkspace(retainedId)
            check(runtime.savedWorkspaces().none { it.id == retainedId })
            result.putString("workspaceVerification", "two saved-source isolates completed; close isolation, grant revocation and independent request IDs verified; no inference")
            finish(0, result)
        } catch (error: Throwable) { result.putString("failure", android.util.Log.getStackTraceString(error)); finish(1, result) }
        finally { runtime?.close(); registry?.close() }
    }
}
