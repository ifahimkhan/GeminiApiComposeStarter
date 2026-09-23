package com.devraj.geminiassistant.data.local.dao

import com.devraj.geminiassistant.data.local.entity.ChatMessageEntity
import kotlinx.coroutines.flow.Flow

interface ChatMessageDao {
    fun getAllMessagesFlow(): Flow<List<ChatMessageEntity>>
    suspend fun getAllMessages(): List<ChatMessageEntity>
    suspend fun insertMessage(message: ChatMessageEntity): Long
    suspend fun clearAllMessages()
}
