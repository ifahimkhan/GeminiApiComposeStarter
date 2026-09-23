package com.fahim.geminiApiComposeStarter.data

import com.fahim.geminiApiComposeStarter.ui.chat.ChatMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface ChatHistoryRepository {
    val messages: Flow<List<ChatMessage>>
    suspend fun add(text: String, isUser: Boolean)
    suspend fun clear()
}

class RoomChatHistoryRepository(private val dao: ChatDao) : ChatHistoryRepository {
    override val messages: Flow<List<ChatMessage>> =
        dao.observeAll().map { rows -> rows.map { ChatMessage(it.id, it.text, it.isUser) } }

    override suspend fun add(text: String, isUser: Boolean) {
        dao.insert(MessageEntity(text = text, isUser = isUser))
    }

    override suspend fun clear() = dao.clearAll()
}