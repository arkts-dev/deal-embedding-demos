package dev.deal.apps.rental

import android.content.pm.PackageManager
import android.content.SharedPreferences
import dev.deal.embedding.capabilities.*

class RentalService : CapabilityService() {
    override val capabilities by lazy { rentalCapabilities(this) }
    override val delayMillis = 1200L
    private val prefs by lazy { getSharedPreferences("provider", 0) }
    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key -> if (key == "allowed") consentChanged() }
    override fun onCreate() { super.onCreate(); prefs.registerOnSharedPreferenceChangeListener(listener) }
    override fun onDestroy() { prefs.unregisterOnSharedPreferenceChangeListener(listener); super.onDestroy() }
    override fun consent() = prefs.getBoolean("allowed", false) || getSharedPreferences("rental", 0).getBoolean("allowed", false)
    override fun authorize(uid: Int) = packageManager.getPackagesForUid(uid)?.toSet() == setOf("dev.deal.apps.organizer") &&
        packageManager.checkSignatures(uid, applicationInfo.uid) == PackageManager.SIGNATURE_MATCH
}
