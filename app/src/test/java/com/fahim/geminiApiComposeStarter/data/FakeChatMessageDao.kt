package com.fahim.geminiApiComposeStarter.data

import com.fahim.geminiApiComposeStarter.data.local.ChatMessageDao
import com.fahim.geminiApiComposeStarter.data.local.ChatMessageEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory fake DAO for testing Room persistence behavior without an instrumented device.
 */
class FakeChatMessageDao(
    initialMessages: List<ChatMessageEntity> = emptyList(),
) : ChatMessageDao {

    private val messages = mutableListOf<ChatMessageEntity>().apply { addAll(initialMessages) }
    private val _messagesFlow = MutableStateFlow(messages.sortedBy { it.timestamp })

    override fun getAllMessages(): Flow<List<ChatMessageEntity>> = _messagesFlow.asStateFlow()

    override suspend fun getAllMessagesSnapshot(): List<ChatMessageEntity> = messages.toList()

    override suspend fun insertMessage(message: ChatMessageEntity) {
        messages.removeAll { it.id == message.id }
        messages.add(message)
        _messagesFlow.value = messages.sortedBy { it.timestamp }
    }

    override suspend fun insertMessages(messages: List<ChatMessageEntity>) {
        messages.forEach { insertMessage(it) }
    }

    override suspend fun clearAll() {
        messages.clear()
        _messagesFlow.value = emptyList()
    }
}
