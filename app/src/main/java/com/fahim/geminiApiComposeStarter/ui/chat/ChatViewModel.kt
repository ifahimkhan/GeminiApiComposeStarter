package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fahim.geminiApiComposeStarter.data.ConversationMessage
import com.fahim.geminiApiComposeStarter.data.ConversationRole
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
    private var nextMessageId = 0L

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

        val history = _uiState.value.messages.map { it.toConversationMessage() }
        val userMessage = ChatMessage(
            id = nextMessageId++,
            text = prompt,
            author = ChatAuthor.USER,
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
            var streamedAnyText = false
            var failed = false
            val geminiMessageId = nextMessageId++

            repository.generateTextStream(prompt, history).collect { result ->
                result.fold(
                    onSuccess = { chunk ->
                        streamedAnyText = true
                        _uiState.update { state ->
                            val existingMessage = state.messages.firstOrNull { it.id == geminiMessageId }
                            val messages = if (existingMessage == null) {
                                state.messages + ChatMessage(
                                    id = geminiMessageId,
                                    text = chunk,
                                    author = ChatAuthor.GEMINI,
                                )
                            } else {
                                state.messages.map { message ->
                                    if (message.id == geminiMessageId) {
                                        message.copy(text = message.text + chunk)
                                    } else {
                                        message
                                    }
                                }
                            }
                            state.copy(messages = messages)
                        }
                    },
                    onFailure = { error ->
                        failed = true
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = error.message ?: "Gemini request failed.",
                            )
                        }
                    },
                )
            }

            if (!failed) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = if (streamedAnyText) null else "Empty response from Gemini",
                    )
                }
            }
        }
    }

    private fun ChatMessage.toConversationMessage(): ConversationMessage = ConversationMessage(
        role = when (author) {
            ChatAuthor.USER -> ConversationRole.USER
            ChatAuthor.GEMINI -> ConversationRole.MODEL
        },
        text = text,
    )

    companion object {
        const val MISSING_API_KEY_MESSAGE =
            "GEMINI_API_KEY is missing. Add it to local.properties and rebuild."

        fun factory(repository: GeminiRepository, hasApiKey: Boolean) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ChatViewModel(repository, hasApiKey) as T
            }
    }
}
