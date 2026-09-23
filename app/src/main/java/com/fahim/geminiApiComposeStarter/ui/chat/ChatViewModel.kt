package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fahim.geminiApiComposeStarter.data.ChatHistoryRepository
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.local.ChatMessageEntity
import com.fahim.geminiApiComposeStarter.data.local.MessageParticipant
import com.fahim.geminiApiComposeStarter.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(
    private val repository: GeminiRepository,
    private val hasApiKey: Boolean,
    private val historyRepository: ChatHistoryRepository? = null,
    private val preferencesRepository: UserPreferencesRepository? = null,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var inMemoryMessageIdCounter = 1L

    init {
        // Observe persistent chat history from Room DB if provided
        historyRepository?.getMessages()
            ?.onEach { entities ->
                val uiModels = entities.map { entity ->
                    ChatMessageUiModel(
                        id = entity.id,
                        text = entity.text,
                        participant = entity.participant,
                        timestamp = entity.timestamp,
                        isError = entity.isError,
                    )
                }
                _uiState.update { it.copy(messages = uiModels) }
            }
            ?.launchIn(viewModelScope)

        // Observe user preferences if provided
        preferencesRepository?.userPreferencesFlow
            ?.onEach { prefs ->
                _uiState.update { it.copy(autoScroll = prefs.autoScroll) }
            }
            ?.launchIn(viewModelScope)
    }

    fun onPromptChange(value: String) {
        _uiState.update { it.copy(prompt = value, promptError = null) }
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

        // Capture previous conversation history for multi-turn context
        val existingHistory = _uiState.value.messages.map {
            ChatMessageEntity(
                id = it.id,
                text = it.text,
                participant = it.participant,
                timestamp = it.timestamp,
                isError = it.isError,
            )
        }

        // Clear prompt field immediately upon sending
        _uiState.update {
            it.copy(
                prompt = "",
                isLoading = true,
                errorMessage = null,
                promptError = null,
            )
        }

        viewModelScope.launch {
            if (historyRepository != null) {
                historyRepository.saveMessage(
                    text = prompt,
                    participant = MessageParticipant.USER,
                )
            } else {
                val userMsg = ChatMessageUiModel(
                    id = inMemoryMessageIdCounter++,
                    text = prompt,
                    participant = MessageParticipant.USER,
                )
                _uiState.update { it.copy(messages = it.messages + userMsg) }
            }

            repository.generateChatResponse(existingHistory, prompt).fold(
                onSuccess = { text ->
                    if (historyRepository != null) {
                        historyRepository.saveMessage(
                            text = text,
                            participant = MessageParticipant.MODEL,
                        )
                    } else {
                        val modelMsg = ChatMessageUiModel(
                            id = inMemoryMessageIdCounter++,
                            text = text,
                            participant = MessageParticipant.MODEL,
                        )
                        _uiState.update {
                            it.copy(
                                messages = it.messages + modelMsg,
                                isLoading = false,
                                response = text,
                            )
                        }
                    }
                    _uiState.update { it.copy(isLoading = false, response = text) }
                },
                onFailure = { error ->
                    val errorText = error.message ?: "Something went wrong"
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = errorText,
                        )
                    }
                },
            )
        }
    }

    fun clearChat() {
        viewModelScope.launch {
            historyRepository?.clearHistory()
            _uiState.update {
                it.copy(
                    messages = emptyList(),
                    response = "",
                    errorMessage = null,
                )
            }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    companion object {
        const val MISSING_API_KEY_MESSAGE =
            "GEMINI_API_KEY is missing. Add it to local.properties and rebuild."

        fun factory(
            repository: GeminiRepository,
            hasApiKey: Boolean,
            historyRepository: ChatHistoryRepository? = null,
            preferencesRepository: UserPreferencesRepository? = null,
        ) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ChatViewModel(
                    repository = repository,
                    hasApiKey = hasApiKey,
                    historyRepository = historyRepository,
                    preferencesRepository = preferencesRepository,
                ) as T
        }
    }
}
