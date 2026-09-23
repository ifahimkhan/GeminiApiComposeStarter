package com.fahim.geminiApiComposeStarter.data

import android.util.Log
import com.fahim.geminiApiComposeStarter.data.local.ChatDao
import com.fahim.geminiApiComposeStarter.data.local.ChatMessageEntity
import com.fahim.geminiApiComposeStarter.data.model.ChatMessage
import com.fahim.geminiApiComposeStarter.data.model.Participant
import com.fahim.geminiApiComposeStarter.data.security.SecureApiKeyStorage
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

private const val TAG = "GeminiRepository"
private const val DEFAULT_MODEL = "gemini-3.6-flash"

class GeminiRepositoryImpl(
    private val secureStorage: SecureApiKeyStorage,
    private val chatDao: ChatDao,
    private val modelName: String = DEFAULT_MODEL,
    fallbackRawApiKey: String = "",
) : GeminiRepository {

    // Decrypt the API key in memory only at the moment GenerativeModel is initialized.
    // If not yet saved in secure storage, encrypt and persist on first launch.
    private val decryptedKey: String by lazy {
        var key = secureStorage.getDecryptedApiKey()
        if (key.isBlank() && fallbackRawApiKey.isNotBlank()) {
            secureStorage.saveApiKey(fallbackRawApiKey)
            key = secureStorage.getDecryptedApiKey()
        }
        key
    }

    private val model: GenerativeModel by lazy {
        GenerativeModel(modelName = modelName, apiKey = decryptedKey)
    }

    override val messagesStream: Flow<List<ChatMessage>> =
        chatDao.getAllMessages().map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun generateText(prompt: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (decryptedKey.isBlank()) {
                return@withContext Result.failure(IllegalStateException("Gemini API key is not configured."))
            }
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
            Log.e(TAG, "generateContent failed: ${e.localizedMessage}")
            Result.failure(e)
        }
    }

    override suspend fun sendMessage(prompt: String): Result<String> = withContext(Dispatchers.IO) {
        val userMessage = ChatMessage(
            text = prompt,
            participant = Participant.USER,
        )
        chatDao.insertMessage(ChatMessageEntity.fromDomain(userMessage))

        val result = generateText(prompt)
        result.fold(
            onSuccess = { responseText ->
                val geminiMessage = ChatMessage(
                    text = responseText,
                    participant = Participant.GEMINI,
                )
                chatDao.insertMessage(ChatMessageEntity.fromDomain(geminiMessage))
            },
            onFailure = { error ->
                val errorMessage = ChatMessage(
                    text = error.message ?: "Failed to generate response. Please try again.",
                    participant = Participant.GEMINI,
                    isError = true,
                )
                chatDao.insertMessage(ChatMessageEntity.fromDomain(errorMessage))
            },
        )
        result
    }

    override suspend fun clearHistory(): Unit = withContext(Dispatchers.IO) {
        chatDao.clearAllMessages()
        Unit
    }
}
