package dev.deal.apps.organizer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import dev.deal.embedding.EmbeddingLog

/** Debug-only capture switch, deliberately absent from generated capabilities. */
class DiagnosticsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE == 0) return
        if (intent.hasExtra("capture")) EmbeddingLog.capture(context, intent.getBooleanExtra("capture", false))
    }
}
