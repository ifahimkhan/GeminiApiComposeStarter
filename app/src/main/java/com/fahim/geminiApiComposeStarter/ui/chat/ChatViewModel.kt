package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fahim.geminiApiComposeStarter.data.ChatMessageEntity
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.MessageDao
import com.fahim.geminiApiComposeStarter.data.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(
    private val repository: GeminiRepository,
    private val messageDao: MessageDao?,
    private val userPreferencesRepository: UserPreferencesRepository? = null,
    private val hasApiKey: Boolean,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        messageDao?.let { dao ->
            viewModelScope.launch {
                dao.getAllMessages().collect { entities ->
                    val chatMessages = entities.map {
                        ChatMessage(
                            id = it.id,
                            text = it.text,
                            isUser = it.isUser,
                            timestamp = it.timestamp
                        )
                    }
                    _uiState.update { it.copy(messages = chatMessages) }
                }
            }
        }

        userPreferencesRepository?.let { prefs ->
            viewModelScope.launch {
                prefs.darkModeFlow.collect { isDark ->
                    if (isDark != null) {
                        _uiState.update { it.copy(isDarkMode = isDark) }
                    }
                }
            }
        }
    }

    fun onPromptChange(value: String) {
        _uiState.update { it.copy(prompt = value, promptError = null) }
    }

    /** Called by the UI after the Snackbar has shown the error, so the same error can show again later. */
    fun onErrorShown() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun toggleDarkMode() {
        val newMode = !_uiState.value.isDarkMode
        _uiState.update { it.copy(isDarkMode = newMode) }
        userPreferencesRepository?.let { prefs ->
            viewModelScope.launch {
                prefs.setDarkMode(newMode)
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

        _uiState.update { it.copy(isLoading = true, errorMessage = null, promptError = null, prompt = "") }

        viewModelScope.launch {
            // Insert user message
            messageDao?.insertMessage(
                ChatMessageEntity(text = prompt, isUser = true)
            )

            repository.generateText(prompt).fold(
                onSuccess = { text ->
                    messageDao?.insertMessage(
                        ChatMessageEntity(text = text, isUser = false)
                    )
                    _uiState.update { it.copy(isLoading = false) }
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
            messageDao: MessageDao? = null,
            userPreferencesRepository: UserPreferencesRepository? = null,
            hasApiKey: Boolean
        ) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ChatViewModel(repository, messageDao, userPreferencesRepository, hasApiKey) as T
        }
    }
}
