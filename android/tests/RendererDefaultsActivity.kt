package dev.deal.connectors.tests

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.deal.embedding.*
import dev.deal.shell.*
import org.json.JSONObject

/** Test APK only: native omitted/enabled/disabled rendering regression. */
class RendererDefaultsActivity : ComponentActivity() {
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        setContent { AppTheme(Accent.Organizer) {
            val screen by remember { mutableStateOf(intent.getStringExtra("screen") ?: "renderer-omitted") }
            var received by remember { mutableStateOf("No action received") }
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(8.dp)) {
                when (screen) {
                    "renderer-omitted", "renderer-enabled", "renderer-disabled" -> {
                        Text(received)
                        DealRenderer(rendererDefaultsFixture(when (screen) { "renderer-enabled" -> true; "renderer-disabled" -> false; else -> null })) { slot, payload ->
                            received = "Received $slot: ${payload ?: "click"}"
                        }
                    }
                    else -> error("Unknown rendering fixture: $screen")
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
            is Int -> put("kind", "number").put("numberValue", value)
            is Boolean -> put("kind", "boolean").put("booleanValue", value)
        }
    }
    fun component(name: String, slot: Int, event: String, vararg values: Pair<String, Any>): JSONObject {
        val props = org.json.JSONArray(values.map { prop(it.first, it.second) })
        if (enabled != null) props.put(prop("enabled", enabled))
        props.put(JSONObject().put("name", event).put("kind", "action").put("actionSlot", slot))
        return JSONObject().put("component", "ui.$name").put("props", props).put("children", org.json.JSONArray())
            .put("structural", name).put("keyKind", "string").put("keyNumber", 0).put("keyString", name)
    }
    return JSONObject().put("component", "root").put("props", org.json.JSONArray()).put("children", org.json.JSONArray(listOf(
        component("Button", 1, "onClick", "text" to "Fixture button", "accessibilityLabel" to "Fixture button"),
        component("Option", 2, "onSelect", "title" to "Fixture option", "value" to "fixture", "accessibilityLabel" to "Fixture option"),
        component("IntField", 3, "onChange", "label" to "Fixture quantity", "accessibilityLabel" to "Fixture quantity", "value" to 2, "minimum" to 1, "maximum" to 4, "step" to 1),
        component("IntField", 6, "onChange", "label" to "Default bounds", "accessibilityLabel" to "Default bounds", "value" to 2),
        component("TextField", 4, "onChange", "label" to "Fixture text", "accessibilityLabel" to "Fixture text", "value" to ""),
        component("SearchField", 5, "onChange", "label" to "Fixture search", "accessibilityLabel" to "Fixture search", "value" to "")
    )))
}
