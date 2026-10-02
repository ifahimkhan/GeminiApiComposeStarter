package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChatViewModel(
    private val repository: GeminiRepository,
    private val hasApiKey: Boolean
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    fun onPromptChange(value: String) {
        _uiState.value = _uiState.value.copy(
            prompt = value,
            promptError = null
        )
    }

    fun onSend() {
        val prompt = _uiState.value.prompt.trim()

        if (prompt.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                promptError = PromptError.EMPTY
            )
            return
        }

        if (!hasApiKey) {
            _uiState.value = _uiState.value.copy(
                errorMessage = MISSING_API_KEY_MESSAGE
            )
            return
        }

        if (_uiState.value.isLoading) return

        val currentMessages = _uiState.value.messages

        _uiState.value = _uiState.value.copy(
            isLoading = true,
            errorMessage = null,
            promptError = null,
            messages = currentMessages + ChatMessage(
                text = prompt,
                isUser = true
            ),
            prompt = ""
        )

        viewModelScope.launch {
            repository.generateText(prompt).fold(
                onSuccess = { text ->

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        response = text,
                        messages = _uiState.value.messages + ChatMessage(
                            text = text,
                            isUser = false
                        )
                    )
                },
                onFailure = { error ->

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Something went wrong"
                    )
                }
            )
        }
    }

    companion object {

        const val MISSING_API_KEY_MESSAGE =
            "GEMINI_API_KEY is missing. Add it to local.properties and rebuild."

        fun factory(
            repository: GeminiRepository,
            hasApiKey: Boolean
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {

                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(
                    modelClass: Class<T>
                ): T {
                    return ChatViewModel(
                        repository,
                        hasApiKey
                    ) as T
                }
            }
    }
}