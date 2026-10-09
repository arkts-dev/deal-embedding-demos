package dev.deal.apps.organizer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.deal.embedding.EmbeddingLog
import dev.deal.embedding.SavedWorkspace
import dev.deal.embedding.WorkspaceOrigin
import dev.deal.shell.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

/** Engineering only: exact source and bounded observed activity, never a second operational engine. */
class DiagnosticsActivity : ComponentActivity() {
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        val workspace = intent.getStringExtra("workspace").orEmpty()
        val since = intent.getLongExtra("since", 0)
        val busy = intent.getBooleanExtra("busy", false)
        val prefs = getSharedPreferences("organizer-experience", 0)
        fun workspaces(): List<SavedWorkspace> = prefs.all.keys.filter { it.startsWith("workspace.") && it.endsWith(".title") }.map { key ->
            val prefix = key.removeSuffix(".title")
            SavedWorkspace(prefix.removePrefix("workspace."), prefs.getString(key, "")!!,
                runCatching { WorkspaceOrigin.valueOf(prefs.getString("$prefix.origin", "SAVED_SOURCE")!!) }.getOrDefault(WorkspaceOrigin.SAVED_SOURCE), prefs.getInt("$prefix.attempts", 0))
        }
        setContent { AppTheme(Accent.Organizer) {
            var entries by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
            var capture by remember { mutableStateOf(getSharedPreferences("embedding-logging", 0).getBoolean("content", false)) }
            var selected by remember { mutableStateOf<JSONObject?>(null) }
            var detail by remember { mutableStateOf("") }
            var health by remember { mutableStateOf("") }
            var readError by remember { mutableStateOf("") }
            var confirm by remember { mutableStateOf(false) }
            var clearing by remember { mutableStateOf(false) }
            var tab by remember { mutableStateOf("Activity") }
            var all by remember { mutableStateOf(workspace.isEmpty() && since == 0L) }
            var source by remember { mutableStateOf<String?>(null) }
            var selectedSource by remember { mutableStateOf<String?>(null) }
            LaunchedEffect(Unit) {
                while (true) {
                    val read = withContext(Dispatchers.IO) { runCatching {
                        val directory = File(filesDir, "embedding-log")
                        val events = listOf("previous.jsonl", "events.jsonl").flatMap { name ->
                            File(directory, name).takeIf { it.isFile }?.readLines().orEmpty()
                        }.map { JSONObject(it) }.reversed()
                        events to File(directory, "health.json").takeIf { it.isFile }?.readText().orEmpty()
                    } }
                    read.onSuccess { (events, status) -> entries = events; health = status; readError = "" }
                        .onFailure { readError = "Couldn’t read activity: ${it.javaClass.simpleName}. Retained display may be stale." }
                    capture = getSharedPreferences("embedding-logging", 0).getBoolean("content", false)
                    delay(500)
                }
            }
            LaunchedEffect(selected) {
                detail = withContext(Dispatchers.IO) {
                    selected?.optString("artifact")?.takeIf { it.isNotEmpty() }?.let { id ->
                        runCatching { File(File(filesDir, "embedding-log"), "$id.json").takeIf { it.isFile }?.readText() }.getOrNull()
                    } ?: "Content omitted or evicted. Capture is explicit and not retroactive."
                }
            }
            val traces = entries.filter { it.optString("workspace") == workspace && workspace.isNotEmpty() }.map { it.optString("trace") }.toSet()
            val visible = entries.filter { all || if (workspace.isNotEmpty()) it.optString("workspace") == workspace || it.optString("trace") in traces else since > 0 && it.optString("time").toLongOrNull()?.let { t -> t >= since } == true }.take(200)
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { finish() }) { Icon(Icons.Outlined.ArrowBack, "Back") }
                        Text("Inspect", style = MaterialTheme.typography.headlineSmall)
                    }
                    Text(intent.getStringExtra("context") ?: "Event activity", style = MaterialTheme.typography.titleSmall)
                    Text("Observed activity · not AI reasoning", style = MaterialTheme.typography.bodySmall)
                    if (health.isNotEmpty()) Text(if (runCatching { JSONObject(health).optBoolean("active", true) }.getOrDefault(true)) "Logger unavailable — log incomplete" else "Logger recovered — earlier log incomplete", color = MaterialTheme.colorScheme.error)
                    if (readError.isNotEmpty()) Text(readError, color = MaterialTheme.colorScheme.error)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Activity", "Source", "Workspaces").forEach { name -> FilterChip(selected = tab == name, onClick = { tab = name }, label = { Text(name) }) }
                    }
                    when (tab) {
                        "Workspaces" -> WorkspaceShelf(workspaces(), "", onOpen = { id ->
                            setResult(RESULT_OK, android.content.Intent().putExtra("openWorkspace", id)); finish()
                        }, onForget = { id -> setResult(RESULT_OK, android.content.Intent().putExtra("deleteWorkspace", id)); finish() }, enabled = !busy)
                        "Source" -> Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (workspace.isNotEmpty()) {
                                val prefix = "workspace.$workspace"
                                Text("Workspace $workspace", style = MaterialTheme.typography.titleSmall)
                                val origin = runCatching { WorkspaceOrigin.valueOf(prefs.getString("$prefix.origin", "SAVED_SOURCE")!!) }.getOrDefault(WorkspaceOrigin.SAVED_SOURCE)
                                val presentation = workspacePresentation(origin, prefs.getInt("$prefix.attempts", 0))
                                Text(presentation.title + "\n" + presentation.explanation + "\n" + presentation.details)
                                listOf("deal", "dealui").forEach { extension ->
                                    OutlinedButton(onClick = { selectedSource = extension; source = prefs.getString("$prefix.$extension", null) ?: "Saved source unavailable." }) { Text("View .$extension") }
                                }
                            } else Text("No accepted workspace selected. Generation and compiler exchanges appear in Activity; full content requires capture before the call.")
                            val failure = intent.getStringExtra("error").orEmpty()
                            if (failure.isNotEmpty()) { Text("Last operation failure", style = MaterialTheme.typography.titleSmall); Text(failure) }
                            val disclosed = intent.getStringExtra("request").orEmpty()
                            if (disclosed.isNotEmpty()) {
                                Text("Native request and disclosed context (untrusted data)", style = MaterialTheme.typography.titleSmall)
                                Text(disclosed)
                            }
                        }
                        else -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                FilterChip(selected = all, onClick = { all = !all }, label = { Text(if (all) "All activity" else if (workspace.isNotEmpty()) "Workspace trace" else "Since request") })
                                Spacer(Modifier.weight(1f))
                                TextButton(onClick = { clearing = true }) { Text("Clear") }
                            }
                            TextButton(onClick = { if (capture) { EmbeddingLog.capture(this@DiagnosticsActivity, false); capture = false } else confirm = true }) {
                                Text(if (capture) "Content capture ON · turn off" else "Capture content")
                            }
                            Text("Latest 200 matching events · bounded retained history. Missing terminal events are incomplete, not success.", style = MaterialTheme.typography.bodySmall)
                            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (visible.isEmpty()) item { Text("No retained activity in this scope. Use All activity or wait for the next call.") }
                                items(visible, key = { it.getString("time") + it.getString("sequence") + it.getString("span") }) { entry ->
                                    OutlinedCard(onClick = { selected = entry }, modifier = Modifier.fillMaxWidth()) {
                                        Column(Modifier.padding(12.dp)) {
                                            Text(entry.getString("summary"), style = MaterialTheme.typography.titleSmall)
                                            Text(dateTimeLabel(entry.getString("time").toLong(), java.time.ZoneId.systemDefault()) + " · " + entry.getString("trace").take(8) + " · " + entry.getString("code") + " · " + entry.getLong("durationMs") + "ms · " + entry.getString("content"), style = MaterialTheme.typography.labelMedium)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (confirm) AlertDialog(onDismissRequest = { confirm = false }, title = { Text("Capture private content?") },
                text = { Text("Future prompts, responses and provider payloads are stored privately with size limits. May contain personal data. Turn off after debugging. Credentials and authorization headers are excluded.") },
                confirmButton = { TextButton(onClick = { EmbeddingLog.capture(this@DiagnosticsActivity, true); capture = true; confirm = false }) { Text("Enable") } },
                dismissButton = { TextButton(onClick = { confirm = false }) { Text("Cancel") } })
            if (clearing) AlertDialog(onDismissRequest = { clearing = false }, title = { Text("Clear retained diagnostics?") }, text = { Text("Deletes retained activity, captured artifacts and logger health evidence. Saved workspaces are kept.") },
                confirmButton = { TextButton(onClick = { EmbeddingLog.clear(this@DiagnosticsActivity); entries = emptyList(); clearing = false }) { Text("Clear") } },
                dismissButton = { TextButton(onClick = { clearing = false }) { Text("Keep") } })
            selected?.let { event -> AlertDialog(onDismissRequest = { selected = null }, title = { Text(event.getString("summary")) },
                text = { Column(Modifier.heightIn(max = 500.dp).verticalScroll(rememberScrollState())) { Text(event.toString(2)); HorizontalDivider(); Text("Captured data (untrusted)"); Text(detail) } },
                confirmButton = { TextButton(onClick = { selected = null }) { Text("Close") } }) }
            source?.let { text -> AlertDialog(onDismissRequest = { source = null }, title = { Text(".$selectedSource · untrusted source") },
                text = { Text(text, Modifier.heightIn(max = 500.dp).verticalScroll(rememberScrollState())) },
                confirmButton = { TextButton(onClick = { source = null }) { Text("Close") } }) }
        } }
    }
}
