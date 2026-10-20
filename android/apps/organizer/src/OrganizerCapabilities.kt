package dev.deal.apps.organizer

import android.content.Context
import dev.deal.embedding.WireType
import dev.deal.embedding.WireField
import dev.deal.embedding.SharedCapabilityFunction
import dev.deal.embedding.SharedCapabilityContract
import dev.deal.embedding.Parameter
import dev.deal.embedding.capabilities.NativeCapabilities
import dev.deal.embedding.nativeCapabilities
import org.json.*

/** Shared protocol vocabulary is the authoring input; native conversion is transport only. */
fun organizerCapabilities(context: Context, invokingWorkspace: () -> String?): NativeCapabilities {
    val text=WireType("string")
    val contract=SharedCapabilityContract("host/organizer","Stage a prepared operation for native review; never a write",functions=listOf(
        SharedCapabilityFunction("stage","Prepare one operation for the organizer to confirm natively",parameters=listOf(
            Parameter("kind",WireType("string",maximum=64)),Parameter("provider",WireType("string",maximum=64)),
            Parameter("title",WireType("string",maximum=120)),Parameter("detail",WireType("string",maximum=512)),
            Parameter("price",WireType("string",maximum=32)),Parameter("deadline",WireType("string",maximum=64))
        ),result=WireType("record",fields=listOf(WireField("staged",WireType("boolean")),WireField("reference",text)))),
        SharedCapabilityFunction("staged","Read the operations prepared so far",result=WireType("array",maximum=16,element=WireType("record",fields=listOf(
            WireField("kind",text),WireField("provider",text),WireField("title",text),WireField("detail",text),WireField("price",text),WireField("deadline",text)))))
    ))
    return nativeCapabilities(contract,mapOf(
        "stage" to { args ->
            val prefs = context.getSharedPreferences("prepared", 0)
            val reference = java.util.UUID.randomUUID().toString()
            val record = JSONObject().put("kind", args.getString(0)).put("provider", args.getString(1)).put("title", args.getString(2))
                .put("detail", args.getString(3)).put("price", args.getString(4)).put("deadline", args.getString(5))
            val all = JSONArray(prefs.getString("operations", "[]")); all.put(record)
            val editor = prefs.edit().putString("operations", all.toString()).putString("last", reference)
            invokingWorkspace()?.let { id -> editor.putString("workspace.$id", record.toString()).putString("workspace.$id.reference", reference) }
            check(editor.commit()) { "Cannot retain prepared selection" }
            JSONObject().put("staged", true).put("reference", reference)
        },
        "staged" to { _ -> JSONArray(context.getSharedPreferences("prepared", 0).getString("operations", "[]")) },
    ))
}
