package com.fahim.geminiApiComposeStarter.data

import android.util.Log
import com.fahim.geminiApiComposeStarter.data.db.ChatDao
import com.fahim.geminiApiComposeStarter.data.db.MessageEntity
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

private const val TAG = "GeminiRepository"
private const val DEFAULT_MODEL = "gemini-1.5-flash"

class GeminiRepositoryImpl(
    private val apiKeyProvider: () -> String,
    private val chatDao: ChatDao? = null,
    private val modelName: String = DEFAULT_MODEL,
) : GeminiRepository {

    constructor(
        apiKey: String,
        chatDao: ChatDao? = null,
        modelName: String = DEFAULT_MODEL,
    ) : this(apiKeyProvider = { apiKey }, chatDao = chatDao, modelName = modelName)

    private val model: GenerativeModel by lazy {
        GenerativeModel(modelName = modelName, apiKey = apiKeyProvider())
    }

    override fun getChatHistory(): Flow<List<MessageEntity>> {
        return chatDao?.getAllMessages() ?: flowOf(emptyList())
    }

    override suspend fun sendMessage(prompt: String): Result<String> {
        val userMessage = MessageEntity(messageText = prompt, isUser = true)
        chatDao?.insertMessage(userMessage)

        val result = generateText(prompt)
        result.onSuccess { responseText ->
            val aiMessage = MessageEntity(messageText = responseText, isUser = false)
            chatDao?.insertMessage(aiMessage)
        }
        return result
    }

    override suspend fun generateText(prompt: String): Result<String> = try {
        val response = model.generateContent(prompt)
        val text = response.text?.takeIf { it.isNotBlank() }
        if (text != null) {
            Result.success(text)
        } else {
            Result.failure(IllegalStateException("Empty response from Gemini"))
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.e(TAG, "generateContent failed", e)
        Result.failure(e)
    }

    override suspend fun clearHistory() {
        chatDao?.deleteAllMessages()
    }
}
