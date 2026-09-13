package com.fahim.geminiApiComposeStarter.data

import android.util.Log
import com.fahim.geminiApiComposeStarter.data.local.ChatMessageDao
import com.fahim.geminiApiComposeStarter.data.local.ChatMessageEntity
import com.fahim.geminiApiComposeStarter.data.local.UserPreferencesRepository
import com.fahim.geminiApiComposeStarter.security.KeyEncryptionManager
import com.fahim.geminiApiComposeStarter.ui.theme.ThemeMode
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

private const val TAG = "GeminiRepository"
private const val DEFAULT_MODEL = "gemini-3.6-flash"

class GeminiRepositoryImpl(
    private val chatMessageDao: ChatMessageDao? = null,
    private val userPreferencesRepository: UserPreferencesRepository? = null,
    private val rawApiKey: String = "",
    private val modelName: String = DEFAULT_MODEL,
) : GeminiRepository {

    private suspend fun getOrDecryptApiKey(): String {
        if (userPreferencesRepository != null) {
            try {
                val storedEncryptedKey = userPreferencesRepository.encryptedApiKey.firstOrNull().orEmpty()
                if (storedEncryptedKey.isNotEmpty()) {
                    val decrypted = KeyEncryptionManager.decrypt(storedEncryptedKey)
                    if (decrypted.isNotEmpty()) return decrypted
                }
                if (rawApiKey.isNotBlank()) {
                    val encrypted = KeyEncryptionManager.encrypt(rawApiKey)
                    if (encrypted.isNotEmpty()) {
                        userPreferencesRepository.saveEncryptedApiKey(encrypted)
                    }
                    return rawApiKey
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to manage encrypted API key", e)
            }
        }
        return rawApiKey
    }

    override fun getMessages(): Flow<List<ChatMessage>> {
        return chatMessageDao?.getAllMessages()?.map { entities ->
            entities.map { entity ->
                ChatMessage(
                    id = entity.id,
                    sender = if (entity.sender == "USER") ChatSender.USER else ChatSender.ASSISTANT,
                    content = entity.content,
                    timestamp = entity.timestamp,
                )
            }
        } ?: emptyFlow()
    }

    override suspend fun sendMessage(prompt: String): Result<String> = try {
        // Save user message to database
        chatMessageDao?.insertMessage(
            ChatMessageEntity(
                sender = "USER",
                content = prompt,
            )
        )

        val apiKey = getOrDecryptApiKey()
        val model = GenerativeModel(modelName = modelName, apiKey = apiKey)
        val response = model.generateContent(prompt)
        val text = response.text?.takeIf { it.isNotBlank() }
        if (text != null) {
            // Save assistant message to database
            chatMessageDao?.insertMessage(
                ChatMessageEntity(
                    sender = "ASSISTANT",
                    content = text,
                )
            )
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
        chatMessageDao?.clearAllMessages()
    }

    override fun getThemeMode(): Flow<ThemeMode> {
        return userPreferencesRepository?.themeMode ?: flowOf(ThemeMode.SYSTEM)
    }

    override suspend fun setThemeMode(themeMode: ThemeMode) {
        userPreferencesRepository?.saveThemeMode(themeMode)
    }
}
