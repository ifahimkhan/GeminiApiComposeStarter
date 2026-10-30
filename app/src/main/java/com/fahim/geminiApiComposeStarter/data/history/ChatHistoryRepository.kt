package com.fahim.geminiApiComposeStarter.data.history

import com.fahim.geminiApiComposeStarter.data.local.ChatDao
import com.fahim.geminiApiComposeStarter.data.local.ChatMessageEntity
import com.fahim.geminiApiComposeStarter.ui.chat.ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

interface ChatHistoryRepository {
    val allMessages: Flow<List<ChatMessage>>
    suspend fun saveMessage(message: ChatMessage)
    suspend fun clearHistory()
}

class ChatHistoryRepositoryImpl(
    private val chatDao: ChatDao,
) : ChatHistoryRepository {

    override val allMessages: Flow<List<ChatMessage>> =
        chatDao.getAllMessages().map { entities ->
            entities.map { entity ->
                ChatMessage(
                    id = entity.id,
                    text = entity.text,
                    isUser = entity.isUser,
                    timestamp = entity.timestamp,
                )
            }
        }

    override suspend fun saveMessage(message: ChatMessage) {
        withContext(Dispatchers.IO) {
            val entity = ChatMessageEntity(
                id = message.id,
                text = message.text,
                isUser = message.isUser,
                timestamp = message.timestamp,
            )
            chatDao.insertMessage(entity)
        }
    }

    override suspend fun clearHistory() {
        withContext(Dispatchers.IO) {
            chatDao.deleteAllMessages()
        }
    }
}
