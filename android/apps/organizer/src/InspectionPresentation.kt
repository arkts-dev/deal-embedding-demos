package dev.deal.apps.organizer

import org.json.JSONArray

/** Decode the captured argument-array envelope for reading; raw bytes stay unchanged in storage. */
internal fun readableLogDetail(detail: String): String = runCatching {
    val args = JSONArray(detail)
    (0 until args.length()).joinToString("\n\n") { index ->
        val label = when (index) { 0 -> "Request / response"; 1 -> "Previous response"; 2 -> "Check feedback"; else -> "Argument ${index + 1}" }
        "$label\n${args.optString(index)}"
    }
}.getOrDefault(detail)
