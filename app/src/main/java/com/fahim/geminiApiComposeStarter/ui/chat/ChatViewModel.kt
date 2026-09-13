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
    private val chatMessageDao: ChatMessageDao? = null,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        observeMessagesFromDb()
    }

    private fun observeMessagesFromDb() {
        chatMessageDao?.let { dao ->
            viewModelScope.launch {
                dao.getAllMessages().collect { entities ->
                    val domainMessages = entities.map { entity ->
                        ChatMessage(
                            id = entity.id.toString(),
                            text = entity.text,
                            isUser = entity.isUser,
                            timestamp = entity.timestamp,
                        )
                    }
                    _uiState.update { it.copy(messages = domainMessages) }
                }
            }
        }
    }

    fun onPromptChange(value: String) {
        _uiState.update { it.copy(prompt = value, promptError = null) }
    }

    fun onSend() {
        val promptText = _uiState.value.prompt.trim()
        if (promptText.isEmpty()) {
            _uiState.update { it.copy(promptError = PromptError.EMPTY) }
            return
        }
        if (!hasApiKey) {
            _uiState.update { it.copy(errorMessage = MISSING_API_KEY_MESSAGE) }
            return
        }
        if (_uiState.value.isLoading) return

        val now = System.currentTimeMillis()
        val userMessage = ChatMessage(text = promptText, isUser = true, timestamp = now)
        _uiState.update {
            it.copy(
                prompt = "",
                messages = if (chatMessageDao != null) it.messages else it.messages + userMessage,
                isLoading = true,
                errorMessage = null,
                promptError = null,
            )
        }

        viewModelScope.launch {
            chatMessageDao?.insertMessage(
                ChatMessageEntity(text = promptText, isUser = true, timestamp = now)
            )

            repository.generateText(promptText).fold(
                onSuccess = { responseText ->
                    val responseTime = System.currentTimeMillis()
                    val geminiMessage = ChatMessage(text = responseText, isUser = false, timestamp = responseTime)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            messages = if (chatMessageDao != null) it.messages else it.messages + geminiMessage,
                        )
                    }
                    chatMessageDao?.insertMessage(
                        ChatMessageEntity(text = responseText, isUser = false, timestamp = responseTime)
                    )
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
        const val MISSING_API_KEY_MESSAGE =
            "GEMINI_API_KEY is missing. Add it to local.properties and rebuild."

        fun factory(
            repository: GeminiRepository,
            hasApiKey: Boolean,
            chatMessageDao: ChatMessageDao? = null,
        ) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ChatViewModel(repository, hasApiKey, chatMessageDao) as T
        }
    }
}
