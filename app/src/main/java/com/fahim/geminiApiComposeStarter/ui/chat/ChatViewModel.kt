package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fahim.geminiApiComposeStarter.data.ChatHistoryRepository
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(
    private val repository: GeminiRepository,
    private val hasApiKey: Boolean,
    private val historyRepository: ChatHistoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())

    val uiState: StateFlow<ChatUiState> =
        _uiState.asStateFlow()

    init {
        loadChatHistory()
    }

    private fun loadChatHistory() {

        viewModelScope.launch {

            historyRepository.getMessages()
                .collect { savedMessages ->

                    val messages = savedMessages.map { savedMessage ->
                        ChatMessage(
                            id = savedMessage.id.toString(),
                            text = savedMessage.text,
                            isUser = savedMessage.isUser
                        )
                    }

                    _uiState.update {
                        it.copy(
                            messages = messages
                        )
                    }
                }
        }
    }

    fun onPromptChange(value: String) {

        _uiState.update {
            it.copy(
                prompt = value,
                promptError = null
            )
        }
    }

    fun onSend() {

        val prompt = _uiState.value.prompt.trim()

        if (prompt.isEmpty()) {

            _uiState.update {
                it.copy(
                    promptError = PromptError.EMPTY
                )
            }

            return
        }

        if (!hasApiKey) {

            _uiState.update {
                it.copy(
                    errorMessage = MISSING_API_KEY_MESSAGE
                )
            }

            return
        }

        if (_uiState.value.isLoading) {
            return
        }

        // Add user message immediately
        _uiState.update {

            it.copy(
                prompt = "",
                isLoading = true,
                errorMessage = null,
                promptError = null,
                messages = it.messages +
                        ChatMessage(
                            text = prompt,
                            isUser = true
                        )
            )
        }

        viewModelScope.launch {

            // Save user message to Room
            historyRepository.saveMessage(
                text = prompt,
                isUser = true
            )

            repository.generateText(prompt).fold(

                onSuccess = { text ->

                    // Add Gemini response
                    _uiState.update {

                        it.copy(
                            isLoading = false,
                            response = text,
                            messages = it.messages +
                                    ChatMessage(
                                        text = text,
                                        isUser = false
                                    )
                        )
                    }

                    // Save Gemini response to Room
                    historyRepository.saveMessage(
                        text = text,
                        isUser = false
                    )
                },

                onFailure = { error ->

                    _uiState.update {

                        it.copy(
                            isLoading = false,
                            errorMessage =
                                error.message
                                    ?: "Something went wrong."
                        )
                    }
                }
            )
        }
    }

    fun clearChat() {

        viewModelScope.launch {

            historyRepository.clearMessages()

            _uiState.update {
                it.copy(
                    messages = emptyList(),
                    response = "",
                    prompt = ""
                )
            }
        }
    }

    companion object {

        const val MISSING_API_KEY_MESSAGE =
            "GEMINI_API_KEY is missing. Add it to local.properties and rebuild."

        fun factory(
            repository: GeminiRepository,
            hasApiKey: Boolean,
            historyRepository: ChatHistoryRepository
        ): ViewModelProvider.Factory {

            return object : ViewModelProvider.Factory {

                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(
                    modelClass: Class<T>
                ): T {

                    return ChatViewModel(
                        repository = repository,
                        hasApiKey = hasApiKey,
                        historyRepository = historyRepository
                    ) as T
                }
            }
        }
    }
}