package dev.deal.apps.todo

import android.content.Context
import dev.deal.embedding.capabilities.*
import org.json.*

fun todoCapabilities(context: Context): NativeCapabilities {
    val task = CapabilityType("record", fields = linkedMapOf(
        "id" to CapabilityType("string", maximum = 128), "title" to CapabilityType("string", maximum = 80),
        "minutes" to CapabilityType("int", minimumInt = 1, maximumInt = 120), "done" to CapabilityType("boolean")))
    val contract = CapabilityContract("host/todo", "User-owned preparation tasks", listOf(
        CapabilityFunction("tasks", "Read tasks including stable identity, preparation minutes and completion", emptyList(), CapabilityType("array", element = task, maximum = 20))))
    return NativeCapabilities(contract, mapOf("tasks" to { _ -> JSONArray(context.getSharedPreferences("provider", 0).getString("tasks", "[]")) }))
}
