package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fahim.geminiApiComposeStarter.data.ChatDefaults
import com.fahim.geminiApiComposeStarter.data.ChatStorage
import com.fahim.geminiApiComposeStarter.data.ConversationMessage
import com.fahim.geminiApiComposeStarter.data.ConversationRole
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.StoredChat
import com.fahim.geminiApiComposeStarter.data.StoredMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(
    private val repository: GeminiRepository,
    private val chatStorage: ChatStorage,
    private val hasApiKey: Boolean,
) : ViewModel() {

    private var storedChats: List<StoredChat> = emptyList()
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()
    private var nextMessageId = 0L
    private var isInitialized = false

    init {
        viewModelScope.launch {
            storedChats = chatStorage.loadChats().ifEmpty { listOf(ChatDefaults.newChat()) }
            val activeChatId = chatStorage.loadActiveChatId()
                ?.takeIf { activeId -> storedChats.any { it.id == activeId } }
                ?: storedChats.first().id
            nextMessageId = storedChats.maxOfOrNull { chat ->
                chat.messages.maxOfOrNull { message -> message.id } ?: -1L
            }?.plus(1L) ?: 0L
            _uiState.value = storedChats.toUiState(activeChatId)
            isInitialized = true
            saveChats()
        }
    }

    fun onPromptChange(value: String) {
        _uiState.update { it.copy(prompt = value, promptError = null) }
    }

    fun showError(message: String) {
        _uiState.update { it.copy(errorMessage = message) }
    }

    fun onNewChat() {
        if (!isInitialized || _uiState.value.isLoading) return
        val newChat = ChatDefaults.newChat()
        storedChats = listOf(newChat) + storedChats
        _uiState.value = storedChats.toUiState(newChat.id)
        saveChats()
    }

    fun onSelectChat(chatId: String) {
        if (!isInitialized || _uiState.value.isLoading || chatId == _uiState.value.activeChatId) return
        if (storedChats.none { it.id == chatId }) return
        _uiState.value = storedChats.toUiState(chatId)
        saveChats()
    }

    fun onSend() {
        if (!isInitialized) return
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

        val history = _uiState.value.messages.map { it.toConversationMessage() }
        val userMessage = ChatMessage(
            id = nextMessageId++,
            text = prompt,
            author = ChatAuthor.USER,
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
        persistActiveChat()
        viewModelScope.launch {
            var streamedAnyText = false
            var failed = false
            val geminiMessageId = nextMessageId++

            repository.generateTextStream(prompt, history).collect { result ->
                result.fold(
                    onSuccess = { chunk ->
                        streamedAnyText = true
                        _uiState.update { state ->
                            val existingMessage = state.messages.firstOrNull { it.id == geminiMessageId }
                            val messages = if (existingMessage == null) {
                                state.messages + ChatMessage(
                                    id = geminiMessageId,
                                    text = chunk,
                                    author = ChatAuthor.GEMINI,
                                )
                            } else {
                                state.messages.map { message ->
                                    if (message.id == geminiMessageId) {
                                        message.copy(text = message.text + chunk)
                                    } else {
                                        message
                                    }
                                }
                            }
                            state.copy(messages = messages)
                        }
                        persistActiveChat()
                    },
                    onFailure = { error ->
                        failed = true
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = error.message ?: "Gemini request failed.",
                            )
                        }
                        persistActiveChat()
                    },
                )
            }

            if (!failed) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = if (streamedAnyText) null else "Empty response from Gemini",
                    )
                }
                persistActiveChat()
            }
        }
    }

    private fun persistActiveChat() {
        val state = _uiState.value
        val title = storedChats.firstOrNull { it.id == state.activeChatId }?.title
            ?.takeUnless { it == ChatDefaults.NEW_CHAT_TITLE }
            ?: state.messages.firstOrNull { it.author == ChatAuthor.USER }?.text?.toChatTitle()
            ?: ChatDefaults.NEW_CHAT_TITLE
        storedChats = storedChats.map { chat ->
            if (chat.id == state.activeChatId) {
                chat.copy(
                    title = title,
                    messages = state.messages.map { it.toStoredMessage() },
                )
            } else {
                chat
            }
        }
        _uiState.update { it.copy(chatSummaries = storedChats.toSummaries()) }
        saveChats()
    }

    private fun saveChats() {
        val activeChatId = _uiState.value.activeChatId
        if (activeChatId.isBlank()) return
        viewModelScope.launch { chatStorage.saveChats(storedChats, activeChatId) }
    }

    private fun ChatMessage.toConversationMessage(): ConversationMessage = ConversationMessage(
        role = when (author) {
            ChatAuthor.USER -> ConversationRole.USER
            ChatAuthor.GEMINI -> ConversationRole.MODEL
        },
        text = text,
    )

    private fun ChatMessage.toStoredMessage(): StoredMessage = StoredMessage(
        id = id,
        text = text,
        role = when (author) {
            ChatAuthor.USER -> ConversationRole.USER
            ChatAuthor.GEMINI -> ConversationRole.MODEL
        },
    )

    private fun StoredMessage.toChatMessage(): ChatMessage = ChatMessage(
        id = id,
        text = text,
        author = when (role) {
            ConversationRole.USER -> ChatAuthor.USER
            ConversationRole.MODEL -> ChatAuthor.GEMINI
        },
    )

    private fun List<StoredChat>.toUiState(activeChatId: String): ChatUiState {
        val activeChat = first { it.id == activeChatId }
        return ChatUiState(
            activeChatId = activeChat.id,
            chatSummaries = toSummaries(),
            messages = activeChat.messages.map { it.toChatMessage() },
        )
    }

    private fun List<StoredChat>.toSummaries(): List<ChatSummary> = map { chat ->
        ChatSummary(id = chat.id, title = chat.title)
    }

    private fun String.toChatTitle(): String {
        val normalized = trim().replace(Regex("\\s+"), " ")
        return if (normalized.length <= 32) normalized else normalized.take(29).trimEnd() + "..."
    }

    companion object {
        const val MISSING_API_KEY_MESSAGE =
            "GEMINI_API_KEY is missing. Add it to local.properties and rebuild."

        fun factory(repository: GeminiRepository, chatStorage: ChatStorage, hasApiKey: Boolean) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ChatViewModel(repository, chatStorage, hasApiKey) as T
            }
    }
}
