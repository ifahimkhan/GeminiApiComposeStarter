package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(
    private val repository: GeminiRepository,
    private val userPreferencesRepository: UserPreferencesRepository? = null,
    private val hasApiKey: Boolean = true,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        // Observe Room message stream for real-time chat persistence
        viewModelScope.launch {
            repository.messagesStream.collect { messageList ->
                _uiState.update { it.copy(messages = messageList) }
            }
        }

        // Observe User Preferences from DataStore
        userPreferencesRepository?.let { prefsRepo ->
            viewModelScope.launch {
                prefsRepo.userPreferencesFlow.collect { prefs ->
                    _uiState.update { it.copy(selectedModel = prefs.selectedModel) }
                }
            }
        }
    }

    fun onPromptChange(value: String) {
        _uiState.update { it.copy(prompt = value, promptError = null) }
    }

    fun onVoiceInput(recognizedText: String) {
        val current = _uiState.value.prompt.trim()
        val updated = if (current.isEmpty()) recognizedText else "$current $recognizedText"
        _uiState.update { it.copy(prompt = updated, promptError = null) }
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

        _uiState.update {
            it.copy(
                prompt = "",
                isLoading = true,
                errorMessage = null,
                promptError = null,
            )
        }

        viewModelScope.launch {
            repository.sendMessage(prompt).fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false) }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Failed to generate response",
                        )
                    }
                },
            )
        }
    }

    fun onClearChat() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    fun onDismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    companion object {
        const val MISSING_API_KEY_MESSAGE =
            "GEMINI_API_KEY is missing. Add it to local.properties and rebuild."

        fun factory(
            repository: GeminiRepository,
            userPreferencesRepository: UserPreferencesRepository? = null,
            hasApiKey: Boolean = true,
        ) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ChatViewModel(repository, userPreferencesRepository, hasApiKey) as T
        }
    }
}
