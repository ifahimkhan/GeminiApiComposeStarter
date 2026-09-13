package com.fahim.geminiApiComposeStarter.data

import com.fahim.geminiApiComposeStarter.data.local.ChatMessageDao
import com.fahim.geminiApiComposeStarter.data.local.ChatMessageEntity
import kotlinx.coroutines.flow.Flow

class ChatHistoryRepository(
    private val dao: ChatMessageDao
) {

    fun getMessages(): Flow<List<ChatMessageEntity>> {
        return dao.getAllMessages()
    }

    suspend fun saveMessage(
        text: String,
        isUser: Boolean
    ) {
        dao.insertMessage(
            ChatMessageEntity(
                text = text,
                isUser = isUser
            )
        )
    }

    suspend fun clearMessages() {
        dao.deleteAllMessages()
    }
}