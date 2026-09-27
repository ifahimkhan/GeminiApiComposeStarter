package com.fahim.geminiApiComposeStarter.data.history

import com.fahim.geminiApiComposeStarter.data.ChatMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** In-memory stand-in for Room so ViewModel tests don't need an instrumented environment. */
class FakeChatHistoryRepository : ChatHistoryRepository {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    private var nextId = 1L

    override fun observeMessages(): StateFlow<List<ChatMessage>> = _messages

    override suspend fun addMessage(message: ChatMessage) {
        _messages.value = _messages.value + message.copy(id = nextId++)
    }
}
