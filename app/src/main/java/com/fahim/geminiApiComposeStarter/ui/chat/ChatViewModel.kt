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
    private val repo: GeminiRepository,
    private val storage: ChatStorage,
    private val hasApiKey: Boolean,
) : ViewModel() {

    private var savedChats: List<StoredChat> = emptyList()
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()
    private var nextId = 0L
    private var ready = false

    init {
        viewModelScope.launch {
            savedChats = storage.loadChats().ifEmpty { listOf(ChatDefaults.newChat()) }
            val activeId = storage.loadActiveChatId()
                ?.takeIf { id -> savedChats.any { it.id == id } }
                ?: savedChats.first().id
            nextId = savedChats.maxOfOrNull { chat ->
                chat.messages.maxOfOrNull { message -> message.id } ?: -1L
            }?.plus(1L) ?: 0L
            _uiState.value = savedChats.toUiState(activeId)
            ready = true
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
        if (!ready || _uiState.value.isLoading) return
        val chat = ChatDefaults.newChat()
        savedChats = listOf(chat) + savedChats
        _uiState.value = savedChats.toUiState(chat.id)
        saveChats()
    }

    fun onSelectChat(chatId: String) {
        if (!ready || _uiState.value.isLoading || chatId == _uiState.value.activeChatId) return
        if (savedChats.none { it.id == chatId }) return
        _uiState.value = savedChats.toUiState(chatId)
        saveChats()
    }

    fun onSend() {
        if (!ready) return
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
        val userMsg = ChatMessage(
            id = nextId++,
            text = prompt,
            author = ChatAuthor.USER,
        )
        _uiState.update {
            it.copy(
                prompt = "",
                messages = it.messages + userMsg,
                isLoading = true,
                errorMessage = null,
                promptError = null,
            )
        }
        persistActiveChat()
        viewModelScope.launch {
            var gotText = false
            var failed = false
            val replyId = nextId++

            repo.generateTextStream(prompt, history).collect { result ->
                result.fold(
                    onSuccess = { chunk ->
                        gotText = true
                        _uiState.update { state ->
                            val reply = state.messages.firstOrNull { it.id == replyId }
                            val messages = if (reply == null) {
                                state.messages + ChatMessage(
                                    id = replyId,
                                    text = chunk,
                                    author = ChatAuthor.GEMINI,
                                )
                            } else {
                                state.messages.map { message ->
                                    if (message.id == replyId) {
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
                        errorMessage = if (gotText) null else "Empty response from Gemini",
                    )
                }
                persistActiveChat()
            }
        }
    }

    private fun persistActiveChat() {
        val state = _uiState.value
        val title = savedChats.firstOrNull { it.id == state.activeChatId }?.title
            ?.takeUnless { it == ChatDefaults.NEW_CHAT_TITLE }
            ?: state.messages.firstOrNull { it.author == ChatAuthor.USER }?.text?.toChatTitle()
            ?: ChatDefaults.NEW_CHAT_TITLE
        savedChats = savedChats.map { chat ->
            if (chat.id == state.activeChatId) {
                chat.copy(
                    title = title,
                    messages = state.messages.map { it.toStoredMessage() },
                )
            } else {
                chat
            }
        }
        _uiState.update { it.copy(chatSummaries = savedChats.toSummaries()) }
        saveChats()
    }

    private fun saveChats() {
        val activeId = _uiState.value.activeChatId
        if (activeId.isBlank()) return
        viewModelScope.launch { storage.saveChats(savedChats, activeId) }
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

    private fun List<StoredChat>.toUiState(activeId: String): ChatUiState {
        val activeChat = first { it.id == activeId }
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

        fun factory(repo: GeminiRepository, storage: ChatStorage, hasApiKey: Boolean) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ChatViewModel(repo, storage, hasApiKey) as T
            }
    }
}
