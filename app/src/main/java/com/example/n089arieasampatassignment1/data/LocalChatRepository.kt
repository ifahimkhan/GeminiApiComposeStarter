package com.example.n089arieasampatassignment1.data

import com.example.n089arieasampatassignment1.model.ChatMessage
import kotlinx.coroutines.flow.Flow

class LocalChatRepository(
    private val chatDao: ChatDao
) {

    fun getMessages(): Flow<List<ChatMessage>> {
        return chatDao.getAllMessages()
    }

    suspend fun saveMessage(
        message: ChatMessage
    ) {
        chatDao.insertMessage(message)
    }

    suspend fun clearMessages() {
        chatDao.deleteAllMessages()
    }
}
