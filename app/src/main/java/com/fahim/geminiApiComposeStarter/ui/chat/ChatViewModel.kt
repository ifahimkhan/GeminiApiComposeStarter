package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.MissingApiKeyException
import com.fahim.geminiApiComposeStarter.data.local.ChatHistoryRepository
import com.fahim.geminiApiComposeStarter.data.model.ChatMessage
import com.fahim.geminiApiComposeStarter.data.model.ResponseStyle
import com.fahim.geminiApiComposeStarter.data.model.Role
import com.fahim.geminiApiComposeStarter.data.model.ThemeMode
import com.fahim.geminiApiComposeStarter.data.prefs.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(
    private val geminiRepository: GeminiRepository,
    private val historyRepository: ChatHistoryRepository,
    private val preferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        // Chat history from Room survives restarts; preferences come from DataStore.
        viewModelScope.launch {
            historyRepository.messages.collect { messages ->
                _uiState.update { it.copy(messages = messages) }
            }
        }
        viewModelScope.launch {
            preferencesRepository.preferences.collect { prefs ->
                _uiState.update { it.copy(preferences = prefs) }
            }
        }
    }

    fun onPromptChange(value: String) {
        _uiState.update { it.copy(prompt = value, promptError = null) }
    }

    fun onSend() {
        val state = _uiState.value
        if (state.isLoading) return
        val prompt = state.prompt.trim()
        if (prompt.isEmpty()) {
            _uiState.update { it.copy(promptError = PromptError.EMPTY) }
            return
        }
        val previousMessages = state.messages
        _uiState.update {
            it.copy(prompt = "", isLoading = true, promptError = null, error = null, retryPrompt = null)
        }
        viewModelScope.launch {
            historyRepository.add(Role.USER, prompt)
            requestReply(previousMessages, prompt)
        }
    }

    fun onRetry() {
        val state = _uiState.value
        val prompt = state.retryPrompt ?: return
        if (state.isLoading) return
        // The failed user message is already stored; don't send it twice as history.
        val previous = state.messages.let { messages ->
            val last = messages.lastOrNull()
            if (last != null && last.role == Role.USER && last.text == prompt) messages.dropLast(1) else messages
        }
        _uiState.update { it.copy(isLoading = true, error = null, retryPrompt = null) }
        viewModelScope.launch { requestReply(previous, prompt) }
    }

    private suspend fun requestReply(previous: List<ChatMessage>, prompt: String) {
        val style = _uiState.value.preferences.responseStyle
        geminiRepository.generateReply(previous, prompt, style).fold(
            onSuccess = { reply ->
                historyRepository.add(Role.MODEL, reply)
                _uiState.update { it.copy(isLoading = false) }
            },
            onFailure = { throwable ->
                val missingKey = throwable is MissingApiKeyException
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = if (missingKey) ChatError.MissingApiKey else ChatError.Request(throwable.message),
                        retryPrompt = if (missingKey) null else prompt,
                    )
                }
            },
        )
    }

    /** Called by the UI once the Snackbar for [ChatUiState.error] has been shown. */
    fun onErrorShown() {
        _uiState.update { it.copy(error = null, retryPrompt = null) }
    }

    /** Text recognised by the speech-to-text activity is appended to the prompt. */
    fun onVoiceResult(text: String) {
        val spoken = text.trim()
        if (spoken.isEmpty()) return
        _uiState.update {
            val combined = if (it.prompt.isBlank()) spoken else it.prompt.trimEnd() + " " + spoken
            it.copy(prompt = combined, promptError = null)
        }
    }

    fun onVoiceUnavailable() {
        _uiState.update { it.copy(error = ChatError.VoiceUnavailable) }
    }

    fun onClearHistory() {
        viewModelScope.launch { historyRepository.clear() }
    }

    fun onOpenSettings() = _uiState.update { it.copy(showSettings = true) }

    fun onDismissSettings() = _uiState.update { it.copy(showSettings = false) }

    fun onThemeModeChange(mode: ThemeMode) {
        viewModelScope.launch { preferencesRepository.setThemeMode(mode) }
    }

    fun onDynamicColorChange(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setDynamicColor(enabled) }
    }

    fun onResponseStyleChange(style: ResponseStyle) {
        viewModelScope.launch { preferencesRepository.setResponseStyle(style) }
    }

    companion object {
        fun factory(
            geminiRepository: GeminiRepository,
            historyRepository: ChatHistoryRepository,
            preferencesRepository: UserPreferencesRepository,
        ) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ChatViewModel(geminiRepository, historyRepository, preferencesRepository) as T
        }
    }
}
