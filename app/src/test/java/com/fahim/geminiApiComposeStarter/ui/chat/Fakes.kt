package com.fahim.geminiApiComposeStarter.ui.chat

import com.fahim.geminiApiComposeStarter.data.Author
import com.fahim.geminiApiComposeStarter.data.ChatHistoryRepository
import com.fahim.geminiApiComposeStarter.data.ChatMessage
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.ThemeMode
import com.fahim.geminiApiComposeStarter.data.UserPreferencesRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/** Returns [result] for every call. Set [gate] to hold a call open until the test completes it. */
class FakeGeminiRepository(var result: Result<String> = Result.success("Fake reply")) : GeminiRepository {
    val prompts = mutableListOf<String>()
    var gate: CompletableDeferred<Unit>? = null

    override suspend fun generateText(prompt: String): Result<String> {
        prompts += prompt
        gate?.await()
        return result
    }
}

class FakeChatHistoryRepository(initial: List<ChatMessage> = emptyList()) : ChatHistoryRepository {
    private val state = MutableStateFlow(initial)
    private var nextId = (initial.maxOfOrNull { it.id } ?: -1L) + 1

    override val messages: Flow<List<ChatMessage>> = state

    override suspend fun add(author: Author, text: String) {
        state.update { it + ChatMessage(nextId++, author, text) }
    }

    override suspend fun clear() {
        state.value = emptyList()
    }
}

class FakeUserPreferencesRepository(initial: ThemeMode = ThemeMode.SYSTEM) : UserPreferencesRepository {
    private val state = MutableStateFlow(initial)

    override val themeMode: Flow<ThemeMode> = state

    override suspend fun setThemeMode(mode: ThemeMode) {
        state.value = mode
    }
}
