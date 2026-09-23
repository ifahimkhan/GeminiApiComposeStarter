package com.example.c008_nishit_assignment1.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.c008_nishit_assignment1.data.GeminiRepository
import com.example.c008_nishit_assignment1.data.local.ChatDao
import com.example.c008_nishit_assignment1.data.local.ChatMessageEntity
import com.example.c008_nishit_assignment1.data.preferences.PreferencesRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ChatViewModel handles business logic, state hoisting, Room database persistence,
 * DataStore preferences, and Gemini AI interaction.
 */
class ChatViewModel(
    private val geminiRepository: GeminiRepository,
    private val chatDao: ChatDao,
    private val preferencesRepository: PreferencesRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        observeMessages()
        observeThemePreference()
    }

    private fun observeMessages() {
        viewModelScope.launch {
            chatDao.getAllMessages()
                .catch { e ->
                    _uiState.update { it.copy(errorMessage = e.localizedMessage ?: "Failed to load chat history.") }
                }
                .collectLatest { entities ->
                    val uiMessages = entities.map { entity ->
                        ChatMessage(
                            id = entity.id,
                            text = entity.text,
                            isUser = "USER" == entity.sender,
                            timestamp = entity.timestamp
                        )
                    }
                    _uiState.update { it.copy(messages = uiMessages) }
                }
        }
    }

    private fun observeThemePreference() {
        viewModelScope.launch {
            preferencesRepository.isDarkMode.collectLatest { isDark ->
                _uiState.update { it.copy(isDarkMode = isDark) }
            }
        }
    }

    fun onPromptChanged(newPrompt: String) {
        _uiState.update { it.copy(currentPrompt = newPrompt) }
    }

    fun sendPrompt(userPrompt: String = _uiState.value.currentPrompt) {
        val trimmedPrompt = userPrompt.trim()
        if (trimmedPrompt.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Prompt cannot be empty.") }
            return
        }

        _uiState.update {
            it.copy(
                isLoading = true,
                errorMessage = null,
                currentPrompt = ""
            )
        }

        viewModelScope.launch(ioDispatcher) {
            try {
                chatDao.insertMessage(
                    ChatMessageEntity(text = trimmedPrompt, sender = "USER")
                )

                val result = geminiRepository.generateResponse(trimmedPrompt)

                result.onSuccess { responseText ->
                    chatDao.insertMessage(
                        ChatMessageEntity(text = responseText, sender = "GEMINI")
                    )
                    _uiState.update { it.copy(isLoading = false) }
                }.onFailure { exception ->
                    val errorMsg = exception.localizedMessage ?: "Failed to generate response."
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = errorMsg
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.localizedMessage ?: "An unexpected error occurred."
                    )
                }
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch(ioDispatcher) {
            try {
                chatDao.clearAllMessages()
            } catch (_: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to clear history.") }
            }
        }
    }

    fun toggleDarkMode() {
        viewModelScope.launch {
            val currentMode = _uiState.value.isDarkMode ?: false
            preferencesRepository.setDarkMode(!currentMode)
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
