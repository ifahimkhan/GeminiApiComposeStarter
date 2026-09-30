package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.local.ChatDao
import com.fahim.geminiApiComposeStarter.data.local.ChatMessageEntity
import com.fahim.geminiApiComposeStarter.data.local.MessageSender
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(
    private val repository: GeminiRepository,
    private val chatDao: ChatDao? = null,
    private val hasApiKey: Boolean,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        // Observe persistent chat messages from Room/SQLite DAO
        chatDao?.let { dao ->
            viewModelScope.launch {
                dao.getAllMessages().collect { list ->
                    _uiState.update { it.copy(messages = list) }
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

        // 1. Immediately create user message
        val userMsg = ChatMessageEntity(
            text = prompt,
            sender = MessageSender.USER,
            timestamp = System.currentTimeMillis(),
        )

        // Clear prompt and set loading
        _uiState.update {
            it.copy(
                prompt = "",
                isLoading = true,
                errorMessage = null,
                promptError = null,
                // If in-memory (no DAO), append directly
                messages = if (chatDao == null) it.messages + userMsg else it.messages
            )
        }

        viewModelScope.launch {
            chatDao?.insertMessage(userMsg)

            // 2. Call Gemini
            repository.generateText(prompt).fold(
                onSuccess = { responseText ->
                    val geminiMsg = ChatMessageEntity(
                        text = responseText,
                        sender = MessageSender.GEMINI,
                        timestamp = System.currentTimeMillis(),
                    )
                    chatDao?.insertMessage(geminiMsg)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            messages = if (chatDao == null) it.messages + geminiMsg else it.messages
                        )
                    }
                },
                onFailure = { error ->
                    val errorMsg = error.message ?: "Failed to generate response. Please try again."
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = errorMsg,
                        )
                    }
                }
            )
        }
    }

    fun onClearChat() {
        viewModelScope.launch {
            chatDao?.clearAllMessages()
            if (chatDao == null) {
                _uiState.update { it.copy(messages = emptyList()) }
            }
        }
    }

    fun onDismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    companion object {
        const val MISSING_API_KEY_MESSAGE =
            "GEMINI_API_KEY is missing. Add it to local.properties and rebuild."

        fun factory(
            repository: GeminiRepository,
            chatDao: ChatDao? = null,
            hasApiKey: Boolean
        ) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ChatViewModel(repository, chatDao, hasApiKey) as T
        }
    }
}
