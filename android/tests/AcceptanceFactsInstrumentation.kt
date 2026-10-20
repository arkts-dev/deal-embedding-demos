package dev.deal.connectors.tests

import android.app.Instrumentation
import android.os.Bundle
import dev.deal.apps.calendar.ShowAgenda
import dev.deal.apps.calendar.AGENDA_ZONE
import java.time.LocalDate

/** Explicit device setup only. No expected answers, authority switches or generated source. */
class AcceptanceFactsInstrumentation : Instrumentation() {
    private var enriched=false
    override fun onCreate(arguments:Bundle?) {super.onCreate(arguments);enriched=arguments?.getString("enriched")=="true";start()}
    override fun onStart() {
        val result=Bundle()
        try {
            val agenda=ShowAgenda(targetContext)
            val day=LocalDate.of(2026,10,23)
            val facts=if(enriched) listOf(
                "Budget reconciliation" to "Unfinished budget comparison: approved EUR 180.00 and current commitments EUR 197.50. Both amounts editable; recalculate the difference and clearly show over, under or on-budget status. Read-only planning; no payment or spending approval.",
                "Equipment collection" to "Collect equipment for the event at The Foundry. Distance 12 km. Use Vehicle's simulated travel estimate, not live traffic. Allow 10 minutes for inspection before driving and 15 minutes for unloading before the event starts. Calculate latest inspection start and departure. Keep event time, distance and buffers editable. Read-only planning; no booking."
            ) else listOf(
                "Acceptance budget" to "Synthetic acceptance demo. Unfinished budget comparison: approved EUR 180.00 and current commitments EUR 197.50. Both amounts editable, recalculate and clearly state over or under budget. No payment or spending approval.",
                "Acceptance departure" to "Synthetic acceptance demo. Unfinished departure planning for this event. Distance 12 km; read Vehicle simulated travel estimate, not live traffic. Allow 10 minutes inspection before driving and 15 minutes unloading before event start. Calculate latest inspection start and departure. Event time, distance and buffers editable. Read-only planning, no proposals or bookings."
            )
            for((title,note) in facts) if(agenda.events(day).none{it.title==title}) {
                val from=day.atTime(16,0).atZone(AGENDA_ZONE).toInstant().toEpochMilli()
                checkNotNull(agenda.add(title,from,from+3600_000,if(enriched) "The Foundry" else "Acceptance demo, The Foundry","",note,null))
            }
            result.putString("facts","PASS additive Calendar fixtures; unrelated records and authorities unchanged")
            finish(0,result)
        }catch(error:Throwable){result.putString("failure",android.util.Log.getStackTraceString(error));finish(1,result)}
    }
}
