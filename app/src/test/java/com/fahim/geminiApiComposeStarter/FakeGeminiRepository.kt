package com.fahim.geminiApiComposeStarter

import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.ui.chat.ChatMessage
import com.fahim.geminiApiComposeStarter.ui.chat.ChatSession
import com.fahim.geminiApiComposeStarter.ui.chat.Participant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeGeminiRepository : GeminiRepository {

    val messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    var shouldFail = false
    var failureMessage = "API failure"
    var generateCallCount = 0
    var lastReceivedHistory: List<ChatMessage> = emptyList()

    override fun getChatHistory(conversationId: String): Flow<List<ChatMessage>> {
        return messages.map { list ->
            list.filter { it.conversationId == conversationId }
        }
    }

    override fun getChatSessions(): Flow<List<ChatSession>> {
        return messages.map { list ->
            list.groupBy { it.conversationId }
                .map { (convId, msgs) ->
                    val firstUserMsg = msgs.firstOrNull { it.participant == Participant.USER }?.text
                    val title = firstUserMsg?.take(32) ?: "New Chat"
                    ChatSession(
                        id = convId,
                        title = title,
                        updatedAt = msgs.maxOfOrNull { it.timestamp } ?: 0L,
                    )
                }
        }
    }

    override suspend fun saveMessage(message: ChatMessage) {
        messages.value = messages.value + message
    }

    override suspend fun deleteConversation(conversationId: String) {
        messages.value = messages.value.filter { it.conversationId != conversationId }
    }

    override suspend fun clearChatHistory() {
        messages.value = emptyList()
    }

    override suspend fun generateText(
        prompt: String,
        conversationHistory: List<ChatMessage>,
    ): Result<String> {
        generateCallCount++
        lastReceivedHistory = conversationHistory
        return if (shouldFail) {
            Result.failure(IllegalStateException(failureMessage))
        } else {
            Result.success("Fake response to: $prompt")
        }
    }
}
