package com.fahim.geminiApiComposeStarter.data

import android.util.Log
import com.fahim.geminiApiComposeStarter.data.local.ChatDao
import com.fahim.geminiApiComposeStarter.data.local.ChatMessageEntity
import com.fahim.geminiApiComposeStarter.data.model.ChatMessage
import com.fahim.geminiApiComposeStarter.data.preferences.UserPreferences
import com.fahim.geminiApiComposeStarter.data.preferences.UserPreferencesRepository
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

private const val TAG = "GeminiRepository"
private const val DEFAULT_MODEL = "gemini-1.5-flash"

class GeminiRepositoryImpl(
    private val chatDao: ChatDao,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : GeminiRepository {

    override suspend fun generateText(prompt: String): Result<String> = withContext(ioDispatcher) {
        try {
            // Decrypt key in memory only for the moment of API call
            val decryptedKey = userPreferencesRepository.getDecryptedApiKey()
            if (decryptedKey.isBlank()) {
                return@withContext Result.failure(
                    IllegalStateException("GEMINI_API_KEY is missing. Configure it in local.properties or Settings.")
                )
            }

            val preferences = userPreferencesRepository.userPreferencesFlow.first()
            val modelName = preferences.selectedModel.ifEmpty { DEFAULT_MODEL }

            // Tone adaptation
            val systemInstruction = when (preferences.tone) {
                "Concise" -> "You are a helpful assistant. Keep your responses short, direct, and concise."
                "Creative" -> "You are an imaginative and enthusiastic AI assistant. Provide creative, engaging, and comprehensive responses."
                else -> "You are a helpful, respectful, and honest AI assistant."
            }

            val generativeModel = GenerativeModel(
                modelName = modelName,
                apiKey = decryptedKey,
                systemInstruction = com.google.ai.client.generativeai.type.content {
                    text(systemInstruction)
                }
            )

            val response = generativeModel.generateContent(prompt)
            val text = response.text?.takeIf { it.isNotBlank() }

            if (text != null) {
                Result.success(text)
            } else {
                Result.failure(IllegalStateException("Empty response received from Gemini"))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "generateContent failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    override fun getChatHistory(): Flow<List<ChatMessage>> {
        return chatDao.getAllMessages().map { entities ->
            entities.map { entity ->
                ChatMessage(
                    id = entity.id,
                    text = entity.text,
                    isUser = entity.isUser,
                    timestamp = entity.timestamp,
                )
            }
        }
    }

    override suspend fun saveMessage(message: ChatMessage): Long = withContext(ioDispatcher) {
        chatDao.insertMessage(
            ChatMessageEntity(
                id = message.id,
                text = message.text,
                isUser = message.isUser,
                timestamp = message.timestamp,
            )
        )
    }

    override suspend fun clearHistory(): Unit = withContext(ioDispatcher) {
        chatDao.clearAllMessages()
        Unit
    }

    override fun getUserPreferences(): Flow<UserPreferences> {
        return userPreferencesRepository.userPreferencesFlow
    }

    override suspend fun updatePreferences(userName: String, model: String, tone: String) {
        withContext(ioDispatcher) {
            userPreferencesRepository.updatePreferences(userName, model, tone)
        }
    }

    override suspend fun saveCustomApiKey(key: String) {
        withContext(ioDispatcher) {
            userPreferencesRepository.saveEncryptedApiKey(key)
        }
    }

    override suspend fun hasApiKey(): Boolean = withContext(ioDispatcher) {
        userPreferencesRepository.getDecryptedApiKey().isNotBlank()
    }
}
