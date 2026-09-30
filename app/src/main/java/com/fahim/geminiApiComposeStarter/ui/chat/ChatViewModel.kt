package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fahim.geminiApiComposeStarter.data.ChatHistoryRepository
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(
    private val gemini: GeminiRepository,
    private val history: ChatHistoryRepository,
    private val prefs: UserPreferencesRepository,
    private val hasApiKey: Boolean,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            history.messages.collect { msgs -> _uiState.update { it.copy(messages = msgs) } }
        }
        viewModelScope.launch {
            prefs.themeMode.collect { mode -> _uiState.update { it.copy(themeMode = mode) } }
        }
    }

    fun onPromptChange(value: String) {
        _uiState.update { it.copy(prompt = value, promptError = null) }
    }

    fun onVoiceResult(spokenText: String) {
        _uiState.update { it.copy(prompt = spokenText, promptError = null) }
    }

    fun onVoiceUnavailable() {
        _uiState.update { it.copy(errorMessage = VOICE_UNAVAILABLE_MESSAGE) }
    }

    fun onErrorShown() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun onSend() {
        if (_uiState.value.isLoading) return
        val prompt = _uiState.value.prompt.trim()
        if (prompt.isEmpty()) {
            _uiState.update { it.copy(promptError = PromptError.EMPTY) }
            return
        }
        if (!hasApiKey) {
            _uiState.update { it.copy(errorMessage = MISSING_API_KEY_MESSAGE) }
            return
        }

        _uiState.update { it.copy(prompt = "", isLoading = true, errorMessage = null, promptError = null) }
        viewModelScope.launch {
            history.add(prompt, isUser = true)
            gemini.generateText(prompt).fold(
                onSuccess = { text ->
                    history.add(text, isUser = false)
                    _uiState.update { it.copy(isLoading = false) }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            prompt = prompt, // give the user their text back so they can retry
                            errorMessage = error.message ?: "Something went wrong",
                        )
                    }
                },
            )
        }
    }

    fun onClearChat() {
        viewModelScope.launch { history.clear() }
    }

    fun onToggleTheme() {
        val next = when (_uiState.value.themeMode) {
            ThemeMode.SYSTEM -> ThemeMode.LIGHT
            ThemeMode.LIGHT -> ThemeMode.DARK
            ThemeMode.DARK -> ThemeMode.SYSTEM
        }
        viewModelScope.launch { prefs.setThemeMode(next) }
    }

    companion object {
        const val MISSING_API_KEY_MESSAGE =
            "GEMINI_API_KEY is missing. Add it to local.properties and rebuild."
        const val VOICE_UNAVAILABLE_MESSAGE = "Speech recognition is not available on this device."

        fun factory(
            gemini: GeminiRepository,
            history: ChatHistoryRepository,
            prefs: UserPreferencesRepository,
            hasApiKey: Boolean,
        ) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ChatViewModel(gemini, history, prefs, hasApiKey) as T
        }
    }
}
