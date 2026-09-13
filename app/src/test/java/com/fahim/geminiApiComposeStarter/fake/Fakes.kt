package com.fahim.geminiApiComposeStarter.fake

import com.fahim.geminiApiComposeStarter.data.ChatHistoryRepository
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.model.Author
import com.fahim.geminiApiComposeStarter.data.model.ChatMessage
import com.fahim.geminiApiComposeStarter.data.prefs.ThemeMode
import com.fahim.geminiApiComposeStarter.data.prefs.UserPreferences
import com.fahim.geminiApiComposeStarter.data.prefs.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeGeminiRepository(
    var nextResult: Result<String> = Result.success("ok"),
) : GeminiRepository {

    val prompts = mutableListOf<String>()
    val historiesSeen = mutableListOf<List<ChatMessage>>()

    override suspend fun generateReply(
        history: List<ChatMessage>,
        prompt: String,
    ): Result<String> {
        prompts += prompt
        historiesSeen += history
        return nextResult
    }
}

/** In-memory stand-in for the Room-backed repository. */
class FakeChatHistoryRepository : ChatHistoryRepository {

    private val state = MutableStateFlow<List<ChatMessage>>(emptyList())
    private var nextId = 1L

    override val messages: Flow<List<ChatMessage>> = state

    val current: List<ChatMessage> get() = state.value

    override suspend fun append(text: String, author: Author): Long {
        val id = nextId++
        state.value = state.value + ChatMessage(
            id = id,
            text = text,
            author = author,
            createdAt = id,
        )
        return id
    }

    override suspend fun clear() {
        state.value = emptyList()
    }
}

class FakeUserPreferencesRepository(
    initial: UserPreferences = UserPreferences(),
) : UserPreferencesRepository {

    private val state = MutableStateFlow(initial)

    override val preferences: Flow<UserPreferences> = state

    val current: UserPreferences get() = state.value

    override suspend fun setThemeMode(mode: ThemeMode) {
        state.value = state.value.copy(themeMode = mode)
    }

    override suspend fun setDynamicColour(enabled: Boolean) {
        state.value = state.value.copy(dynamicColour = enabled)
    }
}
