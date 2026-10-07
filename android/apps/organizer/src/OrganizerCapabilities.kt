package dev.deal.apps.organizer

import android.content.Context
import dev.deal.embedding.capabilities.*
import org.json.*

/**
 * The Organizer publishes exactly one capability to generated workspaces: the preparation path.
 * It stages a descriptor for native review and performs no write.
 */
fun organizerCapabilities(context: Context): NativeCapabilities {
    val store = ShowStore(context)
    val contract = CapabilityContract("host/organizer", "Stage a prepared operation for native review; never a write", listOf(
        CapabilityFunction("stage", "Prepare one operation for the organizer to confirm natively", listOf(
            CapabilityParameter("kind", CapabilityType("string", maximum = 64)),
            CapabilityParameter("provider", CapabilityType("string", maximum = 64)),
            CapabilityParameter("title", CapabilityType("string", maximum = 120)),
            CapabilityParameter("detail", CapabilityType("string", maximum = 512)),
            CapabilityParameter("price", CapabilityType("string", maximum = 32)),
            CapabilityParameter("deadline", CapabilityType("string", maximum = 64)),
        ), CapabilityType("record", fields = mapOf("staged" to CapabilityType("boolean"), "reference" to CapabilityType("string")))),
        CapabilityFunction("staged", "Read the operations prepared so far", emptyList(),
            CapabilityType("array", maximum = 16, element = CapabilityType("record", fields = mapOf(
                "kind" to CapabilityType("string"), "provider" to CapabilityType("string"), "title" to CapabilityType("string"),
                "detail" to CapabilityType("string"), "price" to CapabilityType("string"), "deadline" to CapabilityType("string"))))),
    ))
    return NativeCapabilities(contract, mapOf(
        "stage" to { args ->
            val prefs = context.getSharedPreferences("prepared", 0)
            val reference = java.util.UUID.randomUUID().toString()
            val record = JSONObject().put("kind", args.getString(0)).put("provider", args.getString(1)).put("title", args.getString(2))
                .put("detail", args.getString(3)).put("price", args.getString(4)).put("deadline", args.getString(5))
            val all = JSONArray(prefs.getString("operations", "[]")); all.put(record)
            prefs.edit().putString("operations", all.toString()).putString("last", reference).commit()
            JSONObject().put("staged", true).put("reference", reference)
        },
        "staged" to { _ -> JSONArray(context.getSharedPreferences("prepared", 0).getString("operations", "[]")) },
    ))
}

fun preparedOperations(context: Context): List<JSONObject> {
    val array = JSONArray(context.getSharedPreferences("prepared", 0).getString("operations", "[]"))
    return (0 until array.length()).map { array.getJSONObject(it) }
}

fun clearPrepared(context: Context) { context.getSharedPreferences("prepared", 0).edit().clear().commit() }
