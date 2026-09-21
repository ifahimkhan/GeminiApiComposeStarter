package com.fahim.geminiApiComposeStarter.ui.chat

import android.graphics.Bitmap
import com.fahim.geminiApiComposeStarter.data.Author
import com.fahim.geminiApiComposeStarter.data.ChatHistoryRepository
import com.fahim.geminiApiComposeStarter.data.ChatMessage
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.ImageStore
import com.fahim.geminiApiComposeStarter.data.ThemeMode
import com.fahim.geminiApiComposeStarter.data.UserPreferencesRepository
import com.fahim.geminiApiComposeStarter.speech.TextSpeaker
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Returns [result] for every call. Set [gate] to hold a call open until the test completes it. */
class FakeGeminiRepository(var result: Result<String> = Result.success("Fake reply")) : GeminiRepository {
    val prompts = mutableListOf<String>()
    val imagePaths = mutableListOf<String?>()
    var gate: CompletableDeferred<Unit>? = null

    override suspend fun generateText(prompt: String, imagePath: String?): Result<String> {
        prompts += prompt
        imagePaths += imagePath
        gate?.await()
        return result
    }
}

class FakeChatHistoryRepository(initial: List<ChatMessage> = emptyList()) : ChatHistoryRepository {
    private val state = MutableStateFlow(initial)
    private var nextId = (initial.maxOfOrNull { it.id } ?: -1L) + 1

    override val messages: Flow<List<ChatMessage>> = state

    override suspend fun add(author: Author, text: String, imagePath: String?) {
        state.update { it + ChatMessage(nextId++, author, text, imagePath) }
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

/** "Imports" a source by prefixing it with `stored/`, and records what was deleted. */
class FakeImageStore : ImageStore {
    var failImport = false
    val deleted = mutableListOf<String>()

    override suspend fun import(source: String): String? = if (failImport) null else "stored/$source"

    override suspend fun load(path: String): Bitmap? = null

    override suspend fun delete(path: String) {
        deleted += path
    }
}

class FakeTextSpeaker : TextSpeaker {
    private val _speakingId = MutableStateFlow<Long?>(null)
    override val speakingId: StateFlow<Long?> = _speakingId.asStateFlow()

    var available = true
    var shutDown = false
    val spoken = mutableListOf<Pair<Long, String>>()
    var stopCalls = 0

    override fun speak(id: Long, text: String): Boolean {
        if (!available) return false
        spoken += id to text
        _speakingId.value = id
        return true
    }

    override fun stop() {
        stopCalls++
        _speakingId.value = null
    }

    override fun shutdown() {
        shutDown = true
        _speakingId.value = null
    }
}
