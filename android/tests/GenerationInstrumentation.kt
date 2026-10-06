package dev.deal.connectors.tests

import dev.deal.apps.development.GenerationGateway

import android.app.Instrumentation
import android.os.Bundle
import dev.deal.embedding.*
import dev.deal.embedding.capabilities.*
import org.json.*
import java.io.File
import java.util.concurrent.CancellationException

/** Real inference through the shipped trusted DEAL program; synthetic failures are explicitly tests. */
class GenerationInstrumentation : Instrumentation() {
    override fun onCreate(arguments: Bundle?) { super.onCreate(arguments); start() }
    override fun onStart() {
        val result = Bundle()
        var runtime: ExperienceRuntime? = null
        var registry: CapabilityRegistry? = null
        try {
            val cancellationProbe = GenerationCancellation()
            var interrupts = 0
            val released = cancellationProbe.onCancel { interrupts++ }; released.close()
            val active = cancellationProbe.onCancel { interrupts++ }
            cancellationProbe.cancel(); cancellationProbe.cancel(); active.close()
            check(interrupts == 1)
            check(runCatching { cancellationProbe.onCancel { interrupts++ } }.exceptionOrNull() is CancellationException)
            val context = targetContext
            val discovery = CapabilityDiscovery(context) { uid -> context.packageManager.checkSignatures(uid, context.applicationInfo.uid) == android.content.pm.PackageManager.SIGNATURE_MATCH }
            val local = NativeCapabilities(CapabilityContract("host/calendar", "Synthetic verification context; no provider writes", listOf(
                CapabilityFunction("start", "Read test start minute", emptyList(), CapabilityType.IntType),
                CapabilityFunction("propose", "Stage test proposal", listOf(CapabilityParameter("minute", CapabilityType.IntType)), CapabilityType.NullType))),
                mapOf("start" to { _ -> 600 }, "propose" to { _ -> JSONObject.NULL }))
            registry = CapabilityRegistry(context, discovery, listOf(local))
            val contracts = registry.refresh(); registry.grantAll()
            runtime = ExperienceRuntime(context, registry, EmbeddingConfig("departure", contracts, "generation-verification"))
            val host = runtime
            val real = GenerationGateway("http://127.0.0.1:8787/generate")
            fun nodes(tree: JSONObject): List<JSONObject> = listOf(tree) + (0 until tree.getJSONArray("children").length()).flatMap { nodes(tree.getJSONArray("children").getJSONObject(it)) }
            fun prop(node: JSONObject, name: String): JSONObject = (0 until node.getJSONArray("props").length()).map { node.getJSONArray("props").getJSONObject(it) }.first { it.getString("name") == name }
            fun click(snapshot: JSONObject, label: String): JSONObject {
                val button = nodes(snapshot.getJSONObject("tree")).first { it.getString("component") == "ui.Button" && prop(it, "text").getString("stringValue") == label }
                return host.dispatch(prop(button,"onClick").getInt("actionSlot"), null)!!
            }
            fun waitFor(test: (JSONObject) -> Boolean): JSONObject {
                repeat(150) { val snapshot = host.poll()!!; if (test(snapshot) && nodes(snapshot.getJSONObject("tree")).none { it.getString("component") == "ui.Spinner" }) return snapshot; Thread.sleep(100) }
                error("Generated behavior did not complete")
            }
            fun ints(snapshot: JSONObject) = nodes(snapshot.getJSONObject("tree")).filter { it.getString("component")=="ui.IntText" }.map { prop(it,"value").getInt("intValue") }
            fun strings(snapshot: JSONObject) = nodes(snapshot.getJSONObject("tree")).flatMap { node -> (0 until node.getJSONArray("props").length()).map { node.getJSONArray("props").getJSONObject(it).optString("stringValue") } }
            val first = host.generate("Build an interactive counter headed COUNTER LAB. Initial count 0. Button Add one increments by 1. No external calls.", "{}", real, GenerationCancellation()) {}
            val firstTree = first.use { host.activate(it) }
            check(ints(firstTree) == listOf(0)); check(ints(click(firstTree,"Add one")) == listOf(1))
            val firstSource = host.remembered()!!
            val candidate = host.generate("Build a travel calculator headed TRAVEL LAB. Store distance 12 km. Show distance and resulting travel as separate IntText. Button Estimate trip calls the discovered travel estimate capability asynchronously. Button Add distance adds 2 km. Initial travel is 0. Handle errors as visible text. No Calendar or task reads.", "{}", real, GenerationCancellation()) {}
            val secondTree = candidate.use { host.activate(it) }
            check(firstSource != host.remembered())
            val changed = click(secondTree,"Add distance")
            click(changed,"Estimate trip")
            val trip = waitFor { 33 in ints(it) }; check(14 in ints(trip))
            registry.revoke("host/trip"); click(trip,"Estimate trip")
            val denied = waitFor { strings(it).any { text -> text.contains("not granted") } }
            registry.grant("host/trip"); click(denied,"Estimate trip"); waitFor { 33 in ints(it) }
            val before = host.remembered()!!
            var repairCalls = 0; var observedDiagnostics = ""
            val repairModel = object : ModelClient {
                override fun complete(input: String, previous: String, diagnostics: String, cancellation: GenerationCancellation): String {
                    repairCalls++
                    if (repairCalls == 1) return JSONObject().put("deal", "export function main(): null { return 7; }").put("dealui", "broken").toString()
                    observedDiagnostics = diagnostics
                    // A real model receives the failure and supplies the repaired source.
                    return real.complete(input, previous, diagnostics, cancellation)
                }
            }
            host.generate("Build an interactive counter headed REPAIR LAB. Button Add one increments by 1 from 0.", "{}", repairModel, GenerationCancellation()) {}.use {
                check(it.attempts >= 2 && observedDiagnostics.contains("code")); host.activate(it)
            }
            var failedCalls = 0
            val failingModel = object : ModelClient { override fun complete(input: String, previous: String, diagnostics: String, cancellation: GenerationCancellation): String { failedCalls++; return "invalid" } }
            val stable = host.remembered()!!; val stableTree = host.poll()!!.toString()
            check(runCatching { host.generate("Test failure", "{}", failingModel, GenerationCancellation()) {} }.isFailure)
            check(failedCalls == 3 && host.remembered() == stable && host.poll()!!.toString() == stableTree)
            val token = GenerationCancellation()
            val cancelModel = object : ModelClient { override fun complete(input: String, previous: String, diagnostics: String, cancellation: GenerationCancellation): String { cancellation.cancel(); cancellation.check(); return "" } }
            check(runCatching { host.generate("Test cancellation", "{}", cancelModel, token) {} }.exceptionOrNull() is CancellationException)
            check(host.remembered() == stable && host.poll()!!.toString() == stableTree)
            val unauthorized = before.copy(deal = "import * as forbidden from \"embedding/model\";\n" + before.deal + "\nexport async function illicit(): string { return await forbidden.complete(\"\", \"\", \"\"); }")
            check(runCatching { host.activate(unauthorized) }.isFailure)
            val receipt = JSONObject().put("realModelGeneratedDifferentWorkflows", true).put("discoveredCapabilityExecuted", true).put("grantRevocationRecovered", true)
                .put("realModelRepairedInjectedDiagnostic", true).put("repairAttempts", repairCalls).put("boundedFailureRetainedExperience", true).put("cancellationRetainedExperience", true)
                .put("orchestrationAuthorityDenied", true).put("syntheticFailuresOnlyForFaultInjection", true)
            File(context.filesDir,"generation-verification.json").writeText(receipt.toString(2))
            result.putString("generationVerification", receipt.toString()); finish(0,result)
        } catch (error: Throwable) { result.putString("failure", android.util.Log.getStackTraceString(error)); finish(1,result) }
        finally { runtime?.close(); registry?.close() }
    }
}
