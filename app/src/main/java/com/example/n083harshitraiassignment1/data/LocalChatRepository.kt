package com.example.n083harshitraiassignment1.data

import com.example.n083harshitraiassignment1.model.ChatMessage
import kotlinx.coroutines.flow.Flow

open class LocalChatRepository(
    private val chatDao: ChatDao
) {

    open fun getMessages(): Flow<List<ChatMessage>> {
        return chatDao.getAllMessages()
    }

    open suspend fun saveMessage(
        message: ChatMessage
    ) {
        chatDao.insertMessage(message)
    }

    open suspend fun clearMessages() {
        chatDao.deleteAllMessages()
    }
}