package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(
    private val repository: GeminiRepository,
    private val hasApiKey: Boolean,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var lastFailedPrompt: String? = null

    init {
        observeChatHistory()
    }

    private fun observeChatHistory() {
        repository.getChatHistory()
            .onEach { messages ->
                _uiState.update { it.copy(messages = messages) }
            }
            .launchIn(viewModelScope)
    }

    fun onPromptChange(value: String) {
        _uiState.update { it.copy(prompt = value, promptError = null) }
    }

    fun onVoiceResult(spokenText: String) {
        if (spokenText.isNotBlank()) {
            _uiState.update { current ->
                val updatedPrompt = if (current.prompt.isBlank()) {
                    spokenText
                } else {
                    "${current.prompt} $spokenText"
                }
                current.copy(prompt = updatedPrompt, promptError = null)
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

        executeSendMessage(prompt)
    }

    fun onRetry() {
        val promptToRetry = lastFailedPrompt ?: _uiState.value.prompt.trim()
        if (promptToRetry.isNotBlank() && hasApiKey) {
            executeSendMessage(promptToRetry)
        }
    }

    private fun executeSendMessage(prompt: String) {
        _uiState.update { it.copy(prompt = "", isLoading = true, errorMessage = null, promptError = null) }
        viewModelScope.launch {
            repository.sendMessage(prompt).fold(
                onSuccess = {
                    lastFailedPrompt = null
                    _uiState.update { it.copy(isLoading = false) }
                },
            ) { error ->
                lastFailedPrompt = prompt
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Failed to generate response",
                    )
                }
            }
        }
    }

    fun onClearHistory() {
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

        fun factory(repository: GeminiRepository, hasApiKey: Boolean) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ChatViewModel(repository, hasApiKey) as T
            }
    }
}
