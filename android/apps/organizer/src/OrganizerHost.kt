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
    val registry = CapabilityRegistry(context, discovery, listOf(organizerCapabilities(context)))
    private var contracts: List<CapabilityContract> = emptyList()
    private var runtime: ExperienceRuntime? = null

    fun prepare() {
        contracts = registry.refresh()
        runtime = ExperienceRuntime(context, registry, EmbeddingConfig("experience", contracts, "organizer-experience"))
        registry.grantAll()
    }
    fun environment() = runtime ?: error("Host not prepared")
    fun available(): List<CapabilityContract> = contracts
    fun generate(intent: String, disclosedContext: String, cancellation: GenerationCancellation, progress: (String) -> Unit): CheckedCandidate =
        environment().generate(intent, disclosedContext, GenerationGateway("http://127.0.0.1:8787/generate"), cancellation, progress)
    override fun close() { runtime?.close(); registry.close() }
}
