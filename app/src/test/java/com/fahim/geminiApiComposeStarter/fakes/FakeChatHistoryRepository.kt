package com.fahim.geminiApiComposeStarter.fakes

import com.fahim.geminiApiComposeStarter.data.history.ChatHistoryRepository
import com.fahim.geminiApiComposeStarter.ui.chat.ChatMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeChatHistoryRepository : ChatHistoryRepository {
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    override val allMessages: Flow<List<ChatMessage>> = _messages.asStateFlow()

    override suspend fun saveMessage(message: ChatMessage) {
        _messages.value = _messages.value + message
    }

    override suspend fun clearHistory() {
        _messages.value = emptyList()
    }
}
