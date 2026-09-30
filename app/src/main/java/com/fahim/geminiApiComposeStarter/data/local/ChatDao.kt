package com.fahim.geminiApiComposeStarter.data.local

import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for chat messages.
 * Exposes real-time message stream via Kotlin Coroutines Flow.
 */
interface ChatDao {
    fun getAllMessages(): Flow<List<ChatMessageEntity>>
    suspend fun insertMessage(message: ChatMessageEntity): Long
    suspend fun clearAllMessages()
    suspend fun deleteMessage(id: Long)
}
