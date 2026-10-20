package dev.deal.connectors.tests

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.deal.embedding.*
import dev.deal.embedding.capabilities.*
import java.util.concurrent.Executors

/** Delayed external fixture keeps pending frames observable across the real Binder boundary. */
class ToolkitReadingService:CapabilityService() {
    override val capabilities=NativeCapabilities(CapabilityContract("host/sample","Reading",listOf(CapabilityFunction("value","Read",emptyList(),CapabilityType.IntType))),mapOf("value" to { _ ->
        Thread.sleep(2500)
        if(java.io.File(filesDir,"toolkit-reading-failure").readText()=="true") throw ProtocolFailure("UNAVAILABLE","Reading unavailable")
        42
    }))
    override fun authorize(uid:Int)=uid==applicationInfo.uid
    override fun consent()=true
}

/** Test-only real compiler/runtime/surface gallery. No app-owned session driving. */
class ToolkitActivity : ComponentActivity() {
    private val worker=Executors.newSingleThreadExecutor()
    private var api:ApplicationProtocol?=null
    private var surface:ExperienceSurface?=null
    private var experience:Experience?=null
    private var retained=false
    override fun onCreate(state:Bundle?) {
        super.onCreate(state)
        androidx.core.view.WindowCompat.getInsetsController(window,window.decorView).isAppearanceLightStatusBars = resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK != android.content.res.Configuration.UI_MODE_NIGHT_YES
        val example=intent.getStringExtra("example") ?: "comparison"
        retained=intent.getBooleanExtra("retain",false)
        var owner by mutableStateOf<ExperienceSurface?>(null)
        var problem by mutableStateOf("")
        var inspect by mutableStateOf(false)
        var evidence by mutableStateOf("")
        setContent {
            MaterialTheme(colorScheme=if(isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()) {
                Surface(Modifier.fillMaxSize()) {
                    Column(Modifier.statusBarsPadding().navigationBarsPadding().padding(16.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(16.dp)) {
                        TextButton(onClick={
                            inspect=!inspect
                            if(inspect) worker.execute {
                                val p=api; val e=experience
                                val text=if(p!=null && e!=null) "Saved source: ${p.source(e)==readSource(example)}\nObservation: ${p.health()}" else "Opening"
                                runOnUiThread { evidence=text }
                            }
                        }) { Text(if(inspect) "Back" else "Inspect") }
                        if(inspect) Text(evidence) else {
                            Text(if(example=="comparison") "Budget" else if(example=="values") "Hire options" else "Reading",style=MaterialTheme.typography.headlineSmall)
                            if(problem.isNotEmpty()) Text("Unable to open",color=MaterialTheme.colorScheme.error)
                            owner?.let { ExperienceSurface(it,onSnapshot={snapshot ->
                                val nodes=ToolkitNodes.all(snapshot.tree)
                                if(nodes.any { it.optString("component").endsWith(".Progress") }) {
                                    java.io.File(filesDir,"toolkit-pending.json").writeText(snapshot.tree.toString())
                                }
                            }) }
                        }
                    }
                }
            }
        }
        worker.execute {
            try {
                java.io.File(filesDir,"toolkit-reading-failure").writeText(intent.getBooleanExtra("fail",false).toString())
                if(example=="generation-example") java.io.File(filesDir,"toolkit-pending.json").delete()
                val model=object:ModelClient {override fun complete(input:String,previous:String,diagnostics:String,cancellation:GenerationCancellation,log:EmbeddingLog):String=error("No inference")}
                val p=ApplicationProtocol(this,Embedding(this,CapabilityRegistry(this,CapabilityDiscovery(this){it==applicationInfo.uid},emptyList()),model,
                    EmbeddingConfig("experience",emptyList(),"toolkit-gallery-$example-${java.security.MessageDigest.getInstance("SHA-256").digest(readSource(example).toString().toByteArray()).take(6).joinToString("") { "%02x".format(it) }}",retainState=true)))
                api=p
                if(example=="generation-example") { p.refresh();p.grant("host/sample") }
                val e=if(retained) p.saved().firstOrNull() ?: p.load(example,readSource(example)) else p.load(example,readSource(example))
                experience=e
                val s=ExperienceSurface(p,e); surface=s
                s.activate { runOnUiThread { problem=it.toString() } }
                runOnUiThread { owner=s }
            } catch(error:Throwable) { runOnUiThread { problem=error.toString() };android.util.Log.e("ToolkitGallery","Open failed",error) }
        }
    }
    private fun readSource(name:String)=Source(assets.open("ui/$name.deal").bufferedReader().use {it.readText()},assets.open("ui/$name.dealui").bufferedReader().use {it.readText()})
    override fun onDestroy() {
        worker.execute {
            try { surface?.close() } finally { try { if(!retained) experience?.let {api?.forget(it)} } finally {api?.close()} }
        }
        worker.shutdown()
        super.onDestroy()
    }
}
