package dev.deal.apps.organizer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo

/** Debug-only capture switch, deliberately absent from generated capabilities. */
class DiagnosticsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE == 0) return
        if (intent.hasExtra("capture")) OrganizerProtocol.host(context).embedding.capture(intent.getBooleanExtra("capture", false))
    }
}
