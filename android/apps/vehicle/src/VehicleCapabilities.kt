package dev.deal.apps.vehicle

import dev.deal.embedding.capabilities.*

fun vehicleCapabilities(): NativeCapabilities {
    val contract = CapabilityContract("host/trip", "Simulated vehicle travel, not live telemetry", listOf(
        CapabilityFunction("estimate", "Estimate travel minutes using the simulation 5 + 2 times distance in km", listOf(
            CapabilityParameter("distanceKm", CapabilityType("int", minimumInt = 1, maximumInt = 1000))), CapabilityType("int", minimumInt = 7, maximumInt = 2005))))
    return NativeCapabilities(contract, mapOf("estimate" to { args -> 5 + 2 * args.getInt(0) }))
}
