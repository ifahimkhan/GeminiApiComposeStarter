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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
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
    override fun generateTextStream(prompt: String): Flow<String> =
        generateConversationStream(listOf(ChatMessage(0, ChatRole.USER, prompt)))

    override fun generateConversationStream(messages: List<ChatMessage>): Flow<String> = flow {
        val key = apiKey()
        require(key.isNotBlank()) { "Add your Gemini API key in Settings before sending." }
        val model = GenerativeModel(modelName = modelName(), apiKey = key)
        val contents = messages.map { message ->
            content(role = if (message.role == ChatRole.USER) "user" else "model") { text(message.text) }
        }
        model.generateContentStream(*contents.toTypedArray()).collect { response ->
            response.text?.takeIf { it.isNotBlank() }?.let { emit(it) }
        }
    }.catch { error ->
        throw IllegalStateException(
            if (error is IllegalArgumentException) error.message
            else "Could not get a response. Check your connection, API key, model name, and quota, then retry."
        )
    }.flowOn(Dispatchers.IO)

    override fun generateConversationStream(
        messages: List<ChatMessage>,
        attachments: List<com.fahim.geminiApiComposeStarter.ui.chat.PendingAttachment>,
    ): Flow<String> = flow {
        if (attachments.isEmpty()) {
            generateConversationStream(messages).collect { emit(it) }
            return@flow
        }
        val key = apiKey()
        require(key.isNotBlank()) { "Add your Gemini API key in Settings before sending." }
        val model = GenerativeModel(modelName = modelName(), apiKey = key)
        
        val contents = messages.mapIndexed { index, message ->
            content(role = if (message.role == ChatRole.USER) "user" else "model") {
                if (index == messages.lastIndex) {
                    attachments.forEach { attachment ->
                        val bytes = context.contentResolver.openInputStream(Uri.parse(attachment.uri))?.use { input ->
                            input.readBytesLimited(MAX_ATTACHMENT_BYTES)
                        } ?: throw IllegalArgumentException("Could not read ${attachment.name}.")
                        blob(attachment.mimeType, bytes)
                    }
                }
                text(message.text)
            }
        }
        
        model.generateContentStream(*contents.toTypedArray()).collect { response ->
            response.text?.takeIf { it.isNotBlank() }?.let { emit(it) }
        }
    }.catch { error ->
        throw IllegalStateException(error.message?.takeIf { it.startsWith("Could not read") || it.startsWith("Add your") }
            ?: "Could not process the attachment. Check its type, size, connection, and model access.")
    }.flowOn(Dispatchers.IO)

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
