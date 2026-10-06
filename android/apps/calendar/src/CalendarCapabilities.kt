package dev.deal.apps.calendar

import dev.deal.embedding.capabilities.*
import org.json.JSONObject

fun calendarCapabilities(start: () -> Int, propose: (Int) -> Unit): NativeCapabilities {
    val minute = CapabilityType("int", minimumInt = 0, maximumInt = 1439)
    val contract = CapabilityContract("host/calendar", "Selected Calendar event and native-reviewed departure proposal", listOf(
        CapabilityFunction("start", "Read selected event start minute and capture its revision", emptyList(), minute),
        CapabilityFunction("propose", "Stage a departure minute for native user confirmation; never writes", listOf(CapabilityParameter("minute", minute)), CapabilityType.NullType)))
    return NativeCapabilities(contract, mapOf("start" to { _ -> start() }, "propose" to { args -> propose(args.getInt(0)); JSONObject.NULL }))
}
