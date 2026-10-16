package dev.deal.apps.organizer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.deal.embedding.Experience
import dev.deal.embedding.EventPage
import dev.deal.embedding.inspection
import dev.deal.embedding.dialogueExchanges
import dev.deal.embedding.retainedEvents
import dev.deal.shell.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject

/** Engineering only: exact source and bounded observed activity, never a second operational engine. */
class DiagnosticsActivity : ComponentActivity() {
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        val workspace = intent.getStringExtra("workspace").orEmpty()
        val since = intent.getLongExtra("since", 0)
        val busy = intent.getBooleanExtra("busy", false)
        val host=OrganizerProtocol.host(this)
        val embedding=host.embedding
        fun workspaces(): List<Experience> = embedding.saved()
        setContent { AppTheme(Accent.Organizer) {
            var evidence by remember { mutableStateOf(EventPage(emptyList(), "", false)) }
            var capture by remember { mutableStateOf(embedding.capturesContent()) }
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
                        val page=embedding.retainedEvents()
                        val health=embedding.health()
                        page to if(health.incomplete||page.incomplete)JSONObject().put("active",health.active).toString()else ""
                    } }
                    read.onSuccess { (page, status) -> evidence = page; health = status; readError = "" }
                        .onFailure { readError = "Couldn’t read activity: ${it.javaClass.simpleName}. Retained display may be stale." }
                    capture = embedding.capturesContent()
                    delay(500)
                }
            }
            LaunchedEffect(selected) {
                detail = withContext(Dispatchers.IO) {
                    selected?.optString("artifact")?.takeIf { it.isNotEmpty() }?.let { id ->
                        runCatching { embedding.artifact(id).let{if(it.status=="unavailable")null else it.text} }.getOrNull()
                    } ?: "Content omitted or evicted. Capture is explicit and not retroactive."
                }
            }
            val scopedEvidence = if (all) evidence.inspection() else evidence.inspection(experiences = setOf(workspace), since = if (workspace.isEmpty()) since else 0)
            val scoped = scopedEvidence.events.reversed()
            val visible = scoped.take(200)
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
                    if (scopedEvidence.incomplete && health.isEmpty()) Text("Retained evidence is incomplete; missing outcomes are not success.", color = MaterialTheme.colorScheme.error)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Activity", "Dialogue", "Source", "Workspaces").forEach { name -> FilterChip(selected = tab == name, onClick = { tab = name }, label = { Text(name) }) }
                    }
                    when (tab) {
                        "Dialogue" -> {
                            Text("Actual AI requests and replies · not hidden reasoning", style = MaterialTheme.typography.bodySmall)
                            FilterChip(selected = all, onClick = { all = !all }, label = { Text(if (all) "All activity" else "This request") })
                            TextButton(onClick = { if (capture) { embedding.capture(false); capture = false } else confirm = true }) {
                                Text(if (capture) "Content capture ON · turn off" else "Capture content")
                            }
                            Text("Newest exchange first · request, reply and check feedback in order. Latest 100 retained exchanges; missing events/artifacts may be omitted or evicted.", style = MaterialTheme.typography.bodySmall)
                            val exchanges = dialogueExchanges(scoped)
                            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (exchanges.isEmpty()) item { Text("No retained model exchange in this scope. Enable capture before Find options to inspect full requests and replies. Reopening saved options does not call AI.") }
                                items(exchanges, key = { it.key }) { exchange ->
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("Exchange · " + dateTimeLabel(exchange.events.first().getString("time").toLong(), java.time.ZoneId.systemDefault()) + " · " + exchange.events.first().optString("trace").take(8), style = MaterialTheme.typography.labelMedium)
                                        exchange.events.forEach { entry ->
                                            OutlinedCard(onClick = { selected = entry }, modifier = Modifier.fillMaxWidth()) {
                                                Column(Modifier.padding(12.dp)) {
                                                    Text(dialogueRole(entry) + " · " + if (exchange.stage == "choice") "Choose a suitable interface" else "Write or repair the interface", style = MaterialTheme.typography.titleSmall)
                                                    Text(dateTimeLabel(entry.getString("time").toLong(), java.time.ZoneId.systemDefault()) + " · " + entry.optString("code") + " · " + entry.optString("content"), style = MaterialTheme.typography.bodySmall)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        "Workspaces" -> WorkspaceShelf(workspaces().sortedWith(compareBy<Experience> { it.id != workspace }.thenBy { it.title }), "", onOpen = { id ->
                            setResult(RESULT_OK, android.content.Intent().putExtra("openWorkspace", id)); finish()
                        }, onForget = { id -> setResult(RESULT_OK, android.content.Intent().putExtra("deleteWorkspace", id)); finish() }, enabled = !busy)
                        "Source" -> Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (workspace.isNotEmpty()) {
                                Text("Workspace $workspace", style = MaterialTheme.typography.titleSmall)
                                val experience=embedding.saved().first{it.id==workspace}
                                val presentation = workspacePresentation(experience.origin, experience.attempts)
                                Text(presentation.title + "\n" + presentation.explanation + "\n" + presentation.details)
                                listOf("deal", "dealui").forEach { extension ->
                                    OutlinedButton(onClick = { selectedSource = extension; source = embedding.source(experience).let{if(extension=="deal")it.deal else it.dealui} }) { Text("View .$extension") }
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
                            TextButton(onClick = { if (capture) { embedding.capture(false); capture = false } else confirm = true }) {
                                Text(if (capture) "Content capture ON · turn off" else "Capture content")
                            }
                            Text("Latest 200 matching events · bounded retained history. Missing terminal events are incomplete, not success.", style = MaterialTheme.typography.bodySmall)
                            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (visible.isEmpty()) item { Text("No retained activity in this scope. Use All activity or wait for the next call.") }
                                items(visible, key = { it.getString("time") + it.getString("sequence") + it.getString("span") }) { entry ->
                                    OutlinedCard(onClick = { selected = entry }, modifier = Modifier.fillMaxWidth()) {
                                        Column(Modifier.padding(12.dp)) {
                                            Text(activitySummary(entry), style = MaterialTheme.typography.titleSmall)
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
                confirmButton = { TextButton(onClick = { embedding.capture(true); capture = true; confirm = false }) { Text("Enable") } },
                dismissButton = { TextButton(onClick = { confirm = false }) { Text("Cancel") } })
            if (clearing) AlertDialog(onDismissRequest = { clearing = false }, title = { Text("Clear retained diagnostics?") }, text = { Text("Deletes retained activity, captured artifacts and logger health evidence. Saved workspaces are kept.") },
                confirmButton = { TextButton(onClick = { embedding.clearObservation(); evidence = EventPage(emptyList(), "", false); clearing = false }) { Text("Clear") } },
                dismissButton = { TextButton(onClick = { clearing = false }) { Text("Keep") } })
            selected?.let { event -> AlertDialog(onDismissRequest = { selected = null }, title = { Text(activitySummary(event)) },
                text = { Column(Modifier.heightIn(max = 500.dp).verticalScroll(rememberScrollState())) {
                    if (event.optString("stage") in listOf("choice", "source")) {
                        Text("Actual model exchange (untrusted)"); Text(readableLogDetail(detail)); HorizontalDivider(); Text("Exact event metadata"); Text(event.toString(2))
                    } else {
                        Text(event.toString(2)); HorizontalDivider(); Text("Captured data (untrusted)"); Text(readableLogDetail(detail))
                    }
                } },
                confirmButton = { TextButton(onClick = { selected = null }) { Text("Close") } }) }
            source?.let { text -> AlertDialog(onDismissRequest = { source = null }, title = { Text(".$selectedSource · untrusted source") },
                text = { Text(text, Modifier.heightIn(max = 500.dp).verticalScroll(rememberScrollState())) },
                confirmButton = { TextButton(onClick = { source = null }) { Text("Close") } }) }
        } }
    }
}
