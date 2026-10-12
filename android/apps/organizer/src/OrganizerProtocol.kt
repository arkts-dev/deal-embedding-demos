package dev.deal.apps.organizer
import android.content.Context
/** One bootstrap namespace per application process, shared by shell and Inspect. */
object OrganizerProtocol {
    @Volatile private var instance:OrganizerHost?=null
    fun host(context:Context):OrganizerHost=instance ?: synchronized(this){instance ?: OrganizerHost(context.applicationContext).also{instance=it}}
    fun shutdown(){synchronized(this){instance?.close();instance=null}}
}
