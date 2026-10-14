package dev.deal.apps.calendar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.deal.embedding.*
import dev.deal.shell.*
import org.json.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/** Native lifetime/disclosure decisions; generation/checking/execution belong to embedding DEAL. */
@OptIn(ExperimentalMaterial3Api::class)
class PreparationActivity : ComponentActivity() {
    private val worker = Executors.newSingleThreadExecutor()
    private val generationWorker = Executors.newSingleThreadExecutor()
    private val deadline = android.os.Handler(android.os.Looper.getMainLooper())
    private var api: ApplicationProtocol? = null
    private var owner: ExperienceSurface? = null
    private var surface by mutableStateOf<ExperienceSurface?>(null)
    private var message by mutableStateOf("Opening plan…")
    private var tailoring by mutableStateOf(false)
    private var tailored by mutableStateOf<Experience?>(null)
    @Volatile private var request: GenerationRequest? = null
    @Volatile private var foreground = false
    @Volatile private var cancelled = false
    private lateinit var accepted: String
    private lateinit var recordKey: String
    private var record = JSONObject()
    private val recordLock = Any()
    override fun onResume() {
        super.onResume(); foreground = true
        if (api != null) worker.execute {
            if (!runCatching { valid() }.getOrDefault(false)) runOnUiThread { surface = null; tailored = null; message = "Facts or permissions changed. Return to Calendar." }
        }
    }
    override fun onPause() {
        foreground = false; cancelled = true
        request?.let { current -> runCatching { api?.cancel(current) } }
        super.onPause()
    }
    private fun remember() { synchronized(recordLock) { check(PreparationRecords.prefs(this).edit().putString(recordKey, record.toString()).commit()) } }
    private fun valid(): Boolean = PreparationDiscovery(applicationContext).use { discovery -> discovery.foreground = foreground; discovery.validate(accepted) }
    private fun activate(experience: Experience, generated: Boolean) {
        check(foreground && valid()) { "Facts or permissions changed" }
        val next = ExperienceSurface(api!!, experience)
        next.activate { runOnUiThread { message = "Plan unavailable. Return to Calendar." } }
        val previous = owner; owner = next
        synchronized(recordLock) { record.put("active", experience.id); remember() }
        PreparationRecords.record(this, "execution", JSONObject().put("origin", if (generated) "Cortex generated, checked" else "Authored fallback").put("experience", experience.id))
        runOnUiThread { if (!isDestroyed && foreground) surface = next }
        previous?.close()
    }
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        accepted = intent.getStringExtra("accepted") ?: error("Missing accepted facts")
        val facts = JSONObject(accepted)
        recordKey = "plan." + PreparationRecords.key(accepted)
        record = JSONObject(PreparationRecords.prefs(this).getString(recordKey, "{}"))
        val context = facts.getJSONObject("context")
        val eventId = facts.getString("eventId")
        val taskId = facts.getString("taskId")
        val event = context.getJSONArray("events").let { events -> (0 until events.length()).map { events.getJSONObject(it) }.single { it.getString("id") == eventId } }
        worker.execute {
            try {
                check(valid()) { "Facts or permissions changed" }
                val protocol = PreparationRecords.api(this); api = protocol
                protocol.capture(true)
                // Different disclosed facts invalidate old experiences and their retained drafts.
                val p = PreparationRecords.prefs(this)
                for (key in p.all.keys.filter { it.startsWith("plan.") && it != recordKey }) {
                    val old = JSONObject(p.getString(key, "{}"))
                    if (old.optString("eventId") == eventId) {
                        protocol.saved().filter { it.id == old.optString("fallback") || it.id == old.optString("generated") }.forEach(protocol::forget)
                        check(p.edit().remove(key).commit())
                        PreparationRecords.record(this, "invalidation", JSONObject().put("reason", "Disclosed facts changed").put("eventId", eventId))
                    }
                }
                val saved = protocol.saved()
                val existing = saved.firstOrNull { it.id == record.optString("active") }
                if (existing != null) activate(existing, existing.id == record.optString("generated")) else {
                    val source = fallback(context.getJSONArray("tasks"), taskId, event.getInt("startMinute"))
                    protocol.withPreparation(protocol.load("Preparation plan", source)) { prepared ->
                        record.put("eventId", eventId).put("fallback", prepared.id)
                        activate(prepared, false)
                    }
                }
                val generated = protocol.saved().firstOrNull { it.id == record.optString("generated") }
                if (generated != null && generated.id != record.optString("active")) runOnUiThread { tailored = generated }
                if (!record.optBoolean("generationAdmitted") && getSharedPreferences("preparation", 0).getBoolean("generation", false)) {
                    record.put("generationAdmitted", true); remember()
                    generationWorker.execute { generate(context, event, taskId) }
                }
            } catch (error: Throwable) {
                PreparationRecords.record(this, "open", JSONObject().put("status", "failed").put("reason", error.message?.take(1000)))
                runOnUiThread { message = "This plan is no longer available. Return to Calendar." }
            }
        }
        setContent { AppTheme(Accent.Calendar) {
            Scaffold(topBar = { TopAppBar(title = { Text("Preparation plan") }, navigationIcon = { TextButton(onClick = { finish() }) { Text("Back") } }) }) { padding ->
                Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(event.getString("title"), style = MaterialTheme.typography.titleLarge)
                    Text(event.getString("notes").removePrefix("Preparation demo · "), style = MaterialTheme.typography.bodyMedium)
                    if (tailoring) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp); Text("Tailoring your plan…", style = MaterialTheme.typography.bodySmall) }
                    tailored?.let { next -> TextButton(onClick = { worker.execute { try { activate(next, true); runOnUiThread { tailored = null } } catch (_: Throwable) { runOnUiThread { tailored = null; message = "Facts changed. Return to Calendar." } } } }) { Text("View tailored plan") } }
                    surface?.let { ExperienceSurface(it) } ?: Text(message)
                }
            }
        } }
    }
    private fun fallback(tasks: JSONArray, selected: String, start: Int): Source {
        fun quote(value: String) = "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t") + "\""
        val declarations = (0 until tasks.length()).joinToString(" ") { i -> val t = tasks.getJSONObject(i); "let task$i: Task = {id:${quote(t.getString("id"))},title:${quote(t.getString("title"))},chosen:${t.getString("id") == selected}}; tasks[tasks.length] = task$i;" }
        val initial = "let tasks: Task[] = []; $declarations return {tasks:tasks,start:$start,loaded:true};"
        return Source(assets.open("preparation/preparation.deal").bufferedReader().use { it.readText() }.replace("function acceptedState(): State { return {}; }", "function acceptedState(): State { $initial }"), assets.open("preparation/preparation.dealui").bufferedReader().use { it.readText() })
    }
    private fun generate(context: JSONObject, event: JSONObject, selected: String) {
        val protocol = api ?: return
        val started = android.os.SystemClock.elapsedRealtime()
        var timer: Runnable? = null
        try {
            check(foreground && !cancelled && valid() && getSharedPreferences("preparation", 0).getBoolean("generation", false)) { "Left plan or disclosure changed" }
            val task = context.getJSONArray("tasks").let { a -> (0 until a.length()).map { a.getJSONObject(it) }.single { it.getString("id") == selected } }
            val disclosed = JSONObject().put("eventTitle", event.getString("title")).put("eventNote", event.getString("notes")).put("taskTitle", task.getString("title")).put("startMinute", event.getInt("startMinute"))
            val schema = WireType("record", fields = listOf(WireField("eventTitle", WireType("string", maximum = 120)), WireField("eventNote", WireType("string", maximum = 384)), WireField("taskTitle", WireType("string", maximum = 120)), WireField("startMinute", WireType("int", minimumInt = 0, maximumInt = 1439))))
            val objective = "Create a compact useful preparation experience for this accepted event/task relationship. Specialize interactive behavior to the disclosed note, not just a generic departure form. For requested documents offer an editable preparation checklist; for numerical discussion offer editable integer amounts and a calculated comparison plus meeting notes when useful. These are examples, not required domain branches. Do not invent obligations, amounts, document counts or completed actions. Clearly label user-entered assumptions. Keep a locally editable timing estimate. No capabilities, network, appointment writes or task completion. Use the native pack with Section, Toggle, TextField, IntField and Time; initialize useful state immediately, no load button. Use minimal DEAL, no ternary expressions, declare typed arrays and typed items before appending. Define export class State in preparation.deal and export function initialState(): State. In preparation.dealui import * as app from \"./preparation\" and * as ui from \"./platform.dealui-pack\"; declare // @ui-root then export view Preparation(state: app.State): View. Do not name the state class AppState unless declared; imports precede declarations. @ui-update functions take State and a declared exported action class. Use integers for money, optional user-entered values clearly labelled, never fabricated amounts. State array fields MUST default to []; initialize them in initialState with typed local records, never inline record arrays. Within ForEach an action may use payload but MUST NOT capture item.id together with payload; use Option or Toggle with value:item.id to select/focus the item, then render the focused TextField outside ForEach. Handlers never mutate or alias state: create typed records/arrays and return a new State. These restrictions avoid known unsupported lowering patterns. Return both source files."
            EmbeddingLog.capture(applicationContext, true)
            protocol.capture(true)
            val current = protocol.request("Tailored preparation", objective, protocol.snapshot(schema, disclosed.toString())); request = current
            check(foreground && !cancelled)
            runOnUiThread { tailoring = true }
            // Initial live samples took 80–90s plus 34–40s repair; allow one such cycle.
            // Foreground safeguard, not a call quota; Inspect records actual latency.
            timer = Runnable { cancelled = true; worker.execute { runCatching { protocol.cancel(current) } }; tailoring = false }
            deadline.postDelayed(timer, 180000)
            PreparationRecords.record(this, "generation", JSONObject().put("model", "Cortex").put("status", "started").put("trace", protocol.requestTrace(current)))
            protocol.withPreparation(protocol.generate(current)) { experience ->
                check(foreground && !cancelled && valid()) { "Left plan or facts changed" }
                // Persist checked source without mounting it over the user's current plan.
                ExperienceSurface(protocol, experience).use { checked -> checked.activate { error -> throw error } }
                synchronized(recordLock) { record.put("generated", experience.id); remember() }
                PreparationRecords.record(this, "generation", JSONObject().put("model", "Cortex").put("status", "checked").put("attempts", experience.attempts).put("elapsedMs", android.os.SystemClock.elapsedRealtime() - started).put("trace", protocol.requestTrace(current)))
                runOnUiThread { if (foreground) tailored = experience }
            }
        } catch (error: Throwable) {
            PreparationRecords.record(this, "generation", JSONObject().put("model", "Cortex").put("status", if (cancelled) "interrupted or timed out" else "failed").put("elapsedMs", android.os.SystemClock.elapsedRealtime() - started).put("reason", error.message?.take(2000)))
        } finally { timer?.let(deadline::removeCallbacks); request = null; runOnUiThread { tailoring = false } }
    }
    override fun onDestroy() {
        foreground = false; cancelled = true
        request?.let { runCatching { api?.cancel(it) } }
        generationWorker.shutdown()
        worker.execute {
            generationWorker.awaitTermination(15, TimeUnit.SECONDS)
            teardown({ owner?.close() }, { api?.close() })
        }
        worker.shutdown(); deadline.removeCallbacksAndMessages(null); super.onDestroy()
    }
}
