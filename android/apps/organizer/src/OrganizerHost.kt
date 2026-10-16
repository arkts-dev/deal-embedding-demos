package dev.deal.apps.organizer

import android.content.Context
import dev.deal.embedding.*
import dev.deal.embedding.capabilities.*

/** Native bootstrap and local authority only; orchestration runs through compiled DEAL. */
class OrganizerHost(private val context: Context) : AutoCloseable {
    private val discovery=CapabilityDiscovery(context){uid->context.packageManager.checkSignatures(uid,context.applicationInfo.uid)==android.content.pm.PackageManager.SIGNATURE_MATCH}
    val registry:CapabilityRegistry by lazy {CapabilityRegistry(context,discovery,listOf(organizerCapabilities(context){registry.invokingExperience()}))}
    private val mechanisms=Embedding(context,registry,GenerationBuild.gateway(),EmbeddingConfig("experience",emptyList(),"organizer-protocol",policyAssets="organizer-policy"))
    val embedding=ApplicationProtocol(context,mechanisms)
    fun prepare(){embedding.refresh().forEach{embedding.grant(it.module)}}
    fun forget(experience:Experience)=embedding.forget(experience)
    override fun close(){embedding.shutdown()}
}
