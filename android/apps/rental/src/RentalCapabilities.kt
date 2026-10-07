package dev.deal.apps.rental

import android.content.Context
import dev.deal.embedding.capabilities.*
import org.json.*

/** The called subset: equipment search, availability, quote and reservation outcomes. */
fun rentalCapabilities(context: Context): NativeCapabilities {
    val store = RentalStore(context)
    val contract = CapabilityContract("host/rental", "Rental catalogue, availability, quotes and reservations", listOf(
        CapabilityFunction("equipment", "List hireable equipment with specification and availability for a period", listOf(
            CapabilityParameter("from", CapabilityType("string")), CapabilityParameter("until", CapabilityType("string"))),
            CapabilityType("array", maximum = 32, element = CapabilityType("record", fields = mapOf(
                "id" to CapabilityType("string"), "name" to CapabilityType("string"), "specification" to CapabilityType("string"),
                "cents" to CapabilityType("int"), "available" to CapabilityType("int"), "included" to CapabilityType("string"), "category" to CapabilityType("string"))))),
        CapabilityFunction("quote", "Request a quote for items in a period", listOf(
            CapabilityParameter("items", CapabilityType("string")), CapabilityParameter("from", CapabilityType("string")), CapabilityParameter("until", CapabilityType("string"))),
            CapabilityType("record", fields = mapOf("cents" to CapabilityType("int"), "depositCents" to CapabilityType("int"),
                "expiresInMinutes" to CapabilityType("int"), "terms" to CapabilityType("string"), "available" to CapabilityType("boolean")))),
        CapabilityFunction("proposeReservation", "Stage a reservation for native review", listOf(
            CapabilityParameter("items", CapabilityType("string")), CapabilityParameter("from", CapabilityType("string")),
            CapabilityParameter("until", CapabilityType("string")), CapabilityParameter("collector", CapabilityType("string"))),
            CapabilityType("record", fields = mapOf("proposalId" to CapabilityType("string"), "state" to CapabilityType("string")))),
        CapabilityFunction("outcome", "Read a reservation proposal outcome", listOf(CapabilityParameter("proposalId", CapabilityType("string"))),
            CapabilityType("record", fields = mapOf("state" to CapabilityType("string"), "reservationId" to CapabilityType("string"), "detail" to CapabilityType("string")))),
    ))
    fun period(value: String): Long = runCatching { java.time.Instant.parse(value).toEpochMilli() }.getOrDefault(0L)
    return NativeCapabilities(contract, mapOf(
        "equipment" to { args ->
            val available = store.availability(period(args.getString(0)), period(args.getString(1)))
            JSONArray(store.catalogue.map { item -> JSONObject().put("id", item.id).put("name", item.name).put("specification", item.specification)
                .put("cents", item.cents).put("available", available[item.id] ?: 0).put("included", item.included.joinToString("; ")).put("category", item.category) })
        },
        "quote" to { args ->
            val lines = parseLines(args.getString(0))
            val from = period(args.getString(1)); val until = period(args.getString(2))
            val available = store.availability(from, until)
            JSONObject().put("cents", lines.sumOf { (store.item(it.first)?.cents ?: 0) * it.second })
                .put("depositCents", 5000).put("expiresInMinutes", 15)
                .put("terms", "Collection only. Exact items reserved; substitutions need approval.")
                .put("available", lines.all { (available[it.first] ?: 0) >= it.second })
        },
        "proposeReservation" to { args ->
            val prefs = context.getSharedPreferences("proposals", 0)
            val id = "rental-" + java.util.UUID.randomUUID().toString().take(8)
            prefs.edit().putString("rental:$id", JSONObject().put("proposalId", id).put("state", "pending")
                .put("items", args.getString(0)).put("from", args.getString(1)).put("until", args.getString(2)).put("collector", args.getString(3)).toString()).commit()
            JSONObject().put("proposalId", id).put("state", "pending")
        },
        "outcome" to { args -> JSONObject(context.getSharedPreferences("proposals", 0).getString("rental:" + args.getString(0), "{}")) },
    ))
}

internal fun parseLines(text: String): List<Pair<String, Int>> = text.split(",").mapNotNull { entry ->
    val parts = entry.trim().split(":")
    if (parts.size != 2) null else parts[0].trim() to (parts[1].trim().toIntOrNull() ?: 1)
}
