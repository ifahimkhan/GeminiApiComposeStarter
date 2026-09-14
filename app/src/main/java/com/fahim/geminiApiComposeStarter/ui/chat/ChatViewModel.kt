package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.local.ChatMessageDao
import com.fahim.geminiApiComposeStarter.data.local.ChatMessageEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(
    private val repository: GeminiRepository,
    private val hasApiKey: Boolean,
    private val chatMessageDao: ChatMessageDao,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        loadChatHistory()
    }

    private fun loadChatHistory() {
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
                    it.copy(
                        messages = messages,
                        response = messages.lastOrNull { !it.isUser }?.text ?: "",
                    )
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

        _uiState.update {
            it.copy(
                prompt = "",
                isLoading = true,
                errorMessage = null,
                promptError = null,
            )
        }

        viewModelScope.launch {
            val userMessageId = chatMessageDao.insertMessage(
                ChatMessageEntity(
                    text = prompt,
                    isUser = true,
                )
            )

            _uiState.update {
                it.copy(
                    messages = it.messages + ChatMessage(
                        id = userMessageId,
                        text = prompt,
                        isUser = true,
                    )
                )
            }

            repository.generateText(prompt).fold(
                onSuccess = { text ->

                    val geminiMessageId = chatMessageDao.insertMessage(
                        ChatMessageEntity(
                            text = text,
                            isUser = false,
                        )
                    )

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            response = text,
                            messages = it.messages + ChatMessage(
                                id = geminiMessageId,
                                text = text,
                                isUser = false,
                            ),
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
            chatMessageDao: ChatMessageDao,
        ) =
            object : ViewModelProvider.Factory {

                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(
                    modelClass: Class<T>
                ): T =
                    ChatViewModel(
                        repository = repository,
                        hasApiKey = hasApiKey,
                        chatMessageDao = chatMessageDao,
                    ) as T
            }
    }
}