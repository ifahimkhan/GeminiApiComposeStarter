package com.fahim.geminiApiComposeStarter.data

import com.fahim.geminiApiComposeStarter.data.local.ChatDao
import com.fahim.geminiApiComposeStarter.data.local.toDomain
import com.fahim.geminiApiComposeStarter.data.local.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Conversation history. An interface so the ViewModel can be unit tested against an in-memory
 * fake instead of a real Room database.
 */
interface ChatHistoryRepository {
    val messages: Flow<List<ChatMessage>>
    suspend fun append(message: ChatMessage): Long
    suspend fun clear()
}

/** Room-backed implementation: history survives process death and app restarts. */
class RoomChatHistoryRepository(private val dao: ChatDao) : ChatHistoryRepository {

    override val messages: Flow<List<ChatMessage>> =
        dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override suspend fun append(message: ChatMessage): Long = dao.insert(message.toEntity())

    override suspend fun clear() = dao.clear()
}
