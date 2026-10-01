package com.fahim.geminiApiComposeStarter.data

import com.fahim.geminiApiComposeStarter.data.local.ChatMessageDao
import com.fahim.geminiApiComposeStarter.data.local.ChatMessageEntity
import com.fahim.geminiApiComposeStarter.model.ChatMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface ChatHistoryRepository {
    fun getMessages(): Flow<List<ChatMessage>>
    suspend fun saveUserMessage(text: String): Long
    suspend fun saveGeminiMessage(text: String): Long
    suspend fun clearHistory()
}

class ChatHistoryRepositoryImpl(
    private val dao: ChatMessageDao,
) : ChatHistoryRepository {

    override fun getMessages(): Flow<List<ChatMessage>> {
        return dao.getAllMessages().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun saveUserMessage(text: String): Long {
        val entity = ChatMessageEntity(
            text = text,
            isUser = true,
            timestamp = System.currentTimeMillis(),
        )
        return dao.insertMessage(entity)
    }

    override suspend fun saveGeminiMessage(text: String): Long {
        val entity = ChatMessageEntity(
            text = text,
            isUser = false,
            timestamp = System.currentTimeMillis(),
        )
        return dao.insertMessage(entity)
    }

    override suspend fun clearHistory() {
        dao.clearAll()
    }
}
