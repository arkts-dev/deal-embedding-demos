package dev.deal.apps.calendar

import android.content.Context
import android.content.pm.PackageManager
import dev.deal.embedding.*
import dev.deal.embedding.capabilities.*
import org.json.JSONObject
import java.io.File

/** Host selects a trust policy, not external providers, methods or record shapes. */
class CalendarEmbedding(context: Context, start: () -> Int, propose: (Int) -> Unit) : AutoCloseable {
    private val discovery = CapabilityDiscovery(context) { uid ->
        context.packageManager.checkSignatures(uid, context.applicationInfo.uid) == PackageManager.SIGNATURE_MATCH
    }
    val registry = CapabilityRegistry(context, discovery, listOf(calendarCapabilities(start, propose)))
    private var runtime: ExperienceRuntime? = null
    private val config = EmbeddingConfig("departure", emptyList(), "experience")
    fun prepare(context: Context) {
        val contracts = registry.refresh()
        config.contracts = contracts
        val declarations = File(context.filesDir, "capability-declarations").apply { deleteRecursively(); mkdirs() }
        contracts.forEach { File(declarations, it.module.substringAfter("/") + ".d.deal").writeText(CapabilityBindings.declaration(it)) }
        File(context.filesDir, "capability-contracts.json").writeText(org.json.JSONArray(contracts.map { it.json() }).toString())
        if (runtime == null) runtime = ExperienceRuntime(context, registry, config)
    }
    fun generate(intent: String, context: String, model: ModelClient, cancellation: GenerationCancellation, progress: (String) -> Unit) = runtime!!.generate(intent, context, model, cancellation, progress)
    fun activate(candidate: CheckedCandidate) = runtime!!.activate(candidate)
    fun remembered() = runtime!!.remembered()
    fun activate(source: ExperienceSource): JSONObject = runtime!!.activate(source)
    fun dispatch(slot: Int, payload: String?) = runtime!!.dispatch(slot, payload)
    fun poll() = runtime?.poll()
    override fun close() { runtime?.close(); registry.close() }
}
