package com.fahim.geminiApiComposeStarter.data.history

import com.fahim.geminiApiComposeStarter.data.ChatMessage
import com.fahim.geminiApiComposeStarter.data.local.ChatMessageDao
import com.fahim.geminiApiComposeStarter.data.local.ChatMessageEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Persists the conversation so it survives process death and app restarts. */
interface ChatHistoryRepository {
    fun observeMessages(): Flow<List<ChatMessage>>
    suspend fun addMessage(message: ChatMessage)
}

class ChatHistoryRepositoryImpl(private val dao: ChatMessageDao) : ChatHistoryRepository {

    override fun observeMessages(): Flow<List<ChatMessage>> =
        dao.observeMessages().map { entities -> entities.map { it.toDomain() } }

    override suspend fun addMessage(message: ChatMessage) {
        dao.insert(message.toEntity())
    }
}

private fun ChatMessageEntity.toDomain() = ChatMessage(
    id = id,
    text = text,
    isFromUser = isFromUser,
    isError = isError,
    timestamp = timestamp,
)

private fun ChatMessage.toEntity() = ChatMessageEntity(
    id = id,
    text = text,
    isFromUser = isFromUser,
    isError = isError,
    timestamp = timestamp,
)
