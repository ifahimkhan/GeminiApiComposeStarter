package com.fahim.geminiApiComposeStarter.data

import com.fahim.geminiApiComposeStarter.data.local.ChatMessageEntity
import kotlinx.coroutines.flow.Flow

interface ChatHistoryRepository {
    fun observeMessages(): Flow<List<ChatMessageEntity>>

    suspend fun insertMessage(message: ChatMessageEntity)
}