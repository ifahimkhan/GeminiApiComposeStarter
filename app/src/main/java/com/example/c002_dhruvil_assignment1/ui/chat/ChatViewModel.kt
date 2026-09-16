package com.example.c002_dhruvil_assignment1.ui.chat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.c002_dhruvil_assignment1.data.GeminiRepository
import com.example.c002_dhruvil_assignment1.data.GeminiRepositoryImpl
import com.example.c002_dhruvil_assignment1.data.room.ChatDao
import com.example.c002_dhruvil_assignment1.data.room.ChatDatabase
import com.example.c002_dhruvil_assignment1.data.room.MessageEntity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

open class ChatViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: GeminiRepository = GeminiRepositoryImpl(application),
    private val chatDao: ChatDao = ChatDatabase.getDatabase(application).chatDao(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<ChatUiState>(ChatUiState.Initial)
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    val messages: StateFlow<List<MessageEntity>> = chatDao.getAllMessages()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    open fun sendMessage(prompt: String) {
        if (prompt.isBlank()) return

        _uiState.value = ChatUiState.Loading

        viewModelScope.launch(ioDispatcher) {
            try {
                // Instantly save user prompt in Room so user bubble renders with zero delay
                chatDao.insertMessage(
                    MessageEntity(
                        sender = "user",
                        text = prompt
                    )
                )

                var finalResponse = ""
                // Stream response tokens in real-time as Gemini generates them
                repository.sendMessageStream(prompt).collect { accumulatedText ->
                    finalResponse = accumulatedText
                    _uiState.value = ChatUiState.Success(accumulatedText)
                }

                // Once streaming completes, persist full Gemini response to Room
                if (finalResponse.isNotBlank()) {
                    chatDao.insertMessage(
                        MessageEntity(
                            sender = "gemini",
                            text = finalResponse
                        )
                    )
                }
            } catch (e: Exception) {
                _uiState.value = ChatUiState.Error(e.localizedMessage ?: "An error occurred")
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch(ioDispatcher) {
            chatDao.clearAll()
            _uiState.value = ChatUiState.Initial
        }
    }
}
