package com.fahim.geminiApiComposeStarter.data

import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow

private const val TAG = "GeminiRepository"
private const val DEFAULT_MODEL = "gemini-1.5-flash"

class GeminiRepositoryImpl(
    private val chatDao: ChatDao,
    private val securityManager: SecurityManager,
    private val fallbackApiKey: String,
    modelName: String = DEFAULT_MODEL,
) : GeminiRepository {

    private val model: GenerativeModel by lazy {
        val key = securityManager.getApiKey() ?: run {
            securityManager.saveApiKey(fallbackApiKey)
            fallbackApiKey
        }
        GenerativeModel(modelName = modelName, apiKey = key)
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

    override fun getChatHistory(): Flow<List<ChatEntity>> = chatDao.getAllMessages()

    override suspend fun saveMessage(role: String, content: String) {
        chatDao.insertMessage(ChatEntity(role = role, content = content))
    }

    override suspend fun clearHistory() {
        chatDao.clearHistory()
    }
}
