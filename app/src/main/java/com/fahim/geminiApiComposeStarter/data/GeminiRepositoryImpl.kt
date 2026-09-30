package com.fahim.geminiApiComposeStarter.data

import android.util.Log
import com.fahim.geminiApiComposeStarter.data.local.ChatMessageDao
import com.fahim.geminiApiComposeStarter.data.local.toDomainModel
import com.fahim.geminiApiComposeStarter.data.local.toEntity
import com.fahim.geminiApiComposeStarter.data.security.SecureApiKeyStorage
import com.fahim.geminiApiComposeStarter.ui.chat.ChatMessage
import com.fahim.geminiApiComposeStarter.ui.chat.ChatSession
import com.fahim.geminiApiComposeStarter.ui.chat.Participant
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

private const val TAG = "GeminiRepository"
private const val PRIMARY_MODEL = "gemini-3.5-flash"
private const val FALLBACK_MODEL = "gemini-2.5-flash" // remove after 2026-10-16

class GeminiRepositoryImpl(
    private val apiKey: String,
    private val chatMessageDao: ChatMessageDao? = null,
    private val secureApiKeyStorage: SecureApiKeyStorage? = null,
    private var modelName: String = PRIMARY_MODEL,
) : GeminiRepository {

    override fun getChatHistory(conversationId: String): Flow<List<ChatMessage>> {
        return chatMessageDao?.getMessagesForConversation(conversationId)?.map { entities ->
            entities.map { it.toDomainModel() }
        } ?: emptyFlow()
    }

    override fun getChatSessions(): Flow<List<ChatSession>> {
        return chatMessageDao?.getChatSessions()?.map { summaries ->
            summaries.map { summary ->
                val rawTitle = summary.title.orEmpty().ifBlank { "New Chat" }
                val truncatedTitle = if (rawTitle.length > 32) "${rawTitle.take(32)}..." else rawTitle
                ChatSession(
                    id = summary.id,
                    title = truncatedTitle,
                    updatedAt = summary.updatedAt ?: System.currentTimeMillis(),
                )
            }
        } ?: emptyFlow()
    }

    override suspend fun saveMessage(message: ChatMessage) {
        withContext(Dispatchers.IO) {
            chatMessageDao?.insertMessage(message.toEntity())
        }
    }

    override suspend fun deleteConversation(conversationId: String) {
        withContext(Dispatchers.IO) {
            chatMessageDao?.deleteConversation(conversationId)
        }
    }

    override suspend fun clearChatHistory() {
        withContext(Dispatchers.IO) {
            chatMessageDao?.clearAllMessages()
        }
    }

    override suspend fun generateText(
        prompt: String,
        conversationHistory: List<ChatMessage>,
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val activeKey = secureApiKeyStorage?.getDecryptedApiKey()?.takeIf { it.isNotBlank() } ?: apiKey
            if (activeKey.isBlank()) {
                return@withContext Result.failure(IllegalStateException("Unable to initialize secure Gemini configuration."))
            }

            val resultText = executeModelGeneration(modelName, activeKey, prompt, conversationHistory)
            if (resultText != null) {
                return@withContext Result.success(resultText)
            } else {
                return@withContext Result.failure(IllegalStateException("Empty response from Gemini"))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "generateContent failed for model $modelName")

            if (modelName == PRIMARY_MODEL) {
                try {
                    val activeKey = secureApiKeyStorage?.getDecryptedApiKey()?.takeIf { it.isNotBlank() } ?: apiKey
                    val fallbackText = executeModelGeneration(FALLBACK_MODEL, activeKey, prompt, conversationHistory)
                    if (fallbackText != null) {
                        modelName = FALLBACK_MODEL
                        return@withContext Result.success(fallbackText)
                    }
                } catch (fallbackException: CancellationException) {
                    throw fallbackException
                } catch (fallbackException: Exception) {
                    Log.e(TAG, "Fallback model $FALLBACK_MODEL also failed")
                }
            }

            val sanitizedMessage = sanitizeError(e)
            return@withContext Result.failure(IllegalStateException(sanitizedMessage))
        }
    }

    private suspend fun executeModelGeneration(
        targetModelName: String,
        activeApiKey: String,
        prompt: String,
        history: List<ChatMessage>,
    ): String? {
        val model = GenerativeModel(modelName = targetModelName, apiKey = activeApiKey)
        val validHistory = history.filter { !it.isError }
        val response = if (validHistory.isNotEmpty()) {
            val historyContent = validHistory.map { msg ->
                content(role = if (msg.participant == Participant.USER) "user" else "model") {
                    text(msg.text)
                }
            }
            val chat = model.startChat(history = historyContent)
            chat.sendMessage(prompt)
        } else {
            model.generateContent(prompt)
        }
        return response.text?.takeIf { it.isNotBlank() }
    }

    private fun sanitizeError(e: Exception): String {
        val msg = e.message.orEmpty()
        return when {
            msg.contains("404") || msg.contains("NOT_FOUND") ->
                "The requested Gemini model is currently unavailable on the server. Please try again shortly."

            msg.contains("503") || msg.contains("UNAVAILABLE") || msg.contains("high demand") ->
                "Gemini is currently experiencing high demand. Please try again in a few moments."

            msg.contains("MissingFieldException") || msg.contains("GRpcError") ->
                "Received an unexpected response from Gemini server. Please try again."

            msg.isNotBlank() && !msg.contains("kotlinx.serialization") && !msg.contains("KeyStore") ->
                msg

            else ->
                "Unable to connect to Gemini API. Please check your network connection."
        }
    }
}
