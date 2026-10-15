package dev.deal.apps.calendar

import android.os.Bundle
import android.content.Intent
import android.database.ContentObserver
import android.provider.CalendarContract
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
import dev.deal.embedding.capabilities.*
import dev.deal.embedding.discovery.*
import dev.deal.shell.*
import org.json.*
import java.time.*
import java.util.concurrent.Executors

/** Calendar owns disclosed demo facts, placement and write confirmation. Embedding owns execution. */
@OptIn(ExperimentalMaterial3Api::class)
class PreparationActivity : ComponentActivity() {
    private val worker = Executors.newSingleThreadExecutor()
    private val generationWorker = Executors.newSingleThreadExecutor()
    private val discovery by lazy { Discovery(applicationContext) }
    @Volatile private var api: ApplicationProtocol? = null
    private var owner: ExperienceSurface? = null
    private val ownerLock = Any()
    @Volatile private var activeInput: String? = null
    private var surface by mutableStateOf<ExperienceSurface?>(null)
    private var message by mutableStateOf("Opening preparation…")
    private var suggestion by mutableStateOf<DiscoverySuggestion?>(null)
    private var checking by mutableStateOf(false)
    private var generating by mutableStateOf(false)
    private var connected by mutableStateOf(false)
    private var proposal by mutableStateOf<Int?>(null)
    private var confirm by mutableStateOf(false)
    private var eventUi by mutableStateOf<ShowEvent?>(null)
    @Volatile private var event: ShowEvent? = null
    @Volatile private var foreground = false
    @Volatile private var revision = 0L
    @Volatile private var request: GenerationRequest? = null
    private var eventId = -1L
    private lateinit var day: LocalDate
    @Volatile private var current: DiscoveryInput? = null
    @Volatile private var observed: ShowEvent? = null
    @Volatile private var activeEvent: ShowEvent? = null
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private val observer = object : ContentObserver(handler) { override fun onChange(selfChange: Boolean) { if (foreground) refresh() } }
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        eventId = intent.getLongExtra("eventId", -1L)
        require(eventId >= 0) { "Missing demo event" }
        day = LocalDate.parse(intent.getStringExtra("day") ?: error("Missing demo day"))
        contentResolver.registerContentObserver(CalendarContract.CONTENT_URI, true, observer)
        setContent { AppTheme(Accent.Calendar) {
            Scaffold(topBar = { TopAppBar(title = { Text("Preparation plan") }, navigationIcon = { TextButton(onClick = { finish() }) { Text("Back") } }, actions = {
                TextButton(onClick = { startActivity(Intent(this@PreparationActivity, PreparationInspectActivity::class.java)) }) { Text("Inspect") }
            }) }) { padding ->
                Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    eventUi?.let { e -> Text(e.title, style = MaterialTheme.typography.titleLarge); Text(e.notes.removePrefix("Preparation demo · ")) }
                    Text("On-device discovery uses this Gig Demo event. Opening a suggestion shares its captured facts with ${dev.deal.apps.development.GenerationBuild.label} for generation. Appointments change only after native confirmation.", style = MaterialTheme.typography.bodySmall)
                    if (checking) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp); Text("Looking for useful help…") }
                    suggestion?.let { tip -> Card { Column(Modifier.padding(12.dp)) {
                        Text(tip.title, style = MaterialTheme.typography.titleMedium)
                        Row { TextButton(onClick = { accept(tip) }, enabled = !generating) { Text("Open") }
                            TextButton(onClick = {
                                check(getSharedPreferences("preparation-discovery-dismissals", 0).edit().putBoolean(current!!.identity, true).commit())
                                suggestion = null; discovery.cancel(); message = ""
                            }) { Text("Dismiss") } }
                    } } }
                    if (generating) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp); Text("Building your interaction…") }
                    if (message.isNotEmpty()) Text(message)
                    surface?.let { ExperienceSurface(it, onSnapshot = { snapshot ->
                        DiagnosticFiles.write(filesDir, "preparation-ui.json", snapshot.json().toString())
                    }) }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Allow Calendar capabilities")
                        Switch(connected, { value ->
                            if (value) api?.grant("host/calendar") else api?.revoke("host/calendar")
                            connected = value; proposal = null; confirm = false
                        }, enabled = api != null)
                    }
                    proposal?.let { minute ->
                        Text("Proposed reminder: %02d:%02d. Nothing has changed.".format(minute / 60, minute % 60))
                        TextButton(onClick = { confirm = true }) { Text("Review Calendar change") }
                    }
                }
            }
            if (confirm) AlertDialog(onDismissRequest = { confirm = false }, title = { Text("Confirm Calendar change") }, text = { Text("Add the proposed reminder to this Gig Demo event? Existing reminders stay unchanged.") },
                confirmButton = { TextButton(onClick = {
                    confirm = false
                    val origin = observed; val minute = proposal
                    try {
                        check(foreground && connected && origin != null && origin == event && origin == activeEvent && minute != null)
                        ShowAgenda(this@PreparationActivity).confirmReminder(origin, minute)
                        proposal = null; message = "Reminder saved"
                    } catch (error: Throwable) { proposal = null; message = error.message ?: "Calendar change failed" }
                }) { Text("Confirm write") } }, dismissButton = { TextButton(onClick = { confirm = false }) { Text("Keep unchanged") } })
        } }
    }
    override fun onResume() { super.onResume(); foreground = true; refresh() }
    override fun onPause() {
        foreground = false; invalidate()
        super.onPause()
    }
    private fun invalidate() {
        synchronized(ownerLock) { revision++ }; discovery.cancel(); suggestion = null; checking = false
        api?.revoke("host/calendar"); connected = false; observed = null; surface = null
        request?.let { runCatching { api?.cancel(it) } }
        proposal = null; confirm = false
    }
    private fun readEvent(): ShowEvent = ShowAgenda(this).events(day).singleOrNull { it.id == eventId } ?: error("Demo event changed or removed")
    private fun input(e: ShowEvent, contracts: List<SharedCapabilityContract>): DiscoveryInput = DiscoveryInput(listOf(
        DiscoveryFact("event", "Gig Demo event ${e.id}: ${e.title}. Starts ${Instant.ofEpochMilli(e.from).atZone(AGENDA_ZONE)}; ends ${Instant.ofEpochMilli(e.until).atZone(AGENDA_ZONE)}. Location: ${e.location}. Start minute ${Instant.ofEpochMilli(e.from).atZone(AGENDA_ZONE).let { it.hour * 60 + it.minute }}. Notes: ${e.notes.removePrefix("Preparation demo · ")}")
    ), contracts)
    private fun refresh() {
        invalidate()
        val epoch = revision
        worker.execute {
            try {
                check(foreground && epoch == revision)
                val e = readEvent()
                val protocol = api ?: createApi().also { api = it }
                val facts = input(e, protocol.refresh())
                if (current?.identity != facts.identity) {
                    protocol.revoke("host/calendar"); activeEvent = null
                    val previous = synchronized(ownerLock) { owner.also { owner = null; activeInput = null } }; previous?.close()
                    runOnUiThread { surface = null; connected = false }
                }
                current = facts; event = e
                val retained = PreparationRecords.prefs(this).getString("opportunity.${e.id}", null)?.let(::JSONObject)
                if (activeInput != facts.identity && retained?.optString("input") == facts.identity) {
                    protocol.saved().firstOrNull { it.id == retained.optString("experience") }?.let { saved ->
                        activate(protocol, saved, e, facts.identity, epoch)
                    }
                }
                runOnUiThread {
                    if (!foreground || epoch != revision) return@runOnUiThread
                    eventUi = e
                    if (activeInput == facts.identity) { surface = synchronized(ownerLock) { owner }; message = ""; return@runOnUiThread }
                    if (getSharedPreferences("preparation-discovery-dismissals", 0).getBoolean(facts.identity, false)) { message = ""; return@runOnUiThread }
                    checking = true; message = ""
                    val started = android.os.SystemClock.elapsedRealtime()
                    discovery.discover(facts).whenComplete { tip, error -> runOnUiThread {
                        if (!foreground || epoch != revision) return@runOnUiThread
                        checking = false
                        message = if (error == null) "" else "Discovery failed: ${error.cause?.message ?: error.message}"
                        suggestion = tip
                        PreparationRecords.record(this, "opportunity", JSONObject().put("status", if (error != null) "failed" else if (tip == null) "none" else "suggested").put("elapsedMs", android.os.SystemClock.elapsedRealtime() - started).put("title", tip?.title).put("brief", tip?.brief).put("input", facts.identity).put("reason", error?.toString()))
                    } }
                }
            } catch (error: Throwable) { runOnUiThread { if (foreground && epoch == revision) { checking = false; message = error.message ?: "Preparation unavailable" } } }
        }
    }
    private fun createApi(): ApplicationProtocol {
        val caps = calendarCapabilities({
            val now = readEvent(); check(foreground && now == event && now == activeEvent) { "Event changed" }; observed = now
            Instant.ofEpochMilli(now.from).atZone(AGENDA_ZONE).let { it.hour * 60 + it.minute }
        }, { minute ->
            val origin = observed; check(foreground && origin != null && origin == readEvent() && origin == event && origin == activeEvent) { "Read the current event before proposing a reminder" }
            runOnUiThread { if (foreground && origin == event && connected) proposal = minute }
        })
        val registry = CapabilityRegistry(this, CapabilityDiscovery(this) { false }, listOf(caps))
        return PreparationRecords.opportunityApi(this, registry)
    }
    private fun activate(protocol: ApplicationProtocol, experience: Experience, captured: ShowEvent, identity: String, epoch: Long) {
        check(foreground && epoch == revision && readEvent() == captured) { "Facts changed before activation" }
        val next = ExperienceSurface(protocol, experience)
        try {
            next.activate { error -> runOnUiThread { if (foreground && epoch == revision) message = error.message ?: "Interaction failed" } }
            val previous = synchronized(ownerLock) {
                check(foreground && epoch == revision && readEvent() == captured) { "Facts changed during activation" }
                owner.also { owner = next; activeInput = identity; activeEvent = captured }
            }
            runOnUiThread { if (foreground && epoch == revision) surface = next }
            previous?.close()
        } catch (error: Throwable) { runCatching { next.close() }.onFailure(error::addSuppressed); throw error }
    }
    private fun accept(tip: DiscoverySuggestion) {
        if (generating) return
        val epoch = revision; val facts = current ?: return
        suggestion = null; generating = true; message = ""
        generationWorker.execute {
            val started = android.os.SystemClock.elapsedRealtime()
            try {
                val protocol = api ?: error("Preparation unavailable")
                check(foreground && epoch == revision && readEvent() == event)
                val fresh = input(readEvent(), protocol.refresh())
                check(fresh.identity == facts.identity)
                val admitted = discovery.request(tip, fresh, protocol); request = admitted
                activeEvent = event
                if (!foreground || epoch != revision) protocol.cancel(admitted)
                protocol.withPreparation(protocol.generate(admitted)) { experience ->
                    check(foreground && epoch == revision && readEvent() == event) { "Facts changed during generation" }
                    activate(protocol, experience, event!!, facts.identity, epoch)
                    check(PreparationRecords.prefs(this).edit().putString("opportunity.$eventId", JSONObject().put("input", facts.identity).put("experience", experience.id).toString()).commit())
                    PreparationRecords.record(this, "opportunity-generation", JSONObject().put("status", "activated").put("experience", experience.id).put("trace", protocol.requestTrace(admitted)).put("attempts", experience.attempts).put("elapsedMs", android.os.SystemClock.elapsedRealtime() - started).put("brief", tip.brief))
                }
            } catch (error: Throwable) {
                PreparationRecords.record(this, "opportunity-generation", JSONObject().put("status", "failed").put("reason", error.toString()).put("elapsedMs", android.os.SystemClock.elapsedRealtime() - started))
                runOnUiThread { if (foreground && epoch == revision) message = "Generation failed: ${error.message}" }
            } finally { request = null; runOnUiThread { generating = false } }
        }
    }
    override fun onDestroy() {
        foreground = false; invalidate(); contentResolver.unregisterContentObserver(observer)
        generationWorker.shutdown()
        worker.execute { teardown({ discovery.close() }, { check(generationWorker.awaitTermination(15, java.util.concurrent.TimeUnit.SECONDS)) { "Generation worker did not terminate" } }, { synchronized(ownerLock) { owner }?.close() }, { api?.close() }) }
        worker.shutdown(); super.onDestroy()
    }
}
