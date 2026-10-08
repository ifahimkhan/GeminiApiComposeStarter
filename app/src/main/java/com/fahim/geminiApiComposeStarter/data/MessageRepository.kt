package com.fahim.geminiApiComposeStarter.data

import com.fahim.geminiApiComposeStarter.data.local.MessageDao
import com.fahim.geminiApiComposeStarter.data.local.MessageEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface MessageRepository {
    val messages: Flow<List<ChatMessage>>
    suspend fun addMessage(text: String, isFromUser: Boolean)
    suspend fun clear()
}

/** Room-backed history, so conversations survive app restarts. */
class RoomMessageRepository(
    private val dao: MessageDao,
    private val clock: () -> Long = System::currentTimeMillis,
) : MessageRepository {

    override val messages: Flow<List<ChatMessage>> =
        dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override suspend fun addMessage(text: String, isFromUser: Boolean) {
        dao.insert(MessageEntity(text = text, isFromUser = isFromUser, timestamp = clock()))
    }

    override suspend fun clear() = dao.clear()
}

private fun MessageEntity.toDomain() = ChatMessage(id, text, isFromUser, timestamp)
