package dev.deal.apps.todo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.deal.shell.*
import org.json.JSONArray
import org.json.JSONObject

class TodoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("provider", 0)
        setContent {
            AppTheme(Color(0xff7252a1)) {
                var tasks by remember { mutableStateOf(JSONArray(prefs.getString("tasks", "[]"))) }
                var title by remember { mutableStateOf("") }
                var minutes by remember { mutableStateOf("15") }
                var allowed by remember { mutableStateOf(prefs.getBoolean("allowed", false)) }
                fun save(updated: JSONArray) { prefs.edit().putString("tasks", updated.toString()).commit(); tasks = updated }
                Page("MAKE SPACE", "Small steps.\nClear head.", "Your preparation, all in one place.") {
                    Panel {
                        Text("Before you go", style = MaterialTheme.typography.headlineSmall)
                        if (tasks.length() == 0) Text("A calm start begins with a small task.")
                        for (i in 0 until tasks.length()) {
                            val task = tasks.getJSONObject(i)
                            key(task.getString("id")) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Checkbox(task.getBoolean("done"), { checked -> val next = JSONArray(tasks.toString()); next.getJSONObject(i).put("done", checked); save(next) })
                                    Column(Modifier.padding(top = 10.dp)) {
                                        Text(task.getString("title"), style = MaterialTheme.typography.titleMedium)
                                        Text("${task.getInt("minutes")} min · preparation", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                    Panel {
                        Text("One more thing", style = MaterialTheme.typography.titleLarge)
                        OutlinedTextField(title, { title = it }, label = { Text("Task title") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        OutlinedTextField(minutes, { minutes = it }, label = { Text("Preparation minutes") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        Button(onClick = {
                            val count = minutes.toIntOrNull()
                            if (title.isNotBlank() && title.length <= 80 && count != null && count in 1..120 && tasks.length() < 20) {
                                val next = JSONArray(tasks.toString()).put(JSONObject().put("id", java.util.UUID.randomUUID().toString()).put("title", title).put("minutes", count).put("done", false))
                                save(next); title = ""
                            }
                        }, modifier = Modifier.fillMaxWidth()) { Text("Add task") }
                    }
                    Consent(allowed) { allowed = it; prefs.edit().putBoolean("allowed", it).commit() }
                }
            }
        }
    }
}
