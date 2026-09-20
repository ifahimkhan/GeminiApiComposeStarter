package com.fahim.geminiApiComposeStarter.data.local

import com.fahim.geminiApiComposeStarter.ui.chat.ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class ChatHistoryRepository(
    private val dao: ChatDao,
) {
    fun observeMessages(): Flow<List<ChatMessage>> =
        dao.observeMessages().map { rows -> rows.map { it.toChatMessage() } }

    suspend fun save(message: ChatMessage) {
        withContext(Dispatchers.IO) {
            dao.insert(message.toEntity())
        }
    }

    suspend fun clear() {
        withContext(Dispatchers.IO) {
            dao.clear()
        }
    }
}
