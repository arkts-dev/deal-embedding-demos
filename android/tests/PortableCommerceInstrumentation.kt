package dev.deal.connectors.tests

import android.app.Instrumentation
import android.os.Bundle
import dev.deal.embedding.*
import dev.deal.embedding.capabilities.*
import org.json.*
import java.util.UUID

/** Injected authoring, real public generation/compiler/session and actual read-only providers. */
class PortableCommerceInstrumentation:Instrumentation() {
    override fun onCreate(arguments:Bundle?){super.onCreate(arguments);start()}
    override fun onStart(){
        val receipt=Bundle()
        try {
            for(kind in listOf("food","equipment")) runScenario(kind)
            receipt.putString("portableCommerce","PASS food/equipment public generation, actual provider reads, quantities/budget/deadline edits, whole-basket quotes, failure/retry/grants, reopen/exact reload; no live inference")
            finish(0,receipt)
        }catch(error:Throwable){receipt.putString("failure",error.stackTraceToString());finish(1,receipt)}
    }
    private fun runScenario(kind:String){
        val context=targetContext
        val food=kind=="food";val module=if(food)"host/pizza" else "host/rental"
        val provider=if(food)dev.deal.apps.pizza.pizzaCapabilities(context) else dev.deal.apps.rental.rentalCapabilities(context)
        val calls=mutableListOf<Pair<String,String>>();var failRead=false;var unavailableQuote=false;var codeCalls=0
        val prefs= listOf("pizza","rental","proposals").associateWith{context.getSharedPreferences(it,0).all.toMap()}
        val observed=NativeCapabilities(provider.contract,provider.contract.functions.associate{fn->fn.name to {args:JSONArray->
            check(fn.name !in listOf("proposeOrder","proposeReservation","outcome")){"Read-only scenario invoked ${fn.name}"}
            calls.add(fn.name to args.toString())
            if(failRead)throw IllegalStateException("Read fixture unavailable")
            val result=provider.invoke(fn.name,args)
            if(unavailableQuote && fn.name=="quote") (result as JSONObject).put("available",false) else result
        }})
        val need=if(food)"Load actual Pizza menu and delivery; six 30cm pizzas, at least two vegan, editable quantities, EUR80 including delivery before18:30; recompute feasibility; no ordering" else "Load Rental attributes and availability; find adjustable stand with clip adapter and balanced XLR female to male cable at least10m; compare whole-basket package and separate quotes; editable EUR20 hire budget, refundable deposit separate; read-only"
        val model=object:ModelClient{
            override fun complete(input:String,previous:String,diagnostics:String,cancellation:GenerationCancellation,log:EmbeddingLog):String{
                cancellation.check()
                if(input.contains("\nPORTABLE COMPLETION\n")){
                    when(JSONObject(input.substringAfterLast("\nPORTABLE COMPLETION\n")).getString("operation")){
                        "plan"->return JSONObject().put("title",kind).put("needs",JSONArray().put(JSONObject().put("type","other").put("what",need).put("on","other"))).toString()
                        "code"->{codeCalls++;check(codeCalls==1){"Unexpected $kind repair: "+input.substringAfterLast("It failed:").take(4000)};return if(food)PortableScenarioFixtures.food else PortableScenarioFixtures.equipment}
                        else->error("Unexpected completion")
                    }
                }
                val questions=JSONObject(input.substringAfterLast("\nPORTABLE CHOICE\n")).getJSONObject("questions");val answers=JSONObject()
                questions.keys().forEach{key->val q=questions.getJSONObject(key);if(q.getString("type")=="choice"){
                    val criteria=q.getJSONObject("criteria");val choice=listOf("none","own","no","false").firstOrNull{criteria.has(it)}
                    if(choice!=null)answers.put(key,JSONObject().put("choice",choice).put("confidence",1))
                }}
                return JSONObject().put("answers",answers).toString()
            }
        }
        val config=RuntimeConfig("experience",emptyList(),"portable-$kind-${UUID.randomUUID()}",retainState=true)
        CapabilityRegistry(context,CapabilityDiscovery(context){false},listOf(observed)).use{registry->
            ApplicationProtocol(context,Embedding(context,registry,model,config)).use{api->
                api.refresh();api.grant(module)
                val before=api.saved().map{it.id}.toSet()
                val experience=api.generate(api.request(kind,need,api.snapshot(WireType("record"),"{}")))
                var session:Session?=null
                try{
                    check(codeCalls==1 && calls.isEmpty()){"Generation must not read providers"}
                    val source=api.source(experience);session=api.run(experience);var update=api.next(session!!,"")
                    fun nodes(n:JSONObject):List<JSONObject> = listOf(n)+(0 until n.getJSONArray("children").length()).flatMap{nodes(n.getJSONArray("children").getJSONObject(it))}
                    fun props(n:JSONObject):List<JSONObject>{val p=n.getJSONArray("props");return(0 until p.length()).map{p.getJSONObject(it)}}
                    fun prop(n:JSONObject,k:String)=props(n).single{it.getString("name")==k}
                    fun texts()=nodes(update.snapshot.tree).filter{it.getString("component")=="ui.Text"}.map{prop(it,"value").getString("stringValue")}
                    fun metrics()=nodes(update.snapshot.tree).filter{it.getString("component")=="ui.Metric"}.map{prop(it,"value").getString("stringValue")}
                    fun dispatch(label:String,payload:String?,ready:()->Boolean){
                        val n=nodes(update.snapshot.tree).single{props(it).any{p->p.optString("name") in listOf("label","text") && p.optString("stringValue")==label}}
                        val version=update.snapshot.version;api.dispatch(session!!,prop(n,if(n.getString("component")=="ui.Button")"onClick" else "onChange").getInt("actionSlot"),payload)
                        val deadline=System.nanoTime()+java.util.concurrent.TimeUnit.SECONDS.toNanos(25)
                        do{check(System.nanoTime()<deadline){"$kind update timeout ${texts()} ${metrics()}"};update=api.next(session!!,update.cursor);check(update.snapshot.fault.isEmpty()){update.snapshot.fault}}while(update.snapshot.version<=version || !ready())
                    }
                    val load=if(food)"Load food" else "Read equipment"
                    fun settled()=texts().none{it.startsWith("Loading")||it.startsWith("Quoting")}
                    dispatch(load,null){settled()}
                    if(food){
                        check(calls.map{it.first}==listOf("menu","delivery")){calls.toString()}
                        check(metrics()==listOf("75.00","6","2") && "Feasible" in texts()){"${metrics()} ${texts()}"}
                        dispatch("Vegan quantity","1"){metrics()==listOf("63.00","5","1")};check("Not feasible" in texts())
                        dispatch("Meat quantity","3"){metrics()==listOf("76.00","6","1")};check("Not feasible" in texts())
                        dispatch("Vegan quantity","2"){metrics()==listOf("88.00","7","2")};check("Not feasible" in texts())
                        dispatch("Meat quantity","2"){metrics()==listOf("75.00","6","2")};check("Feasible" in texts())
                        dispatch("Vegan quantity","0"){metrics()[2]=="0"};dispatch("Vegetarian quantity","0"){metrics()[1]=="2"};dispatch("Meat quantity","0"){metrics()==listOf("5.00","0","0")};check("Not feasible" in texts())
                        dispatch("Vegan quantity","2"){metrics()[2]=="2"};dispatch("Vegetarian quantity","2"){metrics()[1]=="4"};dispatch("Meat quantity","2"){"Feasible" in texts()}
                        dispatch("Budget EUR","74"){"Not feasible" in texts()};dispatch("Budget EUR","80"){"Feasible" in texts()}
                        dispatch("Delivery window","1"){"18:15–18:45" in texts()};check("Not feasible" in texts())
                        dispatch("Delivery window","0"){"Feasible" in texts()}
                        dispatch("Required by","1080"){settled()};check("Not feasible" in texts())
                        dispatch("Required by","1110"){settled()};check("Feasible" in texts())
                        check(calls.last().second=="[\"18:30\"]")
                    }else{
                        check(calls.map{it.first}==listOf("equipment","quote","quote")){calls.toString()}
                        check(JSONArray(calls[1].second).getString(0)=="package:1" && JSONArray(calls[2].second).getString(0)=="stand:1,cable10:1")
                        check(metrics()==listOf("12.00","50.00","13.00","50.00") && texts().count{it=="Feasible"}==2){"${metrics()} ${texts()}"}
                        dispatch("Budget EUR","12"){texts().count{it=="Feasible"}==1};check("Over budget" in texts())
                        dispatch("Budget EUR","11"){texts().count{it=="Over budget"}==2}
                        dispatch("Budget EUR","20"){texts().count{it=="Feasible"}==2}
                        dispatch("Until ISO","2026-10-23T21:30:00Z"){settled()};check(calls.last().second.contains("21:30:00Z"))
                        unavailableQuote=true;dispatch(load,null){settled()};check(texts().count{it=="Unavailable"}==2 && "Feasible" !in texts())
                        unavailableQuote=false;dispatch(load,null){settled()};check(texts().count{it=="Feasible"}==2)
                    }
                    failRead=true;dispatch(load,null){settled() && "Read failed; retry" in texts()};failRead=false
                    dispatch(load,null){settled() && "Read failed; retry" !in texts()}
                    api.revoke(module);val reads=calls.size
                    dispatch(load,null){settled() && "Read failed; retry" in texts()};check(calls.size==reads)
                    api.grant(module);dispatch(load,null){settled() && "Read failed; retry" !in texts()}
                    val expected=metrics();val count=calls.size
                    api.close(session!!);session=api.run(experience);update=api.next(session!!,"")
                    check(metrics()==expected && calls.size==count && codeCalls==1 && api.source(experience)==source)
                    api.close(session!!);session=null
                    val cold=api.load("$kind exact reload",source)
                    try{session=api.run(cold);update=api.next(session!!,"");check(calls.size==count && api.source(cold)==source)}finally{session?.let{api.close(it)};session=null;api.forget(cold)}
                }finally{session?.let{api.close(it)};api.forget(experience)}
                check(api.saved().map{it.id}.toSet()==before)
            }
        }
        for((name,value)in prefs)check(context.getSharedPreferences(name,0).all==value){"$kind modified provider data: $name"}
    }
}
