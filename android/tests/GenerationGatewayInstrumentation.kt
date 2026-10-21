package dev.deal.connectors.tests

import android.app.Instrumentation
import android.os.Bundle
import dev.deal.embedding.*
import dev.deal.embedding.capabilities.*
import org.json.JSONObject
import java.net.ServerSocket
import java.net.Socket
import java.util.UUID
import java.util.concurrent.*

/** Local HTTP fixtures only: exercises library connection handling, not live inference. */
class GenerationGatewayInstrumentation : Instrumentation() {
    override fun onCreate(arguments: Bundle?) { super.onCreate(arguments); start() }
    private class Server(private val respond: (Socket, String, JSONObject) -> Unit) : AutoCloseable {
        val socket = ServerSocket(0, 1, java.net.InetAddress.getByName("127.0.0.1"))
        val done = CompletableFuture<Unit>()
        private var accepted: Socket? = null
        private val thread = Thread {
            try {
                socket.accept().use { client ->
                    accepted = client; client.soTimeout = 10_000
                    val stream = client.getInputStream()
                    fun line(): String {
                        val value = StringBuilder()
                        while (true) { val c = stream.read(); check(c >= 0); if (c == 10) return value.toString().removeSuffix("\r"); value.append(c.toChar()); check(value.length < 8192) }
                    }
                    val headers = mutableListOf(line())
                    while (true) { val line = line(); if (line.isEmpty()) break; headers.add(line) }
                    val length = headers.first { it.startsWith("Content-Length:", true) }.substringAfter(':').trim().toInt()
                    val bytes = ByteArray(length); var offset = 0
                    while (offset < length) { val n = stream.read(bytes, offset, length - offset); check(n > 0); offset += n }
                    respond(client, headers.joinToString("\n"), JSONObject(bytes.toString(Charsets.UTF_8)))
                }
                done.complete(Unit)
            } catch (error: Throwable) { done.completeExceptionally(error) }
        }.apply { start() }
        val endpoint get() = "http://127.0.0.1:${socket.localPort}/configured-generation"
        override fun close() { socket.close(); accepted?.close(); thread.join(2000); check(!thread.isAlive) }
    }
    private fun reply(socket: Socket, body: String, status: Int = 200, chunked: Boolean = false) {
        val bytes = body.toByteArray(Charsets.UTF_8)
        val out = socket.getOutputStream()
        out.write(("HTTP/1.1 $status Fixture\r\nContent-Type: application/json\r\n" + if (chunked) "Transfer-Encoding: chunked\r\n\r\n" else "Content-Length: ${bytes.size}\r\n\r\n").toByteArray())
        if (chunked) out.write((bytes.size.toString(16) + "\r\n").toByteArray())
        out.write(bytes)
        if (chunked) out.write("\r\n0\r\n\r\n".toByteArray())
        out.flush()
    }
    private fun envelope(content: String) = JSONObject().put("content", content).put("model", "fixture-model").put("metadata", JSONObject().put("in", 1)).put("request", JSONObject().put("fixture", "request")).put("response", JSONObject().put("fixture", "response")).toString()
    override fun onStart() {
        val result = Bundle()
        val executor = Executors.newSingleThreadExecutor()
        val oldCapture = EmbeddingLog(targetContext).capturesContent
        try {
            for (bad in listOf("http://example.com/generate", "https://user:secret@example.com/generate", "file:///generate", "https://example.com/generate#fragment")) {
                check(runCatching { GenerationGatewayConfig(bad, "fixture") }.isFailure)
            }
            check(runCatching { GenerationGatewayConfig("https://example.com/generate", "bad\r\nheader") }.isFailure)
            val source = Source("""export class State { value:int=7; }
export class Increment {}
export function initialState():State {return {};}
// @ui-update
export function increment(state:State,action:Increment):State {return {value:state.value+1};}
""", """import * as app from "./experience";
import * as ui from "./platform.dealui-pack";
// @ui-root
export view Main(state:app.State):View {ui.Column(){ui.IntText(value:state.value) ui.Button(text:"Increment",onClick:action app.Increment {})}}
""")
            val registry = CapabilityRegistry(targetContext, CapabilityDiscovery(targetContext) { false }, emptyList())
            val pair = JSONObject().put("deal", source.deal).put("dealui", source.dealui).toString()
            EmbeddingLog.capture(targetContext, true)
            Server { client, headers, body ->
                check(headers.startsWith("POST /configured-generation HTTP/1.1"))
                check(headers.contains("X-Deal-Generation-Backend: fixture-backend", true))
                check(body.getBoolean("capture") && body.getString("input").contains("Gateway fixture"))
                check(body.getString("previous").isEmpty())
                check(body.get("diagnostics") is String)
                reply(client, envelope(pair), chunked = true)
            }.use { server ->
                val config = EmbeddingConfig("experience", emptyList(), "gateway-fixture-${UUID.randomUUID()}")
                ApplicationProtocol(targetContext, Embedding(targetContext, registry, GenerationGatewayConfig(server.endpoint, "fixture-backend"), config)).use { api ->
                    val request = api.request("Gateway fixture", "Gateway fixture: editable counter", api.snapshot(WireType("record"), "{}"))
                    val trace = api.requestTrace(request)
                    val experience = try { api.generate(request) } catch (error: Throwable) {
                        runCatching { server.done.get(2, TimeUnit.SECONDS) }.onFailure(error::addSuppressed)
                        throw error
                    }
                    check(api.source(experience) == source && experience.origin == "generated" && experience.attempts == 1)
                    val session = api.run(experience)
                    val initial = api.next(session, "")
                    fun action(node: JSONObject): Int? {
                        val props = node.getJSONArray("props")
                        for (i in 0 until props.length()) if (props.getJSONObject(i).optString("kind") == "action") return props.getJSONObject(i).getInt("actionSlot")
                        val children = node.getJSONArray("children")
                        for (i in 0 until children.length()) action(children.getJSONObject(i))?.let { return it }
                        return null
                    }
                    api.dispatch(session, action(initial.snapshot.tree)!!, null)
                    val changed = api.next(session, initial.cursor)
                    check(changed.snapshot.tree.toString().contains("\"intValue\":8"))
                    EmbeddingLog.flush(targetContext)
                    val inspected = api.retainedEvents().inspection(experiences = setOf(experience.id))
                    check(inspected.events.any { it.optString("stage") == "source" && it.optString("trace") == trace })
                    check(inspected.events.any { it.optString("stage") == "model-provider" && it.optString("outcome") == "completed" })
                    check(inspected.events.any { it.optString("stage") == "snapshot" && it.optString("artifact").isNotEmpty() })
                    api.close(session)
                    val reopened = api.run(experience); check(api.next(reopened, "").snapshot.fault.isEmpty()); api.close(reopened)
                    api.forget(experience)
                    server.done.get(5, TimeUnit.SECONDS)
                    EmbeddingLog.flush(targetContext)
                    val events = api.retainedEvents().events.filter { it.optString("trace") == trace }
                    check(events.any { it.optString("stage") == "transport" && it.optString("code") == "HTTP_200" })
                    val provider = events.filter { it.optString("stage") == "model-provider" }
                    check(provider.any { it.optString("outcome") == "completed" && it.optString("target").contains("fixture-model") })
                    for (outcome in listOf("request", "response")) check(provider.any { it.optString("outcome") == outcome && it.optString("artifact").isNotEmpty() })
                }
            }
            // Remote discovery uses the same transport but the unchanged strict discovery/admission path.
            val discoveryRegistry = CapabilityRegistry(targetContext, CapabilityDiscovery(targetContext) { false }, emptyList())
            val noDiscoveryGeneration = object : ModelClient {
                override fun complete(input: String, previous: String, diagnostics: String, cancellation: GenerationCancellation, log: EmbeddingLog): String = error("Discovery must not generate source")
            }
            ApplicationProtocol(targetContext, Embedding(targetContext, discoveryRegistry, noDiscoveryGeneration, EmbeddingConfig("experience", emptyList(), "remote-discovery-${UUID.randomUUID()}"))).use { api ->
                val facts = dev.deal.embedding.discovery.DiscoveryInput(listOf(dev.deal.embedding.discovery.DiscoveryFact("budget", "Compare unfinished budget 18000 and committed 19750 EUR cents.")))
                val suggestion = """{"title":"Budget","brief":"Editable amounts 18000 and 19750 cents with recomputing difference.","factIds":["budget"],"capabilityIds":[]}"""
                for (answer in listOf(suggestion, "null", "invalid")) {
                    Server { client, headers, body ->
                        check(headers.contains("X-Deal-Generation-Backend: deepseek-flash", true))
                        check(body.getString("previous").isEmpty() && body.getString("diagnostics").isEmpty())
                        check(body.getString("input").contains("18000"))
                        reply(client, envelope(answer))
                    }.use { server ->
                        dev.deal.embedding.discovery.Discovery(targetContext, api, GenerationGatewayConfig(server.endpoint, "deepseek-flash")).use { finder ->
                            if (answer == "invalid") {
                                val error = runCatching { finder.discover(facts).get(10, TimeUnit.SECONDS) }.exceptionOrNull()
                                check(error is ExecutionException && error.cause is IllegalArgumentException)
                            } else {
                                val tip = finder.discover(facts).get(10, TimeUnit.SECONDS)
                                if (answer == "null") check(tip == null) else check(finder.request(checkNotNull(tip), facts).id.isNotBlank())
                            }
                        }
                        server.done.get(5, TimeUnit.SECONDS)
                    }
                }
            }
            fun failure(body: String, status: Int, code: String) {
                Server { client, _, _ -> reply(client, body, status) }.use { server ->
                    val error = runCatching { GatewayModelClient(GenerationGatewayConfig(server.endpoint, "fixture")).complete("input", "previous", "diagnostics", GenerationCancellation(), EmbeddingLog(targetContext)) }.exceptionOrNull()
                    check(error is ProtocolFailure && error.code == code) { error.toString() }
                    check(!error.message.orEmpty().contains("secret"))
                    server.done.get(5, TimeUnit.SECONDS)
                }
            }
            failure("{\"error\":{\"code\":\"BACKEND_UNAVAILABLE\",\"message\":\"secret must not escape\"}}", 503, "BACKEND_UNAVAILABLE")
            failure("redirect", 302, "HTTP_302")
            failure("not json", 200, "GATEWAY_FAILURE")
            failure("{\"content\":42}", 200, "GATEWAY_FAILURE")
            failure(envelope("x".repeat(256 * 1024 + 1)), 200, "GATEWAY_FAILURE")
            failure("x".repeat(2 * 1024 * 1024 + 1), 200, "GATEWAY_FAILURE")
            Server { client, _, body ->
                check(body.getString("previous") == "previous" && body.getString("diagnostics") == "diagnostics")
                reply(client, JSONObject().put("content", "result").put("model", "fixture-model").put("metadata", JSONObject()).toString())
            }.use { server ->
                check(GatewayModelClient(GenerationGatewayConfig(server.endpoint, "fixture")).complete("input", "previous", "diagnostics", GenerationCancellation(), EmbeddingLog(targetContext)) == "result")
                server.done.get(5, TimeUnit.SECONDS)
            }
            val cancelled = GenerationCancellation().apply { cancel() }
            check(runCatching { GatewayModelClient(GenerationGatewayConfig("http://127.0.0.1:1/generate", "fixture")).complete("input", "", "", cancelled, EmbeddingLog(targetContext)) }.exceptionOrNull() is CancellationException)
            check(runCatching { GatewayModelClient(GenerationGatewayConfig("http://127.0.0.1:1/generate", "fixture")).complete("x".repeat(256 * 1024), "", "", GenerationCancellation(), EmbeddingLog(targetContext)) }.exceptionOrNull() is IllegalArgumentException)
            EmbeddingLog.capture(targetContext, false)
            val uncaptured = EmbeddingLog(targetContext)
            Server { client, _, body -> check(!body.getBoolean("capture")); reply(client, envelope("uncaptured")) }.use { server ->
                check(GatewayModelClient(GenerationGatewayConfig(server.endpoint, "fixture")).complete("input", "previous", "diagnostics", GenerationCancellation(), uncaptured) == "uncaptured")
                server.done.get(5, TimeUnit.SECONDS)
            }
            EmbeddingLog.flush(targetContext)
            // Read through the actual public observation adapter.
            val noModel = object : ModelClient { override fun complete(input: String, previous: String, diagnostics: String, cancellation: GenerationCancellation, log: EmbeddingLog): String = error("No inference") }
            ApplicationProtocol(targetContext, Embedding(targetContext, CapabilityRegistry(targetContext, CapabilityDiscovery(targetContext) { false }, emptyList()), noModel, EmbeddingConfig("experience", emptyList(), "gateway-observe"))).use { api ->
                val events = api.retainedEvents().events.filter { it.optString("trace") == uncaptured.run }
                check(events.any { it.optString("stage") == "model-provider" && it.optString("outcome") == "completed" })
                check(events.none { it.optString("artifact").isNotEmpty() })
            }
            val entered = CountDownLatch(1); val release = CountDownLatch(1)
            Server { _, _, _ -> entered.countDown(); release.await(10, TimeUnit.SECONDS) }.use { server ->
                val cancellation = GenerationCancellation()
                val future = executor.submit<String> { GatewayModelClient(GenerationGatewayConfig(server.endpoint, "fixture")).complete("input", "", "", cancellation, EmbeddingLog(targetContext)) }
                try {
                    check(entered.await(5, TimeUnit.SECONDS)); cancellation.cancel()
                    val error = runCatching { future.get(5, TimeUnit.SECONDS) }.exceptionOrNull()
                    check(error is ExecutionException && error.cause is CancellationException) { error.toString() }
                } finally { release.countDown() }
            }
            Server { _, _, _ -> Thread.sleep(300) }.use { server ->
                val error = runCatching { GatewayModelClient(GenerationGatewayConfig(server.endpoint, "fixture", readTimeoutMs = 100)).complete("input", "", "", GenerationCancellation(), EmbeddingLog(targetContext)) }.exceptionOrNull()
                check(error is ProtocolFailure && error.code == "MODEL_TIMEOUT") { error.toString() }
            }
            result.putString("generationGateway", "PASS configured library gateway through compiled generate, checked edit/reopen, backend/path, chunked HTTP, typed failures, bounds, cancellation, timeout and capture policy")
            finish(0, result)
        } catch (error: Throwable) { result.putString("failure", android.util.Log.getStackTraceString(error)); finish(1, result) }
        finally { executor.shutdownNow(); EmbeddingLog.capture(targetContext, oldCapture) }
    }
}
