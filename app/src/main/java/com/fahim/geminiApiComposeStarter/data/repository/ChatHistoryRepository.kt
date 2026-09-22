package com.fahim.geminiApiComposeStarter.data.repository

import com.fahim.geminiApiComposeStarter.ui.chat.ChatMessage
import kotlinx.coroutines.flow.Flow

interface ChatHistoryRepository {
    fun getMessages(): Flow<List<ChatMessage>>
    suspend fun saveMessage(message: ChatMessage): Result<Unit>
    suspend fun clearHistory(): Result<Unit>
}
