package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fahim.geminiApiComposeStarter.data.ChatHistoryRepository
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.prefs.SettingsRepository
import com.fahim.geminiApiComposeStarter.data.prefs.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(
    private val gemini: GeminiRepository,
    private val history: ChatHistoryRepository,
    private val settings: SettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        // Room and DataStore are the source of truth; mirror them into the single UI state.
        viewModelScope.launch {
            history.observeMessages().collect { messages ->
                _uiState.update { it.copy(messages = messages) }
            }
        }
        viewModelScope.launch {
            settings.settings.collect { s ->
                _uiState.update { it.copy(themeMode = s.themeMode, dynamicColor = s.dynamicColor) }
            }
        }
    }

    fun onPromptChange(value: String) {
        _uiState.update { it.copy(prompt = value, promptError = null) }
    }

    fun onSend() {
        val current = _uiState.value
        val prompt = current.prompt.trim()
        if (prompt.isEmpty()) {
            _uiState.update { it.copy(promptError = PromptError.EMPTY) }
            return
        }
        if (current.isLoading) return

        _uiState.update {
            it.copy(prompt = "", isLoading = true, errorMessage = null, promptError = null)
        }
        viewModelScope.launch {
            history.addMessage(prompt, isUser = true)
            gemini.generateText(prompt).fold(
                onSuccess = { text ->
                    history.addMessage(text, isUser = false)
                    _uiState.update { it.copy(isLoading = false) }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Something went wrong",
                            // Give the text back so the user can simply tap send again.
                            prompt = it.prompt.ifBlank { prompt },
                        )
                    }
                },
            )
        }
    }

    fun onVoiceResult(text: String) {
        if (text.isBlank()) return
        _uiState.update {
            it.copy(
                prompt = if (it.prompt.isBlank()) text else "${it.prompt} $text",
                promptError = null,
            )
        }
    }

    fun onVoiceUnavailable() {
        _uiState.update { it.copy(errorMessage = VOICE_UNAVAILABLE_MESSAGE) }
    }

    fun onErrorShown() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun onClearChat() {
        viewModelScope.launch { history.clear() }
    }

    fun onThemeModeChange(mode: ThemeMode) {
        viewModelScope.launch { settings.setThemeMode(mode) }
    }

    fun onDynamicColorChange(enabled: Boolean) {
        viewModelScope.launch { settings.setDynamicColor(enabled) }
    }

    companion object {
        const val VOICE_UNAVAILABLE_MESSAGE = "Speech recognition is not available on this device."

        fun factory(
            gemini: GeminiRepository,
            history: ChatHistoryRepository,
            settings: SettingsRepository,
        ) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ChatViewModel(gemini, history, settings) as T
        }
    }
}
