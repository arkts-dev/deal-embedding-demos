package dev.deal.apps.organizer

import org.json.JSONArray
import org.json.JSONObject

/** Decode the captured argument-array envelope for reading; raw bytes stay unchanged in storage. */
internal fun readableLogDetail(detail: String): String = runCatching {
    val args = JSONArray(detail)
    (0 until args.length()).joinToString("\n\n") { index ->
        val label = when (index) { 0 -> "Request / response"; 1 -> "Previous response"; 2 -> "Check feedback"; else -> "Argument ${index + 1}" }
        "$label\n${args.optString(index)}"
    }
}.getOrDefault(detail)

internal fun dialogueRole(event: JSONObject): String = if (event.optString("stage") == "check") when (event.optString("outcome")) {
    "started" -> "Checking reply"
    "completed" -> "Check passed"
    "failed", "rejected" -> "Check feedback"
    else -> "Check event"
} else when (event.optString("outcome")) {
    "started" -> "Request to AI"
    "response" -> "AI reply"
    "cancelled" -> "Cancelled"
    else -> "Failed"
}
