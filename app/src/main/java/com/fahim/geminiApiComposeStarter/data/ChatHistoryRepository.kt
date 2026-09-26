package com.fahim.geminiApiComposeStarter.data

import com.fahim.geminiApiComposeStarter.ui.chat.ChatMessage
import kotlinx.coroutines.flow.Flow

/** Persists the conversation so it survives app restarts. */
interface ChatHistoryRepository {
    fun observeMessages(): Flow<List<ChatMessage>>
    suspend fun addMessage(text: String, isFromUser: Boolean)
}
