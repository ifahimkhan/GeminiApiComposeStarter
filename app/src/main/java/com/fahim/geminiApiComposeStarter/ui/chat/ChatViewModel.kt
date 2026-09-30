package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.local.ChatHistoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(
    private val repository: GeminiRepository,
    private val historyRepository: ChatHistoryRepository? = null,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var lastPrompt: String? = null

    init {
        historyRepository?.let { historyRepo ->
            viewModelScope.launch {
                historyRepo.getChatHistory().collect { history ->
                    _uiState.update { state ->
                        state.copy(messages = history)
                    }
                }
            }
        }
    }

    fun onPromptChange(value: String) {
        _uiState.update { it.copy(prompt = value, promptError = null) }
    }

    /**
     * Receives recognized speech and updates the prompt without auto-sending.
     */
    fun onVoiceResult(recognizedText: String) {
        val cleanSpeech = recognizedText.trim()
        if (cleanSpeech.isEmpty()) return

        val currentPrompt = _uiState.value.prompt.trim()
        val newPrompt = if (currentPrompt.isEmpty()) {
            cleanSpeech
        } else {
            "$currentPrompt $cleanSpeech"
        }
        _uiState.update { it.copy(prompt = newPrompt, promptError = null) }
    }

    /**
     * Reports a non-retryable voice/device error. Shown as a dismissible snackbar with
     * no Retry action — retrying cannot help when speech services are unavailable.
     */
    fun onVoiceError(message: String = "Speech recognition isn't available on this device.") {
        _uiState.update { it.copy(chatError = ChatError.NonRetryable(message)) }
    }

    fun onSend() {
        val prompt = _uiState.value.prompt.trim()
        if (prompt.isEmpty()) {
            _uiState.update { it.copy(promptError = PromptError.EMPTY) }
            return
        }
        if (_uiState.value.isLoading) return

        lastPrompt = prompt
        val userMessage = ChatMessage(text = prompt, participant = Participant.USER)
        val historySnapshot = _uiState.value.messages

        // Always add the user message optimistically so the chat list updates immediately.
        // When historyRepository is non-null, Room's REPLACE conflict strategy ensures the
        // Flow re-emission converges to the same state without duplicating the message.
        _uiState.update { state ->
            state.copy(
                prompt = "",
                isLoading = true,
                chatError = null,
                promptError = null,
                messages = state.messages + userMessage,
            )
        }

        viewModelScope.launch {
            historyRepository?.insertMessage(userMessage)
            executePrompt(prompt, historySnapshot)
        }
    }

    fun retry() {
        val promptToRetry = lastPrompt ?: return
        if (_uiState.value.isLoading) return
        // Only retry if the current error is retryable
        if (_uiState.value.chatError is ChatError.NonRetryable) return

        _uiState.update { state ->
            state.copy(
                isLoading = true,
                chatError = null,
                promptError = null,
            )
        }
        val historySnapshot = _uiState.value.messages.dropLastWhile { it.participant == Participant.USER }
        viewModelScope.launch {
            executePrompt(promptToRetry, historySnapshot)
        }
    }

    fun clearError() {
        _uiState.update { it.copy(chatError = null) }
    }

    fun clearChat() {
        viewModelScope.launch {
            historyRepository?.clearHistory()
            _uiState.update { state ->
                state.copy(
                    messages = emptyList(),
                    chatError = null,
                )
            }
        }
    }

    private suspend fun executePrompt(prompt: String, history: List<ChatMessage> = emptyList()) {
        repository.generateText(prompt, history).fold(
            onSuccess = { text ->
                val assistantMessage = ChatMessage(
                    text = text,
                    participant = Participant.ASSISTANT,
                )
                historyRepository?.insertMessage(assistantMessage)
                // Add assistant message optimistically; Room Flow convergence handles deduplication.
                _uiState.update { state ->
                    state.copy(
                        messages = state.messages + assistantMessage,
                        isLoading = false,
                        chatError = null,
                    )
                }
            },
            onFailure = { error ->
                // Network/quota/server failures are retryable. Auth/key failures are not.
                val message = error.message ?: "Something went wrong"
                val chatError = if (isRetryableError(error)) {
                    ChatError.Retryable(message)
                } else {
                    ChatError.NonRetryable(message)
                }
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        chatError = chatError,
                    )
                }
            },
        )
    }

    companion object {
        /**
         * Network errors, quota exhaustion, and transient server errors are retryable.
         * Auth failures (invalid key, permission denied) are non-retryable since retrying
         * with the same key will always fail.
         */
        internal fun isRetryableError(error: Throwable): Boolean {
            return when (error) {
                is java.io.IOException -> true
                is IllegalStateException -> {
                    val msg = error.message.orEmpty()
                    msg.contains("high demand", ignoreCase = true) ||
                    msg.contains("quota", ignoreCase = true) ||
                    msg.contains("Rate limit", ignoreCase = true)
                }
                is IllegalArgumentException -> false // invalid API key
                is SecurityException -> false        // permission denied
                else -> false
            }
        }

        fun factory(
            repository: GeminiRepository,
            historyRepository: ChatHistoryRepository? = null,
        ) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ChatViewModel(repository, historyRepository) as T
        }
    }
}
