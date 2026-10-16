package dev.deal.apps.calendar

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.unit.dp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import dev.deal.shell.*
import dev.deal.embedding.*
import org.json.JSONObject
import java.io.File
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
class CalendarActivity : ComponentActivity() {
    private val worker = Executors.newSingleThreadExecutor()
    private lateinit var experience: CalendarEmbedding
    private var surface by mutableStateOf<ExperienceSurface?>(null)
    @Volatile private var ownedSurface:ExperienceSurface?=null
    private var status by mutableStateOf("Preparing your experience…")
    private var events by mutableStateOf<List<CalendarEvent>>(emptyList())
    @Volatile private var selected: CalendarEvent? = null
    private var selectedUi by mutableStateOf<CalendarEvent?>(null)
    private var dialog by mutableStateOf(false)
    private var permission by mutableStateOf(false)
    private var connected by mutableStateOf(false)
    private var ready by mutableStateOf(false)
    private var proposal by mutableStateOf<Int?>(null)
    private var proposalEvent: CalendarEvent? = null
    @Volatile private var observedEvent: CalendarEvent? = null
    private var saved by mutableStateOf("")
    private var generationIntent by mutableStateOf("")
    private var discloseEvent by mutableStateOf(false)
    private var generating by mutableStateOf(false)
    private var generationStatus by mutableStateOf("")
    private var inspecting by mutableStateOf(false)
    @Volatile private var cancellation: GenerationRequest? = null
    @Volatile private var cancelRequested=false
    private fun cancelGeneration(){cancelRequested=true;if(::experience.isInitialized)cancellation?.let{experience.embedding.cancel(it)}}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        worker.execute {
            try{
                experience = CalendarEmbedding(applicationContext, {
                    val event = selected ?: error("Choose an event first")
                    observedEvent = event
                    event.startMinute
                }, { minute ->
                    val origin = observedEvent
                    check(origin != null && origin == selected) { "Event changed. Recalculate before saving." }
                    runOnUiThread { if (origin == selected) { proposalEvent = origin; proposal = minute } }
                })
            }catch(error:Throwable){showError(error)}
        }
        refreshEvents()
        setContent {
            AppTheme(Accent.Calendar) {
                Scaffold(topBar = {
                    TopAppBar(title = { Text(if (inspecting) "Inspect" else "Departure plan") },
                        navigationIcon = { TextButton(onClick = { if (inspecting) inspecting = false else finish() }) { Text("Back") } },
                        actions = { TextButton(onClick = { inspecting = !inspecting }) { Text(if (inspecting) "Done" else "Inspect") } })
                }) { padding ->
                  Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Panel {
                        Text(selectedUi?.title ?: "Choose an event", style = MaterialTheme.typography.titleLarge)
                        selectedUi?.let { Text(it.time, style = MaterialTheme.typography.bodyMedium) }
                        if (!permission) Button(onClick = { requestPermissions(arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR), 1) }) { Text("Connect your calendar") }
                        else if (events.isEmpty()) Text("No upcoming events. Add one in your calendar app to get started.")
                        for (event in if (selectedUi == null) events else emptyList()) {
                            OutlinedButton(onClick = { selected = event; selectedUi = event; proposal = null }, modifier = Modifier.fillMaxWidth()) {
                                Text("${event.time}  ${event.title}")
                            }
                        }
                    }
                    if (inspecting) Panel {
                        Text("Development generation", style = MaterialTheme.typography.titleLarge)
                        OutlinedTextField(generationIntent, { generationIntent = it.take(4096) }, label = { Text("What would you like to do?") }, modifier = Modifier.fillMaxWidth(), minLines = 2, maxLines = 4, enabled = !generating)
                        Row { Checkbox(discloseEvent, { discloseEvent = it }, enabled = !generating); Text("Share selected event with the model") }
                        Text("Development inference uses a remote model. Capability descriptions are shared; provider data is not read during generation.")
                        if (generating) Button(onClick = { cancelGeneration(); generationStatus = "Cancelling generation…" }) { Text("Cancel generation") }
                        else Button(onClick = { generate() }, enabled = ready && generationIntent.isNotBlank()) { Text("Generate experience") }
                        if (generationStatus.isNotEmpty()) Text(generationStatus)
                    }
                    EmbeddedExperience {
                        surface?.let { current->ExperienceSurface(current,onAction={proposal=null},onSnapshot={snapshot->
                            DiagnosticFiles.write(filesDir,"ui-snapshot.json",snapshot.json().toString())
                            if(snapshot.fault.isNotEmpty())status=snapshot.fault
                        }) } ?: Text(status)
                    }
                    Panel {
                        Text("Your connected apps", style = MaterialTheme.typography.titleLarge)
                        Text("Enable discovered capabilities here, then approve access in each provider. Consent stays under native control.")
                        Switch(connected, { enabled ->
                            try{experience.permissions(enabled);connected=enabled;proposal=null;dialog=false}
                            catch(error:Throwable){showError(error)}
                        },enabled=ready)
                    }
                    proposal?.let { departure ->
                        Panel {
                            Text("Keep this plan", style = MaterialTheme.typography.titleLarge)
                            Text("A reminder at %02d:%02d. Nothing changes until you confirm.".format(departure / 60, departure % 60))
                            Button(onClick = { dialog = true }) { Text("Review Calendar change") }
                        }
                    }
                    if (saved.isNotEmpty()) Text(saved)
                    if (surface != null && status.isNotEmpty()) Text(status, color = MaterialTheme.colorScheme.error)
                  }
                }
                if (dialog) AlertDialog(onDismissRequest = { dialog = false }, title = { Text("Confirm Calendar change") }, text = { Text("Add a departure reminder to ${proposalEvent?.title}? Existing reminders are kept.") }, confirmButton = {
                    TextButton(onClick = {
                        dialog = false
                        val event = proposalEvent; val departure = proposal
                        if (event != null && event == selected && departure != null) {
                            try { CalendarEvents(this).confirmReminder(event, departure); saved = "Departure reminder saved"; proposal = null }
                            catch (error: Throwable) { saved = error.message ?: "Could not save reminder" }
                        } else saved = "Event changed. Recalculate before saving."
                    }) { Text("Confirm write") }
                }, dismissButton = { TextButton(onClick = { dialog = false }) { Text("Keep unchanged") } })
            }
        }
        activate(intent.getStringExtra("deal"), intent.getStringExtra("dealui"))

    }
    private fun refreshEvents() {
        permission = checkSelfPermission(Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED
        if (permission) try {
            val requested = intent.getLongExtra("eventId", -1)
            val begin = intent.getLongExtra("eventBegin", -1)
            events = if (requested != -1L && begin >= 0) CalendarEvents(this).upcoming(begin, begin + 1) else CalendarEvents(this).upcoming()
            val refreshed = if (requested != -1L) events.firstOrNull { it.id == requested && it.begin == begin }
                else selected?.let { old -> events.firstOrNull { it.id == old.id && it.begin == old.begin } } ?: events.firstOrNull()
            if (refreshed != selected) { selected = refreshed; selectedUi = refreshed; proposal = null }
        } catch (error: Throwable) { status = error.message ?: "Calendar unavailable" }
    }
    override fun onResume() { super.onResume(); refreshEvents() }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) { super.onRequestPermissionsResult(requestCode, permissions, grantResults); refreshEvents() }
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); setIntent(intent); refreshEvents(); if (intent.hasExtra("deal") || intent.hasExtra("dealui")) activate(intent.getStringExtra("deal"), intent.getStringExtra("dealui")) }
    private fun activate(deal: String?, ui: String?) {
        worker.execute {
            try {
                experience.prepare()
                if (connected) experience.permissions(true)
                require((deal==null)==(ui==null)){"Supply both .deal and .dealui"}
                val remembered=if(deal==null)experience.remembered()else null
                if(remembered!=null)activate(remembered)else{
                    val source=deal ?: assets.open("experience/departure.deal").bufferedReader().use{it.readText()}
                    val view=ui ?: assets.open("experience/departure.dealui").bufferedReader().use{it.readText()}
                    val prepared=experience.embedding.load("Departure",Source(source,view))
                    experience.embedding.withPreparation(prepared){activate(it)}
                }
                runOnUiThread{ready=true}
                File(filesDir, "ui-error.txt").delete()
                runOnUiThread { proposal = null; status = "" }
            } catch (error: Throwable) { showError(error) }
        }
    }
    private fun generate() {
        val objective = generationIntent
        val origin = if (discloseEvent) selected else null
        val context = origin?.let { JSONObject().put("eventTitle", it.title).put("startMinute", it.startMinute).toString() } ?: "{}"
        generating = true; cancelRequested=false; generationStatus = "Discovering capabilities…"
        DiagnosticFiles.write(filesDir,"generation-status.json",JSONObject().put("status", "running").toString())
        worker.execute {
            try {
                experience.prepare()
                val schema=WireType("record",fields=listOf(WireField("eventTitle",WireType("string",maximum=1024),true),WireField("startMinute",WireType("int"),true)))
                val facts=experience.embedding.snapshot(schema,context)
                val request=experience.embedding.request("Departure",objective,facts);cancellation=request
                if(cancelRequested)experience.embedding.cancel(request)
                val prepared=experience.embedding.generate(request)
                experience.embedding.withPreparation(prepared){
                    if(cancelRequested)throw java.util.concurrent.CancellationException("Cancelled")
                    check(origin == null || origin == selected) { "Selected event changed; regenerate" }
                    activate(it)
                }
                DiagnosticFiles.write(filesDir,"generation-status.json",JSONObject().put("status","activated").put("attempts",prepared.attempts).toString())
                runOnUiThread{proposal=null;status="";generationStatus="Generated experience activated"}
            } catch (error: Throwable) {
                DiagnosticFiles.write(filesDir,"generation-status.json",JSONObject().put("status", if (error is java.util.concurrent.CancellationException || error is ProtocolFailure && error.code=="CANCELLED") "cancelled" else "failed").put("message", error.message?.take(1000)).toString())
                runOnUiThread { generationStatus = if (error is java.util.concurrent.CancellationException || error is ProtocolFailure && error.code=="CANCELLED") "Generation cancelled; current experience retained" else "Generation failed; current experience retained" }
            } finally { cancellation = null; runOnUiThread { generating = false } }
        }
    }
    private fun activate(prepared:Experience) {
        val next=ExperienceSurface(experience.embedding,prepared)
        try{next.activate(::showError)}catch(error:Throwable){runCatching{next.close()}.onFailure{error.addSuppressed(it)};throw error}
        val previous=ownedSurface;ownedSurface=next
        runOnUiThread {if(!isDestroyed&&ownedSurface===next)surface=next}
        // Calendar explicitly replaces/closes its prior experience after successful activation.
        try{previous?.close()}catch(error:Throwable){android.util.Log.e("Calendar","Previous experience cleanup failed after replacement",error)}
        if(isDestroyed)next.close()
    }
    private fun showError(error: Throwable) {
        android.util.Log.e("DealUi", "Experience error", error)
        DiagnosticFiles.write(filesDir,"ui-error.txt",android.util.Log.getStackTraceString(error))
        runOnUiThread { if (!isDestroyed) status = error.message ?: "Experience unavailable" }
    }
    override fun onDestroy() {
        try{cancelGeneration()}catch(error:Throwable){android.util.Log.e("Calendar","Cancellation failed during destruction",error)}
        worker.execute {
            try{teardown({ownedSurface?.close()},{if(::experience.isInitialized)experience.close()})}
            catch(error:Throwable){android.util.Log.e("Calendar","Shutdown failed",error)}
            finally{ownedSurface=null}
        }
        worker.shutdown(); super.onDestroy()
    }
}
