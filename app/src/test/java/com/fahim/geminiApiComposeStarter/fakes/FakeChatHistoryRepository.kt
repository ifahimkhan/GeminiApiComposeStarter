package com.fahim.geminiApiComposeStarter.fakes

import com.fahim.geminiApiComposeStarter.data.ChatHistoryRepository
import com.fahim.geminiApiComposeStarter.domain.ChatMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FakeChatHistoryRepository : ChatHistoryRepository {
    private val messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    private var nextId = 1L

    override fun observeMessages(): StateFlow<List<ChatMessage>> = messages

    override suspend fun addMessage(message: ChatMessage) {
        messages.value = messages.value + message.copy(id = nextId++)
    }
}
