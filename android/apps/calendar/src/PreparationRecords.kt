package dev.deal.apps.calendar

import android.content.Context
import dev.deal.embedding.*
import org.json.*

/** App-private provenance and fact identity. Execution/source/draft storage remain in embedding. */
object PreparationRecords {
    fun key(value: String) = java.security.MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString("") { "%02x".format(it) }
    fun prefs(context: Context) = context.getSharedPreferences("preparation-records", 0)
    @Synchronized fun record(context: Context, stage: String, value: JSONObject) {
        val p = prefs(context)
        val events = JSONArray(p.getString("events", "[]"))
        if (events.length() >= 40) events.remove(0)
        events.put(JSONObject(value.toString()).put("stage", stage).put("at", System.currentTimeMillis()))
        check(p.edit().putString("events", events.toString()).commit())
    }
    fun api(context: Context): ApplicationProtocol {
        val registry = dev.deal.embedding.capabilities.CapabilityRegistry(context, dev.deal.embedding.capabilities.CapabilityDiscovery(context) { false }, emptyList())
        return ApplicationProtocol(context, Embedding(context, registry, dev.deal.apps.development.GenerationGateway("http://127.0.0.1:8787/generate"), EmbeddingConfig("preparation", emptyList(), "preparation-plan", retainState = true)))
    }
    fun clear(context: Context) {
        api(context).use { api -> api.saved().forEach(api::forget) }
        check(context.getSharedPreferences("preparation", 0).edit().remove("state").commit())
        check(context.getSharedPreferences("preparation-inputs", 0).edit().clear().commit())
        check(prefs(context).edit().clear().commit())
    }
}
