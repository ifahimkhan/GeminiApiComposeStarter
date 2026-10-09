package com.fahim.geminiApiComposeStarter.ui.chat

import com.fahim.geminiApiComposeStarter.data.MessageRepository
import com.fahim.geminiApiComposeStarter.data.ChatMessage
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.PreferencesRepository
import com.fahim.geminiApiComposeStarter.data.ThemeMode
import com.fahim.geminiApiComposeStarter.data.AppPreferences
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class FakeGeminiRepository : GeminiRepository {
    var nextResult: Result<String> = Result.success("Hello from fake Gemini")
    /** When set, generateReply suspends until the gate is completed (to observe loading). */
    var gate: CompletableDeferred<Unit>? = null

    val prompts = mutableListOf<String>()
    val histories = mutableListOf<List<ChatMessage>>()

    override suspend fun generateReply(history: List<ChatMessage>, prompt: String): Result<String> {
        prompts += prompt
        histories += history
        gate?.await()
        return nextResult
    }
}

class FakeMessageRepository : MessageRepository {
    private val state = MutableStateFlow<List<ChatMessage>>(emptyList())
    private var nextId = 1L
    override val messages: StateFlow<List<ChatMessage>> = state

    override suspend fun addMessage(text: String, isFromUser: Boolean) {
        val id = nextId++
        state.update { it + ChatMessage(id, text, isFromUser, timestamp = id) }
    }

    override suspend fun clear() {
        state.value = emptyList()
    }
}

class FakePreferencesRepository : PreferencesRepository {
    private val state = MutableStateFlow(AppPreferences())
    override val settings: StateFlow<AppPreferences> = state

    override suspend fun setThemeMode(mode: ThemeMode) = state.update { it.copy(themeMode = mode) }
    override suspend fun setDynamicColor(enabled: Boolean) = state.update { it.copy(dynamicColor = enabled) }
}
