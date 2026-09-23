package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.preferences.ThemeMode
import com.fahim.geminiApiComposeStarter.data.preferences.ThemePreferencesRepository
import com.fahim.geminiApiComposeStarter.data.security.SecurityUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(
    private val repository: GeminiRepository,
    private val themePreferencesRepository: ThemePreferencesRepository? = null,
    private val hasApiKey: Boolean,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getChatHistory().collect { entities ->
                val chatMessages = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    entities.map { entity ->
                        ChatMessage(
                            id = entity.id,
                            sender = entity.sender,
                            content = SecurityUtils.decrypt(entity.content),
                            timestamp = entity.timestamp,
                        )
                    }
                }
                _uiState.update { it.copy(messages = chatMessages) }
            }
        }

        themePreferencesRepository?.let { repo ->
            viewModelScope.launch {
                repo.themeModeFlow.collect { mode ->
                    _uiState.update { it.copy(themeMode = mode) }
                }
            }
        }
    }

    fun onPromptChange(value: String) {
        _uiState.update { it.copy(prompt = value, promptError = null) }
    }

    fun onThemeModeChange(mode: ThemeMode) {
        _uiState.update { it.copy(themeMode = mode) }
        viewModelScope.launch {
            themePreferencesRepository?.setThemeMode(mode)
        }
    }

    fun onVoiceResult(recognizedText: String?) {
        if (!recognizedText.isNullOrBlank()) {
            val currentPrompt = _uiState.value.prompt
            val newPrompt = if (currentPrompt.isBlank()) recognizedText else "$currentPrompt $recognizedText"
            _uiState.update { it.copy(prompt = newPrompt, promptError = null) }
        }
    }

    fun onVoiceError(errorMsg: String) {
        _uiState.update { it.copy(errorMessage = errorMsg) }
    }

    fun onClearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
            _uiState.update { it.copy(messages = emptyList(), response = "") }
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

        _uiState.update {
            it.copy(
                prompt = "",
                isLoading = true,
                errorMessage = null,
                promptError = null
            )
        }

        viewModelScope.launch {
            repository.saveMessage("user", prompt)
            repository.generateText(prompt).fold(
                onSuccess = { text ->
                    repository.saveMessage("model", text)
                    _uiState.update { it.copy(isLoading = false, response = text) }
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
            themePreferencesRepository: ThemePreferencesRepository? = null,
            hasApiKey: Boolean,
        ) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ChatViewModel(repository, themePreferencesRepository, hasApiKey) as T
        }
    }
}
