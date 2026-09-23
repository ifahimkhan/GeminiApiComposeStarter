package com.fahim.geminiApiComposeStarter.data

import com.fahim.geminiApiComposeStarter.data.local.ChatDao
import com.fahim.geminiApiComposeStarter.data.local.ChatMessageEntity
import com.fahim.geminiApiComposeStarter.data.local.MessageParticipant
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

interface ChatHistoryRepository {
    fun getMessages(): Flow<List<ChatMessageEntity>>
    suspend fun saveMessage(
        text: String,
        participant: MessageParticipant,
        isError: Boolean = false,
    ): Long
    suspend fun clearHistory()
}

class ChatHistoryRepositoryImpl(
    private val chatDao: ChatDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ChatHistoryRepository {
    override fun getMessages(): Flow<List<ChatMessageEntity>> = chatDao.getAllMessages()

    override suspend fun saveMessage(
        text: String,
        participant: MessageParticipant,
        isError: Boolean,
    ): Long = withContext(ioDispatcher) {
        chatDao.insertMessage(
            ChatMessageEntity(
                text = text,
                participant = participant,
                isError = isError,
            ),
        )
    }

    override suspend fun clearHistory() = withContext(ioDispatcher) {
        chatDao.clearAllMessages()
        Unit
    }
}
