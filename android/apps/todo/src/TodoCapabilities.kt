package dev.deal.apps.todo

import android.content.Context
import dev.deal.embedding.capabilities.*
import org.json.*

/** One contract drives the checker declarations, the sandbox bindings and the native handlers. */
fun todoCapabilities(context: Context): NativeCapabilities {
    val store = TaskStore(context)
    val contract = CapabilityContract("host/todo", "Show tasks: assigned work, deadlines, checklists and proposal outcomes", listOf(
        CapabilityFunction("tasks", "Read tasks shared for a show", listOf(CapabilityParameter("show", CapabilityType("string", maximum = 120))),
            CapabilityType("array", element = CapabilityType("record", fields = mapOf(
                "id" to CapabilityType("string"), "title" to CapabilityType("string"), "assignee" to CapabilityType("string"),
                "due" to CapabilityType("string"), "complete" to CapabilityType("boolean"), "checks" to CapabilityType("int"), "done" to CapabilityType("int"))),
                maximum = 64)),
        CapabilityFunction("task", "Read one shared task", listOf(CapabilityParameter("id", CapabilityType("string"))),
            CapabilityType("record", fields = mapOf("id" to CapabilityType("string"), "title" to CapabilityType("string"), "assignee" to CapabilityType("string"),
                "due" to CapabilityType("string"), "complete" to CapabilityType("boolean"), "status" to CapabilityType("string")))),
        CapabilityFunction("proposeTask", "Stage a task proposal for native review", listOf(
            CapabilityParameter("title", CapabilityType("string", maximum = 120)), CapabilityParameter("assignee", CapabilityType("string", maximum = 60)),
            CapabilityParameter("due", CapabilityType("string")), CapabilityParameter("location", CapabilityType("string", maximum = 120)),
            CapabilityParameter("instructions", CapabilityType("string", maximum = 512)),
            CapabilityParameter("checks", CapabilityType("array", element = CapabilityType("string", maximum = 120), maximum = 16))),
            CapabilityType("record", fields = mapOf("proposalId" to CapabilityType("string"), "state" to CapabilityType("string")))),
        CapabilityFunction("outcome", "Read the persistent outcome of a task proposal", listOf(CapabilityParameter("proposalId", CapabilityType("string"))),
            CapabilityType("record", fields = mapOf("state" to CapabilityType("string"), "taskId" to CapabilityType("string"), "detail" to CapabilityType("string")))),
    ))
    return NativeCapabilities(contract, mapOf(
        "tasks" to { args ->
            val show = args.getString(0)
            JSONArray(store.tasks().filter { it.show == show }.map { task -> JSONObject().put("id", task.id).put("title", task.title)
                .put("assignee", task.assignee).put("due", iso(task.due)).put("complete", task.complete)
                .put("checks", task.checklist.size).put("done", task.checklist.count { it.done }) })
        },
        "task" to { args ->
            val task = store.tasks().firstOrNull { it.id == args.getString(0) } ?: throw IllegalArgumentException("Unknown task")
            JSONObject().put("id", task.id).put("title", task.title).put("assignee", task.assignee).put("due", iso(task.due))
                .put("complete", task.complete).put("status", if (task.complete) "completed" else "open")
        },
        "proposeTask" to { args -> propozal(context, store, args) },
        "outcome" to { args -> JSONObject(context.getSharedPreferences("proposals", 0).getString("todo:" + args.getString(0), "{}")) },
    ))
}

/** Proposals persist, so the organizer can recover an outcome after either app restarts. */
private fun propozal(context: Context, store: TaskStore, args: JSONArray): Any {
    val title = args.getString(0)
    val assignee = args.getString(1)
    val due = args.getString(2)
    val location = args.getString(3)
    val instructions = args.getString(4)
    val checks = args.getJSONArray(5)
    val proposalId = "todo-" + java.util.UUID.randomUUID().toString().take(8)
    val prefs = context.getSharedPreferences("proposals", 0)
    val record = JSONObject().put("proposalId", proposalId).put("state", "pending").put("title", title).put("assignee", assignee)
        .put("due", due).put("location", location).put("instructions", instructions)
        .put("checks", JSONArray((0 until checks.length()).map { checks.getString(it) }))
    prefs.edit().putString("todo:$proposalId", record.toString()).commit()
    return JSONObject().put("proposalId", proposalId).put("state", "pending")
}

internal fun iso(epochMillis: Long): String = java.time.Instant.ofEpochMilli(epochMillis).atZone(TASK_ZONE).toString()
