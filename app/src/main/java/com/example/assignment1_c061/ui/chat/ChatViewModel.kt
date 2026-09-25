package com.example.assignment1_c061.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.assignment1_c061.data.GeminiRepository
import com.example.assignment1_c061.data.db.ChatMessageDao
import com.example.assignment1_c061.data.db.ChatMessageEntity
import com.example.assignment1_c061.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(
    private val repository: GeminiRepository,
    private val hasApiKey: Boolean,
    private val messageDao: ChatMessageDao? = null,
    private val userPreferencesRepository: UserPreferencesRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        observeSavedMessages()
    }

    private fun observeSavedMessages() {
        messageDao?.let { dao ->
            viewModelScope.launch {
                dao.getAllMessages().collect { entities ->
                    val domainMessages = entities.map { entity ->
                        ChatMessage(
                            id = entity.id,
                            sender = if (entity.sender == MessageSender.USER.name) MessageSender.USER else MessageSender.GEMINI,
                            text = entity.text,
                            timestamp = entity.timestamp
                        )
                    }
                    _uiState.update { state ->
                        if (state.messages.isEmpty() && domainMessages.isNotEmpty()) {
                            state.copy(messages = domainMessages)
                        } else {
                            state
                        }
                    }
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

        val userMessage = ChatMessage(sender = MessageSender.USER, text = promptText)
        _uiState.update {
            it.copy(
                prompt = "",
                messages = it.messages + userMessage,
                isLoading = true,
                errorMessage = null,
                promptError = null
            )
        }

        viewModelScope.launch {
            messageDao?.insertMessage(
                ChatMessageEntity(
                    id = userMessage.id,
                    sender = userMessage.sender.name,
                    text = userMessage.text,
                    timestamp = userMessage.timestamp
                )
            )
            userPreferencesRepository?.saveLastQuery(promptText)

            repository.generateText(promptText).fold(
                onSuccess = { responseText ->
                    val geminiMessage = ChatMessage(sender = MessageSender.GEMINI, text = responseText)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            messages = it.messages + geminiMessage
                        )
                    }
                    messageDao?.insertMessage(
                        ChatMessageEntity(
                            id = geminiMessage.id,
                            sender = geminiMessage.sender.name,
                            text = geminiMessage.text,
                            timestamp = geminiMessage.timestamp
                        )
                    )
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Something went wrong"
                        )
                    }
                }
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    companion object {
        const val MISSING_API_KEY_MESSAGE =
            "GEMINI_API_KEY is missing. Add it to local.properties and rebuild."

        fun factory(
            repository: GeminiRepository,
            hasApiKey: Boolean,
            messageDao: ChatMessageDao? = null,
            userPreferencesRepository: UserPreferencesRepository? = null
        ) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ChatViewModel(repository, hasApiKey, messageDao, userPreferencesRepository) as T
        }
    }
}
