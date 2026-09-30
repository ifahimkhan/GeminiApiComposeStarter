package com.fahim.geminiApiComposeStarter.data

import android.util.Log
import com.fahim.geminiApiComposeStarter.data.local.ChatDao
import com.fahim.geminiApiComposeStarter.data.local.ChatMessageEntity
import com.fahim.geminiApiComposeStarter.data.remote.GeminiProxyClient
import com.fahim.geminiApiComposeStarter.security.ApiKeyManager
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val TAG = "GeminiRepository"
private const val DEFAULT_MODEL = "gemini-3.6-flash"

class GeminiRepositoryImpl(
    private val chatDao: ChatDao,
    private val apiKeyManager: ApiKeyManager,
    private val proxyClient: GeminiProxyClient,
) : GeminiRepository {

    override val messages: Flow<List<ChatMessage>> = chatDao.observeMessages().map { entities ->
        entities.map { it.toDomain() }
    }

    override suspend fun generateText(prompt: String, modelName: String): Result<String> {
        return try {
            val apiKey = apiKeyManager.getApiKey()
            if (apiKey.isBlank()) {
                return Result.failure(IllegalStateException("API Key is missing"))
            }
            val model = GenerativeModel(modelName = modelName, apiKey = apiKey)
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
    }

    override suspend fun clearChat() {
        chatDao.clear()
    }

    override suspend fun addMessage(text: String, isFromUser: Boolean, isError: Boolean) {
        chatDao.insert(ChatMessageEntity(text = text, isFromUser = isFromUser, isError = isError))
    }
}
