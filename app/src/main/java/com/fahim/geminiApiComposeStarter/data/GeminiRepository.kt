package com.fahim.geminiApiComposeStarter.data

import com.fahim.geminiApiComposeStarter.data.model.ChatMessage
import com.fahim.geminiApiComposeStarter.data.preferences.UserPreferences
import kotlinx.coroutines.flow.Flow

/**
 * Abstraction over Gemini AI operations and data persistence (Room + DataStore).
 */
interface GeminiRepository {
    suspend fun generateText(prompt: String): Result<String>
    fun getChatHistory(): Flow<List<ChatMessage>>
    suspend fun saveMessage(message: ChatMessage): Long
    suspend fun clearHistory()
    fun getUserPreferences(): Flow<UserPreferences>
    suspend fun updatePreferences(userName: String, model: String, tone: String)
    suspend fun saveCustomApiKey(key: String)
    suspend fun hasApiKey(): Boolean
}
