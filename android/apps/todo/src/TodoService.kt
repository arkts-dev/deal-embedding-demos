package dev.deal.apps.todo

import android.content.SharedPreferences
import android.content.pm.PackageManager
import dev.deal.embedding.capabilities.*

class TodoService : CapabilityService() {
    private val prefs by lazy { getSharedPreferences("provider", 0) }
    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key -> if (key == "allowed") consentChanged() }
    override fun onCreate() { super.onCreate(); prefs.registerOnSharedPreferenceChangeListener(listener) }
    override fun onDestroy() { prefs.unregisterOnSharedPreferenceChangeListener(listener); super.onDestroy() }
    override val capabilities by lazy { todoCapabilities(this) }
    override val delayMillis = 1200L
    override fun consent() = prefs.getBoolean("allowed", false)
    override fun authorize(uid: Int) = packageManager.getPackagesForUid(uid)?.toSet() == setOf("dev.deal.apps.calendar") &&
        packageManager.checkSignatures(uid, applicationInfo.uid) == PackageManager.SIGNATURE_MATCH
}
