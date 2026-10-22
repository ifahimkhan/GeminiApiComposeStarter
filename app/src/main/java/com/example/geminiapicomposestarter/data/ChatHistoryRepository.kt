package com.example.geminiapicomposestarter.data

import com.example.geminiapicomposestarter.data.local.ChatDao
import com.example.geminiapicomposestarter.data.local.ChatMessageEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ChatHistoryRepository(private val chatDao: ChatDao) {
    val messages: Flow<List<ChatMessage>> = chatDao.getAllMessages().map { list ->
        list.map { it.toDomain() }
    }

    suspend fun saveMessage(message: ChatMessage): Long {
        return chatDao.insertMessage(ChatMessageEntity.fromDomain(message))
    }

    suspend fun clearHistory() {
        chatDao.clearHistory()
    }
}
