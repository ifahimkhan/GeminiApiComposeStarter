package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fahim.geminiApiComposeStarter.data.ChatHistoryRepository
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.local.ChatMessageEntity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(
    private val repository: GeminiRepository,
    private val historyRepository: ChatHistoryRepository,
    private val hasApiKey: Boolean,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()
    private val _messages = MutableStateFlow<List<ChatMessageEntity>>(emptyList())
    val messages: StateFlow<List<ChatMessageEntity>> = _messages.asStateFlow()

    init {
        viewModelScope.launch {
            historyRepository.observeMessages()
                .catch { error ->
                    _uiState.update {
                        it.copy(errorMessage = error.message ?: "Unable to load chat history")
                    }
                }
                .collect { messages ->
                    _messages.value = messages
                    _uiState.update { state ->
                        state.copy(
                            response = messages.lastOrNull { it.role == ROLE_GEMINI }?.content.orEmpty(),
                        )
                    }
                }
        }
    }

    fun onPromptChange(value: String) {
        _uiState.update { it.copy(prompt = value, promptError = null) }
    }

    fun onSend() {
        val prompt = _uiState.value.prompt.trim()
        if (prompt.isEmpty()) {
            _uiState.update { it.copy(promptError = PromptError.EMPTY) }
            return
        }
        if (!hasApiKey) {
            _uiState.update { it.copy(errorMessage = MISSING_API_KEY_MESSAGE) }
            return
        }
        if (_uiState.value.isLoading) return

        _uiState.update { it.copy(isLoading = true, errorMessage = null, promptError = null) }
        viewModelScope.launch {
            try {
                historyRepository.insertMessage(
                    ChatMessageEntity(role = ROLE_USER, content = prompt),
                )
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Unable to save chat history",
                    )
                }
                return@launch
            }

            repository.generateText(prompt).fold(
                onSuccess = { text ->
                    try {
                        historyRepository.insertMessage(
                            ChatMessageEntity(role = ROLE_GEMINI, content = text),
                        )
                        _uiState.update { it.copy(isLoading = false, response = text) }
                    } catch (error: CancellationException) {
                        throw error
                    } catch (error: Exception) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                response = text,
                                errorMessage = error.message ?: "Unable to save chat history",
                            )
                        }
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Something went wrong",
                        )
                    }
                },
            )
        }
    }

    companion object {
        private const val ROLE_USER = "user"
        private const val ROLE_GEMINI = "gemini"

        const val MISSING_API_KEY_MESSAGE =
            "GEMINI_API_KEY is missing. Add it to local.properties and rebuild."

        fun factory(
            repository: GeminiRepository,
            historyRepository: ChatHistoryRepository,
            hasApiKey: Boolean,
        ) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ChatViewModel(repository, historyRepository, hasApiKey) as T
            }
    }
}
