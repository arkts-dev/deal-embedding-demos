package dev.deal.apps.calendar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.ui.Alignment
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.deal.embedding.*
import dev.deal.shell.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.*

/** App-specific scope/provenance only; observation correlation and decoding are framework-owned. */
@OptIn(ExperimentalMaterial3Api::class)
class PreparationInspectActivity : ComponentActivity() {
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        setContent { AppTheme(Accent.Calendar) {
            var tab by remember { mutableStateOf("Activity") }
            var records by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
            var events by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
            var sources by remember { mutableStateOf<List<Pair<Experience, Source>>>(emptyList()) }
            var scope by remember { mutableStateOf("") }
            var detail by remember { mutableStateOf<String?>(null) }
            var selected by remember { mutableStateOf<JSONObject?>(null) }
            var warning by remember { mutableStateOf("Reading retained evidence…") }
            var capture by remember { mutableStateOf(EmbeddingLog(this@PreparationInspectActivity).capturesContent) }
            var confirmCapture by remember { mutableStateOf(false) }
            var captureChange by remember { mutableStateOf<Boolean?>(null) }
            LaunchedEffect(captureChange) {
                captureChange?.let { value ->
                    withContext(Dispatchers.IO) { PreparationRecords.opportunityApi(this@PreparationInspectActivity).use { it.capture(value) } }
                    capture = value; captureChange = null
                }
            }
            LaunchedEffect(Unit) {
                withContext(Dispatchers.IO) {
                    runCatching {
                        val raw = JSONArray(PreparationRecords.prefs(this@PreparationInspectActivity).getString("events", "[]"))
                        val local = (0 until raw.length()).map { raw.getJSONObject(it) }.reversed()
                        PreparationRecords.opportunityApi(this@PreparationInspectActivity).use { api ->
                            val page = api.retainedEvents()
                            val traces = local.map { it.optString("trace") }.filter { it.isNotEmpty() }.toSet()
                            val saved = api.saved()
                            val ids = saved.map { it.id }.toSet()
                            val scoped = page.events.filter { it.optString("trace") in traces || it.optString("workspace") in ids }
                            val source = saved.map { it to api.source(it) }
                            withContext(Dispatchers.Main) {
                                records = local; events = scoped.reversed(); sources = source
                                scope = ""
                                warning = if (page.incomplete) "Retained evidence is incomplete; missing outcomes are not success." else ""
                            }
                        }
                    }.onFailure { withContext(Dispatchers.Main) { warning = "Couldn’t read evidence: ${it.javaClass.simpleName}. Nothing was retried." } }
                }
            }
            LaunchedEffect(selected) {
                selected?.let { event ->
                    detail = withContext(Dispatchers.IO) {
                        val id = event.optString("artifact")
                        val captured = if (id.isEmpty()) "Content was not captured or has been evicted." else runCatching {
                            PreparationRecords.opportunityApi(this@PreparationInspectActivity).use { it.artifact(id).text }
                        }.getOrDefault("Captured content unavailable.")
                        readableLogDetail(captured) + "\n\nExact metadata\n" + event.toString(2)
                    }
                }
            }
            val chosen = sources.firstOrNull { it.first.id == scope }
            val traces = events.filter { it.optString("workspace") == scope }.map { it.optString("trace") }.toSet()
            val scopedEvents = events.filter { scope.isEmpty() || it.optString("workspace") == scope || it.optString("trace") in traces }
            Scaffold(topBar = { TopAppBar(title = { Text("Inspect") }, navigationIcon = { TextButton(onClick = { finish() }) { Text("Back") } }) }) { padding ->
                Column(Modifier.padding(padding).fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Observed activity, not hidden AI reasoning", style = MaterialTheme.typography.bodySmall)
                    if (warning.isNotEmpty()) Text(warning, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = { if (capture) captureChange = false else confirmCapture = true }) { Text(if (capture) "Content capture ON · turn off" else "Capture future content") }
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Activity", "Dialogue", "Source").forEach { name -> FilterChip(tab == name, { tab = name }, label = { Text(name) }) }
                    }
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(scope.isEmpty(), { scope = "" }, label = { Text("All assistance") })
                        sources.forEach { (experience, _) -> FilterChip(scope == experience.id, { scope = experience.id }, label = { Text(experience.origin + " · " + experience.id.take(6)) }) }
                    }
                    if (tab != "Source") Text("Tap an entry to expand exact evidence", style = MaterialTheme.typography.bodySmall)
                    when (tab) {
                        "Source" -> Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (chosen == null) Text("Select saved assistance to read its exact checked source.")
                            chosen?.let { (experience, source) ->
                                Text(experience.title, style = MaterialTheme.typography.titleLarge)
                                Text("Origin: ${experience.origin} · ${experience.attempts} source attempts. Saved replay is not fresh inference.")
                                OutlinedButton(onClick = { detail = source.deal }) { Text("View .deal") }
                                OutlinedButton(onClick = { detail = source.dealui }) { Text("View .dealui") }
                            }
                        }
                        "Dialogue" -> {
                            Text("Actual requests, replies and compiler feedback. Content capture is not retroactive.", style = MaterialTheme.typography.bodySmall)
                            val exchanges = dialogueExchanges(scopedEvents)
                            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (exchanges.isEmpty()) item { Text("No retained model exchange in this scope.") }
                                items(exchanges, key = { it.key }) { exchange ->
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("${exchange.stage} · ${exchange.key.takeLast(12)}", style = MaterialTheme.typography.labelMedium)
                                        exchange.events.forEach { entry -> OutlinedCard(onClick = { selected = entry }, modifier = Modifier.fillMaxWidth()) {
                                            Column(Modifier.padding(12.dp)) { Text(dialogueRole(entry), style = MaterialTheme.typography.titleSmall); Text(eventLabel(entry), style = MaterialTheme.typography.bodySmall) }
                                        } }
                                    }
                                }
                            }
                        }
                        else -> LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (scope.isEmpty()) items(records) { record -> OutlinedCard(onClick = { detail = record.toString(2) }, modifier = Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(listOf(record.optString("model"), record.optString("stage"), record.optString("status"), record.optString("origin")).filter { it.isNotEmpty() }.joinToString(" · "), modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                                        Icon(Icons.Outlined.ChevronRight, "Expand evidence")
                                    }
                                    Text(dateTimeLabel(record.optLong("at"), AGENDA_ZONE) + record.optLong("elapsedMs").takeIf { record.has("elapsedMs") }?.let { " · ${it / 1000.0}s" }.orEmpty(), style = MaterialTheme.typography.bodySmall)
                                    record.optString("reason").takeIf { it.isNotEmpty() }?.let { Text(it.take(240), color = MaterialTheme.colorScheme.error) }
                                }
                            } }
                            items(scopedEvents.take(200)) { entry -> OutlinedCard(onClick = { selected = entry }, modifier = Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(12.dp)) { Text(activitySummary(entry), style = MaterialTheme.typography.titleSmall); Text(eventLabel(entry), style = MaterialTheme.typography.bodySmall) }
                            } }
                            if (scopedEvents.isEmpty() && records.isEmpty()) item { Text("No retained activity.") }
                        }
                    }
                }
            }
            if (confirmCapture) AlertDialog(onDismissRequest = { confirmCapture = false }, title = { Text("Capture private content?") }, text = { Text("Store future prompts and replies privately with size limits. May contain event notes and accepted briefs. Capture is not retroactive; turn it off after debugging. Credentials are excluded.") }, confirmButton = { TextButton(onClick = { captureChange = true; confirmCapture = false }) { Text("Enable") } }, dismissButton = { TextButton(onClick = { confirmCapture = false }) { Text("Cancel") } })
            detail?.let { text -> AlertDialog(onDismissRequest = { detail = null; selected = null }, title = { Text("Retained detail · untrusted content") }, text = { Text(text, Modifier.heightIn(max = 500.dp).verticalScroll(rememberScrollState())) }, confirmButton = { TextButton(onClick = { detail = null; selected = null }) { Text("Close") } }) }
        } }
    }
    private fun eventLabel(event: JSONObject): String = dateTimeLabel(event.optString("time").toLongOrNull() ?: 0, AGENDA_ZONE) + " · " + event.optString("outcome") + " · " + event.optString("code") + " · " + event.optLong("durationMs") + "ms"
}
