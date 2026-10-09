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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.deal.embedding.DealRenderer
import dev.deal.embedding.WorkspaceOrigin
import dev.deal.shell.*
import androidx.compose.foundation.horizontalScroll
import org.json.JSONObject

/** Non-interactive labels: technology tags never imply authorship or permission. */
@Composable private fun WorkspaceTag(label: String, icon: ImageVector? = null) {
    Surface(shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface), color = MaterialTheme.colorScheme.surface) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (icon != null) Icon(icon, null, modifier = Modifier.size(16.dp))
            Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        }
    }
}

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
    var problemDetails by remember(problem) { mutableStateOf(false) }
  Scaffold(
    containerColor = Color.Transparent,
    bottomBar = {
      Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 3.dp) {
      Column {
        if (building || problem != null) {
            Surface(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp).semantics { liveRegion = LiveRegionMode.Polite },
                shape = RoundedCornerShape(16.dp), border = BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface), color = MaterialTheme.colorScheme.surface) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (building) CircularProgressIndicator(modifier = Modifier.size(24.dp).semantics { contentDescription = "Generation in progress" }, color = MaterialTheme.colorScheme.onSurface, strokeWidth = 2.dp)
                        else Icon(Icons.Outlined.WarningAmber, null)
                        Text(if (building) compactGenerationStatus(status) else problem!!.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    if (!building) Text("Existing workspace unchanged. Nothing reserved.", style = MaterialTheme.typography.bodyMedium)
                    if (building) {
                        OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Cancel") }
                    } else {
                        OutlinedButton(onClick = onDismissProblem, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Edit request") }
                        OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(if (hasExistingWorkspace) "Keep existing workspace" else "Back to plan") }
                        TextButton(onClick = { problemDetails = true }, modifier = Modifier.heightIn(min = 48.dp)) { Icon(Icons.Outlined.Info, null); Spacer(Modifier.width(6.dp)); Text("Details") }
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
    if (problemDetails && problem != null) AlertDialog(onDismissRequest = { problemDetails = false }, title = { Text(problem.title) },
        text = { Text(problem.explanation + "\n\nBuild again starts a new generation run. There is no automatic retry.") },
        confirmButton = { TextButton(onClick = { problemDetails = false }) { Text("Close details") } })
}

/** A live generated workspace. Its layout is generated; the surface is natively rendered Compose. */
@Composable fun WorkspaceView(title: String, tree: JSONObject, fault: String, onDispatch: (Int, String?) -> Unit, onClose: () -> Unit, onReview: () -> Unit, prepared: Int, origin: WorkspaceOrigin, attempts: Int, onNewBuild: (() -> Unit)? = null) {
    val presentation = workspacePresentation(origin, attempts)
    var details by remember { mutableStateOf(false) }
    var newBuild by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        Surface(color = MaterialTheme.colorScheme.surface, border = BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface), modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(title, maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Row(Modifier.semantics { liveRegion = LiveRegionMode.Polite }, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            WorkspaceTag(presentation.title, when (origin) {
                                WorkspaceOrigin.AI_SOURCE -> Icons.Outlined.AutoAwesome
                                WorkspaceOrigin.CATALOGUE -> Icons.Outlined.GridView
                                WorkspaceOrigin.SAVED_SOURCE -> Icons.Outlined.Code
                            })
                            WorkspaceTag("deal")
                            WorkspaceTag("deal ui")
                        }
                    }
                    IconButton(onClick = onClose) { Icon(Icons.Outlined.ArrowBack, "Back to workspaces") }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (prepared > 0) OutlinedButton(onClick = onReview, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { Icon(Icons.Outlined.FactCheck, null); Spacer(Modifier.width(6.dp)); Text("Review · $prepared") }
                    TextButton(onClick = { details = true }, modifier = Modifier.heightIn(min = 48.dp)) { Icon(Icons.Outlined.Info, null); Spacer(Modifier.width(6.dp)); Text("Details") }
                }
                Text(if (prepared > 0) "$prepared prepared · Not reserved" else "Not reserved", style = MaterialTheme.typography.bodyMedium,
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
        text = { Column {
            Text(presentation.explanation + "\n\n" + presentation.details + "\n\ndeal: application logic. deal ui: screen and events. Both AI-built and template workspaces use these technologies. Native review belongs to Organizer.")
            if (onNewBuild != null) OutlinedButton(onClick = { details = false; newBuild = true }) { Text("New build") }
        } }, confirmButton = { TextButton(onClick = { details = false }) { Text("Close details") } })
    if (newBuild) AlertDialog(onDismissRequest = { newBuild = false }, title = { Text("Build another workspace?") },
        text = { Text("Starts new AI generation. This workspace is kept.") },
        confirmButton = { TextButton(onClick = { newBuild = false; onNewBuild?.invoke() }) { Text("Continue") } },
        dismissButton = { TextButton(onClick = { newBuild = false }) { Text("Keep this") } })
}

/** Native review of locally prepared descriptors; no provider reservation has been made. */
@Composable fun ReviewSheet(operations: List<JSONObject>, onConfirm: () -> Unit, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "Back") }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Outlined.FactCheck, null)
                    Text("Organizer review", modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { WorkspaceTag("Native"); WorkspaceTag("Not reserved") }
            }
        }
        Surface(border = BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
            Text("Prepared locally. Nothing booked.", Modifier.padding(16.dp), style = MaterialTheme.typography.bodyLarge)
        }
        operations.forEach { operation ->
            Panel {
                Text(operation.optString("provider"), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Text(operation.optString("title"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(friendlyDates(operation.optString("detail"), SHOW_ZONE), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (operation.optString("price").isNotEmpty()) KeyValue("Price", operation.optString("price"))
                if (operation.optString("deadline").isNotEmpty()) KeyValue("Deadline", friendlyDates(operation.optString("deadline"), SHOW_ZONE))
            }
        }
        Text("To book, open the provider app.", style = MaterialTheme.typography.bodyMedium)
        Button(onClick = onConfirm, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) { Text("Back to workspace") }
    }
}
