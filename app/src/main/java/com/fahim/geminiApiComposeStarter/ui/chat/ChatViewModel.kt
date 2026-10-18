package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.preferences.UserPreferencesRepository
import com.fahim.geminiApiComposeStarter.data.repository.ChatHistoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(
    private val repository: GeminiRepository,
    private val chatHistoryRepository: ChatHistoryRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val hasApiKey: Boolean,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        // Collect conversation history from Room
        viewModelScope.launch {
            chatHistoryRepository.getMessages().collect { messages ->
                _uiState.update { it.copy(messages = messages) }
            }
        }

        // Collect theme preference from Preferences DataStore
        viewModelScope.launch {
            userPreferencesRepository.isDarkMode.collect { isDark ->
                _uiState.update { it.copy(isDarkMode = isDark) }
            }
        }
    }

    fun onPromptChange(value: String) {
        _uiState.update { it.copy(prompt = value, promptError = null) }
    }

    fun onVoiceInput(spokenText: String) {
        if (spokenText.isBlank()) return
        _uiState.update { current ->
            val newPrompt = if (current.prompt.isBlank()) {
                spokenText.trim()
            } else {
                "${current.prompt.trim()} ${spokenText.trim()}"
            }
            current.copy(prompt = newPrompt, promptError = null)
        }
    }

    fun onDismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun onToggleTheme() {
        viewModelScope.launch {
            userPreferencesRepository.toggleDarkMode()
        }
    }

    fun onClearHistory() {
        viewModelScope.launch {
            chatHistoryRepository.clearHistory()
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

        _uiState.update {
            it.copy(
                prompt = "",
                isLoading = true,
                errorMessage = null,
                promptError = null,
            )
        }

        viewModelScope.launch {
            // Save user prompt to Room database
            chatHistoryRepository.saveMessage(text = prompt, isFromUser = true)

            // Request response from Gemini API
            repository.generateText(prompt).fold(
                onSuccess = { responseText ->
                    chatHistoryRepository.saveMessage(text = responseText, isFromUser = false)
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

    companion object {
        const val MISSING_API_KEY_MESSAGE =
            "GEMINI_API_KEY is missing. Add it to local.properties or environment variable and rebuild."

        fun factory(
            repository: GeminiRepository,
            chatHistoryRepository: ChatHistoryRepository,
            userPreferencesRepository: UserPreferencesRepository,
            hasApiKey: Boolean,
        ) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ChatViewModel(
                    repository = repository,
                    chatHistoryRepository = chatHistoryRepository,
                    userPreferencesRepository = userPreferencesRepository,
                    hasApiKey = hasApiKey,
                ) as T
        }
    }
}
