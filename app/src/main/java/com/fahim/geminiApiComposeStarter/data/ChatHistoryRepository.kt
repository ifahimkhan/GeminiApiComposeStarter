package com.fahim.geminiApiComposeStarter.data

import com.fahim.geminiApiComposeStarter.data.local.ChatMessageDao
import com.fahim.geminiApiComposeStarter.data.local.ChatMessageEntity
import com.fahim.geminiApiComposeStarter.ui.chat.ChatMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Repository interface defining operations for persisting and observing chat conversation history.
 */
interface ChatHistoryRepository {
    fun getMessages(): Flow<List<ChatMessage>>
    suspend fun saveMessage(message: ChatMessage)
    suspend fun clearHistory()
}

/**
 * Room-backed implementation of [ChatHistoryRepository].
 */
class ChatHistoryRepositoryImpl(
    private val chatMessageDao: ChatMessageDao,
) : ChatHistoryRepository {

    override fun getMessages(): Flow<List<ChatMessage>> {
        return chatMessageDao.getAllMessages().map { entities ->
            entities.map { it.toChatMessage() }
        }
    }

    override suspend fun saveMessage(message: ChatMessage) {
        chatMessageDao.insertMessage(ChatMessageEntity.fromChatMessage(message))
    }

    override suspend fun clearHistory() {
        chatMessageDao.clearAllMessages()
    }
}
