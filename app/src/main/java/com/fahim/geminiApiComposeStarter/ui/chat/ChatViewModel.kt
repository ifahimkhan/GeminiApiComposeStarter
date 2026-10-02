package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.local.ChatDao
import com.fahim.geminiApiComposeStarter.data.local.ChatMessageEntity
import com.fahim.geminiApiComposeStarter.data.local.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import com.fahim.geminiApiComposeStarter.data.local.PreferencesRepository

class ChatViewModel(
    private val repository: GeminiRepository,
    private val chatDao: ChatDao,
    private val preferencesRepository: PreferencesRepository,
    private val hasApiKey: Boolean,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState(hasApiKey = hasApiKey))
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        // Collect persistent chat history from Room
        viewModelScope.launch {
            chatDao.getAllMessages().collectLatest { messageList ->
                _uiState.update { it.copy(messages = messageList) }
            }
        }

        // Collect user preferences from DataStore
        viewModelScope.launch {
            preferencesRepository.preferencesFlow.collectLatest { prefs ->
                _uiState.update {
                    it.copy(
                        userName = prefs.userName,
                        systemPrompt = prefs.systemPrompt
                    )
                }
            }
        }
    }

    fun onPromptChange(value: String) {
        _uiState.update { it.copy(prompt = value, promptError = null) }
    }

    fun onSend() {
        val currentPrompt = _uiState.value.prompt.trim()
        if (currentPrompt.isEmpty()) {
            _uiState.update { it.copy(promptError = PromptError.EMPTY) }
            return
        }
        if (!hasApiKey) {
            _uiState.update { it.copy(errorMessage = MISSING_API_KEY_MESSAGE) }
            return
        }
        if (_uiState.value.isLoading) return

        val systemInstruction = _uiState.value.systemPrompt.trim().ifEmpty { null }

        // Clear prompt field immediately and show loading
        _uiState.update {
            it.copy(
                prompt = "",
                isLoading = true,
                errorMessage = null,
                promptError = null
            )
        }

        viewModelScope.launch {
            // Persist the user's message into Room
            chatDao.insertMessage(
                ChatMessageEntity(
                    text = currentPrompt,
                    isUser = true,
                    timestamp = System.currentTimeMillis()
                )
            )

            // Request generation from repository
            repository.generateText(currentPrompt, systemInstruction).fold(
                onSuccess = { responseText ->
                    // Persist assistant's reply into Room
                    chatDao.insertMessage(
                        ChatMessageEntity(
                            text = responseText,
                            isUser = false,
                            timestamp = System.currentTimeMillis()
                        )
                    )
                    _uiState.update { it.copy(isLoading = false) }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Something went wrong while generating response",
                        )
                    }
                }
            )
        }
    }

    fun onVoiceInputResult(spokenText: String) {
        if (spokenText.isBlank()) return
        _uiState.update { current ->
            val newPrompt = if (current.prompt.isBlank()) {
                spokenText.trim()
            } else {
                "${current.prompt.trim()} ${spokenText.trim()}"
            }
            current.copy(prompt = newPrompt, promptError = null)
        }
    }

    fun onClearChat() {
        viewModelScope.launch {
            chatDao.clearMessages()
        }
    }

    fun onDeleteMessage(messageId: Long) {
        viewModelScope.launch {
            chatDao.deleteMessage(messageId)
        }
    }

    fun onToggleSettings(isOpen: Boolean) {
        _uiState.update { it.copy(isSettingsOpen = isOpen) }
    }

    fun onUpdateUserName(name: String) {
        viewModelScope.launch {
            preferencesRepository.updateUserName(name.trim())
        }
    }

    fun onUpdateSystemPrompt(prompt: String) {
        viewModelScope.launch {
            preferencesRepository.updateSystemPrompt(prompt.trim())
        }
    }

    fun onDismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    companion object {
        const val MISSING_API_KEY_MESSAGE =
            "GEMINI_API_KEY is missing. Add it to local.properties or environment variable and rebuild."

        fun factory(
            repository: GeminiRepository,
            chatDao: ChatDao,
            preferencesRepository: PreferencesRepository,
            hasApiKey: Boolean
        ) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ChatViewModel(repository, chatDao, preferencesRepository, hasApiKey) as T
        }
    }
}
