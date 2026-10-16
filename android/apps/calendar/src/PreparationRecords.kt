package dev.deal.apps.calendar

import android.content.Context
import dev.deal.embedding.*
import org.json.*

/** App-private provenance and fact identity. Execution/source/draft storage remain in embedding. */
object PreparationRecords {
    fun prefs(context: Context) = context.getSharedPreferences("preparation-records", 0)
    @Synchronized fun record(context: Context, stage: String, value: JSONObject) {
        val p = prefs(context)
        val events = JSONArray(p.getString("events", "[]"))
        if (events.length() >= 40) events.remove(0)
        events.put(JSONObject(value.toString()).put("stage", stage).put("at", System.currentTimeMillis()))
        check(p.edit().putString("events", events.toString()).commit())
    }
    fun opportunityApi(context: Context, registry: dev.deal.embedding.capabilities.CapabilityRegistry? = null): ApplicationProtocol {
        val capabilities = registry ?: dev.deal.embedding.capabilities.CapabilityRegistry(context, dev.deal.embedding.capabilities.CapabilityDiscovery(context) { false }, emptyList())
        return ApplicationProtocol(context, Embedding(context, capabilities, dev.deal.apps.development.GenerationGateway("http://127.0.0.1:8787/generate"), EmbeddingConfig("experience", emptyList(), "preparation-opportunity", retainState = true)))
    }
    fun clear(context: Context) {
        opportunityApi(context).use { api -> api.saved().forEach(api::forget) }
        check(context.getSharedPreferences("preparation-discovery-dismissals", 0).edit().clear().commit())
        check(prefs(context).edit().clear().commit())
    }
}
