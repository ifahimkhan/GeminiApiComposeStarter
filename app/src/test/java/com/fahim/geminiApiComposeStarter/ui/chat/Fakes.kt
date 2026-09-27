package com.fahim.geminiApiComposeStarter.ui.chat

import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.local.ChatHistoryRepository
import com.fahim.geminiApiComposeStarter.data.model.ChatMessage
import com.fahim.geminiApiComposeStarter.data.model.ResponseStyle
import com.fahim.geminiApiComposeStarter.data.model.Role
import com.fahim.geminiApiComposeStarter.data.model.ThemeMode
import com.fahim.geminiApiComposeStarter.data.model.UserPreferences
import com.fahim.geminiApiComposeStarter.data.prefs.UserPreferencesRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/** Records every call and answers with the queued results (or suspends on [gate] if set). */
class FakeGeminiRepository : GeminiRepository {
    data class Call(val history: List<ChatMessage>, val prompt: String, val style: ResponseStyle)

    val calls = mutableListOf<Call>()
    private val results = ArrayDeque<Result<String>>()
    var gate: CompletableDeferred<Unit>? = null

    fun enqueue(first: Result<String>, second: Result<String>? = null) {
        results.addLast(first)
        if (second != null) results.addLast(second)
    }

    override suspend fun generateReply(
        history: List<ChatMessage>,
        prompt: String,
        style: ResponseStyle,
    ): Result<String> {
        calls += Call(history, prompt, style)
        gate?.await()
        return results.removeFirstOrNull() ?: Result.success("ok")
    }
}

class FakeChatHistoryRepository(initial: List<ChatMessage> = emptyList()) : ChatHistoryRepository {
    private val state = MutableStateFlow(initial)
    private var nextId = (initial.maxOfOrNull { it.id } ?: 0L) + 1

    override val messages: Flow<List<ChatMessage>> = state

    override suspend fun add(role: Role, text: String): Long {
        val id = nextId++
        state.update { it + ChatMessage(id, role, text, timestamp = id) }
        return id
    }

    override suspend fun clear() {
        state.value = emptyList()
    }
}

class FakeUserPreferencesRepository : UserPreferencesRepository {
    private val state = MutableStateFlow(UserPreferences())
    override val preferences: Flow<UserPreferences> = state

    override suspend fun setThemeMode(mode: ThemeMode) = state.update { it.copy(themeMode = mode) }
    override suspend fun setDynamicColor(enabled: Boolean) = state.update { it.copy(dynamicColor = enabled) }
    override suspend fun setResponseStyle(style: ResponseStyle) = state.update { it.copy(responseStyle = style) }
}