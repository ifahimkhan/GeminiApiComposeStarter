package com.fahim.geminiApiComposeStarter.data

import com.fahim.geminiApiComposeStarter.data.local.ChatMessageDao
import com.fahim.geminiApiComposeStarter.data.local.toChatMessage
import com.fahim.geminiApiComposeStarter.data.local.toEntity
import com.fahim.geminiApiComposeStarter.ui.chat.ChatMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomChatHistoryRepository(
    private val dao: ChatMessageDao,
) : ChatHistoryRepository {

    override fun getMessages(): Flow<List<ChatMessage>> =
        dao.getAllMessages().map { entities ->
            entities.map { it.toChatMessage() }
        }

    override suspend fun saveMessage(message: ChatMessage) {
        dao.insertMessage(message.toEntity())
    }

    override suspend fun saveMessages(messages: List<ChatMessage>) {
        dao.insertMessages(messages.map { it.toEntity() })
    }

    override suspend fun clearHistory() {
        dao.clearAllMessages()
    }
}
