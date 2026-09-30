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
    private val historyRepository: ChatHistoryRepository,
    private val hasApiKey: Boolean,
) : ViewModel() {

    private val _uiState =
        MutableStateFlow(ChatUiState())

    val uiState: StateFlow<ChatUiState> =
        _uiState.asStateFlow()

    init {
        observeStoredMessages()
    }

    private fun observeStoredMessages() {
        viewModelScope.launch {

            historyRepository.messages.collect { entities ->

                val messages = entities.map { entity ->

                    ChatMessage(
                        id = entity.id,
                        text = entity.text,
                        sender =
                            when (entity.sender) {
                                MessageSender.USER.name ->
                                    MessageSender.USER

                                else ->
                                    MessageSender.GEMINI
                            }
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
        val prompt =
            _uiState.value.prompt.trim()

        if (prompt.isEmpty()) {
            _uiState.update {
                it.copy(
                    promptError =
                        PromptError.EMPTY
                )
            }
            return
        }

        if (!hasApiKey) {
            _uiState.update {
                it.copy(
                    errorMessage =
                        MISSING_API_KEY_MESSAGE
                )
            }
            return
        }

        if (_uiState.value.isLoading) {
            return
        }

        _uiState.update {
            it.copy(
                prompt = "",
                isLoading = true,
                errorMessage = null,
                promptError = null
            )
        }

        viewModelScope.launch {

            historyRepository.saveMessage(
                text = prompt,
                sender = MessageSender.USER.name
            )

            repository.generateText(prompt)
                .fold(

                    onSuccess = { text ->

                        historyRepository.saveMessage(
                            text = text,
                            sender =
                                MessageSender.GEMINI.name
                        )

                        _uiState.update {
                            it.copy(
                                isLoading = false
                            )
                        }
                    },

                    onFailure = { error ->

                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage =
                                    error.message
                                        ?: "Something went wrong"
                            )
                        }
                    }
                )
        }
    }

    fun clearHistory() {
        viewModelScope.launch {

            historyRepository.clearHistory()

            _uiState.update {
                it.copy(
                    messages = emptyList()
                )
            }
        }
    }

    companion object {

        const val MISSING_API_KEY_MESSAGE =
            "GEMINI_API_KEY is missing. Add it to local.properties and rebuild."

        fun factory(
            repository: GeminiRepository,
            historyRepository: ChatHistoryRepository,
            hasApiKey: Boolean
        ) = object : ViewModelProvider.Factory {

            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>
            ): T {

                return ChatViewModel(
                    repository = repository,
                    historyRepository =
                        historyRepository,
                    hasApiKey = hasApiKey
                ) as T
            }
        }
    }
}