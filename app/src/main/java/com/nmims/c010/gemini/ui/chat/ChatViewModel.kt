package com.nmims.c010.gemini.ui.chat

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nmims.c010.gemini.data.Author
import com.nmims.c010.gemini.data.ChatMessage
import com.nmims.c010.gemini.data.GeminiRepository
import com.nmims.c010.gemini.data.GeminiRepositoryImpl
import com.nmims.c010.gemini.data.local.ChatDao
import com.nmims.c010.gemini.data.local.ChatDatabase
import com.nmims.c010.gemini.data.local.toEntity
import com.nmims.c010.gemini.data.local.toModel
import com.nmims.c010.gemini.security.ApiKeyStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(
    private val repository: GeminiRepository,
    private val historyDao: ChatDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var lastPrompt: String? = null

    init {
        // room backed history, so the chat survives restarts
        viewModelScope.launch {
            historyDao.observeAll().collect { saved ->
                _uiState.update { it.copy(messages = saved.map { e -> e.toModel() }) }
            }
        }
    }

    fun onInputChange(value: String) = _uiState.update { it.copy(input = value) }

    fun onVoiceResult(spoken: String) =
        _uiState.update { it.copy(input = spoken, isListening = false) }

    fun send() {
        val prompt = _uiState.value.input.trim()
        if (prompt.isBlank() || _uiState.value.isLoading) return
        lastPrompt = prompt

        val userMsg = ChatMessage(text = prompt, author = Author.USER)
        _uiState.update {
            it.copy(
                messages = it.messages + userMsg,
                input = "",
                isLoading = true,
                errorMessage = null
            )
        }

        viewModelScope.launch {
            historyDao.insert(userMsg.toEntity())
            repository.send(prompt, _uiState.value.messages)
                .onSuccess { reply ->
                    val botMsg = ChatMessage(text = reply, author = Author.MODEL)
                    historyDao.insert(botMsg.toEntity())
                    _uiState.update {
                        it.copy(messages = it.messages + botMsg, isLoading = false)
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = e.message ?: "Something went wrong. Try again."
                        )
                    }
                }
        }
    }

    fun retry() {
        lastPrompt?.let {
            _uiState.update { s -> s.copy(input = it, errorMessage = null) }
            send()
        }
    }

    fun consumeError() = _uiState.update { it.copy(errorMessage = null) }

    fun clearHistory() = viewModelScope.launch { historyDao.clear() }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val dao = ChatDatabase.get(context).chatDao()
            val repo = GeminiRepositoryImpl(ApiKeyStore(context))
            return ChatViewModel(repo, dao) as T
        }
    }
}
