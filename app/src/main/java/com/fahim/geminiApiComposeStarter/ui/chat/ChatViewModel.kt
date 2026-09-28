package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fahim.geminiApiComposeStarter.data.repository.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel managing the Chat UI state and interactions with [ChatRepository].
 * Exposes a thread-safe immutable StateFlow for Jetpack Compose.
 */
class ChatViewModel(
    private val chatRepository: ChatRepository,
    private val hasApiKey: Boolean,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState(hasApiKey = hasApiKey))
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        // Collect persistent chat history from Room database
        viewModelScope.launch {
            chatRepository.getMessages().collect { messageList ->
                _uiState.update { it.copy(messages = messageList) }
            }
        }
    }

    fun onPromptChange(value: String) {
        _uiState.update { it.copy(prompt = value, promptError = null) }
    }

    fun onVoiceInputResult(recognizedText: String) {
        if (recognizedText.isNotBlank()) {
            _uiState.update { current ->
                val newPrompt = if (current.prompt.isBlank()) {
                    recognizedText.trim()
                } else {
                    "${current.prompt.trim()} ${recognizedText.trim()}"
                }
                current.copy(prompt = newPrompt, promptError = null)
            }
        }
    }

    fun onSend() {
        val currentPrompt = _uiState.value.prompt.trim()
        if (currentPrompt.isEmpty()) {
            _uiState.update { it.copy(promptError = PromptError.EMPTY) }
            return
        }
        if (!hasApiKey) {
            _uiState.update { it.copy(errorMessage = MISSING_API_KEY_MESSAGE) }
            return
        }
        if (_uiState.value.isLoading) return // Duplicate request prevention

        // Clear input bar and transition to loading state
        _uiState.update {
            it.copy(
                prompt = "",
                isLoading = true,
                errorMessage = null,
                promptError = null
            )
        }

        viewModelScope.launch {
            chatRepository.sendUserMessage(currentPrompt).fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            lastFailedPrompt = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            lastFailedPrompt = currentPrompt,
                            errorMessage = error.message ?: "Failed to generate response. Please try again."
                        )
                    }
                }
            )
        }
    }

    fun onRetry() {
        val failedPrompt = _uiState.value.lastFailedPrompt
        if (!failedPrompt.isNullOrBlank() && !_uiState.value.isLoading) {
            _uiState.update { it.copy(prompt = failedPrompt) }
            onSend()
        }
    }

    fun onDismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun onClearChat() {
        viewModelScope.launch {
            chatRepository.clearChat()
        }
    }

    fun onDeleteMessage(id: Long) {
        viewModelScope.launch {
            chatRepository.deleteMessage(id)
        }
    }

    companion object {
        const val MISSING_API_KEY_MESSAGE =
            "GEMINI_API_KEY is missing. Add it to local.properties and rebuild."

        fun factory(chatRepository: ChatRepository, hasApiKey: Boolean) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ChatViewModel(chatRepository, hasApiKey) as T
            }
    }
}
