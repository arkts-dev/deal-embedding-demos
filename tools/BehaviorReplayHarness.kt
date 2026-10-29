package dev.deal.embedding

import dev.deal.embedding.CapabilityContract
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Read-only replay of private captured plan arguments. No inference, repair or provider calls. */
fun main(args:Array<String>) {
    val contracts=JSONArray(File(args[1]).readText()).let{a->(0 until a.length()).map{CapabilityContract.parse(a.getJSONObject(it).toString())}}
    val rows=JSONArray()
    File(args[0]).walkTopDown().filter{it.isFile && it.extension=="json"}.sortedBy{it.path}.forEach { file ->
        val row=JSONObject().put("file",file.relativeTo(File(args[0])).path)
        try {
            val plan=JSONObject(file.readText());val emitter=BehaviorCandidate(contracts)
            val pair=emitter.assemble(plan);check(pair.toString()==emitter.assemble(plan).toString())
            row.put("accepted",true)
        } catch(e:BehaviorCandidate.Rejected) { row.put("accepted",false).put("diagnostics",e.message) }
        rows.put(row)
    }
    File(args[2]).writeText(JSONObject().put("scope","Captured-plan admission only; not repaired behavior or live adherence").put("plans",rows).toString(2)+"\n")
    println("Replayed ${rows.length()} captured plans without inference")
}
