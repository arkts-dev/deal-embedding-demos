package dev.deal.apps.vehicle

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import dev.deal.shell.*

class VehicleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("provider", 0)
        setContent {
            AppTheme(Color(0xff276570)) {
                var allowed by remember { mutableStateOf(prefs.getBoolean("allowed", false)) }
                var distance by remember { mutableFloatStateOf(12f) }
                Page("ON THE MOVE", "The road\nahead.", "A simple estimate. A little more certainty.") {
                    Panel {
                        Text("SIMULATED TRAVEL", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        Text("${5 + 2 * distance.toInt()} min", style = MaterialTheme.typography.displayLarge)
                        Text("${distance.toInt()} km · estimated journey", style = MaterialTheme.typography.titleMedium)
                        Slider(distance, { distance = it }, valueRange = 1f..60f, steps = 58)
                        Text("Demo estimate, not live traffic or a connected vehicle. Uses 5 + 2 × distance in kilometres.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Panel {
                        Text("Ready for your next stop", style = MaterialTheme.typography.headlineSmall)
                        Text("Calendar can request travel time. This app never reads your calendar or changes an event.")
                    }
                    Consent(allowed) { allowed = it; prefs.edit().putBoolean("allowed", it).commit() }
                }
            }
        }
    }
}
