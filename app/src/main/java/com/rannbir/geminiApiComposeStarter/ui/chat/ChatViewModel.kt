package com.rannbir.geminiApiComposeStarter.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rannbir.geminiApiComposeStarter.data.GeminiRepository
import com.rannbir.geminiApiComposeStarter.data.local.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(
    private val repository: GeminiRepository,
    private val preferencesRepository: UserPreferencesRepository? = null,
    private val hasApiKey: Boolean = true,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        // Collect real-time chat history from Room memory
        viewModelScope.launch {
            repository.getChatMessages().collect { messageList ->
                _uiState.update { it.copy(messages = messageList) }
            }
        }

        // Collect user profile preferences from DataStore
        preferencesRepository?.let { prefs ->
            viewModelScope.launch {
                prefs.userProfileFlow.collect { profile ->
                    _uiState.update {
                        it.copy(
                            userDisplayName = profile.userName,
                            rollNumber = profile.rollNumber,
                        )
                    }
                }
            }
        }
    }

    fun onPromptChange(value: String) {
        _uiState.update { it.copy(prompt = value, promptError = null) }
    }

    fun onVoiceInputResult(speechText: String) {
        val trimmed = speechText.trim()
        if (trimmed.isNotBlank()) {
            _uiState.update { current ->
                val newPrompt = if (current.prompt.isBlank()) {
                    trimmed
                } else {
                    "${current.prompt} $trimmed"
                }
                current.copy(prompt = newPrompt, promptError = null)
            }
        }
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

        // Clear input bar and start loading
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
                            errorMessage = com.rannbir.geminiApiComposeStarter.data.GeminiRepositoryImpl.sanitizeErrorMessage(error),
                        )
                    }
                },
            )
        }
    }

    fun onClearChat() {
        viewModelScope.launch {
            repository.clearChatHistory()
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    companion object {
        const val MISSING_API_KEY_MESSAGE =
            "GEMINI_API_KEY is missing. Add it to local.properties and rebuild."

        fun factory(
            repository: GeminiRepository,
            preferencesRepository: UserPreferencesRepository?,
            hasApiKey: Boolean,
        ) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ChatViewModel(repository, preferencesRepository, hasApiKey) as T
        }
    }
}
