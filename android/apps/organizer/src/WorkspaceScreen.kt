package dev.deal.apps.organizer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.deal.embedding.DealRenderer
import dev.deal.shell.*
import org.json.JSONObject

/** State and periods come from the requirement. The user never selects an execution route. */
@Composable fun RequestSheet(
    title: String,
    requirements: List<Requirement>,
    status: String,
    building: Boolean,
    onCancel: () -> Unit,
    onBuild: (goal: String, instruction: String) -> Unit,
    problem: GenerationProblem? = null,
    hasExistingWorkspace: Boolean = false,
    onDismissProblem: () -> Unit = {},
) {
    var budget by remember(title) { mutableStateOf("") }
    var preferences by remember(title) { mutableStateOf("") }
    var extra by remember(title) { mutableStateOf(false) }
    val valid = validBudget(budget)
    Scaffold(containerColor = MaterialTheme.colorScheme.background, bottomBar = {
        Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (building) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) {
                    CircularProgressIndicator(Modifier.size(24.dp).semantics { contentDescription = "Preparing equipment options" }, strokeWidth = 2.dp)
                    Text(compactGenerationStatus(status), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    TextButton(onClick = onCancel) { Text("Cancel") }
                }
                if (problem != null && !building) {
                    Text(problem.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                    Text("Your plan and previous options are unchanged. Nothing booked.", style = MaterialTheme.typography.bodyMedium)
                    Row {
                        TextButton(onClick = onDismissProblem) { Text("Edit request") }
                        TextButton(onClick = onCancel) { Text(if (hasExistingWorkspace) "Keep previous options" else "Back to event") }
                    }
                }
                Button(onClick = { onBuild("Equipment options", equipmentInstruction(budget.trim(), preferences.trim())) },
                    enabled = !building && valid, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                    Text(if (problem == null) "Find options" else "Try again")
                }
            }
        }
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onCancel, enabled = !building) { Icon(Icons.Outlined.ArrowBack, "Back to requirement") }
                Text("Find equipment", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            }
            AssistancePanel {
                Text(title, style = MaterialTheme.typography.titleMedium)
                requirements.forEach { requirement ->
                    Text("${requirement.quantity} required · ${requirement.act}", style = MaterialTheme.typography.bodyMedium)
                    Text(requirement.specification, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${dateTimeLabel(requirement.from, SHOW_ZONE)} – ${dateTimeLabel(requirement.until, SHOW_ZONE)}", style = MaterialTheme.typography.bodyMedium)
                    Text(requirement.location, style = MaterialTheme.typography.bodyMedium)
                }
            }
            OutlinedTextField(budget, { budget = it }, enabled = !building, label = { Text("Total budget (€) · optional") },
                singleLine = true, isError = !valid, supportingText = { Text(if (valid) "Leave empty to compare all prices." else "Enter an amount from 0 to 1000, with up to two decimals.") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(), shape = AssistanceDesign.shape)
            TextButton(onClick = { extra = !extra }, enabled = !building) {
                Icon(if (extra) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, null)
                Text("Preferences")
            }
            if (extra) OutlinedTextField(preferences, { preferences = it }, enabled = !building, label = { Text("Anything else? (optional)") },
                supportingText = { Text("For example, an adapter or an alternative specification.") }, modifier = Modifier.fillMaxWidth(), shape = AssistanceDesign.shape)
            Text("Your requirement, event timing and preferences are sent to AI to prepare these options. Stock is fetched only when you choose Load options.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** The native shell names the task, not its source. Inspect is supplied once by the event toolbar. */
@Composable fun WorkspaceView(title: String, tree: JSONObject, fault: String, onDispatch: (Int, String?) -> Unit, onClose: () -> Unit,
    onReview: () -> Unit, onNewBuild: (() -> Unit)? = null, hasPreparedSelection: Boolean = false) {
    var menu by remember { mutableStateOf(false) }
    var replacing by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        Surface(color = MaterialTheme.colorScheme.surface) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) { Icon(Icons.Outlined.ArrowBack, "Back to requirement") }
                Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                if (onNewBuild != null) Box {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Outlined.MoreVert, "Options") }
                    DropdownMenu(menu, { menu = false }) {
                        DropdownMenuItem(text = { Text("Change requirements") }, onClick = { menu = false; replacing = true })
                    }
                }
            }
        }
        if (fault.isNotEmpty()) AssistancePanel {
            Text("Options unavailable", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
            Text("Return to the requirement and try opening your options again. Your plan is unchanged.")
        }
        Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(16.dp)) { DealRenderer(tree, onDispatch) }
        if (hasPreparedSelection) Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp) {
            Button(onClick = onReview, modifier = Modifier.fillMaxWidth().padding(16.dp).heightIn(min = 56.dp)) {
                Icon(Icons.Outlined.FactCheck, null); Spacer(Modifier.width(8.dp)); Text("Review selection")
            }
        }
    }
    if (replacing) AlertDialog(onDismissRequest = { replacing = false }, title = { Text("Find different options?") },
        text = { Text("Your current options are kept until the new ones are ready.") },
        confirmButton = { TextButton(onClick = { replacing = false; onNewBuild?.invoke() }) { Text("Continue") } },
        dismissButton = { TextButton(onClick = { replacing = false }) { Text("Keep these") } })
}

/** Local review only. No action here claims or performs provider booking. */
@Composable fun ReviewSheet(operations: List<JSONObject>, onConfirm: () -> Unit, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "Back to options") }
            Text("Review selection", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        }
        Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = AssistanceDesign.shape) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Outlined.Info, null)
                Column {
                    Text("Not booked", style = MaterialTheme.typography.titleMedium)
                    Text("This selection is saved for review. Booking must be completed in the provider app.", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        if (operations.isEmpty()) Text("No selection prepared yet.")
        operations.asReversed().forEach { operation -> AssistancePanel {
            Text(operation.optString("title"), style = MaterialTheme.typography.titleMedium)
            Text(friendlyDates(operation.optString("detail"), SHOW_ZONE), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (operation.optString("price").isNotEmpty()) KeyValue("Total", operation.optString("price"))
            if (operation.optString("deadline").isNotEmpty()) KeyValue("Required by", friendlyDates(operation.optString("deadline"), SHOW_ZONE))
        } }
        Button(onClick = onConfirm, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("Back to options") }
    }
}
