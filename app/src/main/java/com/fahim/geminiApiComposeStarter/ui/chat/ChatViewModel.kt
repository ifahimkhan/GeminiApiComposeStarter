package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.local.ChatDao
import com.fahim.geminiApiComposeStarter.data.local.ChatMessageEntity
import com.fahim.geminiApiComposeStarter.data.preferences.UserPreferences
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(
    private val repository: GeminiRepository,
    private val chatDao: ChatDao,
    private val userPreferencesRepository: UserPreferences,
    private val hasApiKey: Boolean,
) : ViewModel() {

    private val _uiState =
        MutableStateFlow(
            ChatUiState()
        )

    val uiState:
            StateFlow<ChatUiState> =
        _uiState.asStateFlow()

    init {
        observeChatHistory()
        observePreferences()
    }

    private fun observeChatHistory() {

        viewModelScope.launch {

            chatDao
                .getAllMessages()
                .collect { entities ->

                    val messages =
                        entities.map { entity ->

                            ChatMessage(
                                id = entity.id,

                                text =
                                    entity.text,

                                sender =
                                    if (
                                        entity.sender ==
                                        MessageSender
                                            .USER
                                            .name
                                    ) {

                                        MessageSender.USER

                                    } else {

                                        MessageSender.GEMINI
                                    },
                            )
                        }

                    _uiState.update {

                        it.copy(
                            messages =
                                messages
                        )
                    }
                }
        }
    }

    private fun observePreferences() {

        viewModelScope.launch {

            userPreferencesRepository
                .conciseReplies
                .collect { enabled ->

                    _uiState.update {

                        it.copy(
                            conciseReplies =
                                enabled
                        )
                    }
                }
        }
    }

    fun onConciseRepliesChange(
        enabled: Boolean
    ) {

        viewModelScope.launch {

            userPreferencesRepository
                .setConciseReplies(
                    enabled
                )
        }
    }

    fun onPromptChange(
        value: String
    ) {

        _uiState.update {

            it.copy(
                prompt = value,
                promptError = null,
            )
        }
    }

    fun onSend() {

        val prompt =
            _uiState.value
                .prompt
                .trim()

        if (prompt.isEmpty()) {

            _uiState.update {

                it.copy(
                    promptError =
                        PromptError.EMPTY
                )
            }

            return
        }

        if (!hasApiKey) {

            _uiState.update {

                it.copy(
                    errorMessage =
                        MISSING_API_KEY_MESSAGE
                )
            }

            return
        }

        if (
            _uiState.value.isLoading
        ) {
            return
        }

        val conciseReplies =
            _uiState.value
                .conciseReplies

        _uiState.update {

            it.copy(
                prompt = "",
                isLoading = true,
                errorMessage = null,
                promptError = null,
            )
        }

        viewModelScope.launch {

            try {

                /*
                 * Save user's message.
                 */
                chatDao.insertMessage(

                    ChatMessageEntity(
                        text = prompt,

                        sender =
                            MessageSender
                                .USER
                                .name,
                    )
                )

                /*
                 * Apply preference.
                 */
                val geminiPrompt =
                    if (
                        conciseReplies
                    ) {

                        """
                        Answer the following question concisely.
                        Keep the response clear and brief.

                        User question:
                        $prompt
                        """.trimIndent()

                    } else {

                        prompt
                    }

                repository
                    .generateText(
                        geminiPrompt
                    )
                    .fold(

                        onSuccess = { text ->

                            chatDao
                                .insertMessage(

                                    ChatMessageEntity(
                                        text =
                                            text,

                                        sender =
                                            MessageSender
                                                .GEMINI
                                                .name,
                                    )
                                )

                            _uiState.update {

                                it.copy(
                                    isLoading =
                                        false
                                )
                            }
                        },

                        onFailure = { error ->

                            _uiState.update {

                                it.copy(
                                    isLoading =
                                        false,

                                    errorMessage =
                                        error.message
                                            ?: "Something went wrong",
                                )
                            }
                        },
                    )

            } catch (
                e: CancellationException
            ) {

                throw e

            } catch (
                e: Exception
            ) {

                _uiState.update {

                    it.copy(
                        isLoading =
                            false,

                        errorMessage =
                            e.message
                                ?: "Unable to process message",
                    )
                }
            }
        }
    }

    fun clearError() {

        _uiState.update {

            it.copy(
                errorMessage = null
            )
        }
    }

    fun clearChat() {

        viewModelScope.launch {

            try {

                chatDao.clearMessages()

            } catch (
                e: Exception
            ) {

                _uiState.update {

                    it.copy(
                        errorMessage =
                            "Unable to clear chat history"
                    )
                }
            }
        }
    }

    companion object {

        const val MISSING_API_KEY_MESSAGE =
            "GEMINI_API_KEY is missing. Add it to local.properties and rebuild."

        fun factory(
            repository: GeminiRepository,
            chatDao: ChatDao,
            userPreferencesRepository:
            UserPreferences,
            hasApiKey: Boolean,
        ) =
            object :
                ViewModelProvider.Factory {

                @Suppress(
                    "UNCHECKED_CAST"
                )
                override fun <T : ViewModel> create(
                    modelClass:
                    Class<T>
                ): T {

                    return ChatViewModel(
                        repository =
                            repository,

                        chatDao =
                            chatDao,

                        userPreferencesRepository =
                            userPreferencesRepository,

                        hasApiKey =
                            hasApiKey,
                    ) as T
                }
            }
    }
}