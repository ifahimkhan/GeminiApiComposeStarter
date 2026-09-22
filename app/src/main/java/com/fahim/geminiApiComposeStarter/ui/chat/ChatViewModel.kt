package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fahim.geminiApiComposeStarter.data.ChatHistoryRepository
import com.fahim.geminiApiComposeStarter.data.ChatMessage
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.SettingsRepository
import com.fahim.geminiApiComposeStarter.data.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(
    private val repository: GeminiRepository,
    private val history: ChatHistoryRepository,
    private val settings: SettingsRepository,
    private val hasApiKey: Boolean,
    private val clock: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    /** The prompt of the last request, kept so a failed request can be retried. */
    private var lastPrompt: String? = null

    init {
        viewModelScope.launch {
            history.messages.collect { messages -> _uiState.update { it.copy(messages = messages) } }
        }
        viewModelScope.launch {
            settings.themeMode.collect { mode -> _uiState.update { it.copy(themeMode = mode) } }
        }
    }

    fun onPromptChange(value: String) {
        _uiState.update { it.copy(prompt = value, promptError = null) }
    }

    /** Appends dictated text to whatever the user already typed. */
    fun onVoiceResult(text: String) {
        _uiState.update {
            val joined = listOf(it.prompt.trim(), text.trim()).filter { part -> part.isNotEmpty() }.joinToString(" ")
            it.copy(prompt = joined, promptError = null)
        }
    }

    fun onSuggestionClick(text: String) {
        _uiState.update { it.copy(prompt = text) }
        onSend()
    }

    fun onSend() {
        val prompt = _uiState.value.prompt.trim()
        if (prompt.isEmpty()) {
            _uiState.update { it.copy(promptError = PromptError.EMPTY) }
            return
        }
        if (!hasApiKey) {
            _uiState.update { it.copy(errorMessage = MISSING_API_KEY_MESSAGE) }
            return
        }
        if (_uiState.value.isLoading) return

        _uiState.update { it.copy(prompt = "", promptError = null, errorMessage = null, isLoading = true) }
        viewModelScope.launch {
            history.add(ChatMessage(text = prompt, isUser = true, timestamp = clock()))
            requestReply(prompt)
        }
    }

    /** Re-sends the last prompt after a failure without adding the user's bubble twice. */
    fun onRetry() {
        val prompt = lastPrompt ?: return
        if (_uiState.value.isLoading) return
        _uiState.update { it.copy(errorMessage = null, isLoading = true) }
        viewModelScope.launch { requestReply(prompt) }
    }

    fun onErrorShown() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun onClearChat() {
        lastPrompt = null
        viewModelScope.launch { history.clear() }
    }

    fun onThemeModeChange(mode: ThemeMode) {
        viewModelScope.launch { settings.setThemeMode(mode) }
    }

    private suspend fun requestReply(prompt: String) {
        lastPrompt = prompt
        repository.generateText(prompt).fold(
            onSuccess = { text ->
                history.add(ChatMessage(text = text, isUser = false, timestamp = clock()))
                lastPrompt = null
                _uiState.update { it.copy(isLoading = false) }
            },
            onFailure = { error ->
                _uiState.update { it.copy(isLoading = false, errorMessage = friendlyError(error)) }
            },
        )
    }

    companion object {
        const val MISSING_API_KEY_MESSAGE =
            "GEMINI_API_KEY is missing. Add it to local.properties and rebuild."
        const val BUSY_MESSAGE = "Gemini is busy right now. Please try again in a moment."
        const val TIMEOUT_MESSAGE = "The request timed out. Check your connection and try again."

        internal fun friendlyError(error: Throwable): String {
            val text = generateSequence(error) { it.cause }.mapNotNull { it.message }.joinToString(" ")
            return when {
                "503" in text || "high demand" in text || "UNAVAILABLE" in text -> BUSY_MESSAGE
                "timeout" in text.lowercase() || "timed out" in text.lowercase() -> TIMEOUT_MESSAGE
                else -> error.message ?: "Something went wrong"
            }
        }

        fun factory(
            repository: GeminiRepository,
            history: ChatHistoryRepository,
            settings: SettingsRepository,
            hasApiKey: Boolean,
        ) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ChatViewModel(repository, history, settings, hasApiKey) as T
        }
    }
}
