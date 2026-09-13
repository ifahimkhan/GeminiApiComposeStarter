package com.fahim.geminiApiComposeStarter.data

import java.util.UUID

interface ChatStorage {
    suspend fun loadChats(): List<StoredChat>
    suspend fun saveChats(chats: List<StoredChat>, activeChatId: String)
    suspend fun loadActiveChatId(): String?
}

object ChatDefaults {
    const val NEW_CHAT_TITLE = "New chat"

    fun newChat(): StoredChat = StoredChat(
        id = UUID.randomUUID().toString(),
        title = NEW_CHAT_TITLE,
        messages = emptyList(),
    )
}

data class StoredChat(
    val id: String,
    val title: String,
    val messages: List<StoredMessage>,
)

data class StoredMessage(
    val id: Long,
    val text: String,
    val role: ConversationRole,
)
