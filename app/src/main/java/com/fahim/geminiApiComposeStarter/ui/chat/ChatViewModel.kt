package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(
    private val repository: GeminiRepository,
    private val hasApiKey: Boolean,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    fun onPromptChange(value: String) {
        _uiState.update { it.copy(prompt = value, promptError = null) }
    }

    fun onClearChat() {
        repository.resetChat()
        _uiState.update { ChatUiState() }
    }

    fun onSend() {
        val userPrompt = _uiState.value.prompt.trim()
        if (userPrompt.isEmpty()) {
            _uiState.update { it.copy(promptError = PromptError.EMPTY) }
            return
        }
        if (!hasApiKey) {
            _uiState.update { it.copy(errorMessage = MISSING_API_KEY_MESSAGE) }
            return
        }
        if (_uiState.value.isLoading) return

        val userMessage = ChatMessage(
            text = userPrompt,
            participant = MessageParticipant.USER,
        )

        val assistantMessage = ChatMessage(
            text = "",
            participant = MessageParticipant.MODEL,
        )

        _uiState.update { current ->
            current.copy(
                prompt = "",
                promptError = null,
                errorMessage = null,
                isLoading = true,
                messages = current.messages + userMessage + assistantMessage,
            )
        }

        viewModelScope.launch {
            repository.sendMessageStream(userPrompt)
                .catch { error ->
                    _uiState.update { current ->
                        current.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Something went wrong",
                        )
                    }
                }
                .collect { chunk ->
                    _uiState.update { current ->
                        val updatedMessages = current.messages.toMutableList()
                        val lastIdx = updatedMessages.lastIndex
                        if (lastIdx >= 0 && updatedMessages[lastIdx].participant == MessageParticipant.MODEL) {
                            val activeMsg = updatedMessages[lastIdx]
                            updatedMessages[lastIdx] = activeMsg.copy(text = activeMsg.text + chunk)
                        }
                        current.copy(
                            isLoading = false,
                            messages = updatedMessages,
                        )
                    }
                }
            _uiState.update { it.copy(isLoading = false) }
        }
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