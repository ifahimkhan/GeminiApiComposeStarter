package com.fahim.geminiApiComposeStarter.ui.chat

import com.fahim.geminiApiComposeStarter.data.ChatHistoryRepository
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeGeminiRepository(
    private var result: Result<String> = Result.success("stub reply"),
) : GeminiRepository {
    var lastPrompt: String? = null

    fun setNextResult(result: Result<String>) {
        this.result = result
    }

    override suspend fun generateText(prompt: String): Result<String> {
        lastPrompt = prompt
        return result
    }
}

class FakeChatHistoryRepository : ChatHistoryRepository {
    private val messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    private var nextId = 0L

    override fun observeMessages(): Flow<List<ChatMessage>> = messages

    override suspend fun addMessage(text: String, isFromUser: Boolean) {
        messages.value = messages.value + ChatMessage(id = nextId++, text = text, isFromUser = isFromUser)
    }
}

class FakeUserPreferencesRepository : UserPreferencesRepository {
    private val darkMode = MutableStateFlow<Boolean?>(null)

    override val darkModeOverride: Flow<Boolean?> = darkMode

    override suspend fun setDarkModeOverride(enabled: Boolean) {
        darkMode.value = enabled
    }
}
