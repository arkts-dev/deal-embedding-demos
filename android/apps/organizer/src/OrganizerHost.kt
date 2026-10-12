package dev.deal.apps.organizer

import android.content.Context
import dev.deal.apps.development.GenerationGateway
import dev.deal.embedding.*
import dev.deal.embedding.capabilities.*

/** Native bootstrap and local authority only; equipment policy is app-linked DEAL. */
class OrganizerHost(private val context: Context) : AutoCloseable {
    private var invokingExperience: String? = null
    private val discovery=CapabilityDiscovery(context){uid->context.packageManager.checkSignatures(uid,context.applicationInfo.uid)==android.content.pm.PackageManager.SIGNATURE_MATCH}
    val registry=CapabilityRegistry(context,discovery,listOf(organizerCapabilities(context){invokingExperience}))
    val embedding=Embedding(context,registry,GenerationGateway("http://127.0.0.1:8787/generate"),EmbeddingConfig("experience",emptyList(),"organizer-protocol",policyAssets="organizer-policy"))
    private val live=linkedMapOf<String,Session>()
    fun prepare(){embedding.refresh().forEach{embedding.grant(it.module)}}
    fun sessions():List<Pair<Experience,Session>> = live.map{(id,session)->embedding.experience(session) to session}
    fun run(experience:Experience):Session=embedding.run(experience).also{live[experience.id]=it}
    fun forget(experience:Experience){embedding.forget(experience);live.remove(experience.id)}
    fun <T> inSession(session:Session,block:()->T):T {val previous=invokingExperience;invokingExperience=embedding.experience(session).id;try{return block()}finally{invokingExperience=previous}}
    override fun close(){embedding.shutdown()}
}
