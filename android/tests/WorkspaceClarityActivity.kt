package dev.deal.connectors.tests

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.deal.apps.organizer.*
import dev.deal.embedding.*
import dev.deal.shell.*
import org.json.JSONObject

/** Test APK only: frozen presentation states, never presented as live generation evidence. */
class WorkspaceClarityActivity : ComponentActivity() {
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        val plan = ShowState.fixture(java.time.LocalDate.of(2026, 10, 10))
        val requirement = plan.requirements.first { it.name == "Boom stand" }
        setContent { AppTheme(Accent.Organizer) {
            var screen by remember { mutableStateOf(intent.getStringExtra("screen") ?: "failure") }
            var problem by remember { mutableStateOf<GenerationProblem?>(generationProblem(GenerationRejected(3, GenerationRejected.Reason.ATTEMPT_LIMIT, "private compiler diagnostics"))) }
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(8.dp)) {
                TextButton(onClick = { screen = "inspection" }) { Text("Inspect") }
                when (screen) {
                    "inspection" -> Text("Test-only engineering surface · frozen presentation facts")
                    "failure", "progress", "writing", "request" -> RequestSheet("Boom stand", listOf(requirement),
                        if (screen == "writing") "AI writing logic and screen · attempt 2 of 3" else "Checking AI-written code · attempt 2 of 3", screen in listOf("progress", "writing"),
                        onCancel = { screen = "saved" }, onBuild = { _, _ -> screen = "progress" },
                        problem = if (screen == "failure") problem else null, hasExistingWorkspace = true, onDismissProblem = { problem = null; screen = "request" })
                    "review" -> ReviewSheet(emptyList(), { screen = "saved" }, { screen = "saved" })
                    else -> WorkspaceView("Equipment options", JSONObject().put("component", "root").put("props", org.json.JSONArray()).put("children", org.json.JSONArray()), "", { _, _ -> },
                        { finish() }, { screen = "review" }, hasPreparedSelection = true)
                }
            }
        } }
    }
}
