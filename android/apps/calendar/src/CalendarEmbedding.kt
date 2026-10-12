package dev.deal.apps.calendar
import android.content.Context
import dev.deal.embedding.*
import dev.deal.embedding.capabilities.*
import dev.deal.apps.development.GenerationGateway

class CalendarEmbedding(context:Context,start:()->Int,propose:(Int)->Unit):AutoCloseable {
    val registry=CapabilityRegistry(context,CapabilityDiscovery(context){uid->context.packageManager.checkSignatures(uid,context.applicationInfo.uid)==android.content.pm.PackageManager.SIGNATURE_MATCH},listOf(calendarCapabilities(start,propose)))
    val embedding=Embedding(context,registry,GenerationGateway("http://127.0.0.1:8787/generate"),EmbeddingConfig("departure",emptyList(),"calendar-protocol"))
    var session:Session?=null
    fun prepare(context:Context){embedding.refresh()}
    fun remembered():Source?=embedding.saved().lastOrNull()?.let{embedding.source(it)}
    fun activate(source:Source):org.json.JSONObject {val e=embedding.load("Departure",source);val previous=session;val next=embedding.run(e);session=next;previous?.let{embedding.close(it)};return embedding.poll(next).json()}
    fun dispatch(slot:Int,payload:String?)=session?.let{embedding.dispatch(it,slot,payload).json()}
    fun poll()=session?.let{embedding.poll(it).json()}
    override fun close(){embedding.shutdown()}
}
