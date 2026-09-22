package com.vaishnavi.n085.geminichat.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vaishnavi.n085.geminichat.data.GeminiRepository
import com.vaishnavi.n085.geminichat.data.local.ChatDao
import com.vaishnavi.n085.geminichat.data.local.Message
import com.vaishnavi.n085.geminichat.data.local.MessageRole
import com.vaishnavi.n085.geminichat.data.prefs.AppPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(
    private val repository: GeminiRepository,
    private val chatDao: ChatDao,
    private val preferences: AppPreferences,
    private val hasApiKey: Boolean,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        loadHistory()
        observeUserName()
    }

    private fun loadHistory() {
        viewModelScope.launch {
            val history = chatDao.getAll()
            _uiState.update { it.copy(messages = history) }
        }
    }

    private fun observeUserName() {
        viewModelScope.launch {
            preferences.userName.collect { name ->
                _uiState.update { it.copy(userName = name) }
            }
        }
    }

    fun onPromptChange(value: String) {
        _uiState.update { it.copy(prompt = value, promptError = null) }
    }

    fun onVoiceInput(value: String) {
        _uiState.update { it.copy(prompt = value, promptError = null) }
    }

    fun onUserNameChange(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch { preferences.setUserName(trimmed) }
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

        val userMessage = Message(id = nextId(), role = MessageRole.USER, content = prompt)
        _uiState.update {
            it.copy(
                messages = it.messages + userMessage,
                prompt = "",
                isLoading = true,
                promptError = null,
                errorMessage = null,
            )
        }
        viewModelScope.launch { chatDao.insert(userMessage) }

        viewModelScope.launch {
            repository.generateText(prompt).fold(
                onSuccess = { text ->
                    val assistantMessage = Message(id = nextId(), role = MessageRole.ASSISTANT, content = text)
                    _uiState.update { it.copy(isLoading = false, messages = it.messages + assistantMessage) }
                    chatDao.insert(assistantMessage)
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = error.message ?: "Something went wrong")
                    }
                },
            )
        }
    }

    fun clearConversation() {
        viewModelScope.launch {
            chatDao.clearAll()
            _uiState.update { it.copy(messages = emptyList(), errorMessage = null) }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    /** Stable, unique id for a new message (max existing id + 1). */
    private fun nextId(): Long = (_uiState.value.messages.maxOfOrNull { it.id } ?: 0L) + 1

    companion object {
        const val MISSING_API_KEY_MESSAGE =
            "GEMINI_API_KEY is missing. Add it to local.properties and rebuild."

        fun factory(
            repository: GeminiRepository,
            chatDao: ChatDao,
            preferences: AppPreferences,
            hasApiKey: Boolean,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ChatViewModel(repository, chatDao, preferences, hasApiKey) as T
        }
    }
}
