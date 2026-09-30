package com.fahim.geminiApiComposeStarter.ui.chat

import com.fahim.geminiApiComposeStarter.data.ChatMessageDao
import com.fahim.geminiApiComposeStarter.data.ChatMessageEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeChatMessageDao : ChatMessageDao {
    private val messages = mutableListOf<ChatMessageEntity>()
    private val flow = MutableStateFlow<List<ChatMessageEntity>>(emptyList())

    override fun getAllMessages(): Flow<List<ChatMessageEntity>> = flow

    override suspend fun insertMessage(message: ChatMessageEntity) {
        messages.add(message)
        flow.value = messages.toList()
    }
}