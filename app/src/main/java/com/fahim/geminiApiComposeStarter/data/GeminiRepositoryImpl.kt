package com.fahim.geminiApiComposeStarter.data

import android.content.Context
import android.net.Uri

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.fahim.geminiApiComposeStarter.ui.chat.ChatMessage
import com.fahim.geminiApiComposeStarter.ui.chat.ChatRole
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Base64

class GeminiRepositoryImpl(
    private val context: Context,
    private val apiKey: () -> String,
    private val modelName: () -> String,
) : GeminiRepository {
    override suspend fun generateText(prompt: String): Result<String> =
        generateConversation(listOf(ChatMessage(0, ChatRole.USER, prompt)))

    override suspend fun generateConversation(messages: List<ChatMessage>): Result<String> = withContext(Dispatchers.IO) {
        try {
            val key = apiKey()
            require(key.isNotBlank()) { "Add your Gemini API key in Settings before sending." }
            val model = GenerativeModel(modelName = modelName(), apiKey = key)
            val contents = messages.map { message ->
                content(role = if (message.role == ChatRole.USER) "user" else "model") { text(message.text) }
            }
            val response = model.generateContent(*contents.toTypedArray())
            val answer = response.text?.takeIf { it.isNotBlank() }
                ?: throw IllegalStateException("Gemini returned an empty response. Try again.")
            Result.success(answer)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            // Never log raw request/SDK exceptions: they can contain credentials or prompt text.
            Result.failure(IllegalStateException(
                if (error is IllegalArgumentException) error.message
                else "Could not get a response. Check your connection, API key, model name, and quota, then retry."
            ))
        }
    }

    override suspend fun generateConversation(
        messages: List<ChatMessage>,
        attachments: List<com.fahim.geminiApiComposeStarter.ui.chat.PendingAttachment>,
    ): Result<String> = withContext(Dispatchers.IO) {
        if (attachments.isEmpty()) return@withContext generateConversation(messages)
        var connection: HttpURLConnection? = null
        try {
            val key = apiKey()
            require(key.isNotBlank()) { "Add your Gemini API key in Settings before sending." }
            val attachmentParts = attachments.map { attachment ->
                val bytes = context.contentResolver.openInputStream(Uri.parse(attachment.uri))?.use { input ->
                    input.readBytesLimited(MAX_ATTACHMENT_BYTES)
                } ?: throw IllegalArgumentException("Could not read ${attachment.name}.")
                JSONObject().put("inlineData", JSONObject()
                    .put("mimeType", attachment.mimeType)
                    .put("data", Base64.getEncoder().encodeToString(bytes)))
            }
            val contents = JSONArray()
            messages.forEachIndexed { index, message ->
                val parts = JSONArray().put(JSONObject().put("text", message.text))
                if (index == messages.lastIndex) attachmentParts.forEach(parts::put)
                contents.put(JSONObject().put("role", if (message.role == ChatRole.USER) "user" else "model").put("parts", parts))
            }
            val url = URL(
                "https://generativelanguage.googleapis.com/v1beta/models/" +
                    URLEncoder.encode(modelName(), Charsets.UTF_8.name()) +
                    ":generateContent?key=" + URLEncoder.encode(key, Charsets.UTF_8.name()),
            )
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"; connectTimeout = 30_000; readTimeout = 120_000; doOutput = true
                setRequestProperty("Content-Type", "application/json")
            }
            connection.outputStream.bufferedWriter().use { it.write(JSONObject().put("contents", contents).toString()) }
            if (connection.responseCode !in 200..299) throw IllegalStateException("Gemini could not process these attachments.")
            val response = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            val parts = response.getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts")
            val answer = (0 until parts.length()).asSequence().mapNotNull { parts.getJSONObject(it).optString("text").takeIf(String::isNotBlank) }
                .joinToString("\n").takeIf(String::isNotBlank)
                ?: throw IllegalStateException("Gemini returned an empty response. Try again.")
            Result.success(answer)
        } catch (error: CancellationException) { throw error }
        catch (error: Exception) {
            Result.failure(IllegalStateException(error.message?.takeIf { it.startsWith("Could not read") || it.startsWith("Add your") }
                ?: "Could not process the attachment. Check its type, size, connection, and model access."))
        } finally { connection?.disconnect() }
    }

    override suspend fun generateImage(prompt: String): Result<GeneratedImage> = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val key = apiKey()
            require(key.isNotBlank()) { "Add your Gemini API key in Settings before creating an image." }
            val url = URL(
                "https://generativelanguage.googleapis.com/v1beta/models/" +
                    "gemini-2.5-flash-image:generateContent?key=" +
                    URLEncoder.encode(key, Charsets.UTF_8.name()),
            )
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 30_000
                readTimeout = 120_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
            }
            val request = JSONObject()
                .put("contents", JSONArray().put(JSONObject().put("parts", JSONArray().put(JSONObject().put("text", prompt)))))
                .put("generationConfig", JSONObject().put("responseModalities", JSONArray().put("IMAGE")))
            connection.outputStream.bufferedWriter().use { it.write(request.toString()) }
            val status = connection.responseCode
            if (status !in 200..299) throw IllegalStateException("The image request was rejected ($status). Check your API key, quota, and image-model access.")
            val response = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            val parts = response.getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts")
            val image = (0 until parts.length()).asSequence()
                .map { parts.getJSONObject(it).optJSONObject("inlineData") }
                .firstOrNull { it != null }
                ?: throw IllegalStateException("Gemini did not return an image. Try a more specific prompt.")
            val encoded = image.getString("data")
            Result.success(GeneratedImage(Base64.getDecoder().decode(encoded), image.optString("mimeType", "image/png")))
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Result.failure(IllegalStateException(
                if (error is IllegalArgumentException) error.message
                else "Could not create the image. Check your connection, API key, image-model access, and quota, then retry.",
            ))
        } finally {
            connection?.disconnect()
        }
    }

    private fun java.io.InputStream.readBytesLimited(limit: Int): ByteArray {
        val output = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val read = read(buffer)
            if (read < 0) return output.toByteArray()
            if (output.size() + read > limit) throw IllegalArgumentException("The selected file is larger than 15 MB.")
            output.write(buffer, 0, read)
        }
    }

    private companion object { const val MAX_ATTACHMENT_BYTES = 15 * 1024 * 1024 }
}
