package dev.deal.apps.calendar
import android.content.Context
import dev.deal.embedding.*
import dev.deal.embedding.capabilities.*
import dev.deal.apps.development.GenerationGateway

class CalendarEmbedding(context:Context,start:()->Int,propose:(Int)->Unit):AutoCloseable {
    private val registry=CapabilityRegistry(context,CapabilityDiscovery(context){uid->context.packageManager.checkSignatures(uid,context.applicationInfo.uid)==android.content.pm.PackageManager.SIGNATURE_MATCH},listOf(calendarCapabilities(start,propose)))
    private val mechanisms=Embedding(context,registry,GenerationGateway("http://127.0.0.1:8787/generate"),EmbeddingConfig("departure",emptyList(),"calendar-protocol"))
    val embedding=ApplicationProtocol(context,mechanisms)
    var session:Session?=null
    fun prepare(context:Context){embedding.refresh()}
    fun permissions(enabled:Boolean){embedding.refresh().forEach{if(enabled)embedding.grant(it.module)else embedding.revoke(it.module)}}
    fun remembered():Source?=embedding.saved().lastOrNull()?.let{embedding.source(it)}
    fun activate(source:Source):Session {val e=embedding.load("Departure",source);val previous=session;val next=embedding.run(e);session=next;previous?.let{embedding.close(it)};return next}
    fun dispatch(slot:Int,payload:String?){session?.let{embedding.dispatch(it,slot,payload)}}
    override fun close(){embedding.shutdown()}
}
