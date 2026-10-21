package dev.deal.apps.organizer

import android.content.Context
import dev.deal.embedding.*
import dev.deal.embedding.capabilities.*

/** Native bootstrap and local authority only; orchestration runs through compiled DEAL. */
class OrganizerHost(private val context: Context) : AutoCloseable {
    private val discovery=CapabilityDiscovery(context){uid->uid != context.applicationInfo.uid && dev.deal.shell.Connections.trusted(context,uid)}
    val registry:CapabilityRegistry by lazy {CapabilityRegistry(context,discovery,listOf(organizerCapabilities(context){registry.invokingExperience()}))}
    private val mechanisms=Embedding(context,registry,GenerationBuild.gateway(),EmbeddingConfig("experience",emptyList(),"organizer-protocol",repairStrategy=dev.deal.shell.AssistanceBuild.repairStrategy,retainState=true,policyAssets="organizer-policy"))
    val embedding=ApplicationProtocol(context,mechanisms)
    override fun close(){embedding.close()}
}
