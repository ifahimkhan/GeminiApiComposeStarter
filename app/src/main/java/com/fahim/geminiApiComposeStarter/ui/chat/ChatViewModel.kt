package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.PreferencesStore
import com.fahim.geminiApiComposeStarter.data.local.ChatMessageDao
import com.fahim.geminiApiComposeStarter.data.local.ChatMessageEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class ChatViewModel(
    private val repository: GeminiRepository,
    private val hasApiKey: Boolean,
    private val chatMessageDao: ChatMessageDao,
    private val userPreferences: PreferencesStore,
) : ViewModel() {

    private val _uiState =
        MutableStateFlow(ChatUiState())

    val uiState: StateFlow<ChatUiState> =
        _uiState.asStateFlow()

    init {
        observeSavedMessages()
    }

    private fun observeSavedMessages() {
        viewModelScope.launch {

            combine(
                chatMessageDao.getAllMessages(),
                userPreferences.showUserMessages
            ) { messages, showUserMessages ->

                val visibleMessages =
                    if (showUserMessages) {
                        messages
                    } else {
                        messages.filter { !it.isUser }
                    }

                val chatMessages =
                    visibleMessages.map { entity ->

                        ChatMessage(
                            id = entity.id,
                            text = entity.text,
                            isUser = entity.isUser
                        )
                    }

                chatMessages to showUserMessages

            }.collectLatest { (messages, showUserMessages) ->

                _uiState.value =
                    _uiState.value.copy(
                        messages = messages,
                        showUserMessages = showUserMessages
                    )
            }
        }
    }

    fun onPromptChange(value: String) {

        _uiState.value =
            _uiState.value.copy(
                prompt = value,
                promptError = null,
                errorMessage = null
            )
    }

    fun onSend() {

        val currentState =
            _uiState.value

        val prompt =
            currentState.prompt.trim()

        if (prompt.isBlank()) {

            _uiState.value =
                currentState.copy(
                    promptError = PromptError.EMPTY
                )

            return
        }

        if (!hasApiKey) {

            _uiState.value =
                currentState.copy(
                    errorMessage =
                        "Gemini API key is missing."
                )

            return
        }

        if (currentState.isLoading) {
            return
        }

        viewModelScope.launch {

            _uiState.value =
                _uiState.value.copy(
                    isLoading = true,
                    promptError = null,
                    errorMessage = null
                )

            try {

                val result =
                    repository.generateText(prompt)

                val response =
                    result.getOrElse { exception ->
                        throw exception
                    }

                val timestamp =
                    System.currentTimeMillis()

                val userMessage =
                    ChatMessageEntity(
                        id = timestamp,
                        text = prompt,
                        isUser = true
                    )

                val geminiMessage =
                    ChatMessageEntity(
                        id = timestamp + 1,
                        text = response,
                        isUser = false
                    )

                chatMessageDao.insertMessages(
                    listOf(
                        userMessage,
                        geminiMessage
                    )
                )

                _uiState.value =
                    _uiState.value.copy(
                        prompt = "",
                        response = response,
                        isLoading = false
                    )

            } catch (e: Exception) {

                _uiState.value =
                    _uiState.value.copy(
                        isLoading = false,
                        errorMessage =
                            e.message
                                ?: "Something went wrong."
                    )
            }
        }
    }

    fun setShowUserMessages(
        enabled: Boolean
    ) {

        viewModelScope.launch {
            userPreferences.setShowUserMessages(
                enabled
            )
        }
    }

    fun clearConversation() {

        viewModelScope.launch {
            chatMessageDao.deleteAllMessages()
        }
    }

    companion object {

        fun factory(
            repository: GeminiRepository,
            hasApiKey: Boolean,
            chatMessageDao: ChatMessageDao,
            userPreferences: PreferencesStore,
        ): ViewModelProvider.Factory {

            return object :
                ViewModelProvider.Factory {

                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(
                    modelClass: Class<T>
                ): T {

                    return ChatViewModel(
                        repository = repository,
                        hasApiKey = hasApiKey,
                        chatMessageDao = chatMessageDao,
                        userPreferences = userPreferences
                    ) as T
                }
            }
        }
    }
}