package com.silas.geminichat.data

import com.silas.geminichat.model.ChatMessage
import kotlinx.coroutines.flow.Flow

/** Persists past conversations so chat history survives app restarts. */
interface ChatHistoryRepository {
    fun observeMessages(): Flow<List<ChatMessage>>
    suspend fun addMessage(message: ChatMessage)
    suspend fun clearHistory()
}
