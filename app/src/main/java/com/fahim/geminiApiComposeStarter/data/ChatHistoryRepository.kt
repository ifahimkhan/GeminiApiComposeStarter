package com.fahim.geminiApiComposeStarter.data

import com.fahim.geminiApiComposeStarter.data.local.ChatDao
import com.fahim.geminiApiComposeStarter.data.local.ChatMessageEntity
import com.fahim.geminiApiComposeStarter.ui.chat.ChatMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Saves and loads past conversations. An interface so tests can use a fake. */
interface ChatHistoryRepository {
    val messages: Flow<List<ChatMessage>>
    suspend fun addMessage(text: String, isUser: Boolean)
    suspend fun clear()
}

/** Room implementation: chat history survives app restarts. */
class RoomChatHistoryRepository(private val dao: ChatDao) : ChatHistoryRepository {

    override val messages: Flow<List<ChatMessage>> =
        dao.getAllMessages().map { rows ->
            rows.map { ChatMessage(id = it.id, text = it.text, isUser = it.isUser) }
        }

    override suspend fun addMessage(text: String, isUser: Boolean) {
        dao.insert(ChatMessageEntity(text = text, isUser = isUser))
    }

    override suspend fun clear() {
        dao.deleteAll()
    }
}