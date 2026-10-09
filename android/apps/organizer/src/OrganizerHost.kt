package dev.deal.apps.organizer

import android.content.Context
import dev.deal.apps.development.GenerationGateway
import dev.deal.embedding.*
import dev.deal.embedding.capabilities.*
import org.json.JSONObject

/** The Organizer discovers providers and hosts generation. It knows no provider's methods or records. */
class OrganizerHost(private val context: Context) : AutoCloseable {
    private val discovery = CapabilityDiscovery(context) { uid ->
        context.packageManager.checkSignatures(uid, context.applicationInfo.uid) == android.content.pm.PackageManager.SIGNATURE_MATCH
    }
    private var invokingWorkspace: String? = null
    val registry = CapabilityRegistry(context, discovery, listOf(organizerCapabilities(context) { invokingWorkspace }))
    /** Serial native invocation context only; source does not choose its review identity. */
    fun <T> inWorkspace(id: String, block: () -> T): T {
        val previous = invokingWorkspace
        invokingWorkspace = id
        try { return block() } finally { invokingWorkspace = previous }
    }
    private var contracts: List<CapabilityContract> = emptyList()
    private var runtime: ExperienceRuntime? = null
    private val config = EmbeddingConfig("experience", emptyList(), "organizer-experience")

    fun prepare() {
        contracts = registry.refresh()
        config.contracts = contracts
        if (runtime == null) runtime = ExperienceRuntime(context, registry, config)
        registry.grantAll()
    }
    fun environment() = runtime ?: error("Host not prepared")
    fun mountedWorkspaces(): List<LiveWorkspace> = runtime?.workspaces().orEmpty()
    fun generate(intent: String, disclosedContext: String, cancellation: GenerationCancellation, progress: (String) -> Unit): CheckedCandidate =
        environment().generate(intent, disclosedContext, GenerationGateway("http://127.0.0.1:8787/generate"), cancellation, progress)
    override fun close() { runtime?.close(); registry.close() }
}
