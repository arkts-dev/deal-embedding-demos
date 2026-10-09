package dev.deal.apps.organizer

import dev.deal.embedding.GenerationRejected
import dev.deal.embedding.WorkspaceOrigin
import java.util.concurrent.CancellationException

/** Native presentation of trusted facts; never parse a model's text to establish provenance. */
data class WorkspacePresentation(val title: String, val explanation: String, val details: String)
fun workspacePresentation(origin: WorkspaceOrigin, attempts: Int): WorkspacePresentation = when (origin) {
    WorkspaceOrigin.AI_SOURCE -> WorkspacePresentation("AI-built workspace", "AI wrote the logic and screen. Checked before opening.",
        if (attempts > 0) "Source attempts: $attempts. Repairs: ${(attempts - 1).coerceAtLeast(0)}. Checks do not guarantee correctness or authorize provider writes." else "Replayed AI-authored source; original attempt count unavailable. Checked again before opening.")
    WorkspaceOrigin.CATALOGUE -> WorkspacePresentation("Catalogue workspace", "Built from a predefined template. Checked before opening.", "The model selected presentation choices; it did not author this workspace's source.")
    WorkspaceOrigin.SAVED_SOURCE -> WorkspacePresentation("Saved-source workspace", "Checked source replay. Original authorship is unknown.", "No inference was used to open this saved pair. Its origin was not recorded; it is not labelled AI-authored or catalogue-built.")
}
data class GenerationProblem(val title: String, val explanation: String)
fun generationProblem(error: Throwable): GenerationProblem = when {
    error is CancellationException -> GenerationProblem("Generation cancelled", "Your existing workspace and plan are unchanged. Nothing was reserved by generation.")
    error is GenerationRejected && error.reason == GenerationRejected.Reason.ATTEMPT_LIMIT -> GenerationProblem("Workspace couldn’t be built", "The code did not pass checks within ${error.attempts} source attempts. Your existing workspace and plan are unchanged. Nothing was reserved by generation.")
    error is GenerationRejected && error.reason == GenerationRejected.Reason.REPEATED_RESPONSE -> GenerationProblem("Workspace couldn’t be built", "The AI repeated a rejected response, so generation stopped after ${error.attempts} source attempts. Your existing workspace and plan are unchanged. Nothing was reserved by generation.")
    error is GenerationRejected -> GenerationProblem("Catalogue workspace couldn’t be built", "The predefined template did not pass checks. Your existing workspace and plan are unchanged. Nothing was reserved by generation.")
    else -> GenerationProblem("Workspace couldn’t be built", "Generation or checking was interrupted. Your existing workspace and plan are unchanged. Nothing was reserved by generation.")
}
