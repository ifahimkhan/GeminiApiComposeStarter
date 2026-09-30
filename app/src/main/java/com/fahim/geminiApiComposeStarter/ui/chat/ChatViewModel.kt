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
    private val repository: GeminiRepository,
    private val history: ChatHistoryRepository,
    private val preferences: UserPreferencesRepository,
    private val hasApiKey: Boolean,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        // Load saved messages from Room (and keep listening for new ones)
        viewModelScope.launch {
            history.messages.collect { saved ->
                _uiState.update { it.copy(messages = saved) }
            }
        }
        // Load the saved dark mode setting from DataStore
        viewModelScope.launch {
            preferences.darkMode.collect { dark ->
                _uiState.update { it.copy(darkMode = dark) }
            }
        }
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

        // Clear the input box and show the loading bubble
        _uiState.update {
            it.copy(prompt = "", isLoading = true, errorMessage = null, promptError = null)
        }

        viewModelScope.launch {
            // Save the user's message; Room then updates the list on screen
            history.addMessage(text = prompt, isUser = true)

            repository.generateText(prompt).fold(
                onSuccess = { text ->
                    history.addMessage(text = text, isUser = false)
                    _uiState.update { it.copy(isLoading = false) }
                },
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

    fun onClearChat() {
        viewModelScope.launch { history.clear() }
    }

    fun onDarkModeChange(enabled: Boolean) {
        viewModelScope.launch { preferences.setDarkMode(enabled) }
    }

    /** Called when the device has no speech-to-text app. */
    fun onVoiceUnavailable() {
        _uiState.update { it.copy(errorMessage = "Voice input is not available on this device") }
    }

    /** Called after the Snackbar has shown the error, so the same error can appear again later. */
    fun onErrorShown() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    companion object {
        const val MISSING_API_KEY_MESSAGE =
            "GEMINI_API_KEY is missing. Add it to local.properties and rebuild."

        fun factory(
            repository: GeminiRepository,
            history: ChatHistoryRepository,
            preferences: UserPreferencesRepository,
            hasApiKey: Boolean,
        ) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ChatViewModel(repository, history, preferences, hasApiKey) as T
        }
    }
}