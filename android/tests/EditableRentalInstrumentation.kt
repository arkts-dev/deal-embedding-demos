package dev.deal.connectors.tests

import android.app.Instrumentation
import android.os.Bundle
import dev.deal.apps.development.GenerationGateway
import dev.deal.embedding.*
import dev.deal.embedding.capabilities.*
import org.json.*
import java.io.File
import java.security.MessageDigest

/** Frozen editable Rental tasks. Default fixture replay never invokes inference. */
class EditableRentalInstrumentation : Instrumentation() {
    private var mode="fixtures"
    private var strategy="full"
    override fun onCreate(arguments: Bundle?) {
        mode=arguments?.getString("mode") ?: "fixtures"; strategy=arguments?.getString("strategy") ?: "full"
        require(mode in listOf("fixtures","real","saved","generated") && strategy in listOf("full","delta"))
        super.onCreate(arguments); start()
    }
    override fun onStart() {
        val output=Bundle(); val context=targetContext
        val dir=File(context.filesDir,"editable-experiment/$strategy").apply { mkdirs() }
        val budget=context.getSharedPreferences("editable-model-budget",0)
        val summaries=JSONArray()
        var registry: CapabilityRegistry?=null; var runtime: ExperienceRuntime?=null
        try {
            val str=CapabilityType("string",maximum=8192)
            val read=CapabilityContract("host/rental","Equipment rental catalogue", listOf(CapabilityFunction("equipment","Read period catalogue",listOf(CapabilityParameter("from",str),CapabilityParameter("until",str)),CapabilityType("array",element=CapabilityType("record",fields=linkedMapOf("id" to str,"name" to str,"specification" to str,"cents" to CapabilityType.IntType,"available" to CapabilityType.IntType))))))
            val stage=CapabilityContract("host/organizer","Local preparation for native review",listOf(CapabilityFunction("stage","Prepare descriptor only",listOf("kind","provider","title","detail","price","deadline").map { CapabilityParameter(it,str) },CapabilityType("record",fields=mapOf("staged" to CapabilityType("boolean"),"reference" to str)))))
            var reads=0; var stages=0; var descriptor: JSONArray?=null
            val fixtures=JSONArray(listOf(
                JSONObject().put("id","stand").put("name","Boom microphone stand").put("specification","Adjustable boom; adapter fitting the venue microphone clip").put("cents",800).put("available",2),
                JSONObject().put("id","package").put("name","Vocal package").put("specification","Microphone, boom stand and cable").put("cents",1200).put("available",1),
                JSONObject().put("id","cable").put("name","Cable").put("specification","10 m XLR").put("cents",500).put("available",3)))
            registry=CapabilityRegistry(context,CapabilityDiscovery(context) { false },listOf(
                NativeCapabilities(read,mapOf("equipment" to { args -> check(args.getString(0)=="2026-10-10T16:00:00Z" && args.getString(1)=="2026-10-10T22:00:00Z");reads++;fixtures })),
                NativeCapabilities(stage,mapOf("stage" to { args -> descriptor=args;stages++;JSONObject().put("staged",true).put("reference","local-review") }))))
            registry.grantAll()
            runtime=ExperienceRuntime(context,registry,EmbeddingConfig("experience",registry.contracts(),"editable-rental",strategy))
            val host=runtime
            if(mode=="fixtures" && strategy=="delta") {
                val deal=this@EditableRentalInstrumentation.context.assets.open("editable-rental/experience.deal").bufferedReader().use { it.readText() }
                val ui=this@EditableRentalInstrumentation.context.assets.open("editable-rental/experience.dealui").bufferedReader().use { it.readText() }.replace("ui.Hero(","ui.Heroo(")
                val digest=MessageDigest.getInstance("SHA-256").digest(("${deal.codePointCount(0,deal.length)}:$deal${ui.codePointCount(0,ui.length)}:$ui").toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
                var syntheticCalls=0
                val synthetic=object: ModelClient {
                    override fun complete(input: String, previous: String, diagnostics: String, cancellation: GenerationCancellation): String {
                        syntheticCalls++
                        if(syntheticCalls==1) return "invalid choice"
                        if(syntheticCalls==2) return JSONObject().put("deal",deal).put("dealui",ui).toString()
                        check(syntheticCalls==3 && input.contains(digest) && input.contains("exact-source-patch-v1") && diagnostics.contains("UI"))
                        return JSONObject().put("baseDigest",digest).put("edits",JSONArray().put(JSONObject().put("file","dealui").put("search","ui.Heroo(").put("replacement","ui.Hero("))).toString()
                    }
                }
                host.generate("Synthetic delta repair, no inference","{}",synthetic,GenerationCancellation()) {}.use { check(it.attempts==2 && syntheticCalls==3) }
                check(reads==0 && stages==0)
            }
            fun nodes(n: JSONObject): List<JSONObject> = listOf(n)+(0 until n.getJSONArray("children").length()).flatMap { nodes(n.getJSONArray("children").getJSONObject(it)) }
            fun prop(n: JSONObject,k: String): JSONObject = (0 until n.getJSONArray("props").length()).map { n.getJSONArray("props").getJSONObject(it) }.first { it.getString("name")==k }
            val tasks=listOf("quantity-budget","search-selection","validation-empty","denial-recovery")
            val common="Build an editable Rental shortlist for the disclosed boom stand requirement. Provide ui.IntField labelled Quantity with accessibilityLabel Quantity, value initially 1, minimum 0 maximum 9, validation allowing only 1 to 8. Provide ui.IntField labelled Total budget in cents with accessibilityLabel Budget, initial 2000, minimum 0 maximum 100000, step 100. Provide ui.SearchField labelled Search with accessibilityLabel Search, initial empty. All changes clear stale rows, selection and preparation success. Show validation in field error/supporting props. Disable editing while loading/preparing. Button Load options enabled only with valid inputs. Read host/rental.equipment only on Load options, forward both disclosed exact timestamps. Require boom and stand across ASCII lowercased name/specification, additionally search substring (trim/lowercase, empty accepts), available >= quantity, nonnegative cents and total cents <= budget. Display qualifying ui.Option rows keyed/value by original id, original title, formatted TOTAL euro price, selected and enabled state. Select via onSelect payload. Button Prepare for review invokes host/organizer.stage locally with kind rental, provider host/rental, original title, detail Item <id>:<quantity> and both exact timestamps, total price and period end deadline. Preserve selection through success, show Prepared for native review, never claim reservation. Show Loading, No qualifying options, and visible capability error with retry. Never invoke provider at initialization, and no other capabilities. Build fresh complete immutable states and typed table field reads. No new language syntax. Application filename experience. Return only JSON deal/dealui strings."
            val focuses=listOf("Emphasize changing quantity and total budget; default 1 unit / 2000 cents yields stand and package.","Emphasize search adapter restricting to stand and editing invalidating the selection.","Emphasize quantity zero rejected BEFORE provider invocation, and zero budget giving an honest empty result.","Emphasize denied catalogue read, visible error and recovery via Load options after native grant restored.")
            for ((index,task) in tasks.withIndex()) {
                if(mode=="generated" && (strategy=="full" && task=="validation-empty" || strategy=="delta" && task in listOf("validation-empty","denial-recovery"))) continue
                val receipt=JSONObject().put("task",task).put("strategy",strategy).put("mode",mode)
                var calls=0; var phase="generation"; val started=android.os.SystemClock.elapsedRealtime()
                val runFile=File(dir,"$task-started")
                try {
                    if(mode=="real") { check(!runFile.exists()) { "Already attempted; no automatic inference repetition" }; runFile.writeText("started") }
                    val model=object: ModelClient {
                        override fun complete(input: String, previous: String, diagnostics: String, cancellation: GenerationCancellation): String {
                            if(input.contains("ISSUED OPTIONS\n")) return "{\"answers\":{\"purpose\":\"unavailable\",\"headline\":\"text\",\"notice\":\"none\",\"read\":\"unavailable\"}}"
                            val used=budget.getInt("calls",0); check(used<24) { "Initial inference ceiling reached" }
                            check(budget.edit().putInt("calls",used+1).commit());calls++
                            val response=GenerationGateway("http://127.0.0.1:8787/generate").complete(input,previous,diagnostics,cancellation)
                            File(dir,"$task-attempt-$calls.json").writeText(JSONObject().put("response",response).put("diagnostics",diagnostics).put("repair",previous.isNotEmpty()).toString())
                            return response
                        }
                    }
                    val beforeReads=reads; val beforeStages=stages
                    val accepted=if(mode=="real") host.generate(common+" "+focuses[index],"{\"requirement\":\"Boom stand\",\"specification\":\"Adjustable boom\",\"quantity\":1,\"budgetCents\":2000,\"searchTerms\":[\"boom\",\"stand\"],\"from\":\"2026-10-10T16:00:00Z\",\"until\":\"2026-10-10T22:00:00Z\"}",model,GenerationCancellation()) {} else {
                        val pair=if(mode=="fixtures") JSONObject().put("deal",this@EditableRentalInstrumentation.context.assets.open("editable-rental/experience.deal").bufferedReader().use { it.readText() }).put("dealui",this@EditableRentalInstrumentation.context.assets.open("editable-rental/experience.dealui").bufferedReader().use { it.readText() }) else if(mode=="generated") JSONObject(this@EditableRentalInstrumentation.context.assets.open("editable-generated/$strategy/$task.json").bufferedReader().use { it.readText() }) else JSONObject(File(dir,"$task-accepted.json").readText())
                        host.check(ExperienceSource(pair.getString("deal"),pair.getString("dealui")))
                    }
                    receipt.put("sourceCalls",calls).put("firstPassAccepted",mode=="real"&&calls==1).put("attempts",accepted.attempts)
                    phase="mount"
                    val (workspace,initial)=accepted.use { host.open(task,it) }
                    try {
                        val prefs=context.getSharedPreferences("editable-rental",0)
                        val pair=JSONObject().put("deal",prefs.getString("workspace.${workspace.id}.deal",null)).put("dealui",prefs.getString("workspace.${workspace.id}.dealui",null))
                        if(mode=="real") File(dir,"$task-accepted.json").writeText(pair.toString())
                        receipt.put("sourcePairSha256",MessageDigest.getInstance("SHA-256").digest((pair.getString("deal")+"\u0000"+pair.getString("dealui")).toByteArray()).joinToString("") { "%02x".format(it) })
                        fun snap()=host.poll(workspace.id)!!.also { check(it.optString("fault").isEmpty()) { "RUNTIME_FAULT" } }
                        fun all(s: JSONObject)=nodes(s.getJSONObject("tree"))
                        fun texts(s: JSONObject)=all(s).flatMap { n -> (0 until n.getJSONArray("props").length()).map { n.getJSONArray("props").getJSONObject(it).optString("stringValue") } }
                        fun waitFor(test: (JSONObject)->Boolean): JSONObject { repeat(200) { val s=snap();if(test(s))return s;Thread.sleep(20) };error("BEHAVIOR_TIMEOUT") }
                        fun click(label: String) {
                            val controls=all(snap())
                            val control=controls.firstOrNull { it.getString("component")=="ui.Button" && prop(it,"text").getString("stringValue")==label && prop(it,"enabled").getBoolean("booleanValue") }
                            if(control!=null) host.dispatch(workspace.id,prop(control,"onClick").getInt("actionSlot"),null)
                            else { val retry=controls.first { it.getString("component")=="ui.Failure" };host.dispatch(workspace.id,prop(retry,"onRetry").getInt("actionSlot"),null) }
                        }
                        fun price(value: String)=value.replace(" ","").removeSuffix("total")
                        fun change(label: String,value: String) { val control=all(snap()).first { it.getString("component") in listOf("ui.IntField","ui.SearchField") && prop(it,"accessibilityLabel").getString("stringValue")==label };host.dispatch(workspace.id,prop(control,"onChange").getInt("actionSlot"),value) }
                        fun rows(s: JSONObject)=all(s).filter { it.getString("component")=="ui.Option" }
                        check(reads==beforeReads && stages==beforeStages)
                        phase="initial-shortlist";click("Load options")
                        val loaded=waitFor { rows(it).size==2 }; check(rows(loaded).map { prop(it,"value").getString("stringValue") }.toSet()==setOf("stand","package"))
                        phase="editable-quantity-budget";change("Quantity","2");change("Budget","1800");click("Load options")
                        val quantity=waitFor { rows(it).size==1 };check(prop(rows(quantity).single(),"value").getString("stringValue")=="stand" && price(prop(rows(quantity).single(),"price").getString("stringValue"))=="€16.00")
                        phase="selection-preparation";host.dispatch(workspace.id,prop(rows(quantity).single(),"onSelect").getInt("actionSlot"),"stand")
                        descriptor=null;click("Prepare for review");waitFor { descriptor!=null && texts(it).any { t -> t.startsWith("Prepared for native review") } }
                        val d=descriptor!!;check(d.getString(1)=="host/rental"&&d.getString(2)=="Boom microphone stand"&&d.getString(3).contains("Item stand:2")&&d.getString(3).contains("2026-10-10T16:00:00Z")&&d.getString(3).contains("2026-10-10T22:00:00Z")&&price(d.getString(4))=="€16.00")
                        check(rows(snap()).any { prop(it,"selected").getBoolean("booleanValue") })
                        phase="search-invalidates";change("Quantity","1");change("Budget","2000");change("Search","adapter");check(rows(snap()).isEmpty());click("Load options")
                        val searched=waitFor { rows(it).size==1 };check(prop(rows(searched).single(),"value").getString("stringValue")=="stand")
                        phase="invalid-before-read";change("Quantity","0");val count=reads
                        val invalid=snap();check(texts(invalid).any { t -> t.contains("quantity",ignoreCase=true) && (t.contains("1")||t.contains("invalid")) }); val load=all(invalid).first { it.getString("component")=="ui.Button" && prop(it,"text").getString("stringValue")=="Load options" };check(!prop(load,"enabled").getBoolean("booleanValue"));check(reads==count)
                        phase="empty";change("Quantity","1");change("Budget","0");click("Load options");waitFor { texts(it).any { t -> t.contains("No qualifying options") } && rows(it).isEmpty() }
                        phase="denial-recovery";change("Budget","2000");change("Search","");registry.revoke("host/rental");click("Load options");waitFor { texts(it).any { t -> t.contains("not granted") } };registry.grant("host/rental");click("Load options");waitFor { rows(it).size==2 }
                        receipt.put("behaviorPassed",true).put("inputValidation",true).put("quantityBudgetSearch",true).put("localReview",true).put("denialRecovery",true)
                        check(context.getSharedPreferences("organizer-experience",0).edit().putString("workspace.editable-$strategy-$task.deal",pair.getString("deal")).putString("workspace.editable-$strategy-$task.dealui",pair.getString("dealui")).putString("workspace.editable-$strategy-$task.title","Editable Rental $task").commit())
                    } finally { host.closeWorkspace(workspace.id) }
                } catch(error: Throwable) {
                    receipt.put("behaviorPassed",false).put("failedPhase",phase).put("sourceCalls",calls).put("errorClass",error.javaClass.simpleName)
                    File(dir,"$task-failure.txt").writeText(android.util.Log.getStackTraceString(error))
                }
                receipt.put("durationMs",android.os.SystemClock.elapsedRealtime()-started);summaries.put(receipt);File(dir,"$mode-summary.json").writeText(summaries.toString(2))
            }
            output.putString("editableVerification",summaries.toString());finish(if((0 until summaries.length()).all { summaries.getJSONObject(it).optBoolean("behaviorPassed") })0 else 1,output)
        } catch(error: Throwable) {output.putString("failure",android.util.Log.getStackTraceString(error));finish(1,output)}
        finally {runtime?.close();registry?.close()}
    }
}
