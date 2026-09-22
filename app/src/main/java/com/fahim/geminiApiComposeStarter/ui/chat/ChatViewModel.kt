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

    private val _uiState = MutableStateFlow(ChatUiState(hasApiKey = hasApiKey))
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        // Collect past conversations persisted in Room so chat survives app restarts
        chatDao?.let { dao ->
            viewModelScope.launch {
                dao.getAllMessages().collect { entities ->
                    _uiState.update { current ->
                        current.copy(
                            messages = entities.map { entity ->
                                ChatMessage(
                                    id = entity.id,
                                    text = entity.text,
                                    isFromUser = entity.isFromUser,
                                    timestamp = entity.timestamp
                                )
                            }
                        )
                    }
                }
            }
        }
    }

    fun onPromptChange(value: String) {
        _uiState.update { it.copy(prompt = value, promptError = null) }
    }

    fun onVoiceInputResult(recognizedText: String) {
        if (recognizedText.isNotBlank()) {
            _uiState.update { it.copy(prompt = recognizedText, promptError = null) }
        }
    }

    fun onDismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun onClearHistory() {
        viewModelScope.launch {
            if (chatDao != null) {
                chatDao.clearAllMessages()
            } else {
                _uiState.update { it.copy(messages = emptyList()) }
            }
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

        // Clear input field and set loading
        _uiState.update { it.copy(prompt = "", isLoading = true, errorMessage = null, promptError = null) }

        viewModelScope.launch {
            // Save user message to Room or in-memory state
            val userTimestamp = System.currentTimeMillis()
            if (chatDao != null) {
                chatDao.insertMessage(
                    ChatMessageEntity(
                        text = prompt,
                        isFromUser = true,
                        timestamp = userTimestamp
                    )
                )
            } else {
                _uiState.update { current ->
                    val userMsg = ChatMessage(
                        id = current.messages.size.toLong() + 1,
                        text = prompt,
                        isFromUser = true,
                        timestamp = userTimestamp
                    )
                    current.copy(messages = current.messages + userMsg)
                }
            }

            // Call Gemini API off the main thread
            repository.generateText(prompt).fold(
                onSuccess = { responseText ->
                    val geminiTimestamp = System.currentTimeMillis()
                    if (chatDao != null) {
                        chatDao.insertMessage(
                            ChatMessageEntity(
                                text = responseText,
                                isFromUser = false,
                                timestamp = geminiTimestamp
                            )
                        )
                    } else {
                        _uiState.update { current ->
                            val geminiMsg = ChatMessage(
                                id = current.messages.size.toLong() + 1,
                                text = responseText,
                                isFromUser = false,
                                timestamp = geminiTimestamp
                            )
                            current.copy(messages = current.messages + geminiMsg)
                        }
                    }
                    _uiState.update { it.copy(isLoading = false) }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Something went wrong while connecting to Gemini",
                        )
                    }
                }
            )
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
