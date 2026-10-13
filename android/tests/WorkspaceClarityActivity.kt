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
            var received by remember { mutableStateOf("No action received") }
            var problem by remember { mutableStateOf<GenerationProblem?>(generationProblem(ProtocolFailure("GENERATION_ATTEMPT_LIMIT", "private compiler diagnostics"))) }
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(8.dp)) {
                TextButton(onClick = { screen = "inspection" }) { Text("Inspect") }
                when (screen) {
                    "inspection" -> Text("Test-only engineering surface · frozen presentation facts")
                    "renderer-omitted", "renderer-enabled", "renderer-disabled" -> {
                        Text(received)
                        DealRenderer(rendererDefaultsFixture(when (screen) { "renderer-enabled" -> true; "renderer-disabled" -> false; else -> null })) { slot, payload ->
                            received = "Received $slot: ${payload ?: "click"}"
                        }
                    }
                    "failure", "progress", "writing", "request" -> RequestSheet("Boom stand", listOf(requirement),
                        if (screen == "writing") "AI writing logic and screen · attempt 2 of 3" else "Checking AI-written code · attempt 2 of 3", screen in listOf("progress", "writing"),
                        onCancel = { screen = "saved" }, onBuild = { _, _ -> screen = "progress" },
                        problem = if (screen == "failure") problem else null, hasExistingWorkspace = true, onDismissProblem = { problem = null; screen = "request" })
                    "review" -> ReviewSheet(emptyList(), { screen = "saved" }, { screen = "saved" })
                    else -> WorkspaceView("Equipment options", "", { finish() }, { screen = "review" }, hasPreparedSelection = true) { Text("Frozen equipment options") }
                }
            }
        } }
    }
}

/** Sparse props reproduce retained compiled trees; explicit false must never use the default. */
private fun rendererDefaultsFixture(enabled: Boolean?): JSONObject {
    fun prop(name: String, value: Any): JSONObject = JSONObject().put("name", name).apply {
        when (value) {
            is String -> put("kind", "string").put("stringValue", value)
            is Int -> put("kind", "int").put("intValue", value)
            is Boolean -> put("kind", "boolean").put("booleanValue", value)
        }
    }
    fun component(name: String, slot: Int, event: String, vararg values: Pair<String, Any>): JSONObject {
        val props = org.json.JSONArray(values.map { prop(it.first, it.second) })
        if (enabled != null) props.put(prop("enabled", enabled))
        props.put(JSONObject().put("name", event).put("kind", "action").put("actionSlot", slot))
        return JSONObject().put("component", "ui.$name").put("props", props).put("children", org.json.JSONArray())
            .put("structural", name).put("keyKind", "string").put("keyInt", 0).put("keyString", name)
    }
    return JSONObject().put("component", "root").put("props", org.json.JSONArray()).put("children", org.json.JSONArray(listOf(
        component("Button", 1, "onClick", "text" to "Fixture button", "accessibilityLabel" to "Fixture button"),
        component("Option", 2, "onSelect", "title" to "Fixture option", "value" to "fixture", "accessibilityLabel" to "Fixture option"),
        component("IntField", 3, "onChange", "label" to "Fixture quantity", "accessibilityLabel" to "Fixture quantity", "value" to 2, "minimum" to 1, "maximum" to 4, "step" to 1),
        component("TextField", 4, "onChange", "label" to "Fixture text", "accessibilityLabel" to "Fixture text", "value" to ""),
        component("SearchField", 5, "onChange", "label" to "Fixture search", "accessibilityLabel" to "Fixture search", "value" to "")
    )))
}
