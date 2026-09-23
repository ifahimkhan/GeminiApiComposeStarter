package com.fahim.geminiApiComposeStarter.data

import com.fahim.geminiApiComposeStarter.data.local.ChatMessageEntity
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for interacting with the Gemini API and persisting chat history.
 */
interface GeminiRepository {

    /** Reactive stream of all chat messages ordered chronologically. */
    val messages: Flow<List<ChatMessageEntity>>

    /** Checks whether an API key is available and configured. */
    fun hasApiKey(): Boolean

    /** Generates a text response for the given [prompt] and persists both user and model messages. */
    suspend fun sendMessage(prompt: String, modelName: String = DEFAULT_MODEL): Result<String>

    /** Clears all stored chat messages from persistent storage. */
    suspend fun clearHistory()

    /** Deletes an individual message by [id]. */
    suspend fun deleteMessage(id: Long)

    companion object {
        const val DEFAULT_MODEL = "gemini-3.6-flash"
    }
}
