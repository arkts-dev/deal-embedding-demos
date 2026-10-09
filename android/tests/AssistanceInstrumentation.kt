package dev.deal.connectors.tests

import android.app.Instrumentation
import android.os.Bundle
import dev.deal.apps.organizer.*
import dev.deal.embedding.*
import java.time.Instant

/** Authored test-only source; actual compiler, sandbox, discovery and durable requirement navigation. No model call. */
class AssistanceInstrumentation : Instrumentation() {
    private var cleanup = false
    private var restoreWorkspace: String? = null
    override fun onCreate(arguments: Bundle?) {
        cleanup = arguments?.getString("mode") == "cleanup"
        if (arguments?.getString("mode") == "restore-link") restoreWorkspace = arguments.getString("workspace")
        super.onCreate(arguments); start()
    }
    override fun onStart() {
        val result = Bundle()
        val host = OrganizerHost(targetContext)
        try {
            restoreWorkspace?.let { id ->
                // Explicit audit recovery only; never infer a link from source or title.
                val show = ShowStore(targetContext).load() ?: error("No show")
                val links = targetContext.getSharedPreferences("organizer-workspace-links", 0)
                val requirement = show.requirement(links.getString("requirements.$id", null) ?: error("No explicit requirement identity")) ?: error("Unknown requirement")
                val key = "${show.date}:${requirement.id}:${requirement.from}:${requirement.until}:${requirement.quantity}:${requirement.specification}"
                check(!links.contains(key)) { "Never overwrite a current assistance link" }
                host.prepare(); host.environment().reopen(id)
                check(links.edit().putString(key, id).commit())
                result.putString("restored", "Explicit retained assistance link restored; checked source; no inference or stock read")
                finish(0, result); return
            }
            if (cleanup) {
                val test = targetContext.getSharedPreferences("assistance-device-test", 0)
                val id = test.getString("workspace", null) ?: error("No assistance fixture to clean up")
                val key = test.getString("key", null)!!
                val links = targetContext.getSharedPreferences("organizer-workspace-links", 0)
                val editor = links.edit().remove("requirements.$id")
                if (links.getString(key, null) == id || !links.contains(key)) {
                    val previous = test.getString("previous", null)
                    if (previous == null) editor.remove(key) else editor.putString(key, previous)
                }
                check(editor.commit())
                host.prepare(); host.environment().forgetWorkspace(id)
                check(test.edit().clear().commit())
                result.putString("cleanup", "Authored test source removed; prior requirement link restored")
                finish(0, result)
                return
            }
            check(!targetContext.getSharedPreferences("assistance-device-test", 0).contains("workspace")) { "Clean up previous fixture before another run" }
            val show = ShowStore(targetContext).load() ?: error("Initialize a show before the assistance test")
            val requirement = show.requirements.first { it.name == "Boom stand" }
            val source = context.assets.open("editable-rental/experience.deal").bufferedReader().use { it.readText() }
                .replace("2026-10-10T16:00:00Z", Instant.ofEpochMilli(requirement.from).toString())
                .replace("2026-10-10T22:00:00Z", Instant.ofEpochMilli(requirement.until).toString())
            val ui = context.assets.open("editable-rental/experience.dealui").bufferedReader().use { it.readText() }
            host.prepare()
            val (workspace, snapshot) = host.environment().check(ExperienceSource(source, ui)).use { host.environment().open("Authored assistance device fixture", it) }
            check(snapshot.optString("fault").isEmpty())
            val key = "${show.date}:${requirement.id}:${requirement.from}:${requirement.until}:${requirement.quantity}:${requirement.specification}"
            val links = targetContext.getSharedPreferences("organizer-workspace-links", 0)
            // Keep the previous link to restore after testing; never infer authorship or overwrite its source.
            targetContext.getSharedPreferences("assistance-device-test", 0).edit().putString("key", key)
                .putString("previous", links.getString(key, null)).putString("workspace", workspace.id).commit()
            check(links.edit().putString(key, workspace.id).putString("requirements.${workspace.id}", requirement.id).commit())
            result.putString("assistanceFixture", workspace.id)
            result.putString("evidence", "Authored fixture only; real toolchain/sandbox, no model or stock calls; linked to exact live requirement period")
            finish(0, result)
        } catch (error: Throwable) { result.putString("failure", android.util.Log.getStackTraceString(error)); finish(1, result) }
        finally { host.close() }
    }
}
