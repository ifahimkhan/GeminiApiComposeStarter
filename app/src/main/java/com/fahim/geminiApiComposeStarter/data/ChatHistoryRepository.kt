package com.fahim.geminiApiComposeStarter.data

import com.fahim.geminiApiComposeStarter.data.local.ChatDao
import com.fahim.geminiApiComposeStarter.data.local.ChatMessageEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Persistent chat history, so conversations survive app restarts. */
interface ChatHistoryRepository {
    val messages: Flow<List<ChatMessage>>
    suspend fun add(author: Author, text: String, imagePath: String? = null)
    suspend fun clear()
}

class RoomChatHistoryRepository(private val dao: ChatDao) : ChatHistoryRepository {

    override val messages: Flow<List<ChatMessage>> =
        dao.observeMessages().map { rows -> rows.map { it.toModel() } }

    override suspend fun add(author: Author, text: String, imagePath: String?) {
        dao.insert(
            ChatMessageEntity(
                author = author.name,
                text = text,
                createdAt = System.currentTimeMillis(),
                imagePath = imagePath,
            ),
        )
    }

    override suspend fun clear() = dao.clear()
}
