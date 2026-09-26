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
    private val chatDao: ChatDao? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        observeSavedMessages()
    }

    private fun observeSavedMessages() {
        chatDao?.let { dao ->
            viewModelScope.launch {
                dao.getAllMessages().collect { entities ->
                    val uiMessages = entities.map { it.toUiModel() }
                    _uiState.update { it.copy(messages = uiMessages) }
                }
            }
        }
    }

    fun onPromptChange(value: String) {
        _uiState.update { it.copy(prompt = value, promptError = null) }
    }

    fun onVoiceResult(recognizedText: String) {
        if (recognizedText.isNotBlank()) {
            _uiState.update { it.copy(prompt = recognizedText, promptError = null) }
            onSend()
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

        val userMessage = ChatMessage(text = prompt, isUser = true)
        
        // Add user message locally or to Room
        if (chatDao != null) {
            viewModelScope.launch {
                chatDao.insertMessage(
                    ChatMessageEntity(text = prompt, isUser = true)
                )
            }
        } else {
            _uiState.update { it.copy(messages = it.messages + userMessage) }
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
            repository.generateText(prompt).fold(
                onSuccess = { responseText ->
                    if (chatDao != null) {
                        chatDao.insertMessage(
                            ChatMessageEntity(text = responseText, isUser = false)
                        )
                    } else {
                        val botMessage = ChatMessage(text = responseText, isUser = false)
                        _uiState.update { state -> state.copy(messages = state.messages + botMessage) }
                    }
                    _uiState.update { it.copy(isLoading = false) }
                },
                onFailure = { error ->
                    val errorMsg = error.message ?: "Failed to generate response"
                    if (chatDao != null) {
                        chatDao.insertMessage(
                            ChatMessageEntity(text = "Error: $errorMsg", isUser = false, isError = true)
                        )
                    } else {
                        val errorUiMessage = ChatMessage(text = "Error: $errorMsg", isUser = false, isError = true)
                        _uiState.update { state -> state.copy(messages = state.messages + errorUiMessage) }
                    }
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            errorMessage = errorMsg
                        ) 
                    }
                }
            )
        }
    }

    fun onClearHistory() {
        if (chatDao != null) {
            viewModelScope.launch {
                chatDao.clearAllMessages()
            }
        } else {
            _uiState.update { it.copy(messages = emptyList()) }
        }
    }

    companion object {
        const val MISSING_API_KEY_MESSAGE =
            "GEMINI_API_KEY is missing. Add it to local.properties and rebuild."

        fun factory(
            repository: GeminiRepository, 
            hasApiKey: Boolean, 
            chatDao: ChatDao? = null
        ) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ChatViewModel(repository, hasApiKey, chatDao) as T
        }
    }
}
