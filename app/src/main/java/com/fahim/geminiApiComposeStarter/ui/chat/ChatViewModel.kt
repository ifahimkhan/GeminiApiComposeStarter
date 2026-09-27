package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fahim.geminiApiComposeStarter.data.ChatMessage
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.history.ChatHistoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(
    private val geminiRepository: GeminiRepository,
    private val historyRepository: ChatHistoryRepository,
    private val hasApiKey: Boolean,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState(hasApiKey = hasApiKey))
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var lastFailedPrompt: String? = null

    init {
        viewModelScope.launch {
            historyRepository.observeMessages().collect { messages ->
                _uiState.update { it.copy(messages = messages) }
            }
        }
    }

    fun onInputChange(value: String) {
        _uiState.update { it.copy(inputText = value, inputError = null) }
    }

    fun onSend() {
        sendPrompt(_uiState.value.inputText.trim())
    }

    fun onSuggestionSelected(prompt: String) {
        sendPrompt(prompt)
    }

    fun onRetry() {
        val prompt = lastFailedPrompt ?: return
        sendPrompt(prompt, isRetry = true)
    }

    fun onErrorDismissed() {
        _uiState.update { it.copy(errorMessage = null, canRetry = false) }
    }

    private fun sendPrompt(prompt: String, isRetry: Boolean = false) {
        if (prompt.isBlank()) {
            _uiState.update { it.copy(inputError = "Type a message before sending.") }
            return
        }
        if (!hasApiKey) {
            _uiState.update { it.copy(errorMessage = MISSING_API_KEY_MESSAGE, canRetry = false) }
            return
        }
        if (_uiState.value.isLoading) return

        _uiState.update {
            it.copy(
                inputText = if (isRetry) it.inputText else "",
                inputError = null,
                isLoading = true,
                errorMessage = null,
                canRetry = false,
            )
        }

        viewModelScope.launch {
            if (!isRetry) {
                historyRepository.addMessage(ChatMessage(text = prompt, isFromUser = true))
            }
            geminiRepository.generateText(prompt).fold(
                onSuccess = { text ->
                    lastFailedPrompt = null
                    historyRepository.addMessage(ChatMessage(text = text, isFromUser = false))
                    _uiState.update { it.copy(isLoading = false) }
                },
                onFailure = { error ->
                    lastFailedPrompt = prompt
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message?.takeIf { msg -> msg.isNotBlank() }
                                ?: "Something went wrong. Please try again.",
                            canRetry = true,
                        )
                    }
                },
            )
        }
    }

    companion object {
        const val MISSING_API_KEY_MESSAGE =
            "GEMINI_API_KEY is missing. Add it to local.properties and rebuild."

        fun factory(
            geminiRepository: GeminiRepository,
            historyRepository: ChatHistoryRepository,
            hasApiKey: Boolean,
        ) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ChatViewModel(geminiRepository, historyRepository, hasApiKey) as T
        }
    }
}
