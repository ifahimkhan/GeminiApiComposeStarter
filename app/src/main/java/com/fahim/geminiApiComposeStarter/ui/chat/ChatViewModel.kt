package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.local.ChatMessageDao
import com.fahim.geminiApiComposeStarter.data.local.ChatMessageEntity
import com.fahim.geminiApiComposeStarter.data.preferences.PreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(
    private val repository: GeminiRepository,
    private val chatMessageDao: ChatMessageDao,
    private val preferencesRepository: PreferencesRepository,
    private val hasApiKey: Boolean,
) : ViewModel() {

    private val _internalState = MutableStateFlow(
        ChatUiState(hasApiKey = hasApiKey)
    )

    val uiState: StateFlow<ChatUiState> = combine(
        _internalState,
        chatMessageDao.getAllMessages(),
        preferencesRepository.themePreference,
        preferencesRepository.isVoiceEnabled
    ) { internal, messages, theme, voiceEnabled ->
        internal.copy(
            messages = messages,
            themePreference = theme,
            isVoiceEnabled = voiceEnabled
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = ChatUiState(hasApiKey = hasApiKey)
    )

    fun onPromptChange(value: String) {
        _internalState.update { it.copy(prompt = value, promptError = null) }
    }

    fun onSend() {
        val currentPrompt = _internalState.value.prompt.trim()
        if (currentPrompt.isEmpty()) {
            _internalState.update { it.copy(promptError = PromptError.EMPTY) }
            return
        }
        if (!hasApiKey) {
            _internalState.update { it.copy(errorMessage = MISSING_API_KEY_MESSAGE) }
            return
        }
        if (_internalState.value.isLoading) return

        _internalState.update {
            it.copy(
                isLoading = true,
                errorMessage = null,
                promptError = null,
                prompt = ""
            )
        }

        viewModelScope.launch {
            // Persist user prompt in Room database
            chatMessageDao.insertMessage(
                ChatMessageEntity(text = currentPrompt, role = "USER")
            )

            // Send to Gemini API
            repository.generateText(currentPrompt).fold(
                onSuccess = { responseText ->
                    chatMessageDao.insertMessage(
                        ChatMessageEntity(text = responseText, role = "GEMINI")
                    )
                    _internalState.update { it.copy(isLoading = false) }
                },
                onFailure = { error ->
                    _internalState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.localizedMessage ?: "Failed to generate response from Gemini API."
                        )
                    }
                }
            )
        }
    }

    fun onVoiceResult(recognizedText: String) {
        if (recognizedText.isNotBlank()) {
            _internalState.update { it.copy(prompt = recognizedText, promptError = null) }
        }
    }

    fun onThemeChange(theme: String) {
        viewModelScope.launch {
            preferencesRepository.setThemePreference(theme)
        }
    }

    fun onClearHistory() {
        viewModelScope.launch {
            chatMessageDao.clearAllMessages()
        }
    }

    fun onDismissError() {
        _internalState.update { it.copy(errorMessage = null) }
    }

    companion object {
        const val MISSING_API_KEY_MESSAGE =
            "GEMINI_API_KEY is missing. Add it to local.properties and rebuild."

        fun factory(
            repository: GeminiRepository,
            chatMessageDao: ChatMessageDao,
            preferencesRepository: PreferencesRepository,
            hasApiKey: Boolean
        ) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ChatViewModel(repository, chatMessageDao, preferencesRepository, hasApiKey) as T
        }
    }
}
