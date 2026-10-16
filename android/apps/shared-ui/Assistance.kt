package dev.deal.shell

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import dev.deal.embedding.*
import dev.deal.embedding.capabilities.*
import dev.deal.embedding.discovery.*
import java.util.concurrent.Executors

fun assistanceProtocol(context: Context): ApplicationProtocol {
    val discovery = CapabilityDiscovery(context) { uid -> uid != context.applicationInfo.uid && Connections.trusted(context, uid) }
    return ApplicationProtocol(context, Embedding(context, CapabilityRegistry(context, discovery, emptyList()),
        AssistanceBuild.gateway(), EmbeddingConfig("experience", emptyList(), "assistance", retainState = true)))
}

/** App lifecycle/placement and retention decisions, not an execution driver. */
private class TaskHelp(private val context: Context, private val facts: () -> List<DiscoveryFact>) : AutoCloseable {
    private val worker = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())
    @Volatile private var foreground = false
    @Volatile private var epoch = 0L
    @Volatile private var api: ApplicationProtocol? = null
    @Volatile private var finder: Discovery? = null
    @Volatile private var request: GenerationRequest? = null
    @Volatile private var modules: List<String> = emptyList()
    private var input: DiscoveryInput? = null
    private var owner: ExperienceSurface? = null
    private var ownerInput: String? = null
    var suggestion by mutableStateOf<DiscoverySuggestion?>(null)
    var surface by mutableStateOf<ExperienceSurface?>(null)
    var status by mutableStateOf("")
    var busy by mutableStateOf(false)
    private fun post(version: Long, action: () -> Unit) { main.post { if (foreground && version == epoch) action() } }
    fun pause() {
        foreground = false; epoch++; finder?.cancel(); suggestion = null; surface = null; busy = false
        request?.let { api?.cancel(it) }
        // Grants have foreground lifetime; settings preserve only the user's decisions.
        api?.let { protocol -> modules.forEach(protocol::revoke) }; input = null
    }
    fun resume() {
        if (foreground) return
        foreground = true
        val version = ++epoch
        status = "Looking ahead…"; busy = true
        worker.execute {
            try {
                val protocol = api ?: assistanceProtocol(context).also { api = it }
                modules = Connections.apply(context, protocol)
                check(foreground && version == epoch)
                val current = DiscoveryInput(facts()); input = current
                if (owner != null && ownerInput != current.identity) { owner?.close(); owner = null; ownerInput = null }
                val settings = Connections.settings(context)
                val retained = settings.getString("saved.${current.identity}", null)
                val saved = retained?.let { id -> protocol.saved().firstOrNull { it.id == id } }
                if (saved != null) { activate(protocol, saved, version); ownerInput = current.identity; post(version) { busy = false; status = "" }; return@execute }
                if (!settings.getBoolean("discovery", true) || settings.getBoolean("dismissed.${current.identity}", false)) {
                    post(version) { busy = false; status = "" }; return@execute
                }
                val discovery = finder ?: Discovery(context, protocol).also { finder = it }
                discovery.discover(current).whenComplete { tip, error -> post(version) {
                    busy = false; suggestion = tip
                    status = if (error != null) "Couldn’t find help" else ""
                } }
            } catch (error: Throwable) { post(version) { busy = false; status = "Help unavailable" } }
        }
    }
    fun dismiss() {
        input?.let { check(Connections.settings(context).edit().putBoolean("dismissed.${it.identity}", true).commit()) }
        finder?.cancel(); suggestion = null; status = ""
    }
    fun open(tip: DiscoverySuggestion) {
        if (busy) return
        if (!Connections.settings(context).getBoolean("generation", false)) { status = "Enable tailored tools in Settings"; return }
        val version = epoch; val captured = input ?: return
        suggestion = null; busy = true; status = "Preparing…"
        worker.execute {
            try {
                val protocol = checkNotNull(api)
                check(foreground && version == epoch && Connections.settings(context).getBoolean("generation", false))
                val current = DiscoveryInput(facts()); check(current.identity == captured.identity)
                val admitted = checkNotNull(finder).request(tip, current); request = admitted
                if (!foreground || version != epoch) protocol.cancel(admitted)
                protocol.withPreparation(protocol.generate(admitted)) { experience ->
                    check(foreground && version == epoch && DiscoveryInput(facts()).identity == captured.identity)
                    activate(protocol, experience, version); ownerInput = captured.identity
                    check(Connections.settings(context).edit().putString("saved.${captured.identity}", experience.id).commit())
                }
                post(version) { busy = false; status = "" }
            } catch (error: Throwable) { post(version) { busy = false; status = "Couldn’t create this tool" } }
            finally { request = null }
        }
    }
    private fun activate(protocol: ApplicationProtocol, experience: Experience, version: Long) {
        check(foreground && version == epoch)
        val next = if (owner?.experience?.id == experience.id) checkNotNull(owner) else ExperienceSurface(protocol, experience)
        try {
            next.activate { post(version) { status = "Tool unavailable" } }
            check(foreground && version == epoch)
            val previous = owner; owner = next
            post(version) { surface = next }
            if (previous !== next) previous?.close()
        } catch (error: Throwable) { if (next !== owner) runCatching { next.close() }.onFailure(error::addSuppressed); throw error }
    }
    override fun close() {
        pause()
        worker.execute { try { teardown({ finder?.close() }, { owner?.close() }, { api?.close() }) } catch (error: Throwable) { android.util.Log.e("Assistance", "Cleanup failed", error) } }
        worker.shutdown()
    }
}

@Composable fun IntelligenceMark(label: String) {
    Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.small) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.AutoAwesome, null, Modifier.size(14.dp))
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}

/** One task facade: native task → marked opportunity → checked tool, with no extra launcher. */
@Composable fun AssistedTask(facts: List<DiscoveryFact>, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val currentFacts by rememberUpdatedState(facts)
    val identity = DiscoveryInput(facts).identity
    val help = remember(identity) { TaskHelp(context.applicationContext) { currentFacts } }
    DisposableEffect(help, lifecycle) {
        val observer = LifecycleEventObserver { _, event -> when (event) {
            Lifecycle.Event.ON_RESUME -> help.resume()
            Lifecycle.Event.ON_PAUSE -> help.pause()
            else -> Unit
        } }
        lifecycle.addObserver(observer)
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) help.resume()
        onDispose { lifecycle.removeObserver(observer); help.close() }
    }
    Column(Modifier.fillMaxSize()) {
        help.surface?.let { surface ->
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                IntelligenceMark("AI-built")
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { context.startActivity(Intent(context, AssistanceInspectActivity::class.java).putExtra("experience", surface.experience.id)) }) { Text("Inspect") }
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (help.status.isNotEmpty()) Text(help.status)
                ExperienceSurface(surface)
            }
        } ?: run {
            if (help.busy) Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp); Text(help.status, style = MaterialTheme.typography.bodySmall)
            } else if (help.status.isNotEmpty()) Text(help.status, Modifier.padding(horizontal = 16.dp))
            help.suggestion?.let { tip -> OutlinedCard(Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    IntelligenceMark("Suggestion")
                    Text(tip.title, style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { help.open(tip) }) { Text("Open") }
                        TextButton(onClick = { help.dismiss() }) { Text("Dismiss") }
                    }
                }
            } }
            Box(Modifier.weight(1f)) { content() }
        }
    }
}
