package dev.deal.connectors.tests

import android.app.Instrumentation
import android.os.Bundle
import android.content.ContentValues
import android.content.ContentUris
import android.provider.CalendarContract as C
import java.io.File

/** Controlled Calendar test event only; never writes an existing personal event. */
class CalendarReminderInstrumentation:Instrumentation(){
 private var mode="setup"
 override fun onCreate(arguments:Bundle?){super.onCreate(arguments);mode=arguments?.getString("mode")?:"setup";start()}
 override fun onStart(){val result=Bundle();val ctx=targetContext;val receipt=File(ctx.cacheDir,"departure-test-event")
 try{
  val resolver=ctx.contentResolver
  if(mode=="setup"){
   val calendar=resolver.query(C.Calendars.CONTENT_URI,arrayOf(C.Calendars._ID),"calendar_access_level>=?",arrayOf(C.Calendars.CAL_ACCESS_CONTRIBUTOR.toString()),null)!!.use{check(it.moveToFirst());it.getLong(0)}
   val begin=java.time.ZonedDateTime.now().plusHours(2).withSecond(0).withNano(0)
   val values=ContentValues().apply{put(C.Events.CALENDAR_ID,calendar);put(C.Events.TITLE,"DEAL controlled departure test");put(C.Events.DTSTART,begin.toInstant().toEpochMilli());put(C.Events.DTEND,begin.plusHours(1).toInstant().toEpochMilli());put(C.Events.EVENT_TIMEZONE,begin.zone.id)}
   val uri=resolver.insert(C.Events.CONTENT_URI,values)!!;receipt.writeText(ContentUris.parseId(uri).toString())
   // Provider may add default reminders asynchronously; retain them as the unchanged baseline.
   Thread.sleep(3000)
   resolver.query(C.Reminders.CONTENT_URI,arrayOf(C.Reminders.MINUTES),"event_id=?",arrayOf(receipt.readText()),null)!!.use{cursor->
    val baseline=mutableListOf<Int>();while(cursor.moveToNext())baseline.add(cursor.getInt(0))
    File(ctx.cacheDir,"departure-test-reminders").writeText(baseline.sorted().joinToString(","))
   }
   result.putString("event",receipt.readText())
  }else{
   val id=receipt.readText().trim()
   if(mode=="cleanup"){resolver.delete(ContentUris.withAppendedId(C.Events.CONTENT_URI,id.toLong()),null,null);receipt.delete()}
   else resolver.query(C.Reminders.CONTENT_URI,arrayOf(C.Reminders.MINUTES),"event_id=?",arrayOf(id),null)!!.use{
    val minutes=mutableListOf<Int>();while(it.moveToNext())minutes.add(it.getInt(0))
    val baseline=File(ctx.cacheDir,"departure-test-reminders").readText().split(',').filter{it.isNotEmpty()}.map{it.toInt()}
    if(mode=="unchanged")check(minutes.sorted()==baseline.sorted()){minutes.toString()}
    else check(minutes.sorted()==(baseline+43).sorted()){"Expected existing reminders plus 43-minute reminder after edits: $minutes"}
    result.putString("reminders",minutes.toString())
   }
  }
  finish(0,result)
 }catch(error:Throwable){result.putString("failure",android.util.Log.getStackTraceString(error));finish(1,result)}
 }
}
