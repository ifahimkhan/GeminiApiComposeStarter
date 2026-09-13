package com.fahim.geminiApiComposeStarter.data

import com.fahim.geminiApiComposeStarter.ui.chat.ChatMessage
import com.fahim.geminiApiComposeStarter.ui.chat.Conversation
import kotlinx.coroutines.flow.Flow

interface ChatHistoryRepository {

    fun observeConversations(): Flow<List<Conversation>>

    suspend fun createConversation(title: String): Long

    suspend fun loadMessages(conversationId: Long): List<ChatMessage>

    suspend fun save(conversationId: Long, message: ChatMessage)

    suspend fun touchConversation(conversationId: Long)

    suspend fun deleteConversation(conversationId: Long)
}