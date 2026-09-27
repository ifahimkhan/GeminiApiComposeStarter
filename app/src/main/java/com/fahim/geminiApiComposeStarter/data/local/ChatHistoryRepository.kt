package com.fahim.geminiApiComposeStarter.data.local

import com.fahim.geminiApiComposeStarter.data.model.ChatMessage
import com.fahim.geminiApiComposeStarter.data.model.Role
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Past conversations, persisted so they survive app restarts. */
interface ChatHistoryRepository {
    val messages: Flow<List<ChatMessage>>
    suspend fun add(role: Role, text: String): Long
    suspend fun clear()
}

class RoomChatHistoryRepository(
    private val dao: ChatDao,
    private val clock: () -> Long = System::currentTimeMillis,
) : ChatHistoryRepository {

    override val messages: Flow<List<ChatMessage>> = dao.observeAll().map { rows ->
        rows.map { ChatMessage(it.id, Role.valueOf(it.role), it.text, it.timestamp) }
    }

    override suspend fun add(role: Role, text: String): Long =
        dao.insert(ChatMessageEntity(role = role.name, text = text, timestamp = clock()))

    override suspend fun clear() = dao.clear()
}
