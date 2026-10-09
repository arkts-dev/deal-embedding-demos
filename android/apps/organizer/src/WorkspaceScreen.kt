package dev.deal.apps.organizer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.deal.embedding.DealRenderer
import dev.deal.embedding.WorkspaceOrigin
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
    problem: GenerationProblem? = null,
    hasExistingWorkspace: Boolean = false,
    onDismissProblem: () -> Unit = {},
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
      Column {
        if (building || problem != null) {
            Surface(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp).semantics { liveRegion = LiveRegionMode.Polite },
                shape = RoundedCornerShape(16.dp), border = BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface), color = MaterialTheme.colorScheme.surface) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(if (building) "Building your workspace" else problem!!.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(if (building) status else problem!!.explanation, style = MaterialTheme.typography.bodyLarge)
                    if (building) {
                        OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Cancel generation") }
                    } else {
                        OutlinedButton(onClick = onDismissProblem, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Change request") }
                        OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(if (hasExistingWorkspace) "Keep existing workspace" else "Back to plan") }
                        Text("Build again starts a new generation run. There is no automatic retry.", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        if (!building) Button(
          onClick = { onBuild(goal, buildString {
            append(goal).append(". ")
            if (goal == "Editable rental") append("Generate an editable shortlist, not the fixed catalogue template. Provide Quantity IntField (initial disclosed quantity, minimum 0, maximum 9, valid 1 to 8), Total budget in cents IntField (initial 2000, minimum 0 maximum 100000 step 100) and SearchField. Editing clears stale results and selection. Show validation and disable Load options when invalid. Read the discovered equipment catalogue only on Load options; filter requirement matches, additional search substring, availability >= quantity and total price <= budget. Show original option titles and total euro prices, retain selected item, and prepare an exact item/quantity/period/total-price descriptor for native review. Disable editing during effects; show honest empty/error/retry states. Never reserve or write to a provider. ")
            if (ceiling.isNotBlank()) append("Spending ceiling ").append(ceiling).append(" euro. ")
            if (responsible.isNotBlank()) append("Responsible person ").append(responsible).append(". ")
            append(if (preserve) "Preserve existing commitments. " else "Existing commitments may be replaced. ")
            append(instruction)
          }.trim()) },
          enabled = !building,
          modifier = Modifier.fillMaxWidth().padding(20.dp).height(56.dp).semantics { contentDescription = "Build workspace" },
          shape = RoundedCornerShape(18.dp),
        ) { Text(if (problem == null) "Build workspace" else "Build again · new run") }
      }
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
            listOf("Compare options", "Arrange rental", "Editable rental", "Arrange hospitality", "Coordinate work").forEach { Chip(it, goal == it) { goal = it } }
        }
        Section("Constraints") { }
        OutlinedTextField(ceiling, { ceiling = it }, label = { Text(if (goal == "Editable rental" || goal == "Arrange rental") "Rental total budget (€)" else "Total spending budget (€)") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
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
    }
  }
}

/** A live generated workspace. Its layout is generated; the surface is natively rendered Compose. */
@Composable fun WorkspaceView(title: String, tree: JSONObject, fault: String, onDispatch: (Int, String?) -> Unit, onClose: () -> Unit, onReview: () -> Unit, prepared: Int, origin: WorkspaceOrigin, attempts: Int) {
    val presentation = workspacePresentation(origin, attempts)
    var details by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        Surface(color = MaterialTheme.colorScheme.surface, border = BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface), modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(presentation.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                        Text(title, maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
                    }
                    IconButton(onClick = onClose) { Icon(Icons.Outlined.Close, "Close workspace") }
                }
                Text(presentation.explanation, style = MaterialTheme.typography.bodyMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (prepared > 0) OutlinedButton(onClick = onReview, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { Text("Organizer review · $prepared") }
                    TextButton(onClick = { details = true }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Source details") }
                }
                Text(if (prepared > 0) "$prepared operations prepared locally for native review; not reservations. Workspace content below." else "Workspace content below. Native review is separate; preparation is not a reservation.", style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
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
    if (details) AlertDialog(onDismissRequest = { details = false }, title = { Text(presentation.title) },
        text = { Text(presentation.details) }, confirmButton = { TextButton(onClick = { details = false }) { Text("Close details") } })
}

/** Native review of locally prepared descriptors; no provider reservation has been made. */
@Composable fun ReviewSheet(operations: List<JSONObject>, onConfirm: () -> Unit, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "Back") }
            Column { Text("Organizer review · native", modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text("Prepared locally; no provider reservation has been made", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        Surface(border = BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
            Text("Prepared operations — not reservations. This screen belongs to Organizer, not the generated workspace.", Modifier.padding(16.dp), style = MaterialTheme.typography.bodyLarge)
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
        Text("This is a review descriptor, not a reservation. Provider confirmation is not connected on this surface; arrange the reservation in the provider app.", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Button(onClick = onConfirm, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) { Text("Back to workspace") }
    }
}
