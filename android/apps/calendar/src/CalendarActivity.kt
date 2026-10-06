package dev.deal.apps.calendar

import dev.deal.apps.development.GenerationGateway

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import dev.deal.shell.*
import dev.deal.embedding.*
import org.json.JSONObject
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class CalendarActivity : ComponentActivity() {
    private val worker = Executors.newSingleThreadScheduledExecutor()
    private lateinit var experience: CalendarEmbedding
    private var tree by mutableStateOf<JSONObject?>(null)
    private var status by mutableStateOf("Preparing your experience…")
    private var events by mutableStateOf<List<CalendarEvent>>(emptyList())
    @Volatile private var selected: CalendarEvent? = null
    private var selectedUi by mutableStateOf<CalendarEvent?>(null)
    private var dialog by mutableStateOf(false)
    private var permission by mutableStateOf(false)
    private var connected by mutableStateOf(false)
    private var proposal by mutableStateOf<Int?>(null)
    private var proposalEvent: CalendarEvent? = null
    @Volatile private var observedEvent: CalendarEvent? = null
    private var saved by mutableStateOf("")
    private var version = -1
    private var generationIntent by mutableStateOf("")
    private var discloseEvent by mutableStateOf(false)
    private var generating by mutableStateOf(false)
    private var generationStatus by mutableStateOf("")
    @Volatile private var cancellation: GenerationCancellation? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        experience = CalendarEmbedding(applicationContext, {
            val event = selected ?: error("Choose an event first")
            observedEvent = event
            event.startMinute
        }, { minute ->
            val origin = observedEvent
            check(origin != null && origin == selected) { "Event changed. Recalculate before saving." }
            runOnUiThread { if (origin == selected) { proposalEvent = origin; proposal = minute } }
        })
        refreshEvents()
        setContent {
            AppTheme(Accent.Calendar) {
                Page("A DAY WITH ROOM", "Good plans.\nEasy departures.", "Your calendar, with a little breathing room.") {
                    Panel {
                        Text("Coming up", style = MaterialTheme.typography.headlineSmall)
                        if (!permission) Button(onClick = { requestPermissions(arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR), 1) }) { Text("Connect your calendar") }
                        else if (events.isEmpty()) Text("No upcoming events. Add one in your calendar app to get started.")
                        for (event in events) {
                            OutlinedButton(onClick = { selected = event; selectedUi = event; proposal = null }, modifier = Modifier.fillMaxWidth()) {
                                Text("${if (selectedUi?.id == event.id) "✓  " else ""}${event.time}  ${event.title}")
                            }
                        }
                    }
                    Panel {
                        Text("Create an experience", style = MaterialTheme.typography.titleLarge)
                        OutlinedTextField(generationIntent, { generationIntent = it.take(4096) }, label = { Text("What would you like to do?") }, modifier = Modifier.fillMaxWidth(), minLines = 2, maxLines = 4, enabled = !generating)
                        Row { Checkbox(discloseEvent, { discloseEvent = it }, enabled = !generating); Text("Share selected event with the model") }
                        Text("Development inference uses a remote model. Capability descriptions are shared; provider data is not read during generation.")
                        if (generating) Button(onClick = { cancellation?.cancel(); generationStatus = "Cancelling generation…" }) { Text("Cancel generation") }
                        else Button(onClick = { generate() }, enabled = generationIntent.isNotBlank()) { Text("Generate experience") }
                        if (generationStatus.isNotEmpty()) Text(generationStatus)
                    }
                    EmbeddedExperience {
                        tree?.let { DealRenderer(it, ::dispatch) } ?: Text(status)
                    }
                    Panel {
                        Text("Your connected apps", style = MaterialTheme.typography.titleLarge)
                        Text("Enable discovered capabilities here, then approve access in each provider. Consent stays under native control.")
                        Switch(connected, { enabled ->
                            connected = enabled
                            if (enabled) experience.registry.grantAll() else experience.registry.revokeAll()
                        })
                    }
                    proposal?.let { departure ->
                        Panel {
                            Text("Keep this plan", style = MaterialTheme.typography.titleLarge)
                            Text("A reminder at %02d:%02d. Nothing changes until you confirm.".format(departure / 60, departure % 60))
                            Button(onClick = { dialog = true }) { Text("Review Calendar change") }
                        }
                    }
                    if (saved.isNotEmpty()) Text(saved)
                    if (tree != null && status.isNotEmpty()) Text(status, color = MaterialTheme.colorScheme.error)
                }
                if (dialog) AlertDialog(onDismissRequest = { dialog = false }, title = { Text("Confirm Calendar change") }, text = { Text("Add a departure reminder to ${proposalEvent?.title}? Existing reminders are kept.") }, confirmButton = {
                    TextButton(onClick = {
                        dialog = false
                        val event = proposalEvent; val departure = proposal
                        if (event != null && event == selected && departure != null) {
                            try { CalendarEvents(this).confirmReminder(event, departure); saved = "Departure reminder saved"; proposal = null }
                            catch (error: Throwable) { saved = error.message ?: "Could not save reminder" }
                        } else saved = "Event changed. Recalculate before saving."
                    }) { Text("Confirm write") }
                }, dismissButton = { TextButton(onClick = { dialog = false }) { Text("Keep unchanged") } })
            }
        }
        activate(intent.getStringExtra("deal"), intent.getStringExtra("dealui"))
        worker.scheduleWithFixedDelay({
            try { experience.poll()?.let(::publish) } catch (error: Throwable) { showError(error) }
        }, 100, 100, TimeUnit.MILLISECONDS)
    }
    private fun refreshEvents() {
        permission = checkSelfPermission(Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED
        if (permission) try {
            events = CalendarEvents(this).upcoming()
            val refreshed = selected?.let { old -> events.firstOrNull { it.id == old.id && it.begin == old.begin } } ?: events.firstOrNull()
            if (refreshed != selected) { selected = refreshed; selectedUi = refreshed; proposal = null }
        } catch (error: Throwable) { status = error.message ?: "Calendar unavailable" }
    }
    override fun onResume() { super.onResume(); refreshEvents() }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) { super.onRequestPermissionsResult(requestCode, permissions, grantResults); refreshEvents() }
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); if (intent.hasExtra("deal") || intent.hasExtra("dealui")) activate(intent.getStringExtra("deal"), intent.getStringExtra("dealui")) }
    private fun activate(deal: String?, ui: String?) {
        worker.execute {
            try {
                experience.prepare(applicationContext)
                if (connected) experience.registry.grantAll()
                val remembered = experience.remembered()
                val source = deal ?: remembered?.deal ?: assets.open("experience/departure.deal").bufferedReader().use { it.readText() }
                val view = ui ?: remembered?.ui ?: assets.open("experience/departure.dealui").bufferedReader().use { it.readText() }
                val snapshot = experience.activate(ExperienceSource(source, view))
                version = -1
                File(filesDir, "ui-error.txt").delete()
                runOnUiThread { proposal = null; status = "" }
                publish(snapshot)
            } catch (error: Throwable) { showError(error) }
        }
    }
    private fun generate() {
        val objective = generationIntent
        val origin = if (discloseEvent) selected else null
        val context = origin?.let { JSONObject().put("eventTitle", it.title).put("startMinute", it.startMinute).toString() } ?: "{}"
        val token = GenerationCancellation(); cancellation = token; generating = true; generationStatus = "Discovering capabilities…"
        File(filesDir, "generation-status.json").writeText(JSONObject().put("status", "running").toString())
        worker.execute {
            try {
                experience.prepare(applicationContext)
                val candidate = experience.generate(objective, context, GenerationGateway("http://127.0.0.1:8787/generate"), token) { message -> runOnUiThread { generationStatus = message } }
                candidate.use {
                    token.check()
                    check(origin == null || origin == selected) { "Selected event changed; regenerate" }
                    val snapshot = experience.activate(candidate)
                    version = -1; publish(snapshot)
                    File(filesDir, "generation-status.json").writeText(JSONObject().put("status", "activated").put("attempts", candidate.attempts).toString())
                    runOnUiThread { proposal = null; status = ""; generationStatus = "Generated experience activated" }
                }
            } catch (error: Throwable) {
                File(filesDir, "generation-status.json").writeText(JSONObject().put("status", if (error is java.util.concurrent.CancellationException) "cancelled" else "failed").put("message", error.message?.take(1000)).toString())
                runOnUiThread { generationStatus = if (error is java.util.concurrent.CancellationException) "Generation cancelled; current experience retained" else "Generation failed; current experience retained" }
            } finally { cancellation = null; runOnUiThread { generating = false } }
        }
    }
    private fun dispatch(slot: Int, payload: String?) {
        proposal = null
        worker.execute { try { experience.dispatch(slot, payload)?.let(::publish) } catch (error: Throwable) { showError(error) } }
    }
    private fun publish(snapshot: JSONObject) {
        if (snapshot.getInt("version") == version) return
        version = snapshot.getInt("version")
        val temporary = File(filesDir, "ui-snapshot.tmp").apply { writeText(snapshot.toString()) }
        check(temporary.renameTo(File(filesDir, "ui-snapshot.json"))) { "Cannot publish UI snapshot" }
        val next = snapshot.getJSONObject("tree")
        runOnUiThread { if (!isDestroyed) { tree = next; if (snapshot.optString("fault").isNotEmpty()) status = snapshot.getString("fault") } }
    }
    private fun showError(error: Throwable) {
        android.util.Log.e("DealUi", "Experience error", error)
        File(filesDir, "ui-error.txt").writeText(android.util.Log.getStackTraceString(error))
        runOnUiThread { if (!isDestroyed) status = error.message ?: "Experience unavailable" }
    }
    override fun onDestroy() {
        cancellation?.cancel()
        worker.execute { experience.close() }
        worker.shutdown(); super.onDestroy()
    }
}
