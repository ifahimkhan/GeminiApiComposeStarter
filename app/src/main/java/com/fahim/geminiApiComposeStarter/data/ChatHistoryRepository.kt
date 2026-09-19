package com.fahim.geminiApiComposeStarter.data

import com.fahim.geminiApiComposeStarter.data.local.MessageDao
import com.fahim.geminiApiComposeStarter.data.local.MessageEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Persisted conversation history, so chats survive app restarts. */
interface ChatHistoryRepository {
    fun observeMessages(): Flow<List<ChatMessage>>
    suspend fun addMessage(text: String, isUser: Boolean)
    suspend fun clear()
}

class RoomChatHistoryRepository(private val dao: MessageDao) : ChatHistoryRepository {

    override fun observeMessages(): Flow<List<ChatMessage>> =
        dao.observeAll().map { list ->
            list.map { ChatMessage(id = it.id, text = it.text, isUser = it.isUser) }
        }

    override suspend fun addMessage(text: String, isUser: Boolean) {
        dao.insert(MessageEntity(text = text, isUser = isUser))
    }

    override suspend fun clear() = dao.clear()
}
