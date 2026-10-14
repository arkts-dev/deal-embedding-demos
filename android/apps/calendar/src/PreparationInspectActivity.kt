package dev.deal.apps.calendar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.deal.shell.*
import org.json.*
import java.util.concurrent.Executors

/** Separate technical evidence surface. Never part of the ordinary agenda/plan flow. */
@OptIn(ExperimentalMaterial3Api::class)
class PreparationInspectActivity : ComponentActivity() {
    private val worker = Executors.newSingleThreadExecutor()
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        var content by mutableStateOf("Reading assistance evidence…")
        worker.execute {
            val evidence = runCatching { buildString {
                append("Jev: semantic relevance. Cortex: fresh DEAL/Deal UI generation. Kotlin: disclosure, placement, storage linkage. DEAL: discovery policy, generation/checking and executable plan.\n\n")
                append("DISCOVERY / GENERATION / INVALIDATION\n")
                append(JSONArray(PreparationRecords.prefs(this@PreparationInspectActivity).getString("events", "[]")).toString(2))
                PreparationRecords.api(this@PreparationInspectActivity).use { api ->
                    for (experience in api.saved()) {
                        val source = api.source(experience)
                        append("\n\n${experience.title} · ${experience.origin} · ${experience.attempts} attempts\n")
                        append("DEAL\n${source.deal}\nDEAL UI\n${source.dealui}\n")
                    }
                    val records = JSONArray(PreparationRecords.prefs(this@PreparationInspectActivity).getString("events", "[]"))
                    val traces = (0 until records.length()).map { records.getJSONObject(it).optString("trace") }.filter { it.isNotEmpty() }.distinct()
                    for (trace in traces) {
                        append("\nTRACE $trace\n")
                        var cursor = ""
                        repeat(4) {
                            val page = runCatching { api.events(trace, cursor, 20) }.getOrElse { error -> append("Trace unavailable: ${error.message}\n"); return@repeat }
                            for (event in page.events) {
                                append(event.toString(2)).append('\n')
                                // Artifacts are read only here, never interpreted as commands/source.
                                for (key in event.keys().asSequence().filter { it.contains("artifact", true) }.toList()) {
                                    val id = event.optString(key)
                                    if (id.isNotEmpty()) runCatching { api.artifact(id) }.getOrNull()?.let { append(it.text.take(8000)).append('\n') }
                                }
                            }
                            if (page.cursor.isEmpty() || page.cursor == cursor) return@repeat
                            cursor = page.cursor
                        }
                    }
                }
            } }.getOrElse { "Evidence unavailable: ${it.message}" }
            runOnUiThread { content = evidence }
        }
        setContent { AppTheme(Accent.Calendar) {
            Scaffold(topBar = { TopAppBar(title = { Text("Inspect") }, navigationIcon = { TextButton(onClick = { finish() }) { Text("Back") } }) }) { padding ->
                Text(content, Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), style = MaterialTheme.typography.bodySmall)
            }
        } }
    }
    override fun onDestroy() { worker.shutdown(); super.onDestroy() }
}
