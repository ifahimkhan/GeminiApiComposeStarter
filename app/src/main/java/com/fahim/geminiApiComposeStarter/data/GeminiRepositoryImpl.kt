package com.fahim.geminiApiComposeStarter.data

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "GeminiRepository"
const val DEFAULT_MODEL = "gemini-3.8-flash"

class GeminiRepositoryImpl(
    private val apiKeyProvider: () -> String,
    private val defaultModelName: String = DEFAULT_MODEL,
) : GeminiRepository {

    constructor(apiKey: String, defaultModelName: String = DEFAULT_MODEL) : this(
        apiKeyProvider = { apiKey },
        defaultModelName = defaultModelName
    )

    private val modelsCache = mutableMapOf<String, GenerativeModel>()

    private fun getModel(modelName: String): GenerativeModel = synchronized(modelsCache) {
        val target = modelName.trim().ifEmpty { defaultModelName }
        modelsCache.getOrPut(target) {
            // Decrypts/retrieves in memory only at the moment GenerativeModel is created
            val activeKey = apiKeyProvider()
            GenerativeModel(modelName = target, apiKey = activeKey)
        }
    }

    override suspend fun generateChatResponse(
        context: Context,
        history: List<ChatMessage>,
        prompt: String,
        attachments: List<ChatAttachment>,
        modelName: String,
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val model = getModel(modelName)

            // Convert conversation history into Gemini Content objects
            val historyContent = history.map { message ->
                content(role = if (message.isUser) "user" else "model") {
                    if (message.isUser) {
                        for (att in message.attachments) {
                            if (att.isImage) {
                                val uri = Uri.parse(att.uriString)
                                val bitmap = AttachmentUtils.loadScaledBitmap(context, uri)
                                if (bitmap != null) {
                                    image(bitmap)
                                }
                            }
                        }
                    }
                    text(message.text)
                }
            }

            val chat = model.startChat(history = historyContent)

            // Build new message content (with prompt + current attachments)
            val currentMessageContent = content(role = "user") {
                var augmentedPrompt = prompt
                for (att in attachments) {
                    val uri = Uri.parse(att.uriString)
                    if (att.isImage) {
                        val bitmap = AttachmentUtils.loadScaledBitmap(context, uri)
                        if (bitmap != null) {
                            image(bitmap)
                        }
                    } else {
                        val fileText = AttachmentUtils.readFileText(context, uri)
                        if (fileText.isNotBlank()) {
                            augmentedPrompt += "\n\n[Attached File: ${att.name}]\n$fileText"
                        }
                    }
                }
                text(augmentedPrompt)
            }

            val response = chat.sendMessage(currentMessageContent)
            val text = response.text?.takeIf { it.isNotBlank() }
            if (text != null) {
                Result.success(text)
            } else {
                Result.failure(IllegalStateException("Empty response from Gemini"))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "generateChatResponse failed for model $modelName", e)
            Result.failure(e)
        }
    }
}
