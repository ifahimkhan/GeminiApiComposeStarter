package com.fahim.geminiApiComposeStarter.data

import com.fahim.geminiApiComposeStarter.domain.ChatMessage
import kotlinx.coroutines.flow.Flow

/** Abstraction over local chat history storage so the ViewModel can be unit tested. */
interface ChatHistoryRepository {
    fun observeMessages(): Flow<List<ChatMessage>>
    suspend fun addMessage(message: ChatMessage)
}
