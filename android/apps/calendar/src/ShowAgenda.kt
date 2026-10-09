package dev.deal.apps.calendar

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
        val until = from + 48 * 3600_000L
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
        if (events(date).any { it.title.contains("Soundcheck") }) return
        fun at(hour: Int, minute: Int = 0) = date.atTime(hour, minute).atZone(AGENDA_ZONE).toInstant().toEpochMilli()
        add("Glass Harbour soundcheck", at(16), at(16, 45), "Stage", "Glass Harbour", "Show schedule", 30)
        add("Static Bloom soundcheck", at(17), at(17, 45), "Stage", "Static Bloom", "Show schedule", 30)
        add("Doors", at(19), at(19, 30), "Front of house", "Venue", "Show schedule", null)
        add("Glass Harbour performance", at(20), at(21), "Stage", "Glass Harbour", "Show schedule", 30)
        add("Static Bloom performance", at(21, 15), at(22, 15), "Stage", "Static Bloom", "Show schedule", 30)
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
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        setContent {
            AppTheme(Accent.Calendar) {
                var date by mutableStateOf(showDate())
                var events by mutableStateOf(emptyList<ShowEvent>())
                var adding by mutableStateOf(false)
                var selected by mutableStateOf<ShowEvent?>(null)
                var consent by mutableStateOf(getSharedPreferences("provider", 0).getBoolean("allowed", false))
                fun refresh() { events = agenda.events(date) }
                LaunchedEffect(date, adding) { runCatching { agenda.seedSchedule(date) }; refresh() }
                AuroraBackdrop(Modifier.fillMaxSize()) {
                    Scaffold(containerColor = Color.Transparent, topBar = {
                        TopAppBar(colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent), title = {
                            Column { Text(SHOW_CALENDAR, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold); Text(dev.deal.shell.dateLabel(date), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        }, actions = {
                            IconButton(onClick = { date = date.minusDays(1) }) { Icon(Icons.Outlined.ChevronLeft, "Previous day") }
                            IconButton(onClick = { date = date.plusDays(1) }) { Icon(Icons.Outlined.ChevronRight, "Next day") }
                            IconButton(onClick = { adding = true }) { Icon(Icons.Outlined.Add, "Add event") }
                        })
                    }) { padding ->
                        Box(Modifier.padding(padding)) {
                            when {
                                adding -> EventEditor(agenda, date) { adding = false; refresh() }
                                selected != null -> EventDetail(selected!!, agenda) { selected = null; refresh() }
                                else -> Agenda(events, date) { selected = it }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable private fun Agenda(events: List<ShowEvent>, date: LocalDate, onOpen: (ShowEvent) -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Hero("GIG DEMO CALENDAR", "Show agenda", "Soundchecks, doors, performances and the logistics around them.", illustration = { Art.Stage(130.dp) }) }
        if (events.isEmpty()) item { Panel { Text("Nothing scheduled on ${dev.deal.shell.dateLabel(date)}.", style = MaterialTheme.typography.titleMedium); Text("Add an event, or move to the show date.", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
        items(events) { event ->
            Card(onClick = { onOpen(event) }, shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Row(Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(64.dp)) {
                        Text(time(event.from), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(time(event.until), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(event.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        if (event.location.isNotEmpty()) Text(event.location, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (event.notes.isNotEmpty()) Text(event.notes.lineSequence().first(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Outlined.NotificationsActive, null, tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable private fun EventDetail(event: ShowEvent, agenda: ShowAgenda, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "Back") }
            Text(event.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        }
        Panel {
            KeyValue("Start", time(event.from))
            KeyValue("End", time(event.until))
            KeyValue("Time zone", AGENDA_ZONE.id)
            if (event.location.isNotEmpty()) KeyValue("Location", event.location)
            if (event.notes.isNotEmpty()) Text(event.notes, style = MaterialTheme.typography.bodyMedium)
        }
        Text("Editing an event changes this calendar only. The organizer reflects the new time on refresh and flags any violated requirement deadline.", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        Text("New event", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
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
