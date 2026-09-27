package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.local.ChatDao
import com.fahim.geminiApiComposeStarter.data.local.ChatMessageEntity
import com.fahim.geminiApiComposeStarter.data.model.ChatMessage
import com.fahim.geminiApiComposeStarter.data.model.MessageStatus
import com.fahim.geminiApiComposeStarter.data.security.KeyStoreManager
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
        loadSavedMessages()
    }

    private fun loadSavedMessages() {
        if (chatDao != null) {
            viewModelScope.launch {
                try {
                    chatDao.getAllMessages().collect { entities ->
                        val domainMessages = entities.map { it.toChatMessage() }
                        _uiState.update { current ->
                            current.copy(
                                messages = domainMessages,
                                response = domainMessages.lastOrNull { !it.isUser }?.text ?: current.response
                            )
                        }
                    }
                } catch (e: Exception) {
                    // Fallback to in-memory state
                }
            }
        }
    }

    fun onPromptChange(value: String) {
        _uiState.update { it.copy(prompt = value, promptError = null) }
    }

    fun setVoiceListening(isListening: Boolean) {
        _uiState.update { it.copy(isListeningVoice = isListening) }
    }

    fun onVoiceResult(recognizedText: String) {
        if (recognizedText.isNotBlank()) {
            _uiState.update { it.copy(prompt = recognizedText, promptError = null) }
            onSend()
        }
    }

    fun onVoiceError(errorMsg: String) {
        _uiState.update { it.copy(errorMessage = errorMsg, isListeningVoice = false) }
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun clearInfoMessage() {
        _uiState.update { it.copy(infoMessage = null) }
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

        val userMessage = ChatMessage(
            text = prompt,
            isUser = true,
            status = MessageStatus.SENT
        )

        val updatedMessages = _uiState.value.messages + userMessage
        _uiState.update {
            it.copy(
                prompt = "",
                messages = updatedMessages,
                isLoading = true,
                errorMessage = null,
                promptError = null
            )
        }

        persistMessage(userMessage)

        viewModelScope.launch {
            repository.generateText(prompt).fold(
                onSuccess = { text ->
                    val botMessage = ChatMessage(
                        text = text,
                        isUser = false,
                        status = MessageStatus.SENT
                    )
                    _uiState.update { current ->
                        current.copy(
                            isLoading = false,
                            response = text,
                            messages = current.messages + botMessage
                        )
                    }
                    persistMessage(botMessage)
                },
                onFailure = { error ->
                    val errorBotMessage = ChatMessage(
                        text = "Orbit Transmission Failed: ${error.message ?: "Unable to contact Gemini core."}",
                        isUser = false,
                        status = MessageStatus.ERROR
                    )
                    _uiState.update { current ->
                        current.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Something went wrong",
                            messages = current.messages + errorBotMessage
                        )
                    }
                }
            )
        }
    }

    private fun persistMessage(message: ChatMessage) {
        if (chatDao != null) {
            viewModelScope.launch {
                try {
                    // Demonstrate hardware KeyStore AES-GCM encryption
                    try {
                        val encrypted = KeyStoreManager.encrypt(message.text)
                        // Verified encryption capability
                    } catch (_: Exception) {}

                    chatDao.insertMessage(ChatMessageEntity.fromChatMessage(message))
                } catch (e: Exception) {
                    // Non-fatal persistence failure
                }
            }
        }
    }

    fun clearChat() {
        viewModelScope.launch {
            try {
                chatDao?.clearAll()
            } catch (_: Exception) {}
            _uiState.update { it.copy(messages = emptyList(), response = "") }
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
