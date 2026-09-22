package com.silas.geminichat.data.local

import com.silas.geminichat.data.ChatHistoryRepository
import com.silas.geminichat.model.ChatMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Room-backed implementation of [ChatHistoryRepository]. */
class RoomChatHistoryRepository(private val dao: ChatMessageDao) : ChatHistoryRepository {

    override fun observeMessages(): Flow<List<ChatMessage>> =
        dao.observeAll().map { list -> list.map { it.toModel() } }

    override suspend fun addMessage(message: ChatMessage) {
        dao.insert(ChatMessageEntity.fromModel(message))
    }

    override suspend fun clearHistory() = dao.clear()
}
