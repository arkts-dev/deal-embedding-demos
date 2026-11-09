package dev.deal.connectors.tests

import android.app.Instrumentation
import android.os.Bundle
import android.content.Context
import android.content.ContextWrapper
import dev.deal.embedding.*
import dev.deal.embedding.capabilities.*
import dev.deal.apps.todo.*
import org.json.*
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

/** Test-only asynchronous Binder transport; all task results come from Todo. */
class CatalogueReaderService : android.app.Service() {
 companion object {
  lateinit var provider: NativeCapabilities
  val reads=AtomicInteger()
  @Volatile var fail=false
  @Volatile var gate:CountDownLatch?=null
  @Volatile var entered:CountDownLatch?=null
  @Volatile var replied:CountDownLatch?=null
 }
 private val workers=Executors.newCachedThreadPool()
 private val binder=object:ICapabilityService.Stub(){
  private fun caller(){check(android.os.Binder.getCallingUid()==applicationInfo.uid)}
  override fun describe():String{caller();return provider.contract.json().toString()}
  override fun invoke(id:String,name:String,arguments:String,callback:ICapabilityResult){
   caller();check(name=="tasks");val args=JSONArray(arguments);reads.incrementAndGet()
   val hold=if(args.getString(0)=="Alpha")gate else null;val signal=replied
   val result=if(fail)null else provider.invoke(name,args);entered?.countDown()
   workers.execute{try{hold?.let{check(it.await(10,TimeUnit.SECONDS))};if(result==null)callback.failure(id,"PROVIDER_ERROR","Task read unavailable") else callback.success(id,JSONArray().put(result).toString())}finally{if(hold!=null)signal?.countDown()}}
  }
  override fun cancel(id:String){caller() /* deliver late to verify session discard */}
 }
 override fun onBind(intent:android.content.Intent):android.os.IBinder=binder
 override fun onDestroy(){workers.shutdownNow();super.onDestroy()}
}

