package dev.deal.apps.calendar

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import java.time.format.DateTimeFormatter
import java.util.Locale
import dev.deal.shell.*
import android.os.Bundle
import java.time.*

val AGENDA_ZONE: ZoneId = ZoneId.of("Europe/Berlin")
const val SHOW_CALENDAR = "Gig Demo"

/** Show events live in a dedicated calendar; personal calendars are never read or written. */
class ShowAgenda(private val context: Context) {
    fun calendarId(): Long? {
        val uri = CalendarContract.Calendars.CONTENT_URI
        val projection = arrayOf(CalendarContract.Calendars._ID, CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, CalendarContract.Calendars.ACCOUNT_NAME)
        context.contentResolver.query(uri, projection, "${CalendarContract.Calendars.CALENDAR_DISPLAY_NAME}=?", arrayOf(SHOW_CALENDAR), null)?.use { cursor ->
            if (cursor.moveToFirst()) return cursor.getLong(0)
        }
        val values = ContentValues().apply {
            put(CalendarContract.Calendars.ACCOUNT_NAME, "deal.demo")
            put(CalendarContract.Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL)
            put(CalendarContract.Calendars.NAME, SHOW_CALENDAR)
            put(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, SHOW_CALENDAR)
            put(CalendarContract.Calendars.CALENDAR_COLOR, 0xff5B3FA8.toInt())
            put(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL, CalendarContract.Calendars.CAL_ACCESS_OWNER)
            put(CalendarContract.Calendars.OWNER_ACCOUNT, "deal.demo")
            put(CalendarContract.Calendars.SYNC_EVENTS, 1)
        }
        val created = context.contentResolver.insert(CalendarContract.Calendars.CONTENT_URI.buildUpon()
            .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
            .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, "deal.demo")
            .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL).build(), values)
        return created?.lastPathSegment?.toLongOrNull()
    }

    fun events(date: LocalDate): List<ShowEvent> {
        val id = calendarId() ?: return emptyList()
        val from = date.atStartOfDay(AGENDA_ZONE).toInstant().toEpochMilli()
        val until = date.plusDays(1).atStartOfDay(AGENDA_ZONE).toInstant().toEpochMilli()
        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon().also {
            android.content.ContentUris.appendId(it, from); android.content.ContentUris.appendId(it, until)
        }.build()
        val projection = arrayOf(CalendarContract.Instances.EVENT_ID, CalendarContract.Instances.TITLE, CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END, CalendarContract.Instances.EVENT_LOCATION, CalendarContract.Instances.DESCRIPTION)
        val result = mutableListOf<ShowEvent>()
        context.contentResolver.query(uri, projection, "${CalendarContract.Instances.CALENDAR_ID}=?", arrayOf(id.toString()), "begin ASC")?.use { cursor ->
            while (cursor.moveToNext()) result += ShowEvent(cursor.getLong(0), cursor.getString(1) ?: "Event", cursor.getLong(2), cursor.getLong(3),
                cursor.getString(4) ?: "", cursor.getString(5) ?: "")
        }
        return result
    }

    fun add(title: String, from: Long, until: Long, location: String, owner: String, notes: String, reminder: Int?): Long? {
        val id = calendarId() ?: return null
        val values = ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, id)
            put(CalendarContract.Events.TITLE, title)
            put(CalendarContract.Events.DTSTART, from)
            put(CalendarContract.Events.DTEND, until)
            put(CalendarContract.Events.EVENT_TIMEZONE, AGENDA_ZONE.id)
            put(CalendarContract.Events.EVENT_LOCATION, location)
            put(CalendarContract.Events.DESCRIPTION, if (owner.isEmpty()) notes else "$notes\nResponsible: $owner")
            put(CalendarContract.Events.HAS_ALARM, if (reminder != null) 1 else 0)
        }
        val created = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values) ?: return null
        val eventId = created.lastPathSegment?.toLongOrNull() ?: return null
        if (reminder != null) {
            context.contentResolver.insert(CalendarContract.Reminders.CONTENT_URI, ContentValues().apply {
                put(CalendarContract.Reminders.EVENT_ID, eventId)
                put(CalendarContract.Reminders.MINUTES, reminder)
                put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
            })
        }
        return eventId
    }

    /** The show schedule is seeded once, so every re-run starts from the same agenda. */
    fun seedSchedule(date: LocalDate) {
        if (events(date).any { it.title.contains("soundcheck", ignoreCase = true) }) return
        fun at(hour: Int, minute: Int = 0) = date.atTime(hour, minute).atZone(AGENDA_ZONE).toInstant().toEpochMilli()
        add("Glass Harbour soundcheck", at(16), at(16, 45), "Stage", "Glass Harbour", "Show schedule", 30)
        add("Static Bloom soundcheck", at(17), at(17, 45), "Stage", "Static Bloom", "Show schedule", 30)
        add("Doors", at(19), at(19, 30), "Front of house", "Venue", "Show schedule", null)
        add("Glass Harbour performance", at(20), at(21), "Stage", "Glass Harbour", "Show schedule", 30)
        add("Static Bloom performance", at(21, 15), at(22, 15), "Stage", "Static Bloom", "Show schedule", 30)
    }

    /** Explicit demo setup; never edits existing personal events. */
    fun preparationDemo(title: String, note: String, day: LocalDate = LocalDate.now(AGENDA_ZONE).plusDays(1)) {
        // Tooling replaces only explicitly labelled preparation fixtures, including
        // the prior scenario's date. Other show and personal records remain untouched.
        (-7L..14L).flatMap { events(LocalDate.now(AGENDA_ZONE).plusDays(it)) }
            .filter { it.notes.startsWith("Preparation demo ·") }.distinctBy { it.id }.forEach { delete(it.id) }
        val start = day.atTime(15, 0).atZone(AGENDA_ZONE).toInstant().toEpochMilli()
        add(title, start, start + 3600_000, "Demo location", "", "Preparation demo · $note", null)
    }

    fun delete(eventId: Long) {
        context.contentResolver.delete(CalendarContract.Events.CONTENT_URI, "${CalendarContract.Events._ID}=? AND ${CalendarContract.Events.CALENDAR_ID}=?",
            arrayOf(eventId.toString(), calendarId()?.toString() ?: "-1"))
    }
}

