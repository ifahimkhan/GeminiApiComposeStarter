package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.HistoryItem
import com.fahim.geminiApiComposeStarter.data.local.ChatDao
import com.fahim.geminiApiComposeStarter.data.local.ChatMessageEntity
import com.fahim.geminiApiComposeStarter.data.local.ThemeManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(
    private val repository: GeminiRepository,
    private val chatDao: ChatDao,
    private val themeManager: ThemeManager,
    private val hasApiKey: Boolean,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var nextMessageId = 1L

    init {
        loadChatHistory()
        observeTheme()
    }

    private fun observeTheme() {
        viewModelScope.launch {
            themeManager.isDarkMode.collectLatest { isDark ->
                _uiState.update { it.copy(isDarkMode = isDark) }
            }
        }
    }

    fun toggleTheme() {
        viewModelScope.launch {
            val current = _uiState.value.isDarkMode ?: false
            themeManager.setDarkMode(!current)
        }
    }

    private fun loadChatHistory() {
        viewModelScope.launch {
            try {
                val savedMessages = chatDao.getAllMessages().first()
                if (savedMessages.isNotEmpty()) {
                    val mappedMessages = savedMessages.map { entity ->
                        ChatMessage(
                            id = entity.uid,
                            text = entity.text,
                            isFromUser = entity.isFromUser
                        )
                    }
                    nextMessageId = (savedMessages.maxOf { it.uid }) + 1
                    _uiState.update { it.copy(messages = mappedMessages) }
                }
            } catch (e: Exception) {
                // Fallback if loading fails
            }
        }
    }

    fun onPromptChange(value: String) {
        _uiState.update {
            it.copy(
                prompt = value,
                promptError = null,
            )
        }
    }

    fun clearChatHistory() {
        viewModelScope.launch {
            chatDao.clearHistory()
            _uiState.update { it.copy(messages = emptyList()) }
        }
    }

    fun onSend() {
        val prompt = _uiState.value.prompt.trim()

        if (prompt.isEmpty()) {
            _uiState.update {
                it.copy(promptError = PromptError.EMPTY)
            }
            return
        }

        if (!hasApiKey) {
            _uiState.update {
                it.copy(errorMessage = MISSING_API_KEY_MESSAGE)
            }
            return
        }

        if (_uiState.value.isLoading) return

        val userMessageId = nextMessageId++
        val userMessage = ChatMessage(
            id = userMessageId,
            text = prompt,
            isFromUser = true,
        )

        _uiState.update {
            it.copy(
                prompt = "",
                messages = it.messages + userMessage,
                isLoading = true,
                errorMessage = null,
                promptError = null,
            )
        }

        viewModelScope.launch {
            // Persist user message
            chatDao.insertMessage(
                ChatMessageEntity(uid = userMessageId, text = prompt, isFromUser = true)
            )

            val history = _uiState.value.messages.map { 
                HistoryItem(it.text, it.isFromUser)
            }

            repository.generateText(prompt, history).fold(
                onSuccess = { text ->
                    val geminiMessageId = nextMessageId++
                    val geminiMessage = ChatMessage(
                        id = geminiMessageId,
                        text = text,
                        isFromUser = false,
                    )

                    chatDao.insertMessage(
                        ChatMessageEntity(uid = geminiMessageId, text = text, isFromUser = false)
                    )

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            response = text,
                            messages = it.messages + geminiMessage,
                        )
                    }
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
            chatDao: ChatDao,
            themeManager: ThemeManager,
            hasApiKey: Boolean,
        ) = object : ViewModelProvider.Factory {

            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
            ): T = ChatViewModel(
                repository,
                chatDao,
                themeManager,
                hasApiKey,
            ) as T
        }
    }
}
