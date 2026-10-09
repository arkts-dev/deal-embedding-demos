package dev.deal.apps.development

import dev.deal.embedding.ModelClient
import dev.deal.embedding.EmbeddingLog
import dev.deal.embedding.GenerationCancellation
import org.json.JSONObject
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Credential-free demo client for the loopback development gateway.
 *
 * Uses a raw socket: HttpURLConnection buffers a POST body unless the response is read
 * concurrently, which leaves client and server waiting on each other.
 */
class GenerationGateway(private val endpoint: String) : ModelClient {
    init { require(endpoint == "http://127.0.0.1:8787/generate") { "Development gateway must use loopback forwarding" } }

    override fun complete(input: String, previous: String, diagnostics: String, cancellation: GenerationCancellation): String = exchange(input, previous, diagnostics, cancellation, null)
    override fun completeLogged(input: String, previous: String, diagnostics: String, cancellation: GenerationCancellation, log: EmbeddingLog): String = exchange(input, previous, diagnostics, cancellation, log)
    private fun exchange(input: String, previous: String, diagnostics: String, cancellation: GenerationCancellation, log: EmbeddingLog?): String {
        cancellation.check()
        val body = JSONObject().put("input", input).put("previous", previous).put("diagnostics", diagnostics).toString().toByteArray()
        val socket = Socket()
        val registration = cancellation.onCancel { runCatching { socket.close() } }
        try {
            socket.connect(InetSocketAddress("127.0.0.1", 8787), 5000)
            socket.soTimeout = 420_000
            val request = buildString {
                append("POST /generate HTTP/1.1\r\n")
                append("Host: 127.0.0.1:8787\r\n")
                append("Content-Type: application/json\r\n")
                append("Content-Length: ").append(body.size).append("\r\n")
                append("Connection: close\r\n\r\n")
            }.toByteArray()
            // Closing this stream closes the socket, so flush without closing it.
            val out = BufferedOutputStream(socket.getOutputStream())
            out.write(request); out.write(body); out.flush()
            val raw = ByteArrayOutputStream().also { buffer -> socket.getInputStream().use { input ->
                val chunk = ByteArray(8192)
                while (true) { val count = input.read(chunk); if (count < 0) break; check(buffer.size() + count <= 2 * 1024 * 1024 + 8192) { "Gateway response too large" }; buffer.write(chunk, 0, count) }
            } }.toByteArray()
            val text = raw.toString(Charsets.ISO_8859_1)
            val headerEnd = text.indexOf("\r\n\r\n")
            check(headerEnd > 0) { "Malformed gateway response" }
            val statusLine = text.substringBefore("\r\n")
            val status = statusLine.split(' ').getOrNull(1)?.trim()?.toIntOrNull() ?: 0
            log?.event("transport", "response", target = "development gateway", code = "HTTP_$status")
            check(status == 200) { "Generation gateway unavailable ($status)" }
            val payload = raw.copyOfRange(headerEnd + 4, raw.size)
            cancellation.check()
            require(payload.size <= 2 * 1024 * 1024) { "Gateway response too large" }
            val envelope = JSONObject(payload.toString(Charsets.UTF_8))
            log?.event("model-provider", "completed", target = envelope.getString("model"), detail = envelope.getJSONObject("request").toString())
            log?.event("model-provider", "response", target = envelope.getString("model"), detail = envelope.getJSONObject("response").toString())
            return envelope.getString("content")
        } finally { registration.close(); runCatching { socket.close() } }
    }
}
