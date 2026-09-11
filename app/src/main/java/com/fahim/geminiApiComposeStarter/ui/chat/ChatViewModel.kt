package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.SettingsRepository
import com.fahim.geminiApiComposeStarter.data.local.ChatDao
import com.fahim.geminiApiComposeStarter.data.local.Conversation
import com.fahim.geminiApiComposeStarter.data.local.Message
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class ChatViewModel(
    private val repository: GeminiRepository,
    private val chatDao: ChatDao,
    private val settingsRepository: SettingsRepository,
    private val hasApiKey: Boolean,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var streamingJob: Job? = null
    private var messagesJob: Job? = null

    init {
        loadConversations()
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            settingsRepository.responseLength.collectLatest { length ->
                _uiState.update { it.copy(responseLengthPreference = length) }
            }
        }
    }

    private fun loadConversations() {
        viewModelScope.launch {
            chatDao.getAllConversations().collectLatest { list ->
                _uiState.update { it.copy(conversations = list) }
            }
        }
    }

    fun onPromptChange(value: String) {
        _uiState.update { it.copy(prompt = value, errorMessage = null) }
    }

    fun onSelectConversation(id: Long) {
        _uiState.update { it.copy(currentConversationId = id, currentBranchId = "main") }
        observeMessages(id, "main")
    }

    fun onNewChat() {
        _uiState.update { 
            it.copy(
                currentConversationId = null, 
                messages = emptyList(), 
                currentBranchId = "main",
                branches = listOf("main")
            ) 
        }
        messagesJob?.cancel()
    }

    private fun observeMessages(conversationId: Long, branchId: String) {
        messagesJob?.cancel()
        messagesJob = viewModelScope.launch {
            // Observe all branches for selector
            launch {
                chatDao.getBranchesForConversation(conversationId).collectLatest { branches ->
                    _uiState.update { it.copy(branches = if (branches.isEmpty()) listOf("main") else branches) }
                }
            }
            
            // Logic to get the path: find latest message in this branch
            chatDao.getMessagesForBranch(conversationId, branchId).collectLatest { list ->
                val latestMsgId = list.lastOrNull()?.id
                if (latestMsgId != null) {
                    chatDao.getMessagesPath(latestMsgId).collectLatest { path ->
                        _uiState.update { it.copy(messages = path) }
                    }
                } else {
                    // If branch is empty (e.g. just forked), we might need to show the path up to the fork point.
                    // For now, if empty, we just show empty or stick to what's there.
                    // In a more robust version, we'd store the fork point.
                }
            }
        }
    }

    fun onSend() {
        val promptText = _uiState.value.prompt.trim()
        if (promptText.isEmpty() || _uiState.value.isStreaming) return
        if (!hasApiKey) {
            _uiState.update { it.copy(errorMessage = "API Key missing") }
            return
        }

        viewModelScope.launch {
            var convId = _uiState.value.currentConversationId
            if (convId == null) {
                val title = if (promptText.length > 30) promptText.take(30) + "..." else promptText
                convId = chatDao.insertConversation(Conversation(title = title))
                _uiState.update { it.copy(currentConversationId = convId) }
                observeMessages(convId, _uiState.value.currentBranchId)
            }

            val parentMessageId = _uiState.value.messages.lastOrNull()?.id
            val userMsg = Message(
                conversationId = convId,
                role = "user",
                content = promptText,
                parentMessageId = parentMessageId,
                branchId = _uiState.value.currentBranchId
            )
            val userMsgId = chatDao.insertMessage(userMsg)

            _uiState.update { it.copy(prompt = "", isStreaming = true, streamingText = "") }

            streamingJob = launch {
                val fullPrompt = buildSystemPrompt() + "\n\n" + promptText
                var fullResponse = ""
                try {
                    repository.generateTextStreaming(fullPrompt).collect { chunk ->
                        fullResponse += chunk
                        _uiState.update { it.copy(streamingText = fullResponse) }
                    }
                    
                    val modelMsg = Message(
                        conversationId = convId,
                        role = "model",
                        content = fullResponse,
                        parentMessageId = userMsgId,
                        branchId = _uiState.value.currentBranchId
                    )
                    chatDao.insertMessage(modelMsg)
                } catch (e: Exception) {
                    _uiState.update { it.copy(errorMessage = e.message ?: "Streaming failed") }
                } finally {
                    _uiState.update { it.copy(isStreaming = false, streamingText = "") }
                }
            }
        }
    }

    private fun buildSystemPrompt(): String {
        return when (_uiState.value.responseLengthPreference) {
            "Short" -> "Provide concise, short answers."
            "Detailed" -> "Provide very detailed, comprehensive explanations."
            else -> "Provide normal length answers."
        }
    }

    fun onFork(messageId: Long) {
        viewModelScope.launch {
            if (_uiState.value.currentConversationId == null) return@launch
            val newBranchId = "Fork-${UUID.randomUUID().toString().take(4)}"
            
            // To make the fork "real" in the UI, we immediately update the state
            _uiState.update { it.copy(currentBranchId = newBranchId) }
            
            // We need to fetch the path up to messageId and keep it in memory 
            // until the user sends a new message in this branch.
            chatDao.getMessagesPath(messageId).collectLatest { path ->
                _uiState.update { it.copy(messages = path) }
            }
        }
    }

    fun onSelectBranch(branchId: String) {
        val conversationId = _uiState.value.currentConversationId ?: return
        _uiState.update { it.copy(currentBranchId = branchId) }
        observeMessages(conversationId, branchId)
    }

    fun deleteConversation(id: Long) {
        viewModelScope.launch {
            chatDao.deleteConversation(id)
            if (_uiState.value.currentConversationId == id) {
                onNewChat()
            }
        }
    }

    fun stopStreaming() {
        streamingJob?.cancel()
        _uiState.update { it.copy(isStreaming = false, streamingText = "") }
    }

    companion object {
        fun factory(
            repository: GeminiRepository,
            chatDao: ChatDao,
            settingsRepository: SettingsRepository,
            hasApiKey: Boolean
        ) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ChatViewModel(repository, chatDao, settingsRepository, hasApiKey) as T
        }
    }
}
