package com.fahim.geminiApiComposeStarter.data

import com.fahim.geminiApiComposeStarter.data.model.ChatMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory test implementation of [ChatHistoryRepository].
 */
class FakeChatHistoryRepository : ChatHistoryRepository {

    private val _messagesFlow = MutableStateFlow<List<ChatMessage>>(emptyList())
    private var idCounter = 1L

    override fun getMessages(): Flow<List<ChatMessage>> = _messagesFlow.asStateFlow()

    override suspend fun insertMessage(message: ChatMessage): Long {
        val assignedId = if (message.id == 0L) idCounter++ else message.id
        val messageWithId = message.copy(id = assignedId)
        _messagesFlow.value = _messagesFlow.value + messageWithId
        return assignedId
    }

    override suspend fun updateMessage(message: ChatMessage) {
        _messagesFlow.value = _messagesFlow.value.map { existing ->
            if (existing.id == message.id) message else existing
        }
    }

    override suspend fun deleteMessage(id: Long) {
        _messagesFlow.value = _messagesFlow.value.filterNot { it.id == id }
    }

    override suspend fun clearHistory() {
        _messagesFlow.value = emptyList()
    }
}
