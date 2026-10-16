package dev.deal.apps.organizer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.deal.embedding.*
import dev.deal.shell.*
import org.json.JSONObject
import java.util.concurrent.Executors
import java.util.concurrent.CancellationException

private enum class Destination(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Overview("Overview", Icons.Outlined.Dashboard), Riders("Riders", Icons.Outlined.Assignment), Resources("Resources", Icons.Outlined.Inventory2), Fulfilment("Fulfilment", Icons.Outlined.Checklist)
}

class OrganizerActivity : ComponentActivity() {
    private val store by lazy { ShowStore(this) }
    private val host by lazy { OrganizerProtocol.host(this) }
    private val log by lazy { EmbeddingLog(this) }
    private val worker = Executors.newSingleThreadExecutor()
    @Volatile private var runningGeneration: GenerationRequest? = null
    private val surfaces=java.util.concurrent.ConcurrentHashMap<String,ExperienceSurface>()
    @Volatile private var cancelRequested=false
    private fun cancelGeneration(){cancelRequested=true;runningGeneration?.let{host.embedding.cancel(it)}}
    override fun onDestroy() {
        try{cancelGeneration()}catch(error:Throwable){android.util.Log.e("Organizer","Cancellation failed during destruction",error)}
        worker.execute {
            try{teardown(*surfaces.values.map{surface->{surface.close()}}.toTypedArray(),{OrganizerProtocol.shutdown()})}
            catch(error:Throwable){android.util.Log.e("Organizer","Shutdown failed",error)}
            finally{surfaces.clear()}
        }
        worker.shutdown();super.onDestroy()
    }
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        var show by mutableStateOf(store.load())
        var request by mutableStateOf<List<String>?>(null)
        var generating by mutableStateOf(false)
        var status by mutableStateOf("")
        var problem by mutableStateOf<GenerationProblem?>(null)
        var saved by mutableStateOf<List<Experience>>(emptyList())
        var opening by mutableStateOf(false)
        var openError by mutableStateOf("")
        var open by mutableStateOf<Experience?>(null)
        val activeWorkspace = java.util.concurrent.atomic.AtomicReference<String?>(null)
        var activeSurface by mutableStateOf<ExperienceSurface?>(null)
        var fault by mutableStateOf("")
        var review by mutableStateOf(false)
        var chooseBuildRequirement by mutableStateOf(false)
        var returnRequirement by mutableStateOf<String?>(null)
        var inspectionRequest by mutableStateOf("")
        var inspectionStartedAt by mutableStateOf(0L)
        var inspectionError by mutableStateOf("")
        var currentPrepared by mutableStateOf(false)
        var currentSelection by mutableStateOf<JSONObject?>(null)
        var preparedReference = ""
        val reviewReady = mutableMapOf<String, Boolean>()
        val links = getSharedPreferences("organizer-workspace-links", 0)
        fun requirementKey(id: String): String {
            val r = show!!.requirement(id)!!
            return "${show!!.date}:$id:${r.from}:${r.until}:${r.quantity}:${r.specification}"
        }
        fun update(block: (ShowState) -> Unit) {
            val current = show ?: return
            // Publish a new plan identity: mutating a shared object can leave child
            // composables showing stale checks, allocations and filtered lists.
            val next = ShowState.parse(current.toJson().toString())
            block(next); store.save(next); show = next
        }
        fun publish(id: String, snapshot: JSONObject) {
            if (activeWorkspace.get() != id) return
            runOnUiThread {
                if (!isDestroyed && activeWorkspace.get() == id) {
                    fault = snapshot.optString("fault")
                    val preparedPrefs = getSharedPreferences("prepared", 0)
                    val prepared = preparedPrefs.getString("workspace.$id", null)?.let { JSONObject(it) }
                    val reference = preparedPrefs.getString("workspace.$id.reference", "").orEmpty()
                    if (reference != preparedReference || prepared?.toString() != currentSelection?.toString()) {
                        preparedReference = reference
                        currentSelection = prepared
                        currentPrepared = prepared != null
                        reviewReady[id] = currentPrepared
                    }
                }
            }
        }
        fun reportFailure(id: String, operation: String, error: Throwable) {
            log.event("host", "failed", workspace = id, operation = operation, code = error.javaClass.simpleName)
            runOnUiThread { if (!isDestroyed && activeWorkspace.get() == id) fault = error.message ?: "Workspace error" }
        }
        fun activate(workspace:Experience):ExperienceSurface {
            val surface=surfaces.getOrPut(workspace.id){ExperienceSurface(host.embedding,workspace)}
            host.embedding.withPreparation(workspace){surface.activate{error->reportFailure(workspace.id,"surface",error)}}
            return surface
        }
        fun openSaved(id: String) {
            if (opening || generating) return
            opening = true; openError = ""; inspectionError = ""
            // A durable preparation record does not prove the freshly mounted UI has
            // a current selection. Only this process's live assistance may resume review.
            val preparedPrefs = getSharedPreferences("prepared", 0)
            currentSelection = preparedPrefs.getString("workspace.$id", null)?.let { JSONObject(it) }
            preparedReference = preparedPrefs.getString("workspace.$id.reference", "").orEmpty()
            currentPrepared = reviewReady[id] == true
            inspectionStartedAt = 0L; inspectionRequest = ""
            worker.execute {
                try {
                    host.prepare()
                    val workspace = host.embedding.saved().first{it.id==id}
                    val surface=activate(workspace)
                    activeWorkspace.set(workspace.id)
                    java.io.File(filesDir, "workspace-error.txt").delete()
                    runOnUiThread { if (!isDestroyed) { activeSurface=surface;open = workspace; request = null; review = false; problem = null; opening = false } }
                } catch (error: Throwable) {
                    log.event("host", "failed", code = error.javaClass.simpleName, target = "reopen")
                    runOnUiThread { if (!isDestroyed) { opening = false; openError = "Couldn’t open your options. They have been kept."; inspectionError = error.javaClass.simpleName + ": " + error.message } }
                }
            }
        }
        val inspect = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK && !generating && !opening) {
                result.data?.getStringExtra("openWorkspace")?.let { id ->
                    returnRequirement = linkedRequirements(links.getString("requirements.$id", null), show ?: return@let).firstOrNull()
                    openSaved(id)
                }
                result.data?.getStringExtra("deleteWorkspace")?.let { id ->
                    opening = true
                    worker.execute {
                        try {
                            surfaces.remove(id)?.close();host.forget(host.embedding.saved().first{it.id==id})
                            val entries = host.embedding.saved()
                            activeWorkspace.compareAndSet(id, null)
                            val editor = links.edit().remove("requirements.$id")
                            links.all.filterValues { it == id }.keys.forEach { editor.remove(it) }
                            check(editor.commit()) { "Cannot remove assistance link" }
                            runOnUiThread { saved = entries; if (open?.id == id) open = null; opening = false }
                        } catch (error: Throwable) {
                            log.event("storage", "failed", workspace = id, code = error.javaClass.simpleName)
                            runOnUiThread { opening = false; openError = "Couldn’t delete your options." }
                        }
                    }
                }
            }
        }
        worker.execute {
            try {
                host.prepare()
                val entries = host.embedding.saved()
                runOnUiThread { if (!isDestroyed) saved = entries }
            } catch (error: Throwable) { log.event("host", "failed", code = error.javaClass.simpleName, target = "list") }
        }
        intent.getStringExtra("replayWorkspace")?.takeIf { applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0 }?.let { openSaved(it) }
        setContent {
            AppTheme(Accent.Organizer) {
                var destination by remember { mutableStateOf(Destination.Overview) }
                var selectedAct by remember { mutableStateOf<String?>(null) }
                var selectedDepartment by remember { mutableStateOf<String?>(null) }
                var selectedRequirement by remember { mutableStateOf<String?>(null) }
                fun findEquipment(id: String) {
                    if (generating || opening) return
                    selectedRequirement = id; returnRequirement = id
                    problem = null; status = ""; openError = ""; inspectionRequest = ""; inspectionStartedAt = 0L; inspectionError = ""
                    val retained = links.getString(requirementKey(id), null)
                    if (retained != null && saved.any { it.id == retained }) openSaved(retained) else request = listOf(id)
                }
                BackHandler(enabled = generating || opening || request != null || review || open != null || selectedRequirement != null) {
                    when {
                        generating -> cancelGeneration()
                        opening -> Unit // Do not abandon a mount halfway through publication.
                        request != null -> { request = null; problem = null }
                        review -> review = false
                        open != null -> { activeWorkspace.set(null); open = null; selectedRequirement = returnRequirement }
                        else -> { selectedRequirement = null; returnRequirement = null }
                    }
                }
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Scaffold(
                        containerColor = MaterialTheme.colorScheme.background,
                        topBar = {
                            Column {
                                OrganizerTopBar(show, onInitialize = { show = store.initialize() }, onInspect = {
                                    val engineering = android.content.Intent(this@OrganizerActivity, DiagnosticsActivity::class.java)
                                    if (request == null) open?.let { engineering.putExtra("workspace", it.id) }
                                    engineering.putExtra("since", inspectionStartedAt)
                                    engineering.putExtra("request", inspectionRequest)
                                    engineering.putExtra("error", if (fault.isNotEmpty() && open != null && request == null) fault else inspectionError)
                                    engineering.putExtra("busy", generating || opening)
                                    engineering.putExtra("context", if (generating) "Preparing equipment options" else request?.mapNotNull { show?.requirement(it)?.name }?.joinToString() ?: returnRequirement?.let { show?.requirement(it)?.name } ?: "Event activity")
                                    inspect.launch(engineering)
                                })
                            }
                        },
                        bottomBar = {
                            if (show != null) NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                                Destination.entries.forEach { item ->
                                    NavigationBarItem(selected = destination == item, enabled = !generating && !opening, onClick = { destination = item; request = null; problem = null; selectedRequirement = null; returnRequirement = null; activeWorkspace.set(null); open = null; review = false }, icon = { Icon(item.icon, null) }, label = { Text(item.label) })
                                }
                            }
                        },
                    ) { padding ->
                        Column(Modifier.padding(padding).fillMaxSize()) {
                            when {
                                opening -> Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) { CircularProgressIndicator(); Text("Opening your options…") }
                                review -> ReviewSheet(listOfNotNull(currentSelection), onConfirm = { runOnUiThread { review = false } }, onBack = { review = false })
                                show == null -> EmptyPlan(onInitialize = { show = store.initialize() })
                                request != null -> RequestSheet(
                                    title = request!!.mapNotNull { id -> show!!.requirement(id)?.name }.joinToString(", "),
                                    requirements = request!!.mapNotNull { id -> show!!.requirement(id) }, status = status, building = generating,
                                    problem = problem, hasExistingWorkspace = open != null,
                                    onDismissProblem = { problem = null; status = "" },
                                    onCancel = { if (generating) cancelGeneration() else { request = null; problem = null; status = "" } },
                                    onBuild = { goal, instruction ->
                                        if (generating || opening) return@RequestSheet
                                        generating = true; cancelRequested=false; problem = null; status = "Discovering connected apps…"
                                        inspectionStartedAt = System.currentTimeMillis(); inspectionError = ""
                                        val selected = request!!.mapNotNull { id -> show!!.requirement(id) }
                                        val intent = "Resolve these requirements for ${selected.firstOrNull()?.act ?: "the show"}: " +
                                            selected.joinToString("; ") { "${it.quantity} × ${it.name} — ${it.specification}, needed at ${it.location} by ${it.end(true)}" } + ". " + instruction
                                        val disclosed = JSONObject().put("show", show!!.title).put("venue", show!!.venue)
                                            .put("date", show!!.date.toString()).put("timeZone", SHOW_ZONE.id)
                                            .put("requirements", org.json.JSONArray(selected.map { it.name })).apply {
                                                if (selected.size == 1) {
                                                    val requirement = selected.single()
                                                    put("requirement", requirement.name).put("specification", requirement.specification)
                                                    put("quantity", requirement.quantity)
                                                    put("from", java.time.Instant.ofEpochMilli(requirement.from).toString())
                                                    put("until", java.time.Instant.ofEpochMilli(requirement.until).toString())
                                                    put("searchTerms", org.json.JSONArray(requirement.name.split(" ").filter { it.length > 2 }))
                                                }
                                            }.toString()
                                        inspectionRequest = intent + "\n\nDISCLOSED CONTEXT\n" + disclosed
                                        val token = java.util.concurrent.atomic.AtomicReference<GenerationRequest?>(null)
                                        worker.execute {
                                            try {
                                                host.prepare()
                                                val context=host.embedding.snapshot(organizerContextSchema(),disclosed)
                                                val generation=host.embedding.request("$goal · ${selected.joinToString { it.name }}",intent,context)
                                                token.set(generation);runningGeneration=generation
                                                if(cancelRequested)host.embedding.cancel(generation)
                                                val workspace=host.embedding.generate(generation)
                                                run {
                                                    if(cancelRequested){host.embedding.release(workspace);throw CancellationException("Generation cancelled")}
                                                    val surface=activate(workspace)
                                                    if(cancelRequested){surfaces.remove(workspace.id)?.close();host.forget(workspace);throw CancellationException("Generation cancelled")}
                                                    val editor = links.edit().putString("requirements.${workspace.id}", selected.joinToString(",") { it.id })
                                                    selected.forEach { editor.putString(requirementKey(it.id), workspace.id) }
                                                    check(editor.commit()) { "Cannot retain workspace link" }
                                                    currentPrepared = false; currentSelection = null; preparedReference = ""
                                                    activeWorkspace.set(workspace.id)
                                                    java.io.File(filesDir, "workspace-error.txt").delete()
                                                    val workspaces = host.embedding.saved()
                                                    runOnUiThread { if (!isDestroyed) { saved = workspaces; activeSurface=surface;open = workspace; request = null; generating = false; status = ""; problem = null; runningGeneration = null } }
                                                }
                                            } catch (error: Throwable) {
                                                log.event("host", "failed", code = error.javaClass.simpleName, target = "generation")
                                                runOnUiThread { if (!isDestroyed && runningGeneration === token.get()) { generating = false; status = ""; problem = generationProblem(error); inspectionError = (if(error is ProtocolFailure)error.code else error.javaClass.simpleName) + ": " + error.message; runningGeneration = null } }
                                            }
                                        }
                                    },
                                )
                                open != null -> WorkspaceView(
                                    title = if (returnRequirement != null) "" else open!!.title,
                                    fault = fault,
                                    onClose = {
                                        val id = open!!.id
                                        worker.execute {
                                            activeWorkspace.compareAndSet(id, null)
                                            // Back keeps the mounted state and saved source; Delete is explicit.
                                            runOnUiThread { open = null; selectedRequirement = returnRequirement }
                                        }
                                    },
                                    onNewBuild = {
                                        inspectionStartedAt = 0L; inspectionRequest = ""
                                        val ids = linkedRequirements(links.getString("requirements.${open!!.id}", null), show!!)
                                        if (ids.isEmpty()) chooseBuildRequirement = true else request = ids
                                        problem = null; status = ""
                                    },
                                    onReview = { review = true },
                                    hasPreparedSelection = currentPrepared,
                                ) {
                                    activeSurface?.let{surface->ExperienceSurface(surface,transformTree=::displayDates,
                                        onAction={currentPrepared=false;reviewReady[surface.experience.id]=false},
                                        onSnapshot={snapshot->publish(surface.experience.id,snapshot.json())})}
                                }
                                selectedRequirement != null -> RequirementDetail(
                                    show = show!!, requirementId = selectedRequirement!!,
                                    onBack = { selectedRequirement = null; returnRequirement = null },
                                    assistanceError = openError,
                                    hasOptions = links.getString(requirementKey(selectedRequirement!!), null)?.let { retained -> saved.any { it.id == retained } } == true,
                                    onEdit = { updated -> update { it.requirements = it.requirements.map { r -> if (r.id == updated.id) updated else r } } },
                                    onVerify = { id -> update { s -> s.requirements = s.requirements.map { r -> if (r.id == id) r.copy(verified = !r.verified) else r } } },
                                    onArrange = ::findEquipment,
                                )
                                else -> when (destination) {
                                    Destination.Overview -> OverviewScreen(state = show!!, hasOptions = { id -> links.getString(requirementKey(id), null)?.let { retained -> saved.any { it.id == retained } } == true }, onOpenRequirement = { selectedAct = null; selectedRequirement = it }, onFindEquipment = ::findEquipment, onOpenRiders = { department -> selectedAct = null; selectedDepartment = department; destination = Destination.Riders })
                                    Destination.Riders -> RidersScreen(state = show!!, selectedAct, selectedDepartment, onSelectDepartment = { selectedDepartment = it }, onSelectAct = { selectedAct = it }, onOpenRequirement = { selectedRequirement = it })
                                    Destination.Resources -> ResourcesScreen(state = show!!, onResolve = { allocation, agreed -> update { s ->
                                        val resource = s.allocations.first { it.id == allocation }.resourceId
                                        s.allocations = s.allocations.map { if (it.resourceId == resource) it.copy(agreed = agreed) else it }
                                    } })
                                    Destination.Fulfilment -> FulfilmentScreen(state = show!!, onVerify = { id -> update { s -> s.requirements = s.requirements.map { r -> if (r.id == id) r.copy(verified = !r.verified) else r } } }, onOpenRequirement = { selectedRequirement = it })
                                }
                            }
                        }
                    }
                }
                if (chooseBuildRequirement) AlertDialog(onDismissRequest = { chooseBuildRequirement = false },
                    title = { Text("Choose requirement") },
                    text = { Column(Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState())) {
                        Text("Choose the equipment requirement you want to resolve.")
                        show!!.requirements.filter { it.canFindEquipment() }.forEach { requirement ->
                            TextButton(onClick = { returnRequirement = requirement.id; selectedRequirement = requirement.id; request = listOf(requirement.id); chooseBuildRequirement = false }) { Text("${requirement.name} · ${requirement.act}") }
                        }
                    } },
                    confirmButton = { TextButton(onClick = { chooseBuildRequirement = false }) { Text("Cancel") } })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun OrganizerTopBar(show: ShowState?, onInitialize: () -> Unit, onInspect: () -> Unit) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
        title = {
            Column {
                Text(show?.title ?: "Gig Organizer", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(show?.let { "${it.venue} · ${dateLabel(it.date)}" } ?: "No show yet", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        actions = {
            if (show == null) TextButton(onClick = onInitialize) { Text("Initialize demo show") }
            TextButton(onClick = onInspect) { Icon(Icons.Outlined.Timeline, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("Inspect") }
        },
    )
}

@Composable private fun EmptyPlan(onInitialize: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        EmptyState("No show planned yet", "Initialize the demo show to plan one gig, one venue and two acts.", art = { Art.Stage(170.dp) })
        Button(onClick = onInitialize, shape = RoundedCornerShape(20.dp), contentPadding = PaddingValues(horizontal = 26.dp, vertical = 16.dp)) { Text("Initialize demo show") }
    }
}

@Composable private fun OverviewScreen(state: ShowState, hasOptions: (String) -> Boolean, onOpenRequirement: (String) -> Unit, onFindEquipment: (String) -> Unit, onOpenRiders: (String) -> Unit) {
    val conflicts = state.conflicts()
    val attention = state.requirements.filter { !it.ready }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("Needs attention", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Text("${attention.size} outstanding needs · ${state.requirements.count { it.ready }} checked and ready", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        items(attention, key = { it.id }) { requirement ->
            AssistancePanel {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(if (requirement.coverage == Coverage.Conflict) Icons.Outlined.WarningAmber else Icons.Outlined.RadioButtonUnchecked, null)
                    Column(Modifier.weight(1f)) {
                        Text(requirement.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("${requirement.act} · ${requirement.location}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${dateLabel(state.date)} · ${state.label(requirement.from)}–${state.label(requirement.until)}", style = MaterialTheme.typography.bodySmall)
                    }
                }
                Button(onClick = { if (requirement.canFindEquipment()) onFindEquipment(requirement.id) else onOpenRequirement(requirement.id) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text(if (requirement.canFindEquipment()) { if (hasOptions(requirement.id)) "View equipment options" else "Find equipment" } else "Review requirement")
                }
            }
        }
        if (state.requirements.any { it.ready }) item {
            Section("Ready") { state.requirements.filter { it.ready }.forEach { RequirementRow(it) { onOpenRequirement(it.id) } } }
        }
        item { Section("Timeline") { ShowTimeline(state, conflicts) } }
        item { Section("Departments") { } }
        items(state.requirements.map { it.department }.distinct()) { department ->
            val items = state.requirements.filter { it.department == department }
            DepartmentCard(department, items) { onOpenRiders(department) }
        }
    }
}

@Composable private fun ShowTimeline(show: ShowState, conflicts: List<Allocation>) {
    val entries = buildList {
        add(Triple("Soundcheck", show.at(16, 0), "Glass Harbour"))
        add(Triple("Soundcheck", show.at(17, 0), "Static Bloom"))
        add(Triple("Doors", show.at(19, 0), show.venue))
        add(Triple("Performance", show.at(20, 0), "Glass Harbour"))
        add(Triple("Performance", show.at(21, 15), "Static Bloom"))
        show.requirements.filter { it.name.contains("pizza") }.forEach { add(Triple("Delivery", it.until, "Dressing room")) }
    }.sortedBy { it.second }
    Panel {
        entries.forEachIndexed { index, (kind, at, who) ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(show.label(at), style = MaterialTheme.typography.titleSmall, modifier = Modifier.width(60.dp), fontWeight = FontWeight.SemiBold)
                Column(Modifier.weight(1f)) {
                    Text(who, style = MaterialTheme.typography.bodyLarge)
                    Text(kind, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (kind != "Delivery") Icon(if (kind == "Doors") Icons.Outlined.DoorFront else Icons.Outlined.MusicNote, null, tint = MaterialTheme.colorScheme.primary)
            }
            if (index != entries.lastIndex) HorizontalDivider(Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh)
        }
        if (conflicts.isNotEmpty()) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Outlined.Warning, null, tint = MaterialTheme.colorScheme.error)
                Text("Two allocations overlap: ${show.label(conflicts.first().from)}–${show.label(conflicts.first().until)}", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable private fun DepartmentCard(department: String, items: List<Requirement>, onClick: () -> Unit) {
    val settled = items.count { it.ready }
    Panel {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(department, style = MaterialTheme.typography.titleLarge)
                Text("$settled of ${items.size} checked and ready", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = onClick) { Text("Open") }
        }
        Meter(if (items.isEmpty()) 0f else settled.toFloat() / items.size)
    }
}

@Composable private fun RidersScreen(state: ShowState, selectedAct: String?, selectedDepartment: String?, onSelectDepartment: (String?) -> Unit, onSelectAct: (String?) -> Unit, onOpenRequirement: (String) -> Unit) {
    var filter by remember { mutableStateOf("All") }
    fun matches(r: Requirement) = (selectedAct == null || r.act == selectedAct) && (selectedDepartment == null || r.department == selectedDepartment) && when (filter) {
        "Missing" -> r.coverage == Coverage.Missing
        "Conflicts" -> r.coverage == Coverage.Conflict
        "Awaiting approval" -> r.commitment == Commitment.Proposed
        else -> true
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            OrganizerHeading("Riders", "Requirements and what still needs arranging.")
            if (selectedDepartment != null) TextButton(onClick = { onSelectDepartment(null) }) { Text("$selectedDepartment · Show all departments") }
        }
        item {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf("All", "Missing", "Conflicts", "Awaiting approval").forEach { Chip(it, filter == it) { filter = it } }
            }
        }
        item {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Chip("Both acts", selectedAct == null) { onSelectAct(null) }
                state.acts().forEach { Chip(it, selectedAct == it) { onSelectAct(it) } }
            }
        }
        if (state.requirements.none { matches(it) }) item { Text("No requirements match these filters.") }
        state.acts().filter { act -> state.requirements.any { it.act == act && matches(it) } }.forEach { act ->
            item { Text(act, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
            state.groups(act).filter { group -> state.requirements.any { it.act == act && it.group == group && matches(it) } }.forEach { group ->
                item {
                    RiderGroupCard(state, act, group, filter, onOpenRequirement)
                }
            }
        }
    }
}

@Composable private fun RiderGroupCard(show: ShowState, act: String, group: String, filter: String, onOpenRequirement: (String) -> Unit) {
    val requirements = show.requirements.filter { it.act == act && it.group == group }.filter {
        when (filter) {
            "Missing" -> it.coverage == Coverage.Missing
            "Conflicts" -> it.coverage == Coverage.Conflict
            "Awaiting approval" -> it.commitment == Commitment.Proposed
            else -> true
        }
    }
    if (requirements.isEmpty()) return
    var expanded by remember { mutableStateOf(true) }
    Panel {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(group, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(requirements.first().department, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            StatusPill(if (show.groupCovered(act, group)) "Complete" else "Incomplete", if (show.groupCovered(act, group)) Tone.Settled else Tone.Attention)
            IconButton(onClick = { expanded = !expanded }) { Icon(if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, if (expanded) "Collapse $group" else "Expand $group") }
        }
        AnimatedVisibility(expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                requirements.forEach { requirement ->
                    RequirementRow(requirement) { onOpenRequirement(requirement.id) }
                }
            }
        }
    }
}

@Composable private fun RequirementRow(requirement: Requirement, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
        Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(if (requirement.coverage == Coverage.Covered) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked, null,
                tint = if (requirement.coverage == Coverage.Covered) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            Column(Modifier.weight(1f)) {
                Text(if (requirement.quantity > 1) "${requirement.quantity} × ${requirement.name}" else requirement.name, style = MaterialTheme.typography.bodyLarge)
                Text(requirement.summary, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (!requirement.mandatory) StatusPill("Negotiable", Tone.Neutral)
        }
    }
}

@Composable private fun ResourcesScreen(state: ShowState, onResolve: (String, Boolean) -> Unit) {
    var department by remember { mutableStateOf("Sound") }
    val conflicts = state.conflicts()
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { OrganizerHeading("Stock and allocations", "Venue equipment, contributions and conflicts.") }
        item {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                state.requirements.map { it.department }.distinct().forEach { Chip(it, department == it) { department = it } }
            }
        }
        if (conflicts.isNotEmpty() && department == "Backline") {
            item {
                Panel {
                    Text("Allocation conflict", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.error)
                    Text("Both acts need the venue bass amplifier for overlapping preparation windows.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val grouped = conflicts.groupBy { it.resourceId }
                    grouped.forEach { (resourceId, list) ->
                        Text(state.resource(resourceId)?.name ?: resourceId, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        list.sortedBy { it.from }.forEach { allocation ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(allocation.act, style = MaterialTheme.typography.bodyMedium)
                                Text("${state.label(allocation.from)}–${state.label(allocation.until)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                    val agreed = conflicts.all { it.agreed }
                    Text(if (agreed) "Agreement recorded. The allocated windows still overlap; neither requirement is marked covered." else "Record an agreement only after both acts accept it. This does not change the allocated windows or mark equipment covered.", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = { conflicts.firstOrNull()?.let { onResolve(it.id, !agreed) } }, shape = RoundedCornerShape(18.dp)) { Text(if (agreed) "Undo agreement" else "Record agreement") }
                }
            }
        }
        items(state.resources.filter { it.department == department }) { resource ->
            val allocated = state.allocations.filter { it.resourceId == resource.id }
            Panel {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(resource.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(resource.specification, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    StatusPill("${resource.quantity} available", Tone.Neutral)
                }
                KeyValue("Source", resource.source)
                if (resource.notes.isNotEmpty()) KeyValue("Notes", resource.notes)
                if (allocated.isNotEmpty()) {
                    Text("Allocated", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    allocated.sortedBy { it.from }.forEach { allocation ->
                        KeyValue(allocation.act, "${state.label(allocation.from)}–${state.label(allocation.until)}")
                    }
                }
            }
        }
    }
}

@Composable private fun FulfilmentScreen(state: ShowState, onVerify: (String) -> Unit, onOpenRequirement: (String) -> Unit) {
    var view by remember { mutableStateOf("By type") }
    val outstanding = state.requirements.filter { it.coverage != Coverage.Covered || !it.verified }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { OrganizerHeading("Event checklist", "Check each requirement on site before marking it ready.") }
        item {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf("By type", "By person", "By deadline").forEach { Chip(it, view == it) { view = it } }
            }
        }
        if (outstanding.isEmpty()) {
            item { Panel { Text("Every requirement is checked and ready.", style = MaterialTheme.typography.titleMedium); Text("The show plan is complete.", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
        }
        val ordered = when (view) {
            "By person" -> outstanding.sortedWith(compareBy({ it.act }, { it.until }))
            "By type" -> outstanding.sortedWith(compareBy({ it.department }, { it.until }))
            else -> outstanding.sortedBy { it.until }
        }
        items(ordered, key = { it.id }) { requirement ->
            Panel {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(requirement.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(if (view == "By person") "${requirement.act} · ${requirement.group}" else "${requirement.act} · ${requirement.department}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    StatusPill(requirement.summary, if (requirement.coverage == Coverage.Conflict) Tone.Attention else Tone.Progress)
                }
                KeyValue("Required by", dateTimeLabel(requirement.until, SHOW_ZONE))
                KeyValue("Location", requirement.location)
                if (view == "By deadline") KeyValue("Group", requirement.group)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = { onVerify(requirement.id) }, shape = RoundedCornerShape(18.dp), enabled = requirement.coverage == Coverage.Covered) {
                        Text(if (requirement.verified) "Undo check" else "Record check")
                    }
                    OutlinedButton(onClick = { onOpenRequirement(requirement.id) }, shape = RoundedCornerShape(18.dp)) { Text("Open") }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun RequirementDetail(show: ShowState, requirementId: String, onBack: () -> Unit, onEdit: (Requirement) -> Unit, onVerify: (String) -> Unit, onArrange: (String) -> Unit, assistanceError: String = "", hasOptions: Boolean = false) {
    val requirement = show.requirement(requirementId) ?: return
    var editing by remember(requirementId) { mutableStateOf(false) }
    var draft by remember(requirementId, requirement) { mutableStateOf(requirement) }
    var quantityText by remember(requirementId, requirement) { mutableStateOf(requirement.quantity.toString()) }
    val validDraft = draft.name.isNotBlank() && (quantityText.toIntOrNull()?.let { it in 1..999 } == true)
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "Back") }
                Column {
                    Text(requirement.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("${requirement.act} · ${requirement.department}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        if (assistanceError.isNotEmpty()) item { Text(assistanceError, color = MaterialTheme.colorScheme.error) }
        if (editing) {
            item {
                Panel {
                    Text("Edit requirement", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    listOf("Name" to draft.name, "Quantity" to quantityText, "Specification" to draft.specification, "Location" to draft.location, "Substitutions" to draft.substitutions, "Notes" to draft.notes, "Rider reference" to draft.riderRef).forEachIndexed { index, (label, value) ->
                        OutlinedTextField(
                            value = value, onValueChange = { text ->
                                draft = when (index) {
                                    0 -> draft.copy(name = text); 1 -> { quantityText = text; draft.copy(quantity = text.toIntOrNull() ?: draft.quantity) }
                                    2 -> draft.copy(specification = text); 3 -> draft.copy(location = text)
                                    4 -> draft.copy(substitutions = text); 5 -> draft.copy(notes = text); else -> draft.copy(riderRef = text)
                                }
                            }, label = { Text(label) }, isError = if (index == 1) quantityText.toIntOrNull()?.let { it !in 1..999 } ?: true else index == 0 && draft.name.isBlank(),
                            supportingText = { if (index == 1) Text("Enter a quantity from 1 to 999.") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
                    }
                    SwitchRow("Mandatory", draft.mandatory) { draft = draft.copy(mandatory = it) }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(enabled = validDraft, onClick = { onEdit(draft.copy(verified = if (draft != requirement) false else draft.verified,
                            commitment = if (draft.specification != requirement.specification && draft.commitment == Commitment.Confirmed) Commitment.Proposed else draft.commitment)); editing = false }, shape = RoundedCornerShape(18.dp)) { Text("Save") }
                        TextButton(onClick = { draft = requirement; quantityText = requirement.quantity.toString(); editing = false }) { Text("Cancel") }
                    }
                    if (draft.specification != requirement.specification) Text("Changing the specification marks any existing commitment for review.", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
                }
            }
        } else {
            item {
                Panel {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        StatusPill(requirement.summary, if (requirement.coverage == Coverage.Covered) Tone.Settled else Tone.Attention)
                        if (!requirement.mandatory) StatusPill("Negotiable", Tone.Neutral)
                    }
                    KeyValue("Quantity", requirement.quantity.toString())
                    KeyValue("Specification", requirement.specification)
                    KeyValue("Location", requirement.location)
                    KeyValue("Required from", "${show.day(requirement.from)} ${show.label(requirement.from)}")
                    KeyValue("Required until", "${show.day(requirement.until)} ${show.label(requirement.until)}")
                    if (requirement.substitutions.isNotEmpty()) KeyValue("Permitted substitutions", requirement.substitutions)
                    if (requirement.supply.isNotEmpty()) KeyValue("Current supply", requirement.supply)
                    if (requirement.riderRef.isNotEmpty()) KeyValue("Rider", requirement.riderRef)
                    if (requirement.notes.isNotEmpty()) Text(requirement.notes, style = MaterialTheme.typography.bodyMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = { editing = true }, shape = RoundedCornerShape(18.dp)) { Text("Edit") }
                        if (requirement.canFindEquipment()) Button(onClick = { onArrange(requirement.id) }, shape = AssistanceDesign.shape) { Text(if (hasOptions) "View equipment options" else "Find equipment") }
                        if (requirement.coverage == Coverage.Covered) TextButton(onClick = { onVerify(requirement.id) }) { Text(if (requirement.verified) "Undo check" else "Record check") }
                    }
                }
            }
        }
        item { Section("Dependencies") { } }
        val dependencies = show.requirements.filter { it.act == requirement.act && it.group == requirement.group && it.id != requirement.id }
        items(dependencies) { dependency ->
            Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
                Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(dependency.name, style = MaterialTheme.typography.bodyLarge)
                    StatusPill(dependency.summary, if (dependency.coverage == Coverage.Covered) Tone.Settled else Tone.Attention)
                }
            }
        }
    }
}

@Composable private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
