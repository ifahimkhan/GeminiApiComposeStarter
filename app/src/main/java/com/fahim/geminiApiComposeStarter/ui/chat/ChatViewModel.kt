package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.UserPreferencesRepository
import com.fahim.geminiApiComposeStarter.data.local.ChatMessageDao
import com.fahim.geminiApiComposeStarter.data.local.toChatMessage
import com.fahim.geminiApiComposeStarter.data.local.toEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(
    private val repository: GeminiRepository,
    private val hasApiKey: Boolean,
    private val userPreferencesRepository: UserPreferencesRepository? = null,
    private val chatMessageDao: ChatMessageDao? = null,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        userPreferencesRepository?.let { prefs ->
            viewModelScope.launch {
                prefs.isExpandedInput.collect { expanded ->
                    _uiState.update { it.copy(isExpandedInput = expanded) }
                }
            }
        }
        chatMessageDao?.let { dao ->
            viewModelScope.launch {
                dao.getAllMessages().collect { entities ->
                    val restoredMessages = entities.map { it.toChatMessage() }
                    _uiState.update { it.copy(messages = restoredMessages) }
                }
            }
        }
    }

    fun onPromptChange(value: String) {
        _uiState.update { it.copy(prompt = value, promptError = null) }
    }

    fun toggleInputMode() {
        val newValue = !_uiState.value.isExpandedInput
        _uiState.update { it.copy(isExpandedInput = newValue) }
        userPreferencesRepository?.let { prefs ->
            viewModelScope.launch {
                prefs.setExpandedInput(newValue)
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

        val userMessage = ChatMessage(text = prompt, isUser = true)
        _uiState.update {
            it.copy(
                prompt = "",
                isLoading = true,
                errorMessage = null,
                promptError = null,
                messages = it.messages + userMessage,
            )
        }
        chatMessageDao?.let { dao ->
            viewModelScope.launch {
                dao.insertMessage(userMessage.toEntity())
            }
        }

        viewModelScope.launch {
            repository.generateText(prompt).fold(
                onSuccess = { text ->
                    val geminiMessage = ChatMessage(text = text, isUser = false)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            response = text,
                            messages = it.messages + geminiMessage,
                        )
                    }
                    chatMessageDao?.let { dao ->
                        viewModelScope.launch {
                            dao.insertMessage(geminiMessage.toEntity())
                        }
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Failed to generate response. Please try again.",
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
            userPreferencesRepository: UserPreferencesRepository? = null,
            chatMessageDao: ChatMessageDao? = null,
        ) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ChatViewModel(
                    repository = repository,
                    hasApiKey = hasApiKey,
                    userPreferencesRepository = userPreferencesRepository,
                    chatMessageDao = chatMessageDao,
                ) as T
        }
    }
}
