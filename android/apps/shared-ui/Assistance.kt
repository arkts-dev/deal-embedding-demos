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

fun assistanceProtocol(context: Context, storageName: String = "assistance"): ApplicationProtocol {
    val discovery = CapabilityDiscovery(context) { uid -> uid != context.applicationInfo.uid && Connections.trusted(context, uid) }
    return ApplicationProtocol(context, Embedding(context, CapabilityRegistry(context, discovery, emptyList()),
        AssistanceBuild.gateway(), EmbeddingConfig("experience", emptyList(), storageName, retainState = true)))
}

/** App lifecycle/placement and retention decisions, not an execution driver. */
internal fun discoveryPreference() = if (AssistanceBuild.discoveryBackend == "nano") "discovery" else "discovery.${AssistanceBuild.discoveryBackend}"
internal fun discoveryEnabled(context: Context) = Connections.settings(context).getBoolean(discoveryPreference(), AssistanceBuild.discoveryBackend == "nano")

internal class TaskHelp(private val context: Context, private val facts: () -> List<DiscoveryFact>, private val protocolFactory: (Context) -> ApplicationProtocol) : AutoCloseable {
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
    var failed by mutableStateOf(false)
        private set
    var trace by mutableStateOf("")
        private set
    var since by mutableLongStateOf(0L)
        private set
    init { AssistanceShell.register(this) { reset() } }
    private fun reset() {
        AssistanceShell.onMain { pause() }
        if (worker.isShutdown) {
            check(worker.awaitTermination(30, java.util.concurrent.TimeUnit.SECONDS)) { "Assistance cleanup still pending" }
            teardown({ finder?.close() }, { owner?.close() }, { api?.close() })
            AssistanceShell.unregister(this)
            return
        }
        worker.submit {
            teardown({ finder?.close() }, { owner?.close() }, { api?.close() })
            finder = null; owner = null; api = null; ownerInput = null; input = null; request = null
        }.get(30, java.util.concurrent.TimeUnit.SECONDS)
        AssistanceShell.onMain { failed = false; status = ""; trace = ""; since = 0 }
    }
    private fun post(version: Long, action: () -> Unit) { main.post { if (foreground && version == epoch) action() } }
    fun pause() {
        foreground = false; epoch++; finder?.cancel(); suggestion = null; surface = null; busy = false
        request?.let { api?.cancel(it) }
        // Grants have foreground lifetime; settings preserve only the user's decisions.
        api?.let { protocol -> modules.forEach(protocol::revoke) }
    }
    fun resume() {
        if (foreground || AssistanceShell.resetting) return
        foreground = true
        val version = ++epoch
        if (!failed) status = "Looking ahead…"
        busy = true
        worker.execute {
            try {
                val current = DiscoveryInput(facts())
                val sameInput = input?.identity == current.identity
                input = current
                // Returning from Inspect/Settings must not silently rerun a failed attempt.
                if (sameInput && failed) {
                    api?.let { modules = Connections.apply(context, it) }
                    post(version) { busy = false }; return@execute
                }
                val protocol = api ?: protocolFactory(context).also { api = it }
                modules = Connections.apply(context, protocol)
                check(foreground && version == epoch)
                if (owner != null && ownerInput != current.identity) { owner?.close(); owner = null; ownerInput = null }
                val settings = Connections.settings(context)
                val retained = settings.getString("saved.${current.identity}", null)
                val saved = retained?.let { id -> protocol.saved().firstOrNull { it.id == id } }
                if (saved != null) { activate(protocol, saved, version); ownerInput = current.identity; post(version) { busy = false; status = "" }; return@execute }
                if (!discoveryEnabled(context) || settings.getBoolean("dismissed.${current.identity}", false)) {
                    post(version) { busy = false; status = "" }; return@execute
                }
                val discovery = finder ?: Discovery(context, protocol, AssistanceBuild.discoveryGateway()).also { finder = it }
                since = System.currentTimeMillis(); trace = ""
                discovery.discover(current).whenComplete { tip, error -> post(version) {
                    busy = false; suggestion = tip; failed = error != null
                    status = if (error != null) "Couldn’t find help" else ""
                } }
            } catch (error: Throwable) { post(version) { busy = false; failed = true; status = "Help unavailable" } }
        }
    }
    fun retry() {
        if (busy || !failed) return
        failed = false; status = ""; trace = ""
        pause(); resume()
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
                trace = protocol.requestTrace(admitted)
                if (!foreground || version != epoch) protocol.cancel(admitted)
                protocol.withPreparation(protocol.generate(admitted)) { experience ->
                    check(foreground && version == epoch && DiscoveryInput(facts()).identity == captured.identity)
                    activate(protocol, experience, version); ownerInput = captured.identity
                    check(foreground && version == epoch) { "Assistance changed before saving link" }
                    check(Connections.settings(context).edit().putString("saved.${captured.identity}", experience.id).commit())
                }
                post(version) { busy = false; status = "" }
            } catch (error: Throwable) { post(version) { busy = false; failed = true; status = "Couldn’t create this tool" } }
            finally { request = null }
        }
    }
    private fun activate(protocol: ApplicationProtocol, experience: Experience, version: Long) {
        check(foreground && version == epoch)
        val next = if (owner?.experience?.id == experience.id) checkNotNull(owner) else ExperienceSurface(protocol, experience)
        try {
            next.activate { post(version) { failed = true; status = "Tool unavailable" } }
            check(foreground && version == epoch)
            val previous = owner; owner = next
            post(version) { surface = next }
            if (previous !== next) previous?.close()
        } catch (error: Throwable) { if (next !== owner) runCatching { next.close() }.onFailure(error::addSuppressed); throw error }
    }
    override fun close() {
        pause()
        worker.execute { try {
            teardown({ finder?.close() }, { owner?.close() }, { api?.close() })
            AssistanceShell.unregister(this)
        } catch (error: Throwable) { android.util.Log.e("Assistance", "Cleanup failed", error) } }
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
@Composable fun AssistedTask(
    facts: List<DiscoveryFact>,
    storageName: String = "assistance",
    title: String = "",
    protocolFactory: (Context) -> ApplicationProtocol = { assistanceProtocol(it, storageName) },
    review: @Composable (Experience) -> Unit = {},
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val currentFacts by rememberUpdatedState(facts)
    val identity = DiscoveryInput(facts).identity
    val help = remember(identity, storageName) { TaskHelp(context.applicationContext, { currentFacts }, protocolFactory) }
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
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            if (title.isNotEmpty()) Text(title, modifier = Modifier.weight(1f).padding(vertical = 12.dp),
                style = MaterialTheme.typography.titleMedium) else Spacer(Modifier.weight(1f))
            TextButton(onClick = { context.startActivity(AssistanceShell.inspect(context, help.surface?.experience?.id.orEmpty(), help.trace, help.since)
                .putExtra("storage", storageName)) }) { Text("Inspect") }
        }
        if (help.failed) Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(help.status, Modifier.weight(1f), color = MaterialTheme.colorScheme.error)
            Button(onClick = help::retry, enabled = !help.busy) { Text("Try again") }
        }
        help.surface?.let { surface ->
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (!help.failed && help.status.isNotEmpty()) Text(help.status)
                ExperienceSurface(surface)
                review(surface.experience)
            }
        } ?: run {
            if (help.busy) Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp); Text(help.status, style = MaterialTheme.typography.bodySmall)
            } else if (!help.failed && help.status.isNotEmpty()) Text(help.status, Modifier.padding(horizontal = 16.dp))
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
