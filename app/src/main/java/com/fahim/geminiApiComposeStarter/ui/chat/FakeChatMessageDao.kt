package com.fahim.geminiApiComposeStarter.ui.chat

import com.fahim.geminiApiComposeStarter.data.ChatMessageDao
import com.fahim.geminiApiComposeStarter.data.ChatMessageEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeChatMessageDao : ChatMessageDao {
    private val _messages = MutableStateFlow<List<ChatMessageEntity>>(emptyList())

    override fun getAllMessages(): Flow<List<ChatMessageEntity>> = _messages

    override suspend fun insert(message: ChatMessageEntity) {
        _messages.value = _messages.value + message
    }

    override suspend fun clearAll() {
        _messages.value = emptyList()
    }
}