package dev.deal.apps.organizer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.deal.embedding.SavedWorkspace

/** Ordinary entry to retained source, not a debug replay switch. */
@Composable fun WorkspaceShelf(entries: List<SavedWorkspace>, error: String, onOpen: (String) -> Unit, onForget: (String) -> Unit, activeIds: Set<String> = emptySet()) {
    var deleting by remember { mutableStateOf<SavedWorkspace?>(null) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Workspaces", style = MaterialTheme.typography.headlineSmall) }
        if (error.isNotEmpty()) item { Text(error) }
        if (entries.isEmpty()) item { Text("No workspaces yet. Choose a requirement, then Arrange fulfilment.") }
        items(entries, key = { it.id }) { entry ->
            OutlinedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(entry.title, style = MaterialTheme.typography.titleMedium)
                    Text(workspacePresentation(entry.origin, entry.attempts).title + " · deal · deal ui", style = MaterialTheme.typography.labelLarge)
                    Row {
                        Button(onClick = { onOpen(entry.id) }, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { Text(if (entry.id in activeIds) "Resume" else "Open") }
                        IconButton(onClick = { deleting = entry }) { Icon(Icons.Outlined.DeleteOutline, "Delete ${entry.title}") }
                    }
                }
            }
        }
    }
    deleting?.let { entry -> AlertDialog(onDismissRequest = { deleting = null }, title = { Text("Delete workspace?") },
        text = { Text("Prepared review entries are kept. A new workspace requires a new build.") },
        confirmButton = { TextButton(onClick = { onForget(entry.id); deleting = null }) { Text("Delete") } },
        dismissButton = { TextButton(onClick = { deleting = null }) { Text("Keep") } }) }
}
