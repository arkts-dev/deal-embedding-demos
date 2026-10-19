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
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.MoreVert
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
    private var discovery: Discovery? = null
    @Volatile private var api: ApplicationProtocol? = null
    private var owner: ExperienceSurface? = null
    private val ownerLock = Any()
    @Volatile private var activeInput: String? = null
    @Volatile private var connectedModules: List<String> = emptyList()
    private var surface by mutableStateOf<ExperienceSurface?>(null)
    private var message by mutableStateOf("Opening preparation…")
    private var suggestion by mutableStateOf<DiscoverySuggestion?>(null)
    private var checking by mutableStateOf(false)
    private var generating by mutableStateOf(false)
    private var failed = false
    private var connected by mutableStateOf(false)
    private var menu by mutableStateOf(false)
    private var access by mutableStateOf(false)
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
            Scaffold(topBar = { TopAppBar(title = { Text(eventUi?.title ?: "Preparation", maxLines = 1) }, navigationIcon = {
                IconButton(onClick = { finish() }) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back") }
            }, actions = {
                if (surface != null) IntelligenceMark("AI-built")
                TextButton(onClick = { startActivity(Intent(this@PreparationActivity, PreparationInspectActivity::class.java)) }) { Text("Inspect") }
                AssistanceSettingsButton(storageName = "preparation-opportunity")
                Box {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Outlined.MoreVert, "More options") }
                    DropdownMenu(menu, { menu = false }) {
                        DropdownMenuItem(text = { Text("Event details") }, onClick = {
                            menu = false
                            startActivity(Intent(this@PreparationActivity, EventDetailsActivity::class.java).putExtra("eventId", eventId).putExtra("day", day.toString()))
                        })
                        DropdownMenuItem(text = { Text("Calendar access") }, onClick = { menu = false; access = true })
                    }
                }
            }) }) { padding ->
                Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    eventUi?.let { selected ->
                        val start = Instant.ofEpochMilli(selected.from).atZone(AGENDA_ZONE)
                        val end = Instant.ofEpochMilli(selected.until).atZone(AGENDA_ZONE)
                        Text("${start.format(java.time.format.DateTimeFormatter.ofPattern("EEE d MMM · HH:mm"))}–${end.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (selected.location.isNotEmpty()) Text(selected.location, style = MaterialTheme.typography.bodyMedium)
                    }
                    if (checking || generating) Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                        Text(if (generating) "Preparing…" else "Looking ahead…", style = MaterialTheme.typography.bodySmall)
                    }
                    suggestion?.let { tip -> OutlinedCard(Modifier.fillMaxWidth()) {
                        Box(Modifier.padding(start = 16.dp, top = 12.dp)) { IntelligenceMark("Suggestion") }
                        Row(Modifier.padding(start = 16.dp, top = 8.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(tip.title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                            IconButton(onClick = {
                                check(getSharedPreferences("preparation-discovery-dismissals", 0).edit().putBoolean(current!!.identity, true).commit())
                                suggestion = null; discovery?.cancel(); message = "No suggestions"
                                PreparationRecords.record(this@PreparationActivity, "opportunity-dismissal", JSONObject().put("input", current!!.identity).put("status", "dismissed"))
                            }) { Icon(Icons.Outlined.Close, "Dismiss") }
                        }
                        Button(onClick = { accept(tip) }, enabled = !generating, modifier = Modifier.padding(start = 16.dp, bottom = 12.dp)) { Text("Open") }
                    } }
                    if (message.isNotEmpty()) Text(message)
                    if (!checking && !generating && message in setOf("Couldn’t find help", "Preparation unavailable", "Couldn’t create this tool", "Tool unavailable")) {
                        TextButton(onClick = { failed = false; refresh() }) { Text("Try again") }
                    }
                    surface?.let { ExperienceSurface(it) }
                    proposal?.let { minute ->
                        OutlinedButton(onClick = { confirm = true }) { Text("Review reminder · %02d:%02d".format(minute / 60, minute % 60)) }
                    }
                }
            }
            if (access) AlertDialog(onDismissRequest = { access = false }, title = { Text("Calendar access") },
                text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Allow this tool to read the selected event and suggest a reminder. Saving still needs your confirmation.")
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Selected event", Modifier.weight(1f))
                        Switch(connected, { value ->
                            if (value) api?.grant("host/calendar") else api?.revoke("host/calendar")
                            connected = value; proposal = null; confirm = false
                        }, enabled = api != null)
                    }
                } }, confirmButton = { TextButton(onClick = { access = false }) { Text("Done") } })
            if (confirm) AlertDialog(onDismissRequest = { confirm = false }, title = { Text("Confirm Calendar change") }, text = { Text("Add a reminder at %02d:%02d?".format((proposal ?: 0) / 60, (proposal ?: 0) % 60)) },
                confirmButton = { TextButton(onClick = {
                    confirm = false
                    val origin = observed; val minute = proposal
                    try {
                        check(foreground && connected && origin != null && origin == event && origin == activeEvent && minute != null)
                        ShowAgenda(this@PreparationActivity).confirmReminder(origin, minute)
                        proposal = null; message = "Reminder saved"
                    } catch (error: Throwable) { proposal = null; failure("reminder", "Couldn’t save reminder", error) }
                }) { Text("Save reminder") } }, dismissButton = { TextButton(onClick = { confirm = false }) { Text("Keep unchanged") } })
        } }
    }
    override fun onResume() { super.onResume(); foreground = true; refresh() }
    override fun onPause() {
        foreground = false; invalidate()
        super.onPause()
    }
    private fun invalidate() {
        synchronized(ownerLock) { revision++ }; discovery?.cancel(); suggestion = null; checking = false
        api?.let { protocol -> connectedModules.forEach(protocol::revoke); protocol.revoke("host/calendar") }; connected = false; observed = null; surface = null
        request?.let { runCatching { api?.cancel(it) } }
        proposal = null; confirm = false; access = false; menu = false
    }
    private fun failure(stage: String, label: String, error: Throwable) {
        PreparationRecords.record(this, stage, JSONObject().put("status", "failed").put("reason", android.util.Log.getStackTraceString(error)))
        message = label; failed = true
    }
    private fun readEvent(): ShowEvent = ShowAgenda(this).events(day).singleOrNull { it.id == eventId } ?: error("Demo event changed or removed")
    private fun input(e: ShowEvent): DiscoveryInput = DiscoveryInput(listOf(
        DiscoveryFact("event", "Gig Demo event ${e.id}: ${e.title}. Starts ${Instant.ofEpochMilli(e.from).atZone(AGENDA_ZONE)}; ends ${Instant.ofEpochMilli(e.until).atZone(AGENDA_ZONE)}. Location: ${e.location}. Start minute ${Instant.ofEpochMilli(e.from).atZone(AGENDA_ZONE).let { it.hour * 60 + it.minute }}. Notes: ${e.notes.removePrefix("Preparation demo · ")}")
    ))
    private fun refresh() {
        invalidate()
        val epoch = revision
        worker.execute {
            try {
                check(foreground && epoch == revision)
                val e = readEvent()
                val protocol = api ?: createApi().also { api = it }
                connectedModules = Connections.apply(this, protocol)
                val facts = input(e)
                val finder = discovery ?: Discovery(applicationContext, protocol).also { discovery = it }
                if (current?.identity != facts.identity) {
                    protocol.revoke("host/calendar"); activeEvent = null
                    val previous = synchronized(ownerLock) { owner.also { owner = null; activeInput = null } }; previous?.close()
                    runOnUiThread { surface = null; connected = false }
                }
                val sameInput = current?.identity == facts.identity
                if (!sameInput) failed = false
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
                    if (sameInput && failed) return@runOnUiThread
                    if (activeInput == facts.identity) { surface = synchronized(ownerLock) { owner }; message = ""; return@runOnUiThread }
                    if (!Connections.settings(this).getBoolean("discovery", true)) { message = "Suggestions are off"; return@runOnUiThread }
                    if (getSharedPreferences("preparation-discovery-dismissals", 0).getBoolean(facts.identity, false)) { message = "No suggestions"; return@runOnUiThread }
                    checking = true; message = ""
                    finder.discover(facts).whenComplete { tip, error -> runOnUiThread {
                        if (!foreground || epoch != revision) return@runOnUiThread
                        checking = false; failed = error != null
                        message = if (error != null) "Couldn’t find help" else if (tip == null) "No suggestions" else ""
                        suggestion = tip
                    } }
                }
            } catch (error: Throwable) { runOnUiThread { if (foreground && epoch == revision) { checking = false; failure("opportunity-open", "Preparation unavailable", error) } } }
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
        val registry = CapabilityRegistry(this, CapabilityDiscovery(this) { uid -> uid != applicationInfo.uid && Connections.trusted(this, uid) }, listOf(caps))
        return PreparationRecords.opportunityApi(this, registry)
    }
    private fun activate(protocol: ApplicationProtocol, experience: Experience, captured: ShowEvent, identity: String, epoch: Long) {
        check(foreground && epoch == revision && readEvent() == captured) { "Facts changed before activation" }
        val next = ExperienceSurface(protocol, experience)
        try {
            next.activate { error -> runOnUiThread { if (foreground && epoch == revision) failure("opportunity-execution", "Tool unavailable", error) } }
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
        if (!Connections.settings(this).getBoolean("generation", false)) {
            message = "Enable tailored tools in Settings"
            return
        }
        val epoch = revision; val facts = current ?: return
        suggestion = null; generating = true; message = ""
        generationWorker.execute {
            val started = android.os.SystemClock.elapsedRealtime()
            try {
                val protocol = api ?: error("Preparation unavailable")
                check(foreground && epoch == revision && readEvent() == event)
                check(Connections.settings(this).getBoolean("generation", false)) { "Generation disclosure disabled" }
                val fresh = input(readEvent())
                check(fresh.identity == facts.identity)
                val admitted = checkNotNull(discovery).request(tip, fresh); request = admitted
                activeEvent = event
                PreparationRecords.record(this, "opportunity-generation", JSONObject().put("status", "started").put("model", GenerationBuild.label).put("trace", protocol.requestTrace(admitted)).put("input", facts.identity).put("brief", tip.brief))
                if (!foreground || epoch != revision) protocol.cancel(admitted)
                protocol.withPreparation(protocol.generate(admitted)) { experience ->
                    check(foreground && epoch == revision && readEvent() == event) { "Facts changed during generation" }
                    activate(protocol, experience, event!!, facts.identity, epoch)
                    check(PreparationRecords.prefs(this).edit().putString("opportunity.$eventId", JSONObject().put("input", facts.identity).put("experience", experience.id).toString()).commit())
                    PreparationRecords.record(this, "opportunity-generation", JSONObject().put("status", "activated").put("experience", experience.id).put("trace", protocol.requestTrace(admitted)).put("attempts", experience.attempts).put("elapsedMs", android.os.SystemClock.elapsedRealtime() - started).put("brief", tip.brief))
                }
            } catch (error: Throwable) {
                PreparationRecords.record(this, "opportunity-generation", JSONObject().put("status", "failed").put("reason", error.toString()).put("elapsedMs", android.os.SystemClock.elapsedRealtime() - started))
                runOnUiThread { if (foreground && epoch == revision) { failed = true; message = "Couldn’t create this tool" } }
            } finally { request = null; runOnUiThread { generating = false } }
        }
    }
    override fun onDestroy() {
        foreground = false; invalidate(); contentResolver.unregisterContentObserver(observer)
        generationWorker.shutdown()
        worker.execute { teardown({ discovery?.close() }, { check(generationWorker.awaitTermination(15, java.util.concurrent.TimeUnit.SECONDS)) { "Generation worker did not terminate" } }, { synchronized(ownerLock) { owner }?.close() }, { api?.close() }) }
        worker.shutdown(); super.onDestroy()
    }
}
