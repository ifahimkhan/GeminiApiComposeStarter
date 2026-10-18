package com.fahim.geminiApiComposeStarter.data.repository

import com.fahim.geminiApiComposeStarter.data.local.ChatMessageDao
import com.fahim.geminiApiComposeStarter.data.local.ChatMessageEntity
import com.fahim.geminiApiComposeStarter.data.local.toDomain
import com.fahim.geminiApiComposeStarter.data.model.ChatMessage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Abstraction over local conversation storage to decouple UI and data layers.
 */
interface ChatHistoryRepository {
    fun getMessages(): Flow<List<ChatMessage>>
    suspend fun saveMessage(text: String, isFromUser: Boolean): Long
    suspend fun clearHistory()
}

class ChatHistoryRepositoryImpl(
    private val dao: ChatMessageDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ChatHistoryRepository {

    override fun getMessages(): Flow<List<ChatMessage>> {
        return dao.getAllMessages()
            .map { list -> list.map { it.toDomain() } }
            .flowOn(ioDispatcher)
    }

    override suspend fun saveMessage(text: String, isFromUser: Boolean): Long =
        withContext(ioDispatcher) {
            val entity = ChatMessageEntity(
                text = text,
                isFromUser = isFromUser,
                timestamp = System.currentTimeMillis(),
            )
            dao.insertMessage(entity)
        }

    override suspend fun clearHistory(): Unit = withContext(ioDispatcher) {
        dao.clearAllMessages()
    }
}
