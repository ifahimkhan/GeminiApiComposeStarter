package com.fahim.geminiApiComposeStarter.data

import com.fahim.geminiApiComposeStarter.data.local.MessageDao
import com.fahim.geminiApiComposeStarter.data.local.MessageEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Persists the conversation so chat history survives app restarts. */
interface ChatHistoryRepository {
    val messages: Flow<List<ChatMessage>>
    suspend fun add(message: ChatMessage)

    /** Inserts all [messages] in order in one step (used to undo a clear). */
    suspend fun addAll(messages: List<ChatMessage>)
    suspend fun clear()
}

class RoomChatHistoryRepository(private val dao: MessageDao) : ChatHistoryRepository {

    override val messages: Flow<List<ChatMessage>> = dao.observeAll().map { rows ->
        rows.map { ChatMessage(it.id, it.text, it.isUser, it.timestamp) }
    }

    override suspend fun add(message: ChatMessage) {
        dao.insert(message.toEntity())
    }

    override suspend fun addAll(messages: List<ChatMessage>) {
        dao.insertAll(messages.map { it.toEntity() })
    }

    override suspend fun clear() = dao.clear()

    private fun ChatMessage.toEntity() = MessageEntity(text = text, isUser = isUser, timestamp = timestamp)
}
