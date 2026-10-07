package dev.deal.apps.organizer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.deal.embedding.DealRenderer
import dev.deal.shell.*
import androidx.compose.foundation.horizontalScroll
import org.json.JSONObject

/** The native request sheet: the user states a goal and constraints instead of writing a prompt. */
@Composable fun RequestSheet(
    title: String,
    requirements: List<Requirement>,
    host: OrganizerHost,
    status: String,
    building: Boolean,
    onCancel: () -> Unit,
    onBuild: (goal: String, instruction: String) -> Unit,
) {
    var goal by remember { mutableStateOf("Compare options") }
    var ceiling by remember { mutableStateOf("") }
    var responsible by remember { mutableStateOf("") }
    var preserve by remember { mutableStateOf(true) }
    var instruction by remember { mutableStateOf("") }
  Scaffold(
    containerColor = Color.Transparent,
    bottomBar = {
      Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 3.dp) {
        Button(
          onClick = { onBuild(goal, buildString {
            append(goal).append(". ")
            if (ceiling.isNotBlank()) append("Spending ceiling ").append(ceiling).append(" euro. ")
            if (responsible.isNotBlank()) append("Responsible person ").append(responsible).append(". ")
            append(if (preserve) "Preserve existing commitments. " else "Existing commitments may be replaced. ")
            append(instruction)
          }.trim()) },
          enabled = !building,
          modifier = Modifier.fillMaxWidth().padding(20.dp).height(56.dp).semantics { contentDescription = "Build workspace" },
          shape = RoundedCornerShape(18.dp),
        ) { Text(if (status.isEmpty()) "Build workspace" else status.take(40)) }
      }
    },
  ) { padding ->
    Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onCancel) { Icon(Icons.Outlined.Close, "Cancel") }
            Column { Text("Arrange fulfilment", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary) }
        }
        Panel {
            Text("Selected", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            requirements.forEach { requirement ->
                Column {
                    Text(if (requirement.quantity > 1) "${requirement.quantity} × ${requirement.name}" else requirement.name, style = MaterialTheme.typography.bodyLarge)
                    Text(requirement.specification, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Section("Goal") { }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf("Compare options", "Arrange rental", "Arrange hospitality", "Coordinate work").forEach { Chip(it, goal == it) { goal = it } }
        }
        Section("Constraints") { }
        OutlinedTextField(ceiling, { ceiling = it }, label = { Text("Spending ceiling in euro") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
        OutlinedTextField(responsible, { responsible = it }, label = { Text("Responsible person") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text("Preserve existing commitments"); Switch(preserve, { preserve = it }) }
        OutlinedTextField(instruction, { instruction = it }, label = { Text("Anything else (optional)") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
        Section("Connected apps") { }
        Panel {
            host.available().forEach { contract ->
                KeyValue(contract.module, contract.description)
            }
        }
        Section("What the model receives") { }
        Panel {
            Text("The selected requirements and their specifications, the show timing and your constraints, and the descriptions of the apps above. Provider data is read later by the experience itself and is never sent to the model.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (status.isNotEmpty()) {
            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.errorContainer, modifier = Modifier.fillMaxWidth()) {
                Text(status, Modifier.padding(14.dp), color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
  }
}

/** A live generated workspace. Its layout is generated; the surface is natively rendered Compose. */
@Composable fun WorkspaceView(title: String, tree: JSONObject, fault: String, onDispatch: (Int, String?) -> Unit, onClose: () -> Unit, onReview: () -> Unit, prepared: Int) {
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("DEAL WORKSPACE · generated", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
            Row {
                if (prepared > 0) Button(onClick = onReview, shape = RoundedCornerShape(16.dp)) { Text("Review $prepared") }
                IconButton(onClick = onClose) { Icon(Icons.Outlined.Close, "Close workspace") }
            }
        }
        if (fault.isNotEmpty()) {
            Surface(color = MaterialTheme.colorScheme.errorContainer, modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                Text(fault, Modifier.padding(14.dp), color = MaterialTheme.colorScheme.onErrorContainer)
            }
        }
        Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(20.dp)) {
            DealRenderer(tree, onDispatch)
        }
    }
}

/** Native review of the operations the workspace prepared. Nothing has been written yet. */
@Composable fun ReviewSheet(operations: List<JSONObject>, onConfirm: () -> Unit, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "Back") }
            Column { Text("Prepared operations", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text("Nothing is written until you confirm it", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        operations.forEach { operation ->
            Panel {
                Text(operation.optString("provider"), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Text(operation.optString("title"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(operation.optString("detail"), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (operation.optString("price").isNotEmpty()) KeyValue("Price", operation.optString("price"))
                if (operation.optString("deadline").isNotEmpty()) KeyValue("Deadline", operation.optString("deadline"))
            }
        }
        Text("Each operation is confirmed in its own app. If one fails, the others stand and the failed step stays outstanding.", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Button(onClick = onConfirm, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) { Text("Continue to confirmation") }
    }
}
