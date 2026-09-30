package com.fahim.geminiApiComposeStarter.data.local

import kotlinx.coroutines.flow.Flow

class ChatHistoryRepository(
    private val dao: ChatMessageDao
) {

    val messages: Flow<List<ChatMessageEntity>> =
        dao.getAllMessages()

    suspend fun saveMessage(
        text: String,
        sender: String
    ) {
        dao.insertMessage(
            ChatMessageEntity(
                id = System.nanoTime(),
                text = text,
                sender = sender
            )
        )
    }

    suspend fun clearHistory() {
        dao.clearMessages()
    }
}