data class ShowEvent(val id: Long, val title: String, val from: Long, val until: Long, val location: String, val notes: String) {
    val overlapping: Boolean get() = false
}

@OptIn(ExperimentalMaterial3Api::class)
class AgendaActivity : ComponentActivity() {
    private val agenda by lazy { ShowAgenda(this) }
    private val worker = java.util.concurrent.Executors.newSingleThreadExecutor()
    private var discovery: PreparationDiscovery? = null
    private var suggestion by mutableStateOf<org.json.JSONObject?>(null)
    private var discoveryMessage by mutableStateOf("")
    private var checking by mutableStateOf(false)
    private var accepting by mutableStateOf(false)
    @Volatile private var showingAgenda = true
    @Volatile private var visible = true
    private var resumeVersion by mutableStateOf(0)
    @Volatile private var discoveryRevision = 0L
    private var pendingDay: LocalDate? = null
    override fun onPause() { visible = false; discoveryRevision++; pendingDay = null; discovery?.foreground = false; suggestion = null; super.onPause() }
    override fun onResume() { super.onResume(); visible = true; discovery?.foreground = showingAgenda; resumeVersion++ }
    private fun checkPreparation(day: LocalDate) {
        pendingDay = day
        val revision = ++discoveryRevision
        suggestion = null
        discovery?.foreground = false
        if (checking || !visible || !showingAgenda) return
        pendingDay = null
        checking = true
        discoveryMessage = "Checking preparation…"
        worker.execute {
            try {
                if (revision != discoveryRevision || !visible || !showingAgenda) return@execute
                val service = discovery ?: PreparationDiscovery(applicationContext).also { discovery = it }
                service.foreground = revision == discoveryRevision && visible && showingAgenda
                val result = service.discover(day)
                runOnUiThread {
                    if (revision == discoveryRevision && visible && showingAgenda && service.enabled()) {
                        suggestion = result.optJSONObject("suggestion")?.takeIf { it.length() > 0 }
                        discoveryMessage = when (result.getString("status")) {
                            "unavailable" -> "AI is unavailable. No suggestion was made; unchanged facts won’t be sent again."
                            "stale" -> "Facts changed during the check. Open the agenda again."
                            "rejected" -> "AI returned an unsupported suggestion. Nothing was added."
                            "already_checked", "dismissed" -> ""
                            "no_relationship" -> "No preparation suggestion for these facts."
                                                        else -> ""
                        }
                    }
                }
            } catch (error: Throwable) { android.util.Log.e("Preparation", "Discovery failed", error); runOnUiThread { if (revision == discoveryRevision) discoveryMessage = "Couldn’t check preparation. Ensure Todo sharing is enabled; no changes were made." } }
            finally { runOnUiThread {
                checking = false
                pendingDay?.let { next -> if (visible && showingAgenda) checkPreparation(next) }
            } }
        }
    }
    override fun onDestroy() { worker.execute { discovery?.close() }; worker.shutdown(); super.onDestroy() }
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        if (intent.hasExtra("demoEvent") && checkSelfPermission(android.Manifest.permission.WRITE_CALENDAR) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            agenda.preparationDemo(intent.getStringExtra("demoEvent")!!, intent.getStringExtra("demoNote") ?: "", intent.getStringExtra("demoDate")?.let(LocalDate::parse) ?: LocalDate.now(AGENDA_ZONE).plusDays(1))
        }
        setContent {
            AppTheme(Accent.Calendar) {
                var date by remember { mutableStateOf(LocalDate.now(AGENDA_ZONE)) }
                var events by remember { mutableStateOf(emptyList<ShowEvent>()) }
                var adding by remember { mutableStateOf(false) }
                var selected by remember { mutableStateOf<ShowEvent?>(null) }
                var granted by remember { mutableStateOf(checkSelfPermission(android.Manifest.permission.READ_CALENDAR) == android.content.pm.PackageManager.PERMISSION_GRANTED && checkSelfPermission(android.Manifest.permission.WRITE_CALENDAR) == android.content.pm.PackageManager.PERMISSION_GRANTED) }
                var error by remember { mutableStateOf("") }
                var disclose by remember { mutableStateOf(getSharedPreferences("preparation", 0).getBoolean("disclose", false)) }
                var access by remember { mutableStateOf(false) }
                var disclosureDialog by remember { mutableStateOf(false) }
                var clearing by remember { mutableStateOf(false) }
                var clearConfirm by remember { mutableStateOf(false) }
                val permissionRequest = androidx.activity.compose.rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
                    granted = checkSelfPermission(android.Manifest.permission.READ_CALENDAR) == android.content.pm.PackageManager.PERMISSION_GRANTED && checkSelfPermission(android.Manifest.permission.WRITE_CALENDAR) == android.content.pm.PackageManager.PERMISSION_GRANTED
                }
                fun refresh() {
                    runCatching { agenda.events(date) }.onSuccess { events = it; error = "" }.onFailure { error = "Couldn’t load your calendar." }
                }
                LaunchedEffect(date, granted, resumeVersion) {
                    if (granted) { runCatching { agenda.seedSchedule(showDate()) }; refresh() }
                }
                LaunchedEffect(date, granted, disclose, resumeVersion, adding, selected, access) {
                    showingAgenda = !adding && selected == null && !access
                    discovery?.foreground = visible && showingAgenda && !checking
                    if (granted && disclose && showingAgenda) checkPreparation(date)
                    else { discoveryRevision++; pendingDay = null; suggestion = null; discovery?.foreground = false }
                }
                BackHandler(enabled = adding || selected != null || access) { adding = false; selected = null; access = false }
                Scaffold(topBar = {
                    TopAppBar(title = {
                        Column {
                            Text(if (access) "Extensions" else "Calendar", style = MaterialTheme.typography.titleLarge)
                            if (!access) Text(date.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())), style = MaterialTheme.typography.bodyMedium)
                        }
                    }, navigationIcon = {
                        if (access) IconButton(onClick = { access = false }) { Icon(Icons.Outlined.ArrowBack, "Back") }
                    }, actions = {
                        if (!access) {
                            TextButton(onClick = { date = LocalDate.now(AGENDA_ZONE); selected = null; adding = false }) { Text("Today") }
                            IconButton(onClick = { adding = true }, enabled = granted) { Icon(Icons.Outlined.Add, "Add event") }
                            IconButton(onClick = { access = true; suggestion = null }) { Icon(Icons.Outlined.Settings, "Extensions") }
                        }
                    })
                }) { padding ->
                    Column(Modifier.padding(padding).fillMaxSize()) {
                        if (!adding && selected == null && !access) WeekStrip(date) { date = it }
                        when {
                            access -> Column(Modifier.padding(20.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                Text("Preparation help", style = MaterialTheme.typography.titleLarge)
                                Text("Optional AI assistance. No appointment or task is changed.", style = MaterialTheme.typography.bodyMedium)
                                Text("AI permissions", style = MaterialTheme.typography.titleMedium)
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) { Text("AI discovery"); Text("Jev uses selected and next-day event notes and task titles.", style = MaterialTheme.typography.bodySmall) }
                                    Switch(disclose, { enabled ->
                                        if (enabled) disclosureDialog = true else {
                                            getSharedPreferences("preparation", 0).edit().putBoolean("disclose", false).commit()
                                            disclose = false; suggestion = null; discovery?.foreground = false
                                            worker.execute { discovery?.allow(false) }
                                        }
                                    })
                                }
                                var generationAllowed by remember { mutableStateOf(getSharedPreferences("preparation", 0).getBoolean("generation", false)) }
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) { Text("Tailored plans"); Text("Cortex uses accepted facts when you open help.", style = MaterialTheme.typography.bodySmall) }
                                    Switch(generationAllowed, { value -> getSharedPreferences("preparation", 0).edit().putBoolean("generation", value).commit(); generationAllowed = value })
                                }
                                Text("Task sharing is controlled separately in Todo → Extensions.", style = MaterialTheme.typography.bodyMedium)
                                HorizontalDivider()
                                Text("Saved assistance", style = MaterialTheme.typography.titleMedium)
                                ListItem(headlineContent = { Text("Inspect") }, supportingContent = { Text("Activity, model exchanges and checked source") }, trailingContent = { Icon(Icons.Outlined.ChevronRight, null) }, modifier = Modifier.clickable { startActivity(Intent(this@AgendaActivity, PreparationInspectActivity::class.java)) })
                                ListItem(headlineContent = { Text("Clear saved assistance", color = MaterialTheme.colorScheme.error) }, supportingContent = { Text("Keep permissions, appointments and tasks") }, trailingContent = { Icon(Icons.Outlined.DeleteOutline, null, tint = MaterialTheme.colorScheme.error) }, modifier = Modifier.clickable(enabled = !clearing) { clearConfirm = true })
                            }
                            !granted -> Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("Connect Calendar to view and manage events.")
                                Button(onClick = { permissionRequest.launch(arrayOf(android.Manifest.permission.READ_CALENDAR, android.Manifest.permission.WRITE_CALENDAR)) }) { Text("Connect calendar") }
                            }
                            adding -> EventEditor(agenda, date) { adding = false; refresh() }
                            selected != null -> EventDetail(selected!!, agenda) { selected = null; refresh() }
                            error.isNotEmpty() -> Column(Modifier.padding(20.dp)) { Text(error); TextButton(onClick = { refresh() }) { Text("Retry") } }
                            else -> Agenda(events, date, suggestion?.optString("eventId"), {
                                if (checking) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                                    Text("Looking ahead…", style = MaterialTheme.typography.bodySmall)
                                }
                                suggestion?.let { tip ->
                                    val eventDay = LocalDate.parse(org.json.JSONObject(tip.getString("input")).getString("day"))
                                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                                    if (date != eventDay) Text("${if (date == LocalDate.now(AGENDA_ZONE)) "Tomorrow" else eventDay.format(DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault()))} · ${tip.getString("eventTitle")}", style = MaterialTheme.typography.labelLarge)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        TextButton(modifier = Modifier.weight(1f), enabled = !accepting, onClick = {
                                            accepting = true
                                            worker.execute {
                                                try { val accepted = discovery!!.accept(tip); runOnUiThread { if (visible && suggestion === tip) { suggestion = null; startActivity(Intent(this@AgendaActivity, PreparationActivity::class.java).putExtra("accepted", accepted)) } } }
                                                catch (error: Throwable) { runOnUiThread { suggestion = null; discoveryMessage = "Facts changed. No plan was opened." } }
                                                finally { runOnUiThread { accepting = false } }
                                            }
                                        }) { Text(if (tip.getBoolean("uncertain")) "Could “${tip.getString("taskTitle")}” help?" else "“${tip.getString("taskTitle")}” might help you prepare") }
                                        IconButton(enabled = !accepting, onClick = { suggestion = null; worker.execute { runCatching { discovery?.dismiss(tip) }.onFailure { android.util.Log.e("Preparation", "Dismissal persistence failed", it) } } }) { Icon(Icons.Outlined.Close, "Dismiss suggestion") }
                                    }
                                }
                            }) { suggestion = null; selected = it }
                        }
                    }
                }
                if (clearConfirm) AlertDialog(onDismissRequest = { clearConfirm = false }, title = { Text("Clear saved assistance?") }, text = { Text("Remove saved plans and discovery history. Permissions, appointments and tasks stay unchanged.") }, confirmButton = { TextButton(onClick = {
                    clearConfirm = false; clearing = true; suggestion = null
                    worker.execute { try { discovery?.close(); discovery = null; PreparationRecords.clear(applicationContext) } finally { runOnUiThread { clearing = false } } }
                }) { Text("Clear", color = MaterialTheme.colorScheme.error) } }, dismissButton = { TextButton(onClick = { clearConfirm = false }) { Text("Cancel") } })
                if (disclosureDialog) AlertDialog(onDismissRequest = { disclosureDialog = false }, title = { Text("Find preparation help?") }, text = { Text("While Calendar is open, send up to four event titles/notes and eight unfinished task titles to Jev for relevance discovery. After you open help, accepted facts also go to Cortex to generate a tailored plan. Enable sharing in Todo separately. No task or appointment is changed. You can stop sharing here.") },
                    confirmButton = { TextButton(onClick = { disclosureDialog = false; getSharedPreferences("preparation", 0).edit().putBoolean("disclose", true).putBoolean("generation", true).commit(); disclose = true }) { Text("Allow") } },
                    dismissButton = { TextButton(onClick = { disclosureDialog = false }) { Text("Not now") } })
            }
        }
    }
}

