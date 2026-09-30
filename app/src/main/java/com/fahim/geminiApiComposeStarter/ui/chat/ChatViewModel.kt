package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fahim.geminiApiComposeStarter.data.ChatMessageDao
import com.fahim.geminiApiComposeStarter.data.ChatMessageEntity
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class ChatViewModel(
    private val repository: GeminiRepository,
    private val chatDao: ChatMessageDao,
    private val userPrefs: UserPreferencesRepository,
    private val hasApiKey: Boolean,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            chatDao.getAllMessages().collect { entities ->
                val messages = entities.map {
                    ChatMessage(id = it.id, text = it.text, isUser = it.isUser, timestamp = it.timestamp)
                }
                _uiState.update { it.copy(messages = messages) }
            }
        }
        viewModelScope.launch {
            userPrefs.userName.collect { name ->
                _uiState.update { it.copy(userName = name) }
            }
        }
    }

    fun onPromptChange(value: String) {
        _uiState.update { it.copy(prompt = value, promptError = null) }
    }

    fun setUserName(name: String) {
        viewModelScope.launch {
            userPrefs.setUserName(name)
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

        val userMsgId = UUID.randomUUID().toString()
        val userMsg = ChatMessageEntity(id = userMsgId, text = prompt, isUser = true, timestamp = System.currentTimeMillis())
        
        _uiState.update { 
            it.copy(
                isLoading = true, 
                errorMessage = null, 
                promptError = null,
                prompt = ""
            ) 
        }

        viewModelScope.launch {
            chatDao.insertMessage(userMsg)
            
            repository.generateText(prompt).fold(
                onSuccess = { text ->
                    val geminiMsgId = UUID.randomUUID().toString()
                    val geminiMsg = ChatMessageEntity(id = geminiMsgId, text = text, isUser = false, timestamp = System.currentTimeMillis())
                    chatDao.insertMessage(geminiMsg)
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
            chatDao: ChatMessageDao, 
            userPrefs: UserPreferencesRepository,
            hasApiKey: Boolean
        ) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ChatViewModel(repository, chatDao, userPrefs, hasApiKey) as T
            }
    }
}