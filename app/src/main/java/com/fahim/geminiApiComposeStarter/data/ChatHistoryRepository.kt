package com.fahim.geminiApiComposeStarter.data

import com.fahim.geminiApiComposeStarter.data.local.MessageDao
import com.fahim.geminiApiComposeStarter.data.local.MessageEntity
import com.fahim.geminiApiComposeStarter.data.local.toDomain
import com.fahim.geminiApiComposeStarter.data.model.Author
import com.fahim.geminiApiComposeStarter.data.model.ChatMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Chat history, abstracted so the ViewModel can be unit tested without Room. */
interface ChatHistoryRepository {
    val messages: Flow<List<ChatMessage>>
    suspend fun append(text: String, author: Author): Long
    suspend fun clear()
}

class RoomChatHistoryRepository(private val dao: MessageDao) : ChatHistoryRepository {

    override val messages: Flow<List<ChatMessage>> =
        dao.observeAll().map { rows -> rows.map(MessageEntity::toDomain) }

    override suspend fun append(text: String, author: Author): Long = dao.insert(
        MessageEntity(
            text = text,
            isFromUser = author == Author.USER,
            createdAt = System.currentTimeMillis(),
        )
    )

    override suspend fun clear() = dao.deleteAll()
}
