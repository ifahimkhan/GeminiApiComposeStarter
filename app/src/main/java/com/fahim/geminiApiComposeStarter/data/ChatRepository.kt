package com.fahim.geminiApiComposeStarter.data

import com.fahim.geminiApiComposeStarter.data.local.RecentConversation
import com.fahim.geminiApiComposeStarter.ui.chat.ChatMessage
import kotlinx.coroutines.flow.Flow

/**
 * Unified repository interface coordinating local Room persistence and remote Gemini API text generation.
 * Extends GeminiRepository to maintain full backward compatibility with the existing architecture.
 */
interface ChatRepository : GeminiRepository {
    fun observeMessages(conversationId: String): Flow<List<ChatMessage>>
    fun observeRecentConversations(): Flow<List<RecentConversation>>
    suspend fun getMostRecentConversationId(): String?
    suspend fun saveMessage(message: ChatMessage, conversationId: String)
    suspend fun deleteConversation(conversationId: String)
    suspend fun clearAllMessages()
}
