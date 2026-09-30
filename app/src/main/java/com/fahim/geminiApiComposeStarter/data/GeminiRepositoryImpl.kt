package com.fahim.geminiApiComposeStarter.data

import android.util.Log
import com.fahim.geminiApiComposeStarter.data.db.ChatDao
import com.fahim.geminiApiComposeStarter.data.db.ChatMessageEntity
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

private const val TAG = "GeminiRepository"
private const val DEFAULT_MODEL = "gemini-3.6-flash"

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

    override fun getChatHistory(): Flow<List<ChatMessageEntity>> {
        return chatDao?.getAllMessages() ?: flowOf(emptyList())
    }

    override suspend fun sendMessage(prompt: String): Result<String> {
        val userEntity = ChatMessageEntity(text = prompt, isUser = true)
        chatDao?.insertMessage(userEntity)

        val result = generateText(prompt)
        result.onSuccess { responseText ->
            val aiEntity = ChatMessageEntity(text = responseText, isUser = false)
            chatDao?.insertMessage(aiEntity)
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
        chatDao?.clearAllMessages()
    }
}
