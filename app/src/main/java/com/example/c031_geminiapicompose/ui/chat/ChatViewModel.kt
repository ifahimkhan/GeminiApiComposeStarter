package com.example.c031_geminiapicompose.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.c031_geminiapicompose.data.GeminiRepository
import com.example.c031_geminiapicompose.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(
    private val repository: GeminiRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val hasApiKey: Boolean,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState(hasApiKey = hasApiKey))
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var lastAttemptedPrompt: String = ""

    init {
        // Collect saved Room chat messages
        viewModelScope.launch {
            repository.getMessagesFlow().collect { messages ->
                _uiState.update { it.copy(messages = messages) }
            }
        }

        // Collect DataStore dark mode preference
        viewModelScope.launch {
            userPreferencesRepository.isDarkModeFlow.collect { isDark ->
                _uiState.update { it.copy(isDarkMode = isDark) }
            }
        }
    }

    fun onPromptChange(value: String) {
        _uiState.update { it.copy(prompt = value, promptError = null) }
    }

    fun onSend() {
        val promptText = _uiState.value.prompt.trim()
        if (promptText.isEmpty()) {
            _uiState.update { it.copy(promptError = PromptError.EMPTY) }
            return
        }
        if (!hasApiKey) {
            _uiState.update { it.copy(errorMessage = MISSING_API_KEY_MESSAGE) }
            return
        }
        if (_uiState.value.isLoading) return

        executeSend(promptText)
    }

    fun onRetry() {
        val promptText = if (lastAttemptedPrompt.isNotBlank()) {
            lastAttemptedPrompt
        } else {
            _uiState.value.prompt.trim()
        }
        if (promptText.isNotBlank() && !_uiState.value.isLoading) {
            executeSend(promptText)
        }
    }

    private fun executeSend(promptText: String) {
        lastAttemptedPrompt = promptText
        _uiState.update {
            it.copy(
                prompt = "",
                isLoading = true,
                errorMessage = null,
                promptError = null
            )
        }

        viewModelScope.launch {
            repository.generateAndSaveResponse(promptText).fold(
                onSuccess = {
                    _uiState.update { state -> state.copy(isLoading = false) }
                },
                onFailure = { error ->
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            errorMessage = error.localizedMessage ?: "Failed to generate response."
                        )
                    }
                }
            )
        }
    }

    fun toggleDarkMode(currentIsDark: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setDarkMode(!currentIsDark)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    companion object {
        const val MISSING_API_KEY_MESSAGE =
            "GEMINI_API_KEY is missing. Add it to local.properties and rebuild."

        fun factory(
            repository: GeminiRepository,
            userPreferencesRepository: UserPreferencesRepository,
            hasApiKey: Boolean
        ) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ChatViewModel(repository, userPreferencesRepository, hasApiKey) as T
            }
        }
    }
}
