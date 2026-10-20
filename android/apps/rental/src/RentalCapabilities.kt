package dev.deal.apps.rental

import android.content.Context
import dev.deal.embedding.capabilities.*
import org.json.*

/** The called subset: equipment search, availability, quote and reservation outcomes. */
fun rentalCapabilities(context: Context): NativeCapabilities {
    val store = RentalStore(context)
    val contract = CapabilityContract("host/rental", "Rental catalogue, availability, quotes and reservations", listOf(
        CapabilityFunction("equipment", "Read before quoting: returns item IDs, exact specification, included contents, hire price in EUR cents and available quantity. from/until are ISO-8601 instants with UTC Z or numeric offset; until must follow from. Package contents consume component stock.", listOf(
            CapabilityParameter("from", CapabilityType("string")), CapabilityParameter("until", CapabilityType("string"))),
            CapabilityType("array", maximum = 32, element = CapabilityType("record", fields = mapOf(
                "id" to CapabilityType("string"), "name" to CapabilityType("string"), "specification" to CapabilityType("string"),
                "cents" to CapabilityType("int"), "available" to CapabilityType("int"), "included" to CapabilityType("string"), "category" to CapabilityType("string"))))),
        CapabilityFunction("quote", "Read-only quote. First read equipment for IDs/specifications. items must be comma-separated ID:quantity pairs from that catalogue, e.g. stand:1,cable10:1; never names or prose. Distinct known IDs, quantity 1..32, nonempty. from/until are ISO-8601 instants with Z or numeric offset, until after from. cents is hire price; depositCents is separate refundable deposit. available=false is not a usable selection.", listOf(
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
    fun period(args: JSONArray, start: Int): Pair<Long, Long> {
        val from = java.time.Instant.parse(args.getString(start)).toEpochMilli()
        val until = java.time.Instant.parse(args.getString(start + 1)).toEpochMilli()
        require(until > from) { "Rental period must end after it starts" }
        return from to until
    }
    return NativeCapabilities(contract, mapOf(
        "equipment" to { args ->
            val (from, until) = period(args, 0)
            val available = store.availability(from, until)
            JSONArray(store.catalogue.map { item -> JSONObject().put("id", item.id).put("name", item.name).put("specification", item.specification)
                .put("cents", item.cents).put("available", available[item.id] ?: 0).put("included", item.included.joinToString("; ")).put("category", item.category) })
        },
        "quote" to { args ->
            val lines = parseLines(args.getString(0))
            require(lines.all { store.item(it.first) != null }) { "Unknown rental item" }
            val (from, until) = period(args, 1)
            val available = store.availability(from, until)
            val consumed = mutableMapOf<String, Int>()
            lines.forEach { line -> store.consumed(line.first, line.second).forEach { (id, count) -> consumed[id] = (consumed[id] ?: 0) + count } }
            JSONObject().put("cents", lines.sumOf { checkNotNull(store.item(it.first)).cents * it.second })
                .put("depositCents", 5000).put("expiresInMinutes", 15)
                .put("terms", "Collection only. Exact items reserved; substitutions need approval.")
                .put("available", lines.all { (available[it.first] ?: 0) >= it.second } && consumed.all { (id, count) -> (available[id] ?: 0) >= count })
        },
        "proposeReservation" to { args ->
            val prefs = context.getSharedPreferences("proposals", 0)
            val id = "rental-" + java.util.UUID.randomUUID().toString().take(8)
            prefs.edit().putString("rental:$id", JSONObject().put("proposalId", id).put("state", "pending")
                .put("items", args.getString(0)).put("from", args.getString(1)).put("until", args.getString(2)).put("collector", args.getString(3)).toString()).commit()
            JSONObject().put("proposalId", id).put("state", "pending")
        },
        "outcome" to { args -> dev.deal.shell.proposalOutcome(context, "rental", args.getString(0), "reservationId") },
    ))
}

internal fun parseLines(text: String): List<Pair<String, Int>> {
    val entries = text.split(",")
    require(entries.size in 1..16) { "Expected 1..16 rental lines" }
    val lines = entries.map { entry ->
        val parts = entry.trim().split(":")
        require(parts.size == 2 && parts[0].trim().isNotEmpty()) { "Expected itemId:quantity" }
        val quantity = parts[1].trim().toIntOrNull()
        require(quantity != null && quantity in 1..32) { "Invalid rental quantity" }
        parts[0].trim() to quantity
    }
    require(lines.map { it.first }.distinct().size == lines.size) { "Duplicate rental item" }
    return lines
}
