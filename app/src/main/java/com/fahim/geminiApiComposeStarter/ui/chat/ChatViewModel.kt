package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.local.ChatDao
import com.fahim.geminiApiComposeStarter.data.local.ChatMessageEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(
    private val repository: GeminiRepository,
    private val hasApiKey: Boolean,
    private val chatDao: ChatDao? = null,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        observePersistedMessages()
    }

    private fun observePersistedMessages() {
        if (chatDao == null) return
        viewModelScope.launch {
            chatDao.getAllMessages().collect { entities ->
                _uiState.update { current ->
                    current.copy(
                        messages = entities.map { entity ->
                            ChatMessage(
                                id = entity.id,
                                sender = if (entity.sender == MessageSender.USER.name) {
                                    MessageSender.USER
                                } else {
                                    MessageSender.GEMINI
                                },
                                text = entity.text,
                                timestamp = entity.timestamp,
                                isError = entity.isError,
                            )
                        },
                        response = entities.lastOrNull { it.sender == MessageSender.GEMINI.name && !it.isError }?.text.orEmpty(),
                    )
                }
            }
        }
    }

    fun onPromptChange(value: String) {
        _uiState.update { it.copy(prompt = value, promptError = null) }
    }

    fun onVoiceInput(spokenText: String) {
        val trimmed = spokenText.trim()
        if (trimmed.isEmpty()) return
        _uiState.update { current ->
            val updated = if (current.prompt.isBlank()) {
                trimmed
            } else {
                "${current.prompt.trim()} $trimmed"
            }
            current.copy(prompt = updated, promptError = null)
        }
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

        executePrompt(prompt)
    }

    fun onRetry(prompt: String) {
        val trimmed = prompt.trim()
        if (trimmed.isEmpty() || _uiState.value.isLoading) return
        if (!hasApiKey) {
            _uiState.update { it.copy(errorMessage = MISSING_API_KEY_MESSAGE) }
            return
        }
        executePrompt(trimmed)
    }

    private fun executePrompt(promptText: String) {
        _uiState.update {
            it.copy(
                prompt = "",
                isLoading = true,
                errorMessage = null,
                promptError = null,
            )
        }

        viewModelScope.launch {
            val userTimestamp = System.currentTimeMillis()
            if (chatDao != null) {
                chatDao.insertMessage(
                    ChatMessageEntity(
                        sender = MessageSender.USER.name,
                        text = promptText,
                        timestamp = userTimestamp,
                    )
                )
            } else {
                _uiState.update { current ->
                    val userMsg = ChatMessage(
                        id = userTimestamp,
                        sender = MessageSender.USER,
                        text = promptText,
                        timestamp = userTimestamp,
                    )
                    current.copy(messages = current.messages + userMsg)
                }
            }

            repository.generateText(promptText).fold(
                onSuccess = { responseText ->
                    val geminiTimestamp = System.currentTimeMillis()
                    if (chatDao != null) {
                        chatDao.insertMessage(
                            ChatMessageEntity(
                                sender = MessageSender.GEMINI.name,
                                text = responseText,
                                timestamp = geminiTimestamp,
                            )
                        )
                    } else {
                        _uiState.update { current ->
                            val geminiMsg = ChatMessage(
                                id = geminiTimestamp,
                                sender = MessageSender.GEMINI,
                                text = responseText,
                                timestamp = geminiTimestamp,
                            )
                            current.copy(
                                isLoading = false,
                                response = responseText,
                                messages = current.messages + geminiMsg,
                            )
                        }
                    }
                    _uiState.update { it.copy(isLoading = false, response = responseText) }
                },
                onFailure = { error ->
                    val errorMsg = error.message ?: "Something went wrong"
                    val errorTimestamp = System.currentTimeMillis()
                    if (chatDao != null) {
                        chatDao.insertMessage(
                            ChatMessageEntity(
                                sender = MessageSender.GEMINI.name,
                                text = errorMsg,
                                timestamp = errorTimestamp,
                                isError = true,
                            )
                        )
                    } else {
                        _uiState.update { current ->
                            val errorChatMsg = ChatMessage(
                                id = errorTimestamp,
                                sender = MessageSender.GEMINI,
                                text = errorMsg,
                                timestamp = errorTimestamp,
                                isError = true,
                            )
                            current.copy(
                                isLoading = false,
                                errorMessage = errorMsg,
                                messages = current.messages + errorChatMsg,
                            )
                        }
                    }
                    _uiState.update { it.copy(isLoading = false, errorMessage = errorMsg) }
                },
            )
        }
    }

    fun onClearHistory() {
        viewModelScope.launch {
            if (chatDao != null) {
                chatDao.clearAllMessages()
            }
            _uiState.update {
                it.copy(
                    messages = emptyList(),
                    response = "",
                    errorMessage = null,
                )
            }
        }
    }

    fun onToggleTheme(isCurrentSystemDark: Boolean) {
        _uiState.update { current ->
            val currentDark = current.isDarkTheme ?: isCurrentSystemDark
            current.copy(isDarkTheme = !currentDark)
        }
    }


    companion object {
        const val MISSING_API_KEY_MESSAGE =
            "GEMINI_API_KEY is missing. Add it to local.properties and rebuild."

        fun factory(
            repository: GeminiRepository,
            hasApiKey: Boolean,
            chatDao: ChatDao? = null,
        ) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ChatViewModel(repository, hasApiKey, chatDao) as T
        }
    }
}