/** Public generation: unmodified pinned checklist + list_summary, actual Todo read handler. */
class CatalogueReaderInstrumentation : Instrumentation() {
 private var partial=false
 override fun onCreate(arguments:Bundle?){super.onCreate(arguments);partial=arguments?.getString("partial")=="true";start()}
 override fun onStart(){val receipt=Bundle();val prefix="reader-${UUID.randomUUID()}-";val files=mutableSetOf<String>()
 val isolated=object:ContextWrapper(targetContext){override fun getSharedPreferences(name:String,mode:Int):android.content.SharedPreferences {files+=prefix+name;return super.getSharedPreferences(prefix+name,mode)}}
 var phase="prepare"
 try{
  TaskStore(isolated).save(listOf(Task("one","Pack cables","Fixture",0,show="Alpha",checklist=listOf(ChecklistItem("Inspect",true)),complete=true),Task("two","Pack stand","Fixture",0,show="Alpha"),Task("three","Load cases","Fixture",0,show="Beta")))
  CatalogueReaderService.provider=todoCapabilities(isolated);CatalogueReaderService.reads.set(0);CatalogueReaderService.fail=false
  fun reads()=CatalogueReaderService.reads.get()
  var code=0;var contents=0;var readerSelected=false;val thinking=mutableListOf<Boolean>()
  val model=object:ModelClient{override fun complete(input:String,previous:String,diagnostics:String,cancellation:GenerationCancellation,log:EmbeddingLog):String{
   cancellation.check()
   if(input.contains("\nPORTABLE COMPLETION\n"))return when(JSONObject(input.substringAfterLast("\nPORTABLE COMPLETION\n")).getString("operation")){
    "plan"->"""{"title":"Task checks","needs":[{"type":"content","what":"Three fixture checklist items to check off"},{"type":"other","what":"Show done total and left of this checklist"},{"type":"other","what":"Read Todo tasks for editable show Alpha and replace checklist with actual tasks and completion; read-only provider, keep prior list on failure and retry"}]}"""
    "content"->{contents++;"""{"section1_items":[["First",""],["Second",""],["Third",""]]}"""}
    "code"->{code++;thinking.add(JSONObject(input.substringAfterLast("\nPORTABLE COMPLETION\n")).getBoolean("thinking"));if(partial){return "<classes></classes><fields></fields><functions>function c_bad( {</functions><view></view>"};check(code==1){"Unexpected repair: "+input.substringAfterLast("It failed:").take(5000)}
     val field=Regex("items[0-9]+: Item\\[\\]").find(input)!!.value.substringBefore(':')
     """<classes></classes><fields>c_show: string = "Alpha";
c_note: string = "Read tasks";</fields><functions>
function c_read(state: AppState, value: number): AppState {
 sdk.call("host/todo.tasks", ["show", json.stringify(state.c_show) as string], "c_answer");
 return {c_note: "Reading" __PASSTHROUGH__};
}
function c_show(state: AppState, value: string): AppState {
 sdk.call("host/todo.tasks", ["show", json.stringify(value) as string], "c_answer");
 return {c_show: value, c_note: "Reading" __PASSTHROUGH__};
}
function c_answer(state: AppState, value: string): AppState {
 if (strings.contains(value, "error:")) { return {c_note: "Read failed; retry" __PASSTHROUGH__}; }
 let rows: unknown[] = json.parse("{\"rows\":" + value + "}").rows as unknown[];
 let items: Item[] = [];
 for (let raw of rows) { let row: Record<string, unknown> = raw as Record<string, unknown>; items.push({label: row.title as string, detail: "", done: row.complete as boolean}); }
 return {$field: items, c_note: "Loaded" __PASSTHROUGH__};
}
</functions><view>
sections.push(ui.Section("content", "Tasks", "", [ui.TextField("default", "Show", state.c_show, "c_show"),ui.Button("default", "Read tasks", "c_read", 0, true),ui.Text("default", state.c_note)]));
</view>"""}
    else->error("Unexpected model completion")
   }
   val questions=JSONObject(input.substringAfterLast("\nPORTABLE CHOICE\n")).getJSONObject("questions");val answers=JSONObject()
   questions.keys().forEach{key->val q=questions.getJSONObject(key);if(q.getString("type")=="choice"){
    val criteria=q.getJSONObject("criteria");val choice=when{
     key=="ready.0"->"checklist"
     key=="whose.0"->"written"
     key.startsWith("need.")&&criteria.has("list_summary")&&q.toString().contains("Show done total")-> {readerSelected=true;"list_summary"}
     else->listOf("none","written","no","false","default").firstOrNull{criteria.has(it)}
    };if(choice!=null)answers.put(key,JSONObject().put("choice",choice).put("confidence",1))
   }}
   return JSONObject().put("answers",answers).toString()
  }}
  val config=RuntimeConfig("experience",emptyList(),"catalogue-reader-${UUID.randomUUID()}",retainState=true)
  CapabilityRegistry(targetContext,CapabilityDiscovery(targetContext){it==targetContext.applicationInfo.uid},emptyList()).use{registry->ApplicationProtocol(targetContext,Embedding(targetContext,registry,model,config)).use{api->
   api.refresh();api.grant("host/todo")
   phase="generate";val e=api.generate(api.request("Task checks","Checklist with done total and left; read actual Todo tasks for editable show Alpha. Preserve list if read fails; retry. Never change provider tasks.",api.snapshot(WireType("record"),"{}")))
   var session:Session?=null
   try{check(readerSelected&&code==(if(partial)3 else 1)&&contents==1&&reads()==0){"reader=$readerSelected code=$code content=$contents reads=${reads()}"};val source=api.source(e)
    phase="run";session=api.run(e);var update=api.next(session!!,"");phase="actions"
    fun nodes(n:JSONObject):List<JSONObject> = listOf(n)+(0 until n.getJSONArray("children").length()).flatMap{nodes(n.getJSONArray("children").getJSONObject(it))}
    fun prop(n:JSONObject,k:String):JSONObject {val ps=n.getJSONArray("props");return(0 until ps.length()).map{ps.getJSONObject(it)}.single{it.getString("name")==k}}
    fun metrics()=nodes(update.snapshot.tree).filter{it.getString("component")=="ui.Metric"}.map{prop(it,"value").getString("stringValue").toDouble()}
    fun note(text:String)=nodes(update.snapshot.tree).any{it.getString("component")=="ui.Text"&&prop(it,"value").getString("stringValue")==text}
    fun dispatch(label:String,payload:String?=null):Int{val node=nodes(update.snapshot.tree).single{it.getString("component") in listOf("ui.Button","ui.TextField")&&prop(it,if(it.getString("component")=="ui.Button")"text" else "label").getString("stringValue")==label};val version=update.snapshot.version;api.dispatch(session!!,prop(node,if(node.getString("component")=="ui.Button")"onClick" else "onChange").getInt("actionSlot"),payload);return version}
    fun await(version:Int,ready:()->Boolean){val until=System.nanoTime()+TimeUnit.SECONDS.toNanos(25);do{check(System.nanoTime()<until){update.snapshot.tree.toString()};update=api.next(session!!,update.cursor);check(update.snapshot.fault.isEmpty()){update.snapshot.fault}}while(update.snapshot.version<=version||!ready())}
    check(metrics()==listOf(0.0,3.0,3.0)){metrics().toString()}
    if(partial){
     check(thinking==listOf(false,false,true)){thinking.toString()}
     check(nodes(update.snapshot.tree).any{it.getString("component")=="ui.Notice"})
     val version=dispatch("First");await(version){metrics()==listOf(1.0,3.0,2.0)}
     check(api.source(e)==source&&reads()==0);api.close(session!!);session=null
     val loaded=api.load("Partial reader reload",source);try{check(api.source(loaded)==source);session=api.run(loaded);update=api.next(session!!,"");check(metrics()==listOf(0.0,3.0,3.0));api.close(session!!);session=null}finally{api.forget(loaded)}
    }else{
    var version=dispatch("Read tasks");await(version){note("Loaded")};check(metrics()==listOf(1.0,2.0,1.0)&&reads()==1)
    phase="toggle";version=dispatch("Pack stand");await(version){metrics()==listOf(2.0,2.0,0.0)}
    // Commit Beta while Alpha is physically pending, then deliver Alpha late.
    phase="pending";CatalogueReaderService.gate=CountDownLatch(1);CatalogueReaderService.entered=CountDownLatch(1);CatalogueReaderService.replied=CountDownLatch(1)
    version=dispatch("Show","Alpha");check(CatalogueReaderService.entered!!.await(3,TimeUnit.SECONDS));await(version){note("Reading")};version=dispatch("Show","Beta")
    await(version){note("Loaded")&&metrics()==listOf(0.0,1.0,1.0)};check(reads()==3)
    val betaVersion=update.snapshot.version
    CatalogueReaderService.gate!!.countDown();check(CatalogueReaderService.replied!!.await(3,TimeUnit.SECONDS));CatalogueReaderService.gate=null;CatalogueReaderService.entered=null
    // Consume the stale no-op commit before using its newly issued row handles.
    await(betaVersion){note("Loaded")&&metrics()==listOf(0.0,1.0,1.0)}
    version=dispatch("Load cases");await(version){metrics()==listOf(1.0,1.0,0.0)}
    phase="failure";CatalogueReaderService.fail=true;version=dispatch("Read tasks");await(version){note("Read failed; retry")};check(metrics()==listOf(1.0,1.0,0.0)&&reads()==4)
    CatalogueReaderService.fail=false;api.revoke("host/todo");version=dispatch("Read tasks");await(version){note("Read failed; retry")};check(reads()==4)
    api.grant("host/todo");version=dispatch("Read tasks");await(version){note("Loaded")};check(reads()==5)
    phase="reopen";check(api.source(e)==source);api.close(session!!);session=null;session=api.run(e);update=api.next(session!!,"");check(metrics()==listOf(0.0,1.0,1.0)&&reads()==5&&code==1);api.close(session!!);session=null
    phase="reload";val loaded=api.load("Exact reader reload",source);try{check(api.source(loaded)==source);session=api.run(loaded);update=api.next(session!!,"");check(metrics()==listOf(0.0,3.0,3.0)&&reads()==5);api.close(session!!);session=null}finally{api.forget(loaded)}
    }
    check(TaskStore(isolated).tasks().size==3&&!TaskStore(isolated).tasks()[1].complete)
   }finally{session?.let{api.close(it)};api.forget(e)}
  }}
  receipt.putString("result",if(partial)"PASS public pinned checklist/list_summary retained after three rejected SDK section constructions; row actions and exact reload; no live inference" else "PASS public pinned checklist/list_summary with actual Todo Binder reads, local checks, editable show refresh, late stale reply, failure/retry/revocation, retained reopen/exact reload; no live inference")
  finish(0,receipt)
 }catch(error:Throwable){receipt.putString("failure",phase+": "+android.util.Log.getStackTraceString(error));finish(1,receipt)}finally{CatalogueReaderService.gate?.countDown();CatalogueReaderService.gate=null;CatalogueReaderService.entered=null;CatalogueReaderService.replied=null;files.forEach{targetContext.deleteSharedPreferences(it)}}
 }
}
