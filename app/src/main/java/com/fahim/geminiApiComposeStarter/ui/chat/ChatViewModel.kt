package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.local.room.ChatDao
import com.fahim.geminiApiComposeStarter.data.local.room.ChatMessageEntity
import com.fahim.geminiApiComposeStarter.data.repository.ChatRepository
import com.fahim.geminiApiComposeStarter.data.repository.ChatRepositoryImpl
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(
    private val repository: ChatRepository,
    private val hasApiKey: Boolean,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState(hasApiKey = hasApiKey))
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        // Collect persistent messages from Room database
        viewModelScope.launch {
            repository.messagesFlow.collect { messageList ->
                _uiState.update { it.copy(messages = messageList) }
            }
        }
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
        // Duplicate request protection
        if (_uiState.value.isLoading) return

        _uiState.update {
            it.copy(
                prompt = "",
                isLoading = true,
                errorMessage = null,
                promptError = null,
                lastFailedPrompt = null,
            )
        }

        viewModelScope.launch {
            repository.sendMessage(prompt).fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false) }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Something went wrong. Please try again.",
                            lastFailedPrompt = prompt,
                        )
                    }
                }
            )
        }
    }

    fun onRetry() {
        val failedPrompt = _uiState.value.lastFailedPrompt ?: return
        _uiState.update {
            it.copy(
                isLoading = true,
                errorMessage = null,
                lastFailedPrompt = null,
            )
        }
        viewModelScope.launch {
            repository.sendMessage(failedPrompt).fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false) }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Retry failed. Please check connection.",
                            lastFailedPrompt = failedPrompt,
                        )
                    }
                }
            )
        }
    }

    fun onVoiceResult(spokenText: String) {
        if (spokenText.isNotBlank()) {
            _uiState.update {
                val current = it.prompt.trim()
                val updated = if (current.isEmpty()) spokenText else "$current $spokenText"
                it.copy(prompt = updated, promptError = null)
            }
        }
    }

    fun onClearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    fun onDismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    companion object {
        const val MISSING_API_KEY_MESSAGE =
            "GEMINI_API_KEY is missing. Add it to local.properties or set the environment variable, then rebuild."

        fun factory(repository: ChatRepository, hasApiKey: Boolean) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ChatViewModel(repository, hasApiKey) as T
            }

        /**
         * Convenience factory that adapts a simple GeminiRepository into a ChatRepository
         * with an in-memory message store for unit tests or simple preview setups.
         */
        fun factory(repository: GeminiRepository, hasApiKey: Boolean) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val inMemoryDao = object : ChatDao {
                        private val list = MutableStateFlow<List<ChatMessageEntity>>(emptyList())
                        override fun getAllMessages(): Flow<List<ChatMessageEntity>> = list
                        override suspend fun insertMessage(message: ChatMessageEntity): Long {
                            val id = (list.value.maxOfOrNull { it.id } ?: 0L) + 1L
                            val assigned = message.copy(id = id)
                            list.update { it + assigned }
                            return id
                        }
                        override suspend fun deleteMessageById(id: Long): Int {
                            val before = list.value.size
                            list.update { it.filterNot { m -> m.id == id } }
                            return before - list.value.size
                        }
                        override suspend fun clearAll(): Int {
                            val count = list.value.size
                            list.value = emptyList()
                            return count
                        }
                        override suspend fun getLastMessage(): ChatMessageEntity? = list.value.lastOrNull()
                    }
                    val chatRepo = ChatRepositoryImpl(repository, inMemoryDao)
                    return ChatViewModel(chatRepo, hasApiKey) as T
                }
            }
    }
}
