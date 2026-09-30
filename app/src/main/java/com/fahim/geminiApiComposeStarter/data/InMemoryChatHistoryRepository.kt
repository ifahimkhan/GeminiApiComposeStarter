package com.fahim.geminiApiComposeStarter.data

import com.fahim.geminiApiComposeStarter.ui.chat.ChatMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class InMemoryChatHistoryRepository : ChatHistoryRepository {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())

    override fun getMessages(): Flow<List<ChatMessage>> = _messages.asStateFlow()

    override suspend fun saveMessage(message: ChatMessage) {
        _messages.update { current ->
            val index = current.indexOfFirst { it.id == message.id }
            if (index >= 0) {
                current.toMutableList().apply { set(index, message) }
            } else {
                (current + message).sortedBy { it.timestamp }
            }
        }
    }

    override suspend fun saveMessages(messages: List<ChatMessage>) {
        _messages.update { current ->
            val updated = current.toMutableList()
            for (msg in messages) {
                val index = updated.indexOfFirst { it.id == msg.id }
                if (index >= 0) {
                    updated[index] = msg
                } else {
                    updated.add(msg)
                }
            }
            updated.sortedBy { it.timestamp }
        }
    }

    override suspend fun clearHistory() {
        _messages.value = emptyList()
    }
}