@Composable private fun WeekStrip(date: LocalDate, onSelect: (LocalDate) -> Unit) {
    val monday = date.minusDays((date.dayOfWeek.value - 1).toLong())
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            IconButton(onClick = { onSelect(date.minusWeeks(1)) }) { Icon(Icons.Outlined.ChevronLeft, "Previous week") }
            IconButton(onClick = { onSelect(date.plusWeeks(1)) }) { Icon(Icons.Outlined.ChevronRight, "Next week") }
        }
        Row(Modifier.fillMaxWidth()) {
            repeat(7) { index ->
                val day = monday.plusDays(index.toLong())
                val chosen = day == date
                Surface(onClick = { onSelect(day) }, modifier = Modifier.weight(1f).heightIn(min = 64.dp).semantics { selected = chosen; contentDescription = dev.deal.shell.dateLabel(day) },
                    shape = RoundedCornerShape(8.dp), color = if (chosen) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface) {
                    Column(Modifier.padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(day.format(DateTimeFormatter.ofPattern("EEEEE", Locale.getDefault())), style = MaterialTheme.typography.labelMedium)
                        Text(day.dayOfMonth.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = if (chosen) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
        }
    }
}

@Composable private fun Agenda(events: List<ShowEvent>, date: LocalDate, target: String?, assistance: @Composable () -> Unit, onOpen: (ShowEvent) -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Text(date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault())), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
        }
        if (events.none { "${it.id}:${it.from}" == target }) item { assistance() }
        if (events.isEmpty()) item { Text("No events this day.", modifier = Modifier.padding(vertical = 24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        items(events, key = { "${it.id}:${it.from}" }) { event ->
            Surface(onClick = { onOpen(event) }, shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surface) {
                Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                    Column(Modifier.width(56.dp)) {
                        Text(time(event.from), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text(time(event.until), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(event.title, style = MaterialTheme.typography.titleMedium)
                        if (event.location.isNotEmpty()) Text(event.location, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${Duration.between(Instant.ofEpochMilli(event.from), Instant.ofEpochMilli(event.until)).toMinutes()} min", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            if ("${event.id}:${event.from}" == target) assistance()
        }
    }
}

@Composable private fun EventDetail(event: ShowEvent, agenda: ShowAgenda, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "Back") }
            Text(event.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        }
        Text(Instant.ofEpochMilli(event.from).atZone(AGENDA_ZONE).toLocalDate().format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault())), style = MaterialTheme.typography.titleMedium)
        KeyValue("Start", time(event.from))
        KeyValue("End", time(event.until))
        KeyValue("Time zone", AGENDA_ZONE.id)
        if (event.location.isNotEmpty()) KeyValue("Location", event.location)
        if (event.notes.isNotEmpty()) Text(event.notes, style = MaterialTheme.typography.bodyMedium)
        val context = androidx.compose.ui.platform.LocalContext.current
        Button(onClick = { context.startActivity(Intent(context, CalendarActivity::class.java).putExtra("eventId", event.id).putExtra("eventBegin", event.from)) }) { Text("Plan departure") }
        OutlinedButton(onClick = { agenda.delete(event.id); onBack() }, shape = RoundedCornerShape(18.dp)) { Text("Delete event") }
    }
}

@Composable private fun EventEditor(agenda: ShowAgenda, date: LocalDate, onDone: () -> Unit) {
    var title by remember { mutableStateOf("") }
    var start by remember { mutableStateOf("16:00") }
    var end by remember { mutableStateOf("16:45") }
    var location by remember { mutableStateOf("") }
    var owner by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var reminder by remember { mutableStateOf(true) }
    val valid = runCatching { LocalTime.parse(start) < LocalTime.parse(end) }.getOrDefault(false)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("New event", style = MaterialTheme.typography.headlineSmall)
            TextButton(onClick = onDone) { Text("Cancel") }
        }
        Text(dev.deal.shell.dateLabel(date), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        OutlinedTextField(title, { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(start, { start = it }, label = { Text("Start") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp))
            OutlinedTextField(end, { end = it }, label = { Text("End") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp))
        }
        OutlinedTextField(location, { location = it }, label = { Text("Location") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
        OutlinedTextField(owner, { owner = it }, label = { Text("Responsible person") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
        OutlinedTextField(notes, { notes = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text("Reminder 30 minutes before"); Switch(reminder, { reminder = it }) }
        if (!valid) Text("End must be after start.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        Button(onClick = {
            val from = date.atTime(LocalTime.parse(start)).atZone(AGENDA_ZONE).toInstant().toEpochMilli()
            val until = date.atTime(LocalTime.parse(end)).atZone(AGENDA_ZONE).toInstant().toEpochMilli()
            agenda.add(title.ifBlank { "Event" }, from, until, location, owner, notes, if (reminder) 30 else null)
            onDone()
        }, enabled = valid, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) { Text("Add event") }
    }
}

private fun time(epochMillis: Long): String = Instant.ofEpochMilli(epochMillis).atZone(AGENDA_ZONE).toLocalTime().toString().take(5)

/** Prefer a Friday, so re-running the demo lands on the seeded show schedule. */
internal fun showDate(today: LocalDate = LocalDate.now()): LocalDate {
    if (today.dayOfWeek.value == 5) return today
    var candidate = today.plusDays(1)
    while (candidate.dayOfWeek.value != 5) candidate = candidate.plusDays(1)
    return candidate
}
