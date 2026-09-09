package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fahim.geminiApiComposeStarter.data.ChatMessageDao
import com.fahim.geminiApiComposeStarter.data.ChatMessageEntity
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(
    private val repository: GeminiRepository,
    private val chatMessageDao: ChatMessageDao,
    private val hasApiKey: Boolean,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        observeChatHistory()
    }

    private fun observeChatHistory() {
        viewModelScope.launch {
            chatMessageDao.observeMessages().collect { entities ->
                val messages = entities.map { entity ->
                    ChatMessage(
                        id = entity.id,
                        text = entity.text,
                        isUser = entity.isUser,
                    )
                }

                _uiState.update {
                    it.copy(messages = messages)
                }
            }
        }
    }

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
                it.copy(errorMessage = MISSING_API_KEY_MESSAGE)
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
                isLoading = true,
                errorMessage = null,
                promptError = null,
            )
        }

        viewModelScope.launch {

            // Save the user's message immediately.
            chatMessageDao.insertMessage(
                ChatMessageEntity(
                    id = userMessage.id,
                    text = userMessage.text,
                    isUser = true,
                )
            )

            repository.generateText(prompt).fold(

                onSuccess = { text ->
                    val assistantMessage = ChatMessage(
                        id = System.currentTimeMillis(),
                        text = text,
                        isUser = false,
                    )

                    // Save Gemini's response to Room.
                    chatMessageDao.insertMessage(
                        ChatMessageEntity(
                            id = assistantMessage.id,
                            text = assistantMessage.text,
                            isUser = false,
                        )
                    )

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = null,
                        )
                    }
                },

                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message
                                ?: "Something went wrong",
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
            chatMessageDao: ChatMessageDao,
            hasApiKey: Boolean,
        ) = object : ViewModelProvider.Factory {

            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
            ): T {
                return ChatViewModel(
                    repository = repository,
                    chatMessageDao = chatMessageDao,
                    hasApiKey = hasApiKey,
                ) as T
            }
        }
    }
}