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
import dev.deal.embedding.*
import dev.deal.embedding.capabilities.*
import dev.deal.apps.development.GenerationGateway
import dev.deal.shell.*
import org.json.*
import java.util.concurrent.Executors

/** App-owned disclosure/placement around an ordinary checked executable experience. */
@OptIn(ExperimentalMaterial3Api::class)
class PreparationActivity : ComponentActivity() {
    private val worker = Executors.newSingleThreadExecutor()
    private var protocol: ApplicationProtocol? = null
    private var owner: ExperienceSurface? = null
    private var surface by mutableStateOf<ExperienceSurface?>(null)
    private var message by mutableStateOf("Checking preparation plan…")
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        val accepted = JSONObject(intent.getStringExtra("accepted") ?: "{}")
        val context = accepted.getJSONObject("context")
        val eventId = accepted.getString("eventId")
        val taskId = accepted.getString("taskId")
        val events = context.getJSONArray("events")
        val event = (0 until events.length()).map { events.getJSONObject(it) }.single { it.getString("id") == eventId }
        val tasks = context.getJSONArray("tasks")
        worker.execute {
            try {
                val registry = CapabilityRegistry(this, CapabilityDiscovery(this) { false }, emptyList())
                val api = ApplicationProtocol(this, Embedding(this, registry, GenerationGateway("http://127.0.0.1:8787/generate"), EmbeddingConfig("preparation", emptyList(), "preparation-plan")))
                protocol = api
                fun quote(value: String) = "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t") + "\""
                val initialTasks = (0 until tasks.length()).joinToString(" ") { i ->
                    val t = tasks.getJSONObject(i)
                    "let task$i: Task = {id:${quote(t.getString("id"))},title:${quote(t.getString("title"))},chosen:${t.getString("id") == taskId}}; tasks[tasks.length] = task$i;"
                }
                val initial = "let tasks: Task[] = []; $initialTasks return {tasks:tasks,start:${event.getInt("startMinute")},loaded:true};"
                val deal = assets.open("preparation/preparation.deal").bufferedReader().use { it.readText() }
                    .replace("function acceptedState(): State { return {}; }", "function acceptedState(): State { $initial }")
                val source = Source(deal, assets.open("preparation/preparation.dealui").bufferedReader().use { it.readText() })
                val prepared = api.load("Preparation plan", source)
                api.withPreparation(prepared) {
                    val next = ExperienceSurface(api, it)
                    next.activate { error -> runOnUiThread { message = "Plan unavailable: ${error.message}" } }
                    owner = next
                    runOnUiThread { if (!isDestroyed) surface = next }
                    if (isDestroyed) next.close()
                }
            } catch (error: Throwable) { android.util.Log.e("Preparation", "Plan failed", error); runOnUiThread { message = "Couldn’t open plan: ${error.message}" } }
        }
        setContent {
            AppTheme(Accent.Calendar) {
                Scaffold(topBar = { TopAppBar(title = { Text("Preparation plan") }, navigationIcon = { TextButton(onClick = { finish() }) { Text("Back") } }) }) { padding ->
                    Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(event.getString("title"), style = MaterialTheme.typography.titleLarge)
                        Text("Note: ${event.getString("notes")}", style = MaterialTheme.typography.bodyMedium)
                        surface?.let { ExperienceSurface(it, onSnapshot = { snapshot -> if (snapshot.fault.isNotEmpty()) message = "Plan unavailable. Return to Calendar." }) } ?: Text(message)
                    }
                }
            }
        }
    }
    override fun onDestroy() {
        worker.execute { teardown({ owner?.close() }, { protocol?.close() }) }
        worker.shutdown()
        super.onDestroy()
    }
}
