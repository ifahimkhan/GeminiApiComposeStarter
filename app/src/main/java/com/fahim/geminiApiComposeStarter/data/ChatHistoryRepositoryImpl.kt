package com.fahim.geminiApiComposeStarter.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class ChatHistoryRepositoryImpl(
    private val chatMessageDao: ChatMessageDao
) : ChatHistoryRepository {
    override fun getAllMessages(): Flow<List<ChatMessageEntity>> {
        return chatMessageDao.getAllMessages()
    }

    override suspend fun addMessage(text: String, isUser: Boolean) {
        withContext(Dispatchers.IO) {
            chatMessageDao.insertMessage(
                ChatMessageEntity(
                    text = text,
                    isUser = isUser,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    override suspend fun clearHistory() {
        withContext(Dispatchers.IO) {
            chatMessageDao.clearHistory()
        }
    }
}
