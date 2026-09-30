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

    private var lastAttemptedPrompt: String? = null

    init {
        loadConversationHistory()
    }

    private fun loadConversationHistory() {
        repository.getChatHistory()
            .onEach { messageList ->
                _uiState.update { it.copy(messages = messageList) }
            }
            .launchIn(viewModelScope)
    }

    fun onPromptChange(value: String) {
        _uiState.update { it.copy(prompt = value, promptError = null) }
    }

    fun onVoiceResult(recognizedText: String) {
        if (recognizedText.isNotBlank()) {
            _uiState.update { state ->
                val newPrompt = if (state.prompt.isBlank()) {
                    recognizedText
                } else {
                    "${state.prompt} $recognizedText"
                }
                state.copy(prompt = newPrompt, promptError = null)
            }
        }
    }

    fun onSend() {
        val cleanPrompt = _uiState.value.prompt.trim()
        if (cleanPrompt.isEmpty()) {
            _uiState.update { it.copy(promptError = PromptError.EMPTY) }
            return
        }
        if (!hasApiKey) {
            _uiState.update { it.copy(errorMessage = MISSING_KEY_NOTICE) }
            return
        }
        if (_uiState.value.isLoading) return

        dispatchSend(cleanPrompt)
    }

    fun onRetry() {
        val target = lastAttemptedPrompt ?: _uiState.value.prompt.trim()
        if (target.isNotBlank() && hasApiKey) {
            dispatchSend(target)
        }
    }

    private fun dispatchSend(prompt: String) {
        _uiState.update { it.copy(prompt = "", isLoading = true, errorMessage = null, promptError = null) }
        viewModelScope.launch {
            repository.sendMessage(prompt).fold(
                onSuccess = {
                    lastAttemptedPrompt = null
                    _uiState.update { it.copy(isLoading = false) }
                },
            ) { error ->
                lastAttemptedPrompt = prompt
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Failed to generate AI response",
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
        const val MISSING_KEY_NOTICE =
            "GEMINI_API_KEY is missing in local.properties. Please add it and rebuild."

        fun factory(repository: GeminiRepository, hasApiKey: Boolean) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ChatViewModel(repository, hasApiKey) as T
            }
    }
}
