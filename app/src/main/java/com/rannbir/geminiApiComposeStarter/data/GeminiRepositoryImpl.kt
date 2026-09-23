package com.rannbir.geminiApiComposeStarter.data

import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.rannbir.geminiApiComposeStarter.data.local.ChatDao
import com.rannbir.geminiApiComposeStarter.data.local.ChatMessageEntity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

private const val TAG = "GeminiRepository"
private const val DEFAULT_MODEL = "gemini-3.6-flash"

class GeminiRepositoryImpl(
    private val apiKeyProvider: () -> String,
    private val chatDao: ChatDao,
    private val modelName: String = DEFAULT_MODEL,
) : GeminiRepository {

    // Decrypts API key in-memory only when creating the GenerativeModel instance
    private val model by lazy {
        val key = apiKeyProvider()
        GenerativeModel(modelName = modelName, apiKey = key)
    }

    override fun getChatMessages(): Flow<List<ChatMessageEntity>> {
        return chatDao.getAllMessagesFlow()
    }

    override suspend fun sendMessage(prompt: String): Result<String> = withContext(Dispatchers.IO) {
        val trimmedPrompt = prompt.trim()
        if (trimmedPrompt.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("Prompt cannot be empty"))
        }

        try {
            // 1. Fetch current conversation history to build memory context
            val priorMessages = chatDao.getAllMessages()

            // 2. Persist user's query into Room database
            chatDao.insertMessage(
                ChatMessageEntity(
                    text = trimmedPrompt,
                    isFromUser = true,
                    timestamp = System.currentTimeMillis()
                )
            )

            // 3. Format history for multi-turn Gemini conversation memory
            val historyContent = priorMessages.map { message ->
                content(role = if (message.isFromUser) "user" else "model") {
                    text(message.text)
                }
            }

            // 4. Send request with automatic retry on temporary high-demand (503) spikes
            var lastException: Exception? = null
            for (attempt in 1..2) {
                try {
                    val chat = model.startChat(history = historyContent)
                    val response = chat.sendMessage(trimmedPrompt)
                    val responseText = response.text?.takeIf { it.isNotBlank() }

                    if (responseText != null) {
                        // 5. Persist Gemini's response into Room database
                        chatDao.insertMessage(
                            ChatMessageEntity(
                                text = responseText,
                                isFromUser = false,
                                timestamp = System.currentTimeMillis()
                            )
                        )
                        return@withContext Result.success(responseText)
                    } else {
                        return@withContext Result.failure(IllegalStateException("Empty response received from Gemini"))
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    lastException = e
                    Log.w(TAG, "Attempt $attempt failed: ${e.message}")
                    if (attempt < 2 && (e.message?.contains("503") == true || e.message?.contains("high demand") == true)) {
                        delay(1500)
                    }
                }
            }

            val sanitizedError = sanitizeErrorMessage(lastException ?: IllegalStateException("Failed to communicate with Gemini"))
            Log.e(TAG, "Gemini generation failed: $sanitizedError", lastException)
            Result.failure(Exception(sanitizedError))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val sanitized = sanitizeErrorMessage(e)
            Log.e(TAG, "Error in sendMessage: $sanitized", e)
            Result.failure(Exception(sanitized))
        }
    }

    override suspend fun clearChatHistory(): Unit = withContext(Dispatchers.IO) {
        chatDao.clearAllMessages()
    }

    companion object {
        fun sanitizeErrorMessage(error: Throwable): String {
            val raw = error.message ?: return "An unexpected error occurred. Please try again."

            if (raw.contains("503") || raw.contains("high demand", ignoreCase = true) || raw.contains("UNAVAILABLE", ignoreCase = true)) {
                return "Gemini is currently experiencing high demand. Please try again in a few moments."
            }
            if (raw.contains("429") || raw.contains("RESOURCE_EXHAUSTED", ignoreCase = true) || raw.contains("quota", ignoreCase = true)) {
                return "API request limit reached. Please wait a moment before sending another prompt."
            }
            if (raw.contains("400") && raw.contains("API key", ignoreCase = true)) {
                return "Gemini API key is invalid or not configured. Please check local.properties."
            }
            if (raw.contains("timeout", ignoreCase = true) || raw.contains("SocketTimeout", ignoreCase = true)) {
                return "Network connection timed out. Please check your internet connection."
            }

            // Extract clean message from JSON {"message": "..."} if present
            val messageRegex = Regex(""""message"\s*:\s*"([^"]+)"""")
            val match = messageRegex.find(raw)
            if (match != null) {
                return match.groupValues[1]
            }

            val cleanLine = raw.lines().firstOrNull {
                it.isNotBlank() && !it.startsWith("{") && !it.startsWith("Unexpected Response") && !it.contains("MissingFieldException")
            }
            return cleanLine?.take(120) ?: "Failed to generate response. Please try again."
        }
    }
}
