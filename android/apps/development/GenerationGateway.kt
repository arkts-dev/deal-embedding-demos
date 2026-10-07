package dev.deal.apps.development

import dev.deal.embedding.ModelClient
import dev.deal.embedding.GenerationCancellation
import org.json.JSONObject
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.TimeUnit

/**
 * Credential-free demo client for the loopback development gateway.
 *
 * Uses a raw socket: HttpURLConnection buffers a POST body unless the response is read
 * concurrently, which leaves client and server waiting on each other.
 */
class GenerationGateway(private val endpoint: String) : ModelClient {
    init { require(endpoint == "http://127.0.0.1:8787/generate") { "Development gateway must use loopback forwarding" } }

    override fun complete(input: String, previous: String, diagnostics: String, cancellation: GenerationCancellation): String {
        cancellation.check()
        val body = JSONObject().put("input", input).put("previous", previous).put("diagnostics", diagnostics).toString().toByteArray()
        val started = System.nanoTime()
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
            val raw = ByteArrayOutputStream().also { buffer -> socket.getInputStream().use { it.copyTo(buffer) } }.toByteArray()
            val text = raw.toString(Charsets.ISO_8859_1)
            val headerEnd = text.indexOf("\r\n\r\n")
            check(headerEnd > 0) { "Malformed gateway response" }
            val statusLine = text.substringBefore("\r\n")
            val status = statusLine.split(' ').getOrNull(1)?.trim()?.toIntOrNull() ?: 0
            android.util.Log.i("Generation", "POST /generate -> $status in ${TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started)} ms, sent ${body.size} bytes, received ${raw.size} bytes")
            check(status == 200) { "Generation gateway unavailable ($status)" }
            val payload = raw.copyOfRange(headerEnd + 4, raw.size)
            cancellation.check()
            require(payload.size <= 256 * 1024) { "Gateway response too large" }
            return payload.toString(Charsets.UTF_8)
        } finally { registration.close(); runCatching { socket.close() } }
    }
}
