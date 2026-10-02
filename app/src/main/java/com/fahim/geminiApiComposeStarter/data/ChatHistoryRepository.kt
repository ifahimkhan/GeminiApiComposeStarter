package com.fahim.geminiApiComposeStarter.data

import com.fahim.geminiApiComposeStarter.data.local.ChatMessageDao
import com.fahim.geminiApiComposeStarter.data.local.ChatMessageEntity
import com.fahim.geminiApiComposeStarter.data.local.toModel
import com.fahim.geminiApiComposeStarter.data.model.ChatMessage
import com.fahim.geminiApiComposeStarter.data.model.Role
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Persistent conversation history, so chats survive app restarts. */
interface ChatHistoryRepository {
    val messages: Flow<List<ChatMessage>>
    suspend fun add(role: Role, text: String): Long
    suspend fun delete(id: Long)
    suspend fun clear()
}

class RoomChatHistoryRepository(
    private val dao: ChatMessageDao,
    private val clock: () -> Long = System::currentTimeMillis,
) : ChatHistoryRepository {

    override val messages: Flow<List<ChatMessage>> =
        dao.observeAll().map { rows -> rows.map { it.toModel() } }

    override suspend fun add(role: Role, text: String): Long =
        dao.insert(ChatMessageEntity(role = role.name, text = text, timestamp = clock()))

    override suspend fun delete(id: Long) = dao.delete(id)

    override suspend fun clear() = dao.clear()
}
