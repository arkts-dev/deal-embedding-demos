package dev.deal.apps.pizza

import android.content.SharedPreferences
import android.content.pm.PackageManager
import dev.deal.embedding.capabilities.*

class PizzaService : CapabilityService() {
    override val capabilities by lazy { pizzaCapabilities(this) }
    override val delayMillis = 1200L
    private val prefs by lazy { getSharedPreferences("provider", 0) }
    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key -> if (key == "allowed") consentChanged() }
    override fun onCreate() { super.onCreate(); prefs.registerOnSharedPreferenceChangeListener(listener) }
    override fun onDestroy() { prefs.unregisterOnSharedPreferenceChangeListener(listener); super.onDestroy() }
    override fun consent() = prefs.getBoolean("allowed", false) || getSharedPreferences("pizza", 0).getBoolean("allowed", false)
    override fun authorize(uid: Int) = packageManager.getPackagesForUid(uid)?.toSet() == setOf("dev.deal.apps.organizer") &&
        packageManager.checkSignatures(uid, applicationInfo.uid) == PackageManager.SIGNATURE_MATCH
}
