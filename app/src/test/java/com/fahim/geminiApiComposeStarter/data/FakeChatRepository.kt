package com.fahim.geminiApiComposeStarter.data

import com.fahim.geminiApiComposeStarter.data.local.RecentConversation
import com.fahim.geminiApiComposeStarter.ui.chat.ChatMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

/**
 * Deterministic fake implementation of [ChatRepository] for fast, reliable unit testing.
 * Does NOT perform network calls or interact with the real Gemini API or Room SQLite.
 */
class FakeChatRepository : ChatRepository {

    var generateTextResult: Result<String> = Result.success("This is a simulated Gemini AI response.")
    val generatedPrompts = mutableListOf<String>()
    val receivedHistories = mutableListOf<List<ChatMessage>>()
    val savedMessages = mutableListOf<ChatMessage>()

    private val conversationMessages = mutableMapOf<String, MutableStateFlow<List<ChatMessage>>>()
    private val _recentConversations = MutableStateFlow<List<RecentConversation>>(emptyList())

    override suspend fun generateText(prompt: String): Result<String> {
        return generateText(prompt, emptyList())
    }

    override suspend fun generateText(prompt: String, history: List<ChatMessage>): Result<String> {
        generatedPrompts.add(prompt)
        receivedHistories.add(history)
        return generateTextResult
    }

    override fun observeMessages(conversationId: String): Flow<List<ChatMessage>> {
        return getOrCreateConversationFlow(conversationId).asStateFlow()
    }

    override fun observeRecentConversations(): Flow<List<RecentConversation>> {
        return _recentConversations.asStateFlow()
    }

    override suspend fun getMostRecentConversationId(): String? {
        return _recentConversations.value.firstOrNull()?.conversationId
    }

    override suspend fun saveMessage(message: ChatMessage, conversationId: String) {
        savedMessages.add(message)
        val flow = getOrCreateConversationFlow(conversationId)
        val current = flow.value.toMutableList()
        current.add(message)
        flow.value = current

        // Update recent conversations list
        val recents = _recentConversations.value.filter { it.conversationId != conversationId }.toMutableList()
        recents.add(
            0,
            RecentConversation(
                conversationId = conversationId,
                title = message.text.take(30),
                lastTimestamp = System.currentTimeMillis(),
            ),
        )
        _recentConversations.value = recents
    }

    override suspend fun deleteConversation(conversationId: String) {
        conversationMessages.remove(conversationId)
        _recentConversations.value = _recentConversations.value.filter { it.conversationId != conversationId }
    }

    override suspend fun clearAllMessages() {
        conversationMessages.clear()
        savedMessages.clear()
        _recentConversations.value = emptyList()
    }

    private fun getOrCreateConversationFlow(conversationId: String): MutableStateFlow<List<ChatMessage>> {
        return conversationMessages.getOrPut(conversationId) {
            MutableStateFlow(emptyList())
        }
    }
}
