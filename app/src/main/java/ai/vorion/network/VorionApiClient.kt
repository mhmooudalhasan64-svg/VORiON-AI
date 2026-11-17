package ai.vorion.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

data class VorionMessage(
    val role: String,
    val content: String
) {
    init {
        require(role == "user" || role == "assistant") {
            "Only user and assistant messages can be sent."
        }
        require(content.isNotBlank()) { "Message content cannot be blank." }
    }
}

data class VorionReply(
    val text: String,
    val model: String
)

/**
 * Client for the VORION gateway. Provider credentials remain on the server.
 * Supply the gateway URL and an authenticated session token from the app's
 * future sign-in flow; never hardcode either secret in the APK.
 */
class VorionApiClient(
    private val gatewayBaseUrl: String,
    private val sessionTokenProvider: suspend () -> String?,
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(50, TimeUnit.SECONDS)
        .callTimeout(55, TimeUnit.SECONDS)
        .build()
) {
    suspend fun sendMessage(messages: List<VorionMessage>): VorionReply =
        withContext(Dispatchers.IO) {
            require(messages.isNotEmpty()) { "At least one message is required." }
            require(messages.size <= 30) { "Conversation is too long." }

            val baseUrl = gatewayBaseUrl.trim().trimEnd('/')
            require(baseUrl.startsWith("https://") || baseUrl.startsWith("http://10.0.2.2")) {
                "Use HTTPS for the VORION gateway."
            }
            val token = sessionTokenProvider()?.takeIf { it.isNotBlank() }
                ?: throw IOException("Sign-in is required before using VORION AI.")

            val messageArray = JSONArray()
            messages.forEach { message ->
                messageArray.put(
                    JSONObject()
                        .put("role", message.role)
                        .put("content", message.content)
                )
            }

            val body = JSONObject()
                .put("messages", messageArray)
                .put("temperature", 0.6)
                .put("max_tokens", 800)
                .toString()
                .toRequestBody("application/json; charset=utf-8".toMediaType())

            val request = Request.Builder()
                .url("$baseUrl/v1/chat/completions")
                .header("Authorization", "Bearer $token")
                .post(body)
                .build()

            httpClient.newCall(request).execute().use { response ->
                val responseText = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val detail = runCatching {
                        JSONObject(responseText).optString("detail").takeIf { it.isNotBlank() }
                    }.getOrNull()
                    val suffix = detail?.let { ": $it" }.orEmpty()
                    throw IOException("VORION gateway request failed (${response.code})$suffix")
                }
                val json = JSONObject(responseText)
                val answer = json.optString("assistant").trim()
                if (answer.isEmpty()) throw IOException("VORION returned an empty response.")
                VorionReply(
                    text = answer,
                    model = json.optString("model", "external")
                )
            }
        }
}
