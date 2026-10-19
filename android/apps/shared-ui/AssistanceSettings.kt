package dev.deal.shell

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.deal.embedding.SharedCapabilityContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun appAccent(packageName: String) = when (packageName.substringAfterLast('.')) {
    "calendar" -> Accent.Calendar
    "organizer" -> Accent.Organizer
    "pizza" -> Accent.Pizza
    "todo" -> Accent.Todo
    else -> Accent.Rental
}

@Composable fun AssistanceSettingsButton(localModule: String = "", storageName: String = "assistance") {
    val context = LocalContext.current
    IconButton(onClick = { context.startActivity(Intent(context, AssistanceSettingsActivity::class.java).putExtra("localModule", localModule).putExtra("storage", storageName)) }) { Icon(Icons.Outlined.Settings, "Settings") }
}

@OptIn(ExperimentalMaterial3Api::class)
class AssistanceSettingsActivity : ComponentActivity() {
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        setContent { AppTheme(appAccent(packageName)) {
            val settings = remember { Connections.settings(this) }
            val sharing = remember { getSharedPreferences("provider", 0) }
            var discover by remember { mutableStateOf(settings.getBoolean("discovery", true)) }
            var generate by remember { mutableStateOf(settings.getBoolean("generation", false)) }
            var shared by remember { mutableStateOf(sharing.getBoolean("allowed", false)) }
            var contracts by remember { mutableStateOf<List<SharedCapabilityContract>>(emptyList()) }
            var grants by remember { mutableStateOf(settings.all.filter { it.key.startsWith("grant.") }.mapValues { it.value == true }) }
            var problem by remember { mutableStateOf("") }
            LaunchedEffect(Unit) { withContext(Dispatchers.IO) {
                runCatching { assistanceProtocol(this@AssistanceSettingsActivity).use { it.refresh() } }
                    .onSuccess { contracts = it }.onFailure { problem = "Couldn’t find connected apps" }
            } }
            Scaffold(topBar = { TopAppBar(title = { Text("Settings") }, navigationIcon = { TextButton(onClick = { finish() }) { Text("Back") } }) }) { padding ->
                Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    TextButton(onClick = { startActivity(Intent(this@AssistanceSettingsActivity, AssistanceInspectActivity::class.java).putExtra("storage", intent.getStringExtra("storage") ?: "assistance")) }) { Text("Inspect") }
                    Text("Suggestions", style = MaterialTheme.typography.titleLarge)
                    Text("Find useful help on this device using this app’s demo content and connected apps’ available actions. No other app’s records are read to find a suggestion.")
                    SettingSwitch("On-device suggestions", discover) { discover = it; check(settings.edit().putBoolean("discovery", it).commit()) }
                    Text("When you tap Open, the suggestion and its supporting demo content are sent to the configured code service to build a tool. Nothing is booked or purchased by opening it.")
                    SettingSwitch("Tailored tools", generate) { generate = it; check(settings.edit().putBoolean("generation", it).commit()) }
                    HorizontalDivider()
                    Text("Sharing", style = MaterialTheme.typography.titleLarge)
                    Text("Allow the other demo apps to read shared records and submit proposals for review. Available actions remain visible while sharing is off. Changes still need confirmation in the app that owns them.")
                    SettingSwitch("Share with connected apps", shared) { shared = it; check(sharing.edit().putBoolean("allowed", it).commit()) }
                    HorizontalDivider()
                    Text("Tool access", style = MaterialTheme.typography.titleLarge)
                    Text("Choose which connected apps tools may use. This does not turn on sharing in those apps. Revoking access does not delete their records.")
                    if (problem.isNotEmpty()) Text(problem)
                    intent.getStringExtra("localModule")?.takeIf { it.isNotEmpty() }?.let { module ->
                        val key = "grant.$module"
                        SettingSwitch("${Connections.name(module)} review", grants[key] == true) { value ->
                            check(settings.edit().putBoolean(key, value).commit()); grants = grants + (key to value)
                        }
                    }
                    contracts.forEach { contract ->
                        val key = "grant.${contract.module}"
                        SettingSwitch(Connections.name(contract.module), grants[key] == true) { value ->
                            check(settings.edit().putBoolean(key, value).commit()); grants = grants + (key to value)
                        }
                    }
                }
            }
        } }
    }
}

@Composable private fun SettingSwitch(label: String, value: Boolean, change: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().toggleable(value = value, role = Role.Switch, onValueChange = change), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, Modifier.weight(1f)); Switch(value, null)
    }
}
