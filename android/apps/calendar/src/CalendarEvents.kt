package dev.deal.apps.calendar

import android.content.*
import android.provider.CalendarContract
import java.time.*

data class CalendarEvent(val id: Long, val title: String, val begin: Long, val end: Long, val calendarId: Long) {
    val startMinute: Int get() = Instant.ofEpochMilli(begin).atZone(ZoneId.systemDefault()).let { it.hour * 60 + it.minute }
    val time: String get() = Instant.ofEpochMilli(begin).atZone(ZoneId.systemDefault()).toLocalTime().toString().take(5)
}

/** Calendar Provider is the source of truth. Writes re-read before committing. */
class CalendarEvents(private val context: Context) {
    fun upcoming(): List<CalendarEvent> {
        val begin = System.currentTimeMillis() - 60 * 60 * 1000
        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon().also { ContentUris.appendId(it, begin); ContentUris.appendId(it, begin + 14L * 24 * 60 * 60 * 1000) }.build()
        val projection = arrayOf(CalendarContract.Instances.EVENT_ID, CalendarContract.Instances.TITLE, CalendarContract.Instances.BEGIN, CalendarContract.Instances.END, CalendarContract.Instances.CALENDAR_ID)
        return context.contentResolver.query(uri, projection, null, null, "begin ASC")!!.use { cursor ->
            buildList { while (cursor.moveToNext() && size < 20) add(CalendarEvent(cursor.getLong(0), cursor.getString(1) ?: "Untitled event", cursor.getLong(2), cursor.getLong(3), cursor.getLong(4))) }
        }
    }
    fun confirmReminder(event: CalendarEvent, departure: Int) {
        check(departure in 0..1439)
        check(upcoming().any { it == event }) { "Event changed. Recalculate before saving." }
        val delta = event.startMinute - departure
        check(delta in 0..1440) { "Departure must be before the selected event" }
        val values = ContentValues().apply { put(CalendarContract.Events.HAS_ALARM, 1) }
        val reminder = ContentValues().apply {
            put(CalendarContract.Reminders.EVENT_ID, event.id)
            put(CalendarContract.Reminders.MINUTES, delta)
            put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
        }
        // Single provider transaction. Do not remove unrelated reminders.
        val ops = arrayListOf(ContentProviderOperation.newUpdate(CalendarContract.Events.CONTENT_URI).withValues(values)
            .withSelection("_id=? AND dtstart=? AND dtend=? AND calendar_id=? AND title=? AND deleted=0", arrayOf(event.id.toString(), event.begin.toString(), event.end.toString(), event.calendarId.toString(), event.title))
            .withExpectedCount(1).build(),
            ContentProviderOperation.newInsert(CalendarContract.Reminders.CONTENT_URI).withValues(reminder).build())
        context.contentResolver.applyBatch(CalendarContract.AUTHORITY, ops)
    }
}
