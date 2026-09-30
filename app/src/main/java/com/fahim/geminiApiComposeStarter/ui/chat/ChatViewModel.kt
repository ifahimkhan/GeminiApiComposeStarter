package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.preferences.PreferencesRepository
import com.fahim.geminiApiComposeStarter.data.preferences.ThemeMode
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class ChatViewModel(
    private val repository: GeminiRepository,
    private val preferencesRepository: PreferencesRepository? = null,
    private val hasApiKey: Boolean,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var historyObservationJob: Job? = null
    private var lastUserPrompt: String? = null

    init {
        viewModelScope.launch {
            val savedConvId = preferencesRepository?.lastConversationId?.firstOrNull()?.takeIf { it.isNotBlank() }
            val initialId = savedConvId ?: UUID.randomUUID().toString()

            _uiState.update { it.copy(activeConversationId = initialId) }
            observeConversationHistory(initialId)

            repository.getChatSessions().collect { sessions ->
                _uiState.update { state ->
                    state.copy(conversations = sessions)
                }
            }
        }

        preferencesRepository?.let { prefs ->
            viewModelScope.launch {
                prefs.themeMode.collect { mode ->
                    _uiState.update { state ->
                        state.copy(themeMode = mode)
                    }
                }
            }
        }
    }

    private fun observeConversationHistory(conversationId: String) {
        historyObservationJob?.cancel()
        historyObservationJob = viewModelScope.launch {
            repository.getChatHistory(conversationId).collect { history ->
                _uiState.update { state ->
                    state.copy(
                        activeConversationId = conversationId,
                        messages = history,
                    )
                }
            }
        }
        preferencesRepository?.let { prefs ->
            viewModelScope.launch {
                prefs.setLastConversationId(conversationId)
            }
        }
    }

    fun onNewChat() {
        if (_uiState.value.messages.isEmpty()) return

        val newConversationId = UUID.randomUUID().toString()
        _uiState.update {
            it.copy(
                activeConversationId = newConversationId,
                prompt = "",
                errorMessage = null,
            )
        }
        observeConversationHistory(newConversationId)
    }

    fun onSelectConversation(conversationId: String) {
        if (conversationId == _uiState.value.activeConversationId) return
        _uiState.update {
            it.copy(
                prompt = "",
                errorMessage = null,
            )
        }
        observeConversationHistory(conversationId)
    }

    fun onThemeModeSelected(mode: ThemeMode) {
        preferencesRepository?.let { prefs ->
            viewModelScope.launch {
                prefs.setThemeMode(mode)
            }
        }
    }

    fun onPromptChange(value: String) {
        _uiState.update { it.copy(prompt = value) }
    }

    fun onSendPrompt(text: String) {
        onPromptChange(text)
        onSend()
    }

    fun onErrorShown() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun onSend() {
        val promptText = _uiState.value.prompt.trim()
        if (promptText.isEmpty()) return
        if (!hasApiKey) {
            _uiState.update { it.copy(errorMessage = MISSING_API_KEY_MESSAGE) }
            return
        }
        if (_uiState.value.isLoading) return

        lastUserPrompt = promptText
        val activeConvId = _uiState.value.activeConversationId
        val historyBeforePrompt = _uiState.value.messages.takeLast(20)

        val userMessage = ChatMessage(
            conversationId = activeConvId,
            text = promptText,
            participant = Participant.USER,
        )

        _uiState.update {
            it.copy(
                prompt = "",
                loadingConversationId = activeConvId,
                errorMessage = null,
            )
        }

        viewModelScope.launch {
            repository.saveMessage(userMessage)

            repository.generateText(promptText, historyBeforePrompt).fold(
                onSuccess = { responseText ->
                    val modelMessage = ChatMessage(
                        conversationId = activeConvId,
                        text = responseText,
                        participant = Participant.MODEL,
                    )
                    repository.saveMessage(modelMessage)
                    _uiState.update {
                        it.copy(
                            loadingConversationId = null,
                        )
                    }
                },
                onFailure = { error ->
                    val errorMsg = error.message ?: "Failed to generate response. Please try again."
                    _uiState.update {
                        it.copy(
                            loadingConversationId = null,
                            errorMessage = errorMsg,
                        )
                    }
                },
            )
        }
    }

    fun onRetry() {
        val promptToRetry = lastUserPrompt ?: return
        val activeConvId = _uiState.value.activeConversationId
        val historyBeforeRetry = _uiState.value.messages
            .filter { it.text != promptToRetry }
            .takeLast(20)

        if (!hasApiKey) {
            _uiState.update { it.copy(errorMessage = MISSING_API_KEY_MESSAGE) }
            return
        }
        if (_uiState.value.isLoading) return

        _uiState.update {
            it.copy(
                loadingConversationId = activeConvId,
                errorMessage = null,
            )
        }

        viewModelScope.launch {
            repository.generateText(promptToRetry, historyBeforeRetry).fold(
                onSuccess = { responseText ->
                    val modelMessage = ChatMessage(
                        conversationId = activeConvId,
                        text = responseText,
                        participant = Participant.MODEL,
                    )
                    repository.saveMessage(modelMessage)
                    _uiState.update {
                        it.copy(
                            loadingConversationId = null,
                        )
                    }
                },
                onFailure = { error ->
                    val errorMsg = error.message ?: "Failed to generate response. Please try again."
                    _uiState.update {
                        it.copy(
                            loadingConversationId = null,
                            errorMessage = errorMsg,
                        )
                    }
                },
            )
        }
    }

    fun onClearChat() {
        val activeConvId = _uiState.value.activeConversationId
        viewModelScope.launch {
            repository.deleteConversation(activeConvId)
            val newConversationId = UUID.randomUUID().toString()
            _uiState.update {
                it.copy(
                    activeConversationId = newConversationId,
                    prompt = "",
                    loadingConversationId = null,
                    errorMessage = null,
                )
            }
            observeConversationHistory(newConversationId)
        }
    }

    companion object {
        const val MISSING_API_KEY_MESSAGE =
            "GEMINI_API_KEY is missing. Add it to local.properties and rebuild."

        fun factory(
            repository: GeminiRepository,
            preferencesRepository: PreferencesRepository? = null,
            hasApiKey: Boolean,
        ) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ChatViewModel(repository, preferencesRepository, hasApiKey) as T
            }
    }
}
