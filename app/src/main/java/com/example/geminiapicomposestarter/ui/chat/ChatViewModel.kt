package com.example.geminiapicomposestarter.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.geminiapicomposestarter.data.ChatHistoryRepository
import com.example.geminiapicomposestarter.data.ChatMessage
import com.example.geminiapicomposestarter.data.GeminiRepository
import com.example.geminiapicomposestarter.data.Participant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(
    private val geminiRepository: GeminiRepository = GeminiRepository(),
    private val historyRepository: ChatHistoryRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        // Observe and load persisted chat history from Room DB
        historyRepository?.let { repo ->
            viewModelScope.launch {
                repo.messages.collect { persistedList ->
                    _uiState.update { it.copy(messages = persistedList) }
                }
            }
        }
    }

    fun updatePrompt(prompt: String) {
        _uiState.update { it.copy(currentPrompt = prompt) }
    }

    fun toggleDarkMode(enabled: Boolean) {
        _uiState.update { it.copy(isDarkMode = enabled) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun sendMessage(prompt: String) {
        val trimmedPrompt = prompt.trim()
        if (trimmedPrompt.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Prompt cannot be empty.") }
            return
        }

        val userMessage = ChatMessage(
            text = trimmedPrompt,
            participant = Participant.USER
        )

        // Optimistically append user message and set loading
        val currentHistory = _uiState.value.messages
        val updatedMessagesWithUser = currentHistory + userMessage
        _uiState.update {
            it.copy(
                messages = updatedMessagesWithUser,
                currentPrompt = "",
                isLoading = true,
                errorMessage = null
            )
        }

        viewModelScope.launch {
            historyRepository?.saveMessage(userMessage)

            val result = geminiRepository.sendMessage(currentHistory, trimmedPrompt)
            result.onSuccess { responseText ->
                val modelMessage = ChatMessage(
                    text = responseText,
                    participant = Participant.MODEL
                )
                historyRepository?.saveMessage(modelMessage)
                _uiState.update {
                    it.copy(
                        messages = it.messages + modelMessage,
                        isLoading = false
                    )
                }
            }.onFailure { exception ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = exception.localizedMessage ?: "Failed to get response from Gemini."
                    )
                }
            }
        }
    }

    fun clearChat() {
        viewModelScope.launch {
            historyRepository?.clearHistory()
            _uiState.update { it.copy(messages = emptyList()) }
        }
    }
}
