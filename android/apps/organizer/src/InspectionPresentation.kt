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

internal data class DialogueExchange(val key: String, val stage: String, val events: List<JSONObject>)

/** Newest exchange first; chronological records inside it. Never infer missing replies.
 * Model records have explicit span/parent correlation. Check operations are sequential
 * within a generation trace, so attach only observed checks between adjacent model calls.
 * Limit whole exchanges, not individual records that could drop the matching request.
 */
internal fun dialogueExchanges(entries: List<JSONObject>, limit: Int = 100): List<DialogueExchange> {
    val chronological = entries.sortedWith(compareBy<JSONObject> { it.optString("time").toLongOrNull() ?: 0L }.thenBy { it.optLong("sequence") })
    val groups = linkedMapOf<String, MutableList<JSONObject>>()
    val stages = mutableMapOf<String, String>()
    val latest = mutableMapOf<String, String>()
    for (event in chronological) {
        val trace = event.optString("trace")
        val stage = event.optString("stage")
        val outcome = event.optString("outcome")
        if (stage in listOf("choice", "source") && outcome in listOf("started", "response", "failed", "cancelled")) {
            val span = if (outcome == "response") event.optString("parent").ifEmpty { event.optString("span") } else event.optString("span")
            val key = "$trace:$span"
            groups.getOrPut(key) { mutableListOf() }.add(event)
            stages[key] = stage
            latest[trace] = key
        } else if (stage == "check" && outcome in listOf("started", "completed", "failed", "rejected")) {
            latest[trace]?.let { key -> groups.getValue(key).add(event) }
        }
    }
    return groups.entries.reversed().take(limit).map { (key, events) -> DialogueExchange(key, stages.getValue(key), events.toList()) }
}

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
