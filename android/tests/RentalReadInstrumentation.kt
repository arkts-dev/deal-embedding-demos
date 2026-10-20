package dev.deal.connectors.tests

import android.app.Instrumentation
import android.content.Context
import android.content.ContextWrapper
import android.os.Bundle
import dev.deal.apps.rental.*
import org.json.JSONArray
import org.json.JSONObject

/** Domain validation with isolated storage, no inference, sharing changes or native writes. */
class RentalReadInstrumentation : Instrumentation() {
    override fun onCreate(arguments: Bundle?) { super.onCreate(arguments); start() }
    override fun onStart() {
        val result = Bundle()
        val namespace = "rental-read-test-${java.util.UUID.randomUUID()}"
        val ctx = object : ContextWrapper(targetContext) {
            override fun getSharedPreferences(name: String, mode: Int) = super.getSharedPreferences("$namespace-$name", mode)
        }
        try {
            val native = rentalCapabilities(ctx)
            val start = "2026-10-23T14:15:00Z"
            val end = "2026-10-23T20:30:00Z"
            fun quote(lines: String, from: String = start, until: String = end) = native.invoke("quote", JSONArray().put(lines).put(from).put(until)) as JSONObject
            val catalogue = native.invoke("equipment", JSONArray().put(start).put(end)) as JSONArray
            check(catalogue.length() == 5)
            check(quote("package:1").let { it.getInt("cents") == 1200 && it.getInt("depositCents") == 5000 && it.getBoolean("available") })
            check(quote("stand:1,cable10:1").getInt("cents") == 1300)
            for (bad in listOf("", "adjustable boom stand, balanced cable", "missing:1", "stand:no", "stand:0", "stand:-1", "stand:33", "stand:1,", "stand:1,stand:1")) {
                check(runCatching { quote(bad) }.isFailure) { "Accepted invalid basket: $bad" }
            }
            for ((from, until) in listOf("2026-10-23T16:15" to end, start to start, end to start)) {
                check(runCatching { quote("package:1", from, until) }.isFailure)
                check(runCatching { native.invoke("equipment", JSONArray().put(from).put(until)) }.isFailure)
            }
            check(!quote("package:2,stand:1").getBoolean("available"))
            val store = RentalStore(ctx)
            store.saveReservations(listOf(Reservation("fixture", "fixture", "Synthetic test", listOf(QuoteLine("stand", 2)), java.time.Instant.parse(start).toEpochMilli(), java.time.Instant.parse(end).toEpochMilli(), "Test")))
            check(store.availability(java.time.Instant.parse(start).toEpochMilli(), java.time.Instant.parse(end).toEpochMilli()).getValue("package") == 0)
            check(!quote("package:1").getBoolean("available"))
            check(store.quotes().isEmpty() && store.reservations().size == 1)
            result.putString("rental", "PASS exact quote prices/deposit, strict basket/instant validation, aggregate component stock, no quote persistence")
            finish(0, result)
        } catch (error: Throwable) { result.putString("failure", android.util.Log.getStackTraceString(error)); finish(1, result) }
        finally { listOf("rental", "proposals").forEach { targetContext.deleteSharedPreferences("$namespace-$it") } }
    }
}
