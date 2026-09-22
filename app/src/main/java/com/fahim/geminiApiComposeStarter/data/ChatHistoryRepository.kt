package com.fahim.geminiApiComposeStarter.data

import com.fahim.geminiApiComposeStarter.data.local.ChatMessageDao
import com.fahim.geminiApiComposeStarter.data.local.ChatMessageEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

interface ChatHistoryRepository {
    val messages: Flow<List<ChatMessage>>
    suspend fun saveMessage(message: ChatMessage)
    suspend fun clearHistory()
}

class ChatHistoryRepositoryImpl(
    private val dao: ChatMessageDao,
) : ChatHistoryRepository {

    override val messages: Flow<List<ChatMessage>> =
        dao.getAllMessages().map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun saveMessage(message: ChatMessage) {
        withContext(Dispatchers.IO) {
            dao.insertMessage(ChatMessageEntity.fromDomain(message))
        }
    }

    override suspend fun clearHistory() {
        withContext(Dispatchers.IO) {
            dao.clearAllMessages()
        }
    }
}
