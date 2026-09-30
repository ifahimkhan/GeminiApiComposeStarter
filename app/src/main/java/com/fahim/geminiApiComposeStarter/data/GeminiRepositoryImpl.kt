package com.fahim.geminiApiComposeStarter.data

import android.util.Log
import com.fahim.geminiApiComposeStarter.data.db.ConversationDao
import com.fahim.geminiApiComposeStarter.data.db.ConversationMessage
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

private const val TAG = "GeminiRepository"
private const val DEFAULT_MODEL = "gemini-3.6-flash"

class GeminiRepositoryImpl(
    private val apiKeyProvider: () -> String,
    private val conversationDao: ConversationDao? = null,
    private val modelName: String = DEFAULT_MODEL,
) : GeminiRepository {

    constructor(
        apiKey: String,
        conversationDao: ConversationDao? = null,
        modelName: String = DEFAULT_MODEL,
    ) : this(apiKeyProvider = { apiKey }, conversationDao = conversationDao, modelName = modelName)

    private val generativeModel: GenerativeModel by lazy {
        GenerativeModel(modelName = modelName, apiKey = apiKeyProvider())
    }

    override fun getChatHistory(): Flow<List<ConversationMessage>> {
        return conversationDao?.getConversationStream() ?: flowOf(emptyList())
    }

    override suspend fun sendMessage(prompt: String): Result<String> {
        val userItem = ConversationMessage(content = prompt, isUserSent = true)
        conversationDao?.saveMessage(userItem)

        val result = generateText(prompt)
        result.onSuccess { textResponse ->
            val aiItem = ConversationMessage(content = textResponse, isUserSent = false)
            conversationDao?.saveMessage(aiItem)
        }
        return result
    }

    override suspend fun generateText(prompt: String): Result<String> = try {
        val response = generativeModel.generateContent(prompt)
        val responseText = response.text?.takeIf { it.isNotBlank() }
        if (responseText != null) {
            Result.success(responseText)
        } else {
            Result.failure(IllegalStateException("Empty response from Gemini model"))
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.e(TAG, "generateContent request failed", e)
        Result.failure(e)
    }

    override suspend fun clearHistory() {
        conversationDao?.clearConversation()
    }
}
