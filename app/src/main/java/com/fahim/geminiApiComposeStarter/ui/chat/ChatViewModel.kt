package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.history.ChatHistoryRepository
import com.fahim.geminiApiComposeStarter.data.preferences.ThemeMode
import com.fahim.geminiApiComposeStarter.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(
    private val repository: GeminiRepository,
    private val chatHistoryRepository: ChatHistoryRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var lastAttemptedPrompt: String? = null

    init {
        checkApiKey()
        loadHistory()
        loadPreferences()
    }

    private fun checkApiKey() {
        viewModelScope.launch {
            val isConfigured = repository.isApiKeyConfigured()
            _uiState.update { state ->
                state.copy(
                    isApiKeyConfigured = isConfigured,
                    errorMessage = if (!isConfigured) MISSING_API_KEY_MESSAGE else null,
                )
            }
        }
    }

    private fun loadHistory() {
        viewModelScope.launch {
            chatHistoryRepository.allMessages.collect { messages ->
                _uiState.update { it.copy(messages = messages) }
            }
        }
    }

    private fun loadPreferences() {
        viewModelScope.launch {
            userPreferencesRepository.userPreferences.collect { prefs ->
                _uiState.update {
                    it.copy(
                        themeMode = prefs.themeMode,
                        dynamicColor = prefs.dynamicColor,
                    )
                }
            }
        }
    }

    fun onPromptChange(value: String) {
        _uiState.update { it.copy(prompt = value, promptError = null) }
    }

    fun onDismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun onShowSpeechError(message: String) {
        _uiState.update { it.copy(errorMessage = message) }
    }

    fun onShowClearHistoryDialog(show: Boolean) {
        _uiState.update { it.copy(showClearHistoryDialog = show) }
    }

    fun onConfirmClearHistory() {
        _uiState.update { it.copy(showClearHistoryDialog = false) }
        viewModelScope.launch {
            chatHistoryRepository.clearHistory()
        }
    }

    fun onShowSettingsDialog(show: Boolean) {
        _uiState.update { it.copy(showSettingsDialog = show) }
    }

    fun onSetThemeMode(themeMode: ThemeMode) {
        viewModelScope.launch {
            userPreferencesRepository.setThemeMode(themeMode)
        }
    }

    fun onSetDynamicColor(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setDynamicColor(enabled)
        }
    }

    fun onSend() {
        val promptText = _uiState.value.prompt.trim()
        if (promptText.isEmpty()) {
            _uiState.update { it.copy(promptError = PromptError.EMPTY) }
            return
        }
        if (!_uiState.value.isApiKeyConfigured) {
            _uiState.update { it.copy(errorMessage = MISSING_API_KEY_MESSAGE) }
            return
        }
        if (_uiState.value.isLoading) return

        lastAttemptedPrompt = promptText
        val userMessage = ChatMessage(text = promptText, isUser = true)

        _uiState.update {
            it.copy(
                prompt = "",
                isLoading = true,
                errorMessage = null,
                promptError = null,
            )
        }

        viewModelScope.launch {
            chatHistoryRepository.saveMessage(userMessage)
            executeGeneration(promptText)
        }
    }

    fun onRetry() {
        val promptText = lastAttemptedPrompt
            ?: _uiState.value.messages.lastOrNull { it.isUser }?.text
            ?: return

        if (!_uiState.value.isApiKeyConfigured) {
            _uiState.update { it.copy(errorMessage = MISSING_API_KEY_MESSAGE) }
            return
        }
        if (_uiState.value.isLoading) return

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        executeGeneration(promptText)
    }

    private fun executeGeneration(promptText: String) {
        viewModelScope.launch {
            try {
                repository.generateText(promptText).fold(
                    onSuccess = { text ->
                        val assistantMessage = ChatMessage(text = text, isUser = false)
                        chatHistoryRepository.saveMessage(assistantMessage)
                        _uiState.update { it.copy(isLoading = false) }
                    },
                    onFailure = { error ->
                        val displayError = error.message?.takeIf { it.isNotBlank() }
                            ?: "Failed to generate response. Please try again."
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = displayError,
                            )
                        }
                    },
                )
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "An unexpected error occurred.",
                    )
                }
            }
        }
    }

    companion object {
        const val MISSING_API_KEY_MESSAGE =
            "API key not configured. Add GEMINI_API_KEY to local.properties and rebuild."

        fun factory(
            repository: GeminiRepository,
            chatHistoryRepository: ChatHistoryRepository,
            userPreferencesRepository: UserPreferencesRepository,
        ) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ChatViewModel(
                    repository = repository,
                    chatHistoryRepository = chatHistoryRepository,
                    userPreferencesRepository = userPreferencesRepository,
                ) as T
        }
    }
}
