package com.fahim.geminiApiComposeStarter.data

import com.fahim.geminiApiComposeStarter.data.local.ChatMessageDao
import com.fahim.geminiApiComposeStarter.data.local.ChatMessageEntity
import kotlinx.coroutines.flow.Flow

class ChatHistoryRepositoryImpl(
    private val dao: ChatMessageDao,
) : ChatHistoryRepository {

    override fun observeMessages(): Flow<List<ChatMessageEntity>> = dao.getMessages()

    override suspend fun insertMessage(message: ChatMessageEntity) {
        dao.insertMessage(message)
    }
}