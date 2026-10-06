package dev.deal.apps.development

import dev.deal.embedding.ModelClient
import dev.deal.embedding.GenerationCancellation

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Credential-free demo client. The development gateway holds provider credentials. */
class GenerationGateway(private val endpoint: String) : ModelClient {
    init { require(endpoint == "http://127.0.0.1:8787/generate") { "Development gateway must use loopback forwarding" } }
    override fun complete(input: String, previous: String, diagnostics: String, cancellation: GenerationCancellation): String {
        cancellation.check()
        val connection = URL(endpoint).openConnection() as HttpURLConnection
        var registration: AutoCloseable? = null
        try {
            registration = cancellation.onCancel { connection.disconnect() }
            connection.requestMethod = "POST"; connection.doOutput = true
            connection.connectTimeout = 5000; connection.readTimeout = 180000
            connection.setRequestProperty("Content-Type", "application/json")
            connection.outputStream.use { it.write(JSONObject().put("input", input).put("previous", previous).put("diagnostics", diagnostics).toString().toByteArray()) }
            check(connection.responseCode == 200) { "Generation gateway unavailable (${connection.responseCode})" }
            val bytes = connection.inputStream.use { it.readNBytes(256 * 1024 + 1) }
            require(bytes.size <= 256 * 1024)
            cancellation.check()
            return bytes.toString(Charsets.UTF_8)
        } catch (error: Exception) {
            cancellation.check() // A disconnected transport reports cancellation, not an inference failure.
            throw error
        } finally { registration?.close(); connection.disconnect() }
    }
}
