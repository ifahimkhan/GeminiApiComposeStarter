package com.example.c031_geminiapicompose.data

import android.util.Log
import com.example.c031_geminiapicompose.data.local.ChatMessageDao
import com.example.c031_geminiapicompose.data.local.ChatMessageEntity
import com.example.c031_geminiapicompose.security.CryptoManager
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

private const val TAG = "GeminiRepository"
private const val DEFAULT_MODEL = "gemini-1.5-flash"

class GeminiRepositoryImpl(
    encryptedApiKey: CryptoManager.EncryptedResult,
    private val chatMessageDao: ChatMessageDao,
    modelName: String = DEFAULT_MODEL,
) : GeminiRepository {

    // Decrypt API key in memory only when initializing the Gemini model instance
    private val decryptedKeyInStore: String = CryptoManager.decrypt(encryptedApiKey)
    private val model: GenerativeModel? = if (decryptedKeyInStore.isNotBlank()) {
        GenerativeModel(modelName = modelName, apiKey = decryptedKeyInStore)
    } else {
        null
    }

    override fun getMessagesFlow(): Flow<List<ChatMessage>> {
        return chatMessageDao.getAllMessages().map { entities ->
            entities.map { entity ->
                ChatMessage(
                    id = entity.id,
                    text = entity.text,
                    isUser = entity.sender == "USER",
                    timestamp = entity.timestamp
                )
            }
        }
    }

    override suspend fun generateAndSaveResponse(prompt: String): Result<String> = withContext(Dispatchers.IO) {
        if (model == null) {
            return@withContext Result.failure(IllegalStateException("GEMINI_API_KEY is missing or invalid."))
        }

        // Save user prompt to Room database
        chatMessageDao.insertMessage(
            ChatMessageEntity(text = prompt, sender = "USER")
        )

        try {
            val response = model.generateContent(prompt)
            val text = response.text?.takeIf { it.isNotBlank() }
            if (text != null) {
                // Save Gemini response to Room database
                chatMessageDao.insertMessage(
                    ChatMessageEntity(text = text, sender = "GEMINI")
                )
                Result.success(text)
            } else {
                val errorMsg = "Empty response received from Gemini"
                Result.failure(IllegalStateException(errorMsg))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "generateContent failed", e)
            Result.failure(e)
        }
    }

    override suspend fun clearHistory() {
        withContext(Dispatchers.IO) {
            chatMessageDao.clearAllMessages()
        }
    }
}
