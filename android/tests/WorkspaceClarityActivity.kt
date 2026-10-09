package dev.deal.connectors.tests

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import dev.deal.apps.organizer.*
import dev.deal.embedding.*
import dev.deal.shell.*
import org.json.JSONObject

/** Test APK only: renders real native surfaces with frozen facts; never invokes inference. */
class WorkspaceClarityActivity : ComponentActivity() {
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        setContent {
            AppTheme(Accent.Organizer) {
                val host = remember { OrganizerHost(this) }
                DisposableEffect(host) { onDispose { host.close() } }
                var screen by remember { mutableStateOf(intent.getStringExtra("screen") ?: "failure") }
                var problem by remember { mutableStateOf<GenerationProblem?>(generationProblem(GenerationRejected(3, GenerationRejected.Reason.ATTEMPT_LIMIT, "private compiler diagnostics"))) }
                when (screen) {
                    "failure", "progress", "writing" -> RequestSheet("Boom stand", emptyList(), host,
                        if (screen == "writing") "AI writing logic and screen · attempt 2 of 3" else "Checking AI-written code · attempt 2 of 3", screen != "failure",
                        onCancel = { screen = "saved" }, onBuild = { _, _ -> screen = "progress" },
                        problem = if (screen == "failure") problem else null, hasExistingWorkspace = true, onDismissProblem = { problem = null })
                    "review" -> ReviewSheet(emptyList(), { screen = "ai" }, { screen = "ai" })
                    else -> WorkspaceView("Bedtime readability fixture", JSONObject().put("component", "root").put("props", org.json.JSONArray()).put("children", org.json.JSONArray()), "", { _, _ -> },
                        { finish() }, { screen = "review" }, 1,
                        when (screen) { "ai" -> WorkspaceOrigin.AI_SOURCE; "catalogue" -> WorkspaceOrigin.CATALOGUE; else -> WorkspaceOrigin.SAVED_SOURCE }, 2)
                }
            }
        }
    }
}
