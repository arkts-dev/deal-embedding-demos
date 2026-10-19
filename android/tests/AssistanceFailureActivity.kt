package dev.deal.connectors.tests

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.deal.embedding.discovery.DiscoveryFact
import dev.deal.shell.*

/** Explicit test-only failure surface; no live inference and no saved source injection. */
class AssistanceFailureActivity : ComponentActivity() {
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        setContent { AppTheme(Accent.Todo) {
            var attempts by remember { mutableIntStateOf(0) }
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                Text("Synthetic failure fixture · attempts $attempts", Modifier.padding(16.dp))
                AssistedTask(listOf(DiscoveryFact("fixture", "Synthetic checklist test")),
                    protocolFactory = { attempts++; error("Test-only bootstrap failure") }) {
                    Text("Native task stays available", Modifier.padding(16.dp))
                }
            }
        } }
    }
}
