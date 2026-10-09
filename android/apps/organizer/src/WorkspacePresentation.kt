package dev.deal.apps.organizer

import dev.deal.embedding.GenerationRejected
import dev.deal.embedding.WorkspaceOrigin
import java.util.concurrent.CancellationException
import org.json.JSONObject
import dev.deal.shell.friendlyDates

/** Unknown saved/debug source is never assigned an inferred request. */
internal fun linkedRequirements(link: String?, show: ShowState): List<String> = link?.split(",")?.filter { show.requirement(it) != null }.orEmpty()

/** Display copy only. Never rewrite input values, action payloads, keys or saved source. */
internal fun displayDates(tree: JSONObject): JSONObject {
    val copy = JSONObject(tree.toString())
    fun visit(node: JSONObject) {
        val component = node.getString("component").substringAfterLast('.')
        val names = when (component) {
            "Text", "Hero" -> setOf("value")
            "Option", "Item" -> setOf("title", "detail", "meta", "trailing")
            "Section" -> setOf("title", "summary")
            "Notice", "Failure", "Toggle", "Button" -> setOf("text")
            "Progress" -> setOf("label")
            else -> emptySet()
        }
        val props = node.getJSONArray("props")
        for (i in 0 until props.length()) {
            val prop = props.getJSONObject(i)
            if (prop.optString("name") in names && prop.has("stringValue")) prop.put("stringValue", friendlyDates(prop.getString("stringValue"), SHOW_ZONE))
        }
        val children = node.getJSONArray("children")
        for (i in 0 until children.length()) visit(children.getJSONObject(i))
    }
    visit(copy)
    return copy
}

/** Native presentation of trusted facts; never parse a model's text to establish provenance. */
data class WorkspacePresentation(val title: String, val explanation: String, val details: String)
fun workspacePresentation(origin: WorkspaceOrigin, attempts: Int): WorkspacePresentation = when (origin) {
    WorkspaceOrigin.AI_SOURCE -> WorkspacePresentation("AI-built", "AI wrote the logic and screen. Checked before opening.",
        if (attempts > 0) "Source attempts: $attempts. Repairs: ${(attempts - 1).coerceAtLeast(0)}. Checks do not guarantee correctness or authorize provider writes." else "Replayed AI-authored source; original attempt count unavailable. Checked again before opening.")
    WorkspaceOrigin.CATALOGUE -> WorkspacePresentation("Template", "Built from a predefined template. Checked before opening.", "The model selected presentation choices; it did not author this workspace's source.")
    WorkspaceOrigin.SAVED_SOURCE -> WorkspacePresentation("Workspace", "Checked source replay. Original authorship is unknown.", "No inference was used to open this saved pair. Its origin was not recorded; it is not labelled AI-authored or catalogue-built.")
}
/** Short native status; source adapter text is presentation input, not provenance. */
fun compactGenerationStatus(status: String): String {
    return when {
        status.startsWith("Checking") -> "Checking…"
        status.startsWith("AI writing") -> if (status.contains("attempt 1 of")) "Building…" else "Refining…"
        status.startsWith("Finding") -> "Choosing template…"
        status.startsWith("Opening") -> "Opening…"
        else -> "Connecting…"
    }
}
data class GenerationProblem(val title: String, val explanation: String)
fun generationProblem(error: Throwable): GenerationProblem = when {
    error is CancellationException -> GenerationProblem("Cancelled", "Your existing workspace and plan are unchanged. Nothing was reserved by generation.")
    error is GenerationRejected && error.reason == GenerationRejected.Reason.ATTEMPT_LIMIT -> GenerationProblem("Couldn’t build", "The code did not pass checks within ${error.attempts} source attempts. Your existing workspace and plan are unchanged. Nothing was reserved by generation.")
    error is GenerationRejected && error.reason == GenerationRejected.Reason.REPEATED_RESPONSE -> GenerationProblem("Couldn’t build", "The AI repeated a rejected response, so generation stopped after ${error.attempts} source attempts. Your existing workspace and plan are unchanged. Nothing was reserved by generation.")
    error is GenerationRejected -> GenerationProblem("Template unavailable", "The predefined template did not pass checks. Your existing workspace and plan are unchanged. Nothing was reserved by generation.")
    else -> GenerationProblem("Couldn’t build", "Generation or checking was interrupted. Your existing workspace and plan are unchanged. Nothing was reserved by generation.")
}
