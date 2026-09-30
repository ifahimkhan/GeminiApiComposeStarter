package com.fahim.geminiApiComposeStarter.data

import kotlinx.coroutines.flow.Flow

interface ChatHistoryRepository {
    fun getAllMessages(): Flow<List<ChatMessageEntity>>
    suspend fun addMessage(text: String, isUser: Boolean)
    suspend fun clearHistory()
}
