package dev.deal.apps.organizer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.deal.embedding.EmbeddingLog
import dev.deal.shell.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

/** Live observed execution. Data is untrusted text, never an instruction or executable content. */
class DiagnosticsActivity : ComponentActivity() {
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        setContent { AppTheme(Accent.Organizer) {
            var entries by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
            var capture by remember { mutableStateOf(getSharedPreferences("embedding-logging", 0).getBoolean("content", false)) }
            var selected by remember { mutableStateOf<JSONObject?>(null) }
            var detail by remember { mutableStateOf("") }
            var health by remember { mutableStateOf("") }
            var confirm by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                while (true) {
                    entries = withContext(Dispatchers.IO) {
                        listOf("previous.jsonl", "events.jsonl").flatMap { name ->
                            File(File(filesDir, "embedding-log"), name).takeIf { it.exists() }?.readLines().orEmpty()
                        }.mapNotNull { runCatching { JSONObject(it) }.getOrNull() }.takeLast(200).reversed()
                    }
                    health = withContext(Dispatchers.IO) { File(File(filesDir, "embedding-log"), "health.json").takeIf { it.exists() }?.readText().orEmpty() }
                    delay(500)
                }
            }
            LaunchedEffect(selected) {
                detail = withContext(Dispatchers.IO) {
                    selected?.optString("artifact")?.takeIf { it.isNotEmpty() }?.let { id ->
                        File(File(filesDir, "embedding-log"), "$id.json").takeIf { it.exists() }?.readText()
                    } ?: "Content omitted or evicted."
                }
            }
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(16.dp)) {
                Text("Execution", style = MaterialTheme.typography.headlineSmall)
                Text("Live activity · not AI reasoning", style = MaterialTheme.typography.bodyMedium)
                if (health.isNotEmpty()) Text(if (runCatching { JSONObject(health).optBoolean("active", true) }.getOrDefault(true)) "Logger unavailable — log incomplete" else "Logger recovered — earlier log incomplete", color = MaterialTheme.colorScheme.error)
                Row {
                    TextButton(onClick = { finish() }) { Text("Back") }
                    TextButton(onClick = { EmbeddingLog.clear(this@DiagnosticsActivity); entries = emptyList() }) { Text("Clear") }
                    TextButton(onClick = { if (capture) { EmbeddingLog.capture(this@DiagnosticsActivity, false); capture = false } else confirm = true }) { Text(if (capture) "Content capture ON" else "Capture content") }
                }
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(entries, key = { it.getString("time") + it.getString("sequence") + it.getString("span") }) { entry ->
                        OutlinedCard(onClick = { selected = entry }, modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(10.dp)) {
                                Text(entry.getString("summary"), style = MaterialTheme.typography.titleSmall)
                                Text(entry.getString("trace").take(8) + " · " + entry.getString("code") + " · " + entry.getLong("durationMs") + "ms · " + entry.getString("content"), style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
            if (confirm) AlertDialog(onDismissRequest = { confirm = false }, title = { Text("Capture private content?") },
                text = { Text("Future prompts, responses and provider payloads are stored privately with size limits. May contain personal data. Turn off after debugging.") },
                confirmButton = { TextButton(onClick = { EmbeddingLog.capture(this@DiagnosticsActivity, true); capture = true; confirm = false }) { Text("Enable") } },
                dismissButton = { TextButton(onClick = { confirm = false }) { Text("Cancel") } })
            selected?.let { event -> AlertDialog(onDismissRequest = { selected = null }, title = { Text(event.getString("summary")) },
                text = { Column(Modifier.heightIn(max = 500.dp).verticalScroll(rememberScrollState())) { Text(event.toString(2)); HorizontalDivider(); Text("Captured data (untrusted)"); Text(detail) } },
                confirmButton = { TextButton(onClick = { selected = null }) { Text("Close") } }) }
        } }
    }
}
