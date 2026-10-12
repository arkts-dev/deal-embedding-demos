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
        status.startsWith("AI writing") -> "Getting your options ready…"
        status.startsWith("Finding") -> "Preparing options…"
        status.startsWith("Opening") -> "Opening…"
        else -> "Connecting…"
    }
}
data class GenerationProblem(val title: String, val explanation: String)
fun generationProblem(error: Throwable): GenerationProblem = when {
    error is CancellationException -> GenerationProblem("Cancelled", "Your request, previous options and plan are unchanged. Nothing booked.")
    error is GenerationRejected && error.reason == GenerationRejected.Reason.ATTEMPT_LIMIT -> GenerationProblem("Couldn’t prepare options", "The AI could not prepare usable options for this request. Your previous options and plan are unchanged. Edit the request or tap Retry to make a new attempt. Nothing booked.")
    error is GenerationRejected && error.reason == GenerationRejected.Reason.REPEATED_RESPONSE -> GenerationProblem("Couldn’t prepare options", "The AI could not improve these options, so preparation stopped. Your previous options and plan are unchanged. Edit the request or tap Retry to make a new attempt. Nothing booked.")
    error is GenerationRejected -> GenerationProblem("Options unavailable", "These options could not be prepared safely. Your previous options and plan are unchanged. Return to your previous options or edit the request. Nothing booked.")
    error is dev.deal.apps.development.GenerationGatewayFailure && error.code == "AUDIT_LIMIT" -> GenerationProblem("Demo connection needs attention", "The connected AI service is still in limited test mode. Ask the demonstrator to start its demo connection, then tap Retry. Your previous options are kept.")
    error is java.net.ConnectException -> GenerationProblem("AI connection unavailable", "Check that the demo AI connection is running and the device is connected, then tap Retry. Your previous options are kept.")
    error is java.net.SocketTimeoutException || error is dev.deal.apps.development.GenerationGatewayFailure && error.code == "MODEL_TIMEOUT" -> GenerationProblem("AI took too long", "The AI service did not finish in time. Your request and previous options are kept. Tap Retry when the connection is ready.")
    error is dev.deal.apps.development.GenerationGatewayFailure -> GenerationProblem("AI service unavailable", "The connected AI service could not complete this request. Check the demo connection and tap Retry. Your previous options are kept.")
    else -> GenerationProblem("Couldn’t prepare options", "Preparing or checking your options was interrupted. Your request and previous options are kept. You can edit the request or tap Retry. Nothing booked.")
}
