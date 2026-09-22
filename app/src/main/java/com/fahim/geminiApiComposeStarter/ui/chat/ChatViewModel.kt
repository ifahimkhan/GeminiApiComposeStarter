package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.prefs.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(
    private val repository: GeminiRepository,
    private val userPreferences: UserPreferences,
    private val hasApiKey: Boolean,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        // Collect chat history from Room — auto-updates on every DB change
        repository.getChatHistory()
            .onEach { messages -> _uiState.update { it.copy(messages = messages) } }
            .launchIn(viewModelScope)

        // Collect user preferences (name, last session)
        userPreferences.userPrefsFlow
            .onEach { prefs -> _uiState.update { it.copy(userName = prefs.userName) } }
            .launchIn(viewModelScope)

        // Stamp last-session time on launch
        viewModelScope.launch { userPreferences.updateLastSession() }
    }

    fun onPromptChange(value: String) {
        _uiState.update { it.copy(prompt = value, promptError = null) }
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

        _uiState.update { it.copy(isLoading = true, errorMessage = null, promptError = null, prompt = "") }
        viewModelScope.launch {
            repository.sendMessage(prompt).fold(
                onSuccess = { _uiState.update { it.copy(isLoading = false) } },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Something went wrong",
                        )
                    }
                },
            )
        }
    }

    /** Called by the voice launcher when speech recognition returns a result. */
    fun onVoiceResult(text: String) {
        _uiState.update { it.copy(prompt = text, isListening = false) }
    }

    fun onListeningStateChange(listening: Boolean) {
        _uiState.update { it.copy(isListening = listening) }
    }

    fun onClearHistory() {
        viewModelScope.launch { repository.clearHistory() }
    }

    fun onErrorDismissed() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    companion object {
        const val MISSING_API_KEY_MESSAGE =
            "GEMINI_API_KEY is missing. Add it to local.properties and rebuild."

        fun factory(
            repository: GeminiRepository,
            userPreferences: UserPreferences,
            hasApiKey: Boolean,
        ) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ChatViewModel(repository, userPreferences, hasApiKey) as T
        }
    }
}
