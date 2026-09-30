package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.model.ChatMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(
    private val repository: GeminiRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        observeChatHistory()
        observePreferences()
    }

    private fun observeChatHistory() {
        viewModelScope.launch {
            repository.getChatHistory().collect { history ->
                _uiState.update { it.copy(messages = history) }
            }
        }
    }

    private fun observePreferences() {
        viewModelScope.launch {
            repository.getUserPreferences().collect { prefs ->
                _uiState.update {
                    it.copy(
                        userName = prefs.userName,
                        selectedModel = prefs.selectedModel,
                        tone = prefs.tone,
                        hasApiKey = prefs.isKeyConfigured,
                    )
                }
            }
        }
    }

    fun onPromptChange(value: String) {
        _uiState.update { it.copy(prompt = value, promptError = null) }
    }

    fun onSend() {
        val currentPrompt = _uiState.value.prompt.trim()
        if (currentPrompt.isEmpty()) {
            _uiState.update { it.copy(promptError = PromptError.EMPTY) }
            return
        }
        if (!_uiState.value.hasApiKey) {
            _uiState.update { it.copy(errorMessage = MISSING_API_KEY_MESSAGE) }
            return
        }
        if (_uiState.value.isLoading) return

        val userMessage = ChatMessage(
            text = currentPrompt,
            isUser = true,
            timestamp = System.currentTimeMillis(),
        )

        // Clear input field and set loading state
        _uiState.update {
            it.copy(
                prompt = "",
                isLoading = true,
                errorMessage = null,
                promptError = null,
            )
        }

        viewModelScope.launch {
            // Persist user prompt immediately to Room
            repository.saveMessage(userMessage)

            // Request AI completion
            repository.generateText(currentPrompt).fold(
                onSuccess = { generatedText ->
                    val aiMessage = ChatMessage(
                        text = generatedText,
                        isUser = false,
                        timestamp = System.currentTimeMillis(),
                    )
                    repository.saveMessage(aiMessage)
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

    fun onClearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    fun onVoiceInput(transcription: String) {
        if (transcription.isNotBlank()) {
            _uiState.update {
                val updated = if (it.prompt.isBlank()) transcription else "${it.prompt} $transcription"
                it.copy(prompt = updated, promptError = null)
            }
        }
    }

    fun onSelectPromptSuggestion(suggestion: String) {
        _uiState.update { it.copy(prompt = suggestion, promptError = null) }
    }

    fun onToggleSettings(isOpen: Boolean) {
        _uiState.update { it.copy(isSettingsOpen = isOpen) }
    }

    fun onUpdatePreferences(userName: String, model: String, tone: String) {
        viewModelScope.launch {
            repository.updatePreferences(userName, model, tone)
        }
    }

    fun onSaveCustomApiKey(key: String) {
        viewModelScope.launch {
            repository.saveCustomApiKey(key)
        }
    }

    fun onDismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    companion object {
        const val MISSING_API_KEY_MESSAGE =
            "GEMINI_API_KEY is missing. Add it to local.properties or enter it in Settings."

        fun factory(repository: GeminiRepository) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ChatViewModel(repository) as T
            }
    }
}
