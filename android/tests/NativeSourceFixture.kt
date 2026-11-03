package dev.deal.connectors.tests

import org.json.JSONArray
import org.json.JSONObject

/** Explicit needs → uncovered → full-source fixture; never a production compatibility bypass. */
internal fun nativeGenerationPrelude(input: String): String? {
    if (input.contains("\nGENERATION OPERATION\n")) {
        val evidence=input.substringAfter("\n\nUSER INTENT\n").substringBefore("\nDISCLOSED CONTEXT").trim().ifEmpty { "Fixture" }
        return JSONObject().put("title","Source lifecycle fixture").put("terms",JSONArray()).put("needs",JSONArray().put(JSONObject().put("id","fixtureHole").put("kind","custom").put("label","Source lifecycle").put("evidence",evidence).put("config",JSONObject().put("parameters",JSONArray()).put("arguments",JSONArray()).put("result","number").put("format","integer")))).toString()
    }
    if (input.contains("\nNATIVE TOOL TURN\n") && JSONObject(input.substringAfterLast("\nNATIVE TOOL TURN\n")).optBoolean("holes")) return nativeFallbackFixture()
    return null
}
/** Native tool message fixture. Only the portable core executes these operations. */
internal fun nativeSourceFixture(deal: String, dealui: String, input: String = ""): String {
    nativeGenerationPrelude(input)?.let { return it }
    fun write(file: String, source: String) = JSONObject().put("id", "fixture-$file").put("type", "function").put("function", JSONObject().put("name", "write_file").put("arguments", JSONObject().put("file", file).put("source", source).toString()))
    return JSONObject().put("role", "assistant").put("content", "").put("tool_calls", JSONArray().put(write("deal", deal)).put(write("dealui", dealui))).toString()
}

internal fun nativeFallbackFixture(): String = JSONObject().put("role", "assistant").put("content", "").put("tool_calls", JSONArray().put(JSONObject().put("id", "fixture-fallback").put("type", "function").put("function", JSONObject().put("name", "source_fallback").put("arguments", JSONObject().put("reason", "Explicit full-source lifecycle fixture").toString())))).toString()
internal fun nativePatchFixture(revision: String, search: String, replacement: String): String {
    val edits = JSONArray().put(JSONObject().put("file", "deal").put("search", if (revision == "wrong-revision") "absent fixture search" else search).put("replacement", replacement))
    val call = JSONObject().put("id", "fixture-edit").put("type", "function").put("function", JSONObject().put("name", "edit_sources").put("arguments", JSONObject().put("edits", edits).toString()))
    return JSONObject().put("role", "assistant").put("content", "").put("tool_calls", JSONArray().put(call)).toString()
}
