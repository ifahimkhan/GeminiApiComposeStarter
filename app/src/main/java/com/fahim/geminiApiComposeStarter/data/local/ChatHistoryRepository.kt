package com.fahim.geminiApiComposeStarter.data.local

import com.fahim.geminiApiComposeStarter.ui.chat.ChatMessage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Abstraction for persistent chat history operations.
 */
interface ChatHistoryRepository {
    fun getChatHistory(): Flow<List<ChatMessage>>
    suspend fun insertMessage(message: ChatMessage)
    suspend fun clearHistory()
}

/**
 * Room-backed implementation of [ChatHistoryRepository].
 */
class ChatHistoryRepositoryImpl(
    private val chatDao: ChatDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ChatHistoryRepository {

    override fun getChatHistory(): Flow<List<ChatMessage>> {
        return chatDao.getAllMessages()
            .map { entities -> entities.map { it.toChatMessage() } }
            .flowOn(ioDispatcher)
    }

    override suspend fun insertMessage(message: ChatMessage) {
        withContext(ioDispatcher) {
            chatDao.insertMessage(ChatMessageEntity.fromChatMessage(message))
        }
    }

    override suspend fun clearHistory() {
        withContext(ioDispatcher) {
            chatDao.clearAllMessages()
        }
    }
}
