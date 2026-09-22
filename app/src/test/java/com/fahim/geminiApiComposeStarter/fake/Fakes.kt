package com.fahim.geminiApiComposeStarter.fake

import com.fahim.geminiApiComposeStarter.data.ChatHistoryRepository
import com.fahim.geminiApiComposeStarter.data.ChatMessage
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.prefs.UserPreferences
import com.fahim.geminiApiComposeStarter.data.prefs.UserPreferencesRepository
import com.fahim.geminiApiComposeStarter.ui.theme.ThemeMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** Records what the ViewModel asked for and returns a canned result, with no network. */
class FakeGeminiRepository : GeminiRepository {

    var result: Result<String> = Result.success("Fake answer")

    /** Virtual milliseconds the call takes, so a test can assert the in-flight UI state. */
    var delayMillis: Long = 0
    var callCount: Int = 0
        private set
    var lastPrompt: String? = null
        private set
    var lastHistory: List<ChatMessage> = emptyList()
        private set

    override suspend fun generateText(
        prompt: String,
        history: List<ChatMessage>,
    ): Result<String> {
        callCount++
        lastPrompt = prompt
        lastHistory = history
        if (delayMillis > 0) delay(delayMillis)
        return result
    }
}

/** In-memory stand-in for the Room-backed history. */
class FakeChatHistoryRepository(
    initial: List<ChatMessage> = emptyList(),
) : ChatHistoryRepository {

    private val state = MutableStateFlow(initial)
    private var nextId = initial.size.toLong() + 1

    override val messages: Flow<List<ChatMessage>> = state

    val current: List<ChatMessage> get() = state.value

    override suspend fun append(message: ChatMessage): Long {
        val id = nextId++
        state.value = state.value + message.copy(id = id)
        return id
    }

    override suspend fun clear() {
        state.value = emptyList()
    }
}

/** In-memory stand-in for the DataStore-backed preferences. */
class FakeUserPreferencesRepository(
    initial: UserPreferences = UserPreferences(),
) : UserPreferencesRepository {

    private val state = MutableStateFlow(initial)

    override val preferences: Flow<UserPreferences> = state

    val current: UserPreferences get() = state.value

    override suspend fun setThemeMode(mode: ThemeMode) {
        state.value = state.value.copy(themeMode = mode)
    }

    override suspend fun setDynamicColor(enabled: Boolean) {
        state.value = state.value.copy(dynamicColor = enabled)
    }
}
