package com.fahim.geminiApiComposeStarter.testing

import com.fahim.geminiApiComposeStarter.data.ChatHistoryRepository
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.UserPreferencesRepository
import com.fahim.geminiApiComposeStarter.data.model.ChatMessage
import com.fahim.geminiApiComposeStarter.data.model.Role
import com.fahim.geminiApiComposeStarter.data.model.ThemeMode
import com.fahim.geminiApiComposeStarter.data.model.UserPreferences
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class FakeGeminiRepository(var result: Result<String> = Result.success("reply")) : GeminiRepository {
    val requests = mutableListOf<Pair<List<ChatMessage>, String>>()

    /** When set, calls suspend until it is completed, to observe the loading state. */
    var gate: CompletableDeferred<Unit>? = null

    override suspend fun generateText(history: List<ChatMessage>, prompt: String): Result<String> {
        requests += history to prompt
        gate?.await()
        return result
    }
}

class FakeChatHistoryRepository : ChatHistoryRepository {
    override val messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    private var nextId = 1L

    override suspend fun add(role: Role, text: String): Long {
        val id = nextId++
        messages.update { it + ChatMessage(id, role, text, timestamp = id) }
        return id
    }

    override suspend fun delete(id: Long) = messages.update { list -> list.filterNot { it.id == id } }

    override suspend fun clear() {
        messages.value = emptyList()
    }
}

class FakeUserPreferencesRepository : UserPreferencesRepository {
    override val preferences = MutableStateFlow(UserPreferences())

    override suspend fun setThemeMode(mode: ThemeMode) =
        preferences.update { it.copy(themeMode = mode) }

    override suspend fun setDynamicColor(enabled: Boolean) =
        preferences.update { it.copy(dynamicColor = enabled) }
}
