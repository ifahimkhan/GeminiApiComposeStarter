package com.fahim.geminiApiComposeStarter.data.local

import kotlinx.coroutines.flow.Flow

interface ChatHistoryRepository {
    fun observeMessages(): Flow<List<ChatMessage>>
    suspend fun addMessage(author: MessageAuthor, text: String)
    suspend fun clearHistory()
}
