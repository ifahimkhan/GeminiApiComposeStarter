package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(
    private val repository: GeminiRepository,
    private val hasApiKey: Boolean,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    fun onPromptChange(value: String) {
        _uiState.update {
            it.copy(
                prompt = value,
                promptError = null,
            )
        }
    }

    fun onSend() {
        val prompt = _uiState.value.prompt.trim()

        if (prompt.isEmpty()) {
            _uiState.update {
                it.copy(promptError = PromptError.EMPTY)
            }
            return
        }

        if (!hasApiKey) {
            _uiState.update {
                it.copy(
                    errorMessage = MISSING_API_KEY_MESSAGE,
                )
            }
            return
        }

        if (_uiState.value.isLoading) return

        val userMessage = ChatMessage(
            id = System.currentTimeMillis(),
            text = prompt,
            isUser = true,
        )

        _uiState.update {
            it.copy(
                prompt = "",
                messages = it.messages + userMessage,
                isLoading = true,
                errorMessage = null,
                promptError = null,
            )
        }

        viewModelScope.launch {
            repository.generateText(prompt).fold(
                onSuccess = { text ->
                    val assistantMessage = ChatMessage(
                        id = System.currentTimeMillis(),
                        text = text,
                        isUser = false,
                    )

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            messages = it.messages + assistantMessage,
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage =
                                error.message ?: "Something went wrong",
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
            repository: GeminiRepository,
            hasApiKey: Boolean,
        ) =
            object : ViewModelProvider.Factory {

                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(
                    modelClass: Class<T>,
                ): T =
                    ChatViewModel(
                        repository,
                        hasApiKey,
                    ) as T
            }
    }
}