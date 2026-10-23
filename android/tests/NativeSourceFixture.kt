package dev.deal.connectors.tests

import org.json.JSONArray
import org.json.JSONObject

/** Native tool message fixture. Only the portable core executes these operations. */
internal fun nativeSourceFixture(deal: String, dealui: String): String {
    fun write(file: String, source: String) = JSONObject().put("id", "fixture-$file").put("type", "function").put("function", JSONObject().put("name", "write_file").put("arguments", JSONObject().put("file", file).put("source", source).toString()))
    return JSONObject().put("role", "assistant").put("content", "").put("tool_calls", JSONArray().put(write("deal", deal)).put(write("dealui", dealui))).toString()
}
internal fun nativePatchFixture(revision: String, search: String, replacement: String): String {
    val edits = JSONArray().put(JSONObject().put("file", "deal").put("search", if (revision == "wrong-revision") "absent fixture search" else search).put("replacement", replacement))
    val call = JSONObject().put("id", "fixture-edit").put("type", "function").put("function", JSONObject().put("name", "edit_sources").put("arguments", JSONObject().put("edits", edits).toString()))
    return JSONObject().put("role", "assistant").put("content", "").put("tool_calls", JSONArray().put(call)).toString()
}
