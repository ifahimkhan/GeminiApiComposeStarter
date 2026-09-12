package com.fahim.geminiApiComposeStarter.data.local

import kotlinx.coroutines.flow.Flow

class ChatHistoryRepository(
    private val dao: ChatMessageDao
) {

    fun getMessages(): Flow<List<ChatMessageEntity>> {
        return dao.getAllMessages()
    }

    suspend fun insertMessage(message: ChatMessageEntity) {
        dao.insertMessage(message)
    }

    suspend fun clearMessages() {
        dao.clearMessages()
    }
}