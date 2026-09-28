package com.fahim.geminiApiComposeStarter.data

import com.fahim.geminiApiComposeStarter.data.local.ChatDao
import com.fahim.geminiApiComposeStarter.data.local.toDomain
import com.fahim.geminiApiComposeStarter.data.local.toEntity
import com.fahim.geminiApiComposeStarter.data.model.ChatMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Interface for chat history persistence.
 */
interface ChatHistoryRepository {
    fun getMessages(): Flow<List<ChatMessage>>
    suspend fun insertMessage(message: ChatMessage): Long
    suspend fun updateMessage(message: ChatMessage)
    suspend fun deleteMessage(id: Long)
    suspend fun clearHistory()
}

/**
 * Room-backed implementation of [ChatHistoryRepository].
 */
class RoomChatHistoryRepository(
    private val chatDao: ChatDao,
) : ChatHistoryRepository {

    override fun getMessages(): Flow<List<ChatMessage>> {
        return chatDao.getAllMessages().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun insertMessage(message: ChatMessage): Long {
        return chatDao.insertMessage(message.toEntity())
    }

    override suspend fun updateMessage(message: ChatMessage) {
        chatDao.updateMessage(message.toEntity())
    }

    override suspend fun deleteMessage(id: Long) {
        chatDao.deleteMessage(id)
    }

    override suspend fun clearHistory() {
        chatDao.clearAllMessages()
    }
}
