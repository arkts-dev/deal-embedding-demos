package dev.deal.connectors.tests

import android.app.Instrumentation
import android.os.Bundle
import android.content.ContentValues
import android.provider.CalendarContract
import dev.deal.apps.calendar.*
import java.time.LocalDate

/** Controlled Gig Demo revision/confirmation adapter checks, never personal calendars. */
class DiscoveryCalendarInstrumentation : Instrumentation() {
    override fun onCreate(arguments: Bundle?) { super.onCreate(arguments); start() }
    override fun onStart() {
        val result = Bundle(); val agenda = ShowAgenda(targetContext)
        var id: Long? = null
        try {
            check(targetContext.checkSelfPermission(android.Manifest.permission.WRITE_CALENDAR) == android.content.pm.PackageManager.PERMISSION_GRANTED) { "Grant test Calendar access explicitly" }
            val day = LocalDate.of(2026, 11, 22)
            val from = day.atTime(15, 0).atZone(AGENDA_ZONE).toInstant().toEpochMilli()
            id = agenda.add("Discovery confirmation fixture", from, from + 3600000, "Fixture", "", "Synthetic adapter test", 30)!!
            val event = agenda.events(day).single { it.id == id }
            fun reminders(): List<Int> {
                val values = mutableListOf<Int>()
                targetContext.contentResolver.query(CalendarContract.Reminders.CONTENT_URI, arrayOf(CalendarContract.Reminders.MINUTES), "event_id=?", arrayOf(id.toString()), null)!!.use { c -> while(c.moveToNext()) values.add(c.getInt(0)) }
                return values.sorted()
            }
            check(reminders() == listOf(30))
            // Discovery/preparation itself never calls this native confirmation adapter.
            agenda.confirmReminder(event, 850)
            check(reminders() == listOf(30, 50))
            check(runCatching { agenda.confirmReminder(event, 901) }.isFailure)
            targetContext.contentResolver.update(CalendarContract.Events.CONTENT_URI, ContentValues().apply { put(CalendarContract.Events.EVENT_LOCATION, "Changed fixture") }, "_id=? AND calendar_id=?", arrayOf(id.toString(), agenda.calendarId().toString()))
            check(runCatching { agenda.confirmReminder(event, 840) }.isFailure)
            check(reminders() == listOf(30, 50))
            result.putString("discoveryCalendar", "PASS explicit confirmation adapter preserves existing reminders, rejects later time and stale event revision")
            finish(0, result)
        } catch(error: Throwable) { result.putString("failure", android.util.Log.getStackTraceString(error)); finish(1, result) }
        finally { id?.let(agenda::delete) }
    }
}
