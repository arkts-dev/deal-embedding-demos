package dev.deal.apps.pizza

import android.content.Context
import dev.deal.embedding.capabilities.*
import org.json.*

/** The called subset: menu with dietary data, delivery options, order proposals and outcomes. */
fun pizzaCapabilities(context: Context): NativeCapabilities {
    val store = PizzaStore(context)
    val contract = CapabilityContract("host/pizza", "Menu, dietary information, delivery windows and orders", listOf(
        CapabilityFunction("menu", "List the menu with prices, ingredients and dietary data", emptyList(),
            CapabilityType("array", maximum = 32, element = CapabilityType("record", fields = mapOf(
                "id" to CapabilityType("string"), "name" to CapabilityType("string"), "size" to CapabilityType("string"),
                "cents" to CapabilityType("int"), "vegan" to CapabilityType("boolean"), "vegetarian" to CapabilityType("boolean"),
                "ingredients" to CapabilityType("string"), "allergens" to CapabilityType("string"))))),
        CapabilityFunction("delivery", "List delivery windows with whether each can meet a required-by time", listOf(
            CapabilityParameter("requiredBy", CapabilityType("string"))),
            CapabilityType("array", maximum = 8, element = CapabilityType("record", fields = mapOf(
                "id" to CapabilityType("string"), "from" to CapabilityType("string"), "until" to CapabilityType("string"),
                "meetsRequirement" to CapabilityType("boolean"), "deliveryCents" to CapabilityType("int"))))),
        CapabilityFunction("proposeOrder", "Stage an order for native review", listOf(
            CapabilityParameter("items", CapabilityType("string")), CapabilityParameter("windowId", CapabilityType("string")),
            CapabilityParameter("recipient", CapabilityType("string"))),
            CapabilityType("record", fields = mapOf("proposalId" to CapabilityType("string"), "state" to CapabilityType("string")))),
        CapabilityFunction("outcome", "Read an order proposal outcome", listOf(CapabilityParameter("proposalId", CapabilityType("string"))),
            CapabilityType("record", fields = mapOf("state" to CapabilityType("string"), "orderId" to CapabilityType("string"), "detail" to CapabilityType("string")))),
    ))
    return NativeCapabilities(contract, mapOf(
        "menu" to { _ -> JSONArray(store.menu.map { item -> JSONObject().put("id", item.id).put("name", item.name).put("size", item.size)
            .put("cents", item.cents).put("vegan", item.vegan).put("vegetarian", item.vegetarian)
            .put("ingredients", item.ingredients).put("allergens", item.allergens) }) },
        "delivery" to { args ->
            val required = runCatching { java.time.LocalTime.parse(args.getString(0)) }.getOrDefault(java.time.LocalTime.of(18, 30))
            JSONArray(store.windows.map { window -> JSONObject().put("id", window.id).put("from", window.from.toString()).put("until", window.until.toString())
                .put("meetsRequirement", window.until <= required).put("deliveryCents", store.deliveryCents) })
        },
        "proposeOrder" to { args ->
            val prefs = context.getSharedPreferences("proposals", 0)
            val id = "pizza-" + java.util.UUID.randomUUID().toString().take(8)
            prefs.edit().putString("pizza:$id", JSONObject().put("proposalId", id).put("state", "pending")
                .put("items", args.getString(0)).put("windowId", args.getString(1)).put("recipient", args.getString(2)).toString()).commit()
            JSONObject().put("proposalId", id).put("state", "pending")
        },
        "outcome" to { args -> JSONObject(context.getSharedPreferences("proposals", 0).getString("pizza:" + args.getString(0), "{}")) },
    ))
}
