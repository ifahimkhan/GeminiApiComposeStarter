package com.fahim.geminiApiComposeStarter.ui.chat

import android.net.Uri

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope

import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.UserPreferences

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import java.util.UUID


class ChatViewModel(
    private val repository: GeminiRepository,
    private val userPreferences: UserPreferences,
    private val hasApiKey: Boolean
) : ViewModel() {


    private val _uiState =
        MutableStateFlow(
            ChatUiState()
        )


    val uiState: StateFlow<ChatUiState> =
        _uiState.asStateFlow()


    /*
     * Keeps track of API-key initialization.
     */
    private var initializationJob: Job? = null


    init {

        /*
         * Load saved user name.
         */
        viewModelScope.launch {

            userPreferences.userName.collect { name ->

                _uiState.update {

                    it.copy(
                        userName = name,
                        nameDraft = name
                    )
                }
            }
        }


        /*
         * Load saved dark-mode preference.
         */
        viewModelScope.launch {

            userPreferences.darkMode.collect { enabled ->

                _uiState.update {

                    it.copy(
                        isDarkMode = enabled
                    )
                }
            }
        }


        /*
         * Initialize encrypted API-key storage.
         */
        if (hasApiKey) {

            initializationJob =
                viewModelScope.launch {

                    try {

                        repository.initialize()

                    } catch (exception: Exception) {

                        exception.printStackTrace()

                        _uiState.update {

                            it.copy(
                                errorMessage =
                                    exception.message
                                        ?: "Unable to initialize secure API key storage."
                            )
                        }
                    }
                }
        }
    }


    /*
     * Updates ONLY the input field.
     *
     * It does NOT modify the conversation history.
     */
    fun onPromptChange(
        value: String
    ) {

        _uiState.update {

            it.copy(
                inputText = value,
                promptError = null,
                errorMessage = null
            )
        }
    }


    /*
     * Sends the current input as a new conversation message.
     */
    fun onSend() {

        val currentState =
            _uiState.value


        val prompt =
            currentState.inputText.trim()


        /*
         * Validate empty input.
         */
        if (prompt.isEmpty()) {

            _uiState.update {

                it.copy(
                    promptError =
                        PromptError.EMPTY
                )
            }

            return
        }


        /*
         * Validate API key.
         */
        if (!hasApiKey) {

            _uiState.update {

                it.copy(
                    errorMessage =
                        MISSING_API_KEY_MESSAGE
                )
            }

            return
        }


        /*
         * Prevent multiple simultaneous requests.
         */
        if (currentState.isLoading) {
            return
        }




        /*
         * Preserve the selected image before clearing
         * the input state.
         */
        val imageUriString =
            currentState.selectedImageUri


        /*
         * Create a permanent user message.
         *
         * This is what fixes the original bug:
         * the message is copied into the messages list
         * and becomes independent of inputText.
         */
        val userMessage =
            ChatMessage(
                id = UUID.randomUUID().toString(),
                isUser = true,
                text = prompt,
                imageUri = imageUriString
            )


        /*
         * Immediately place the user's message into
         * the conversation and clear the input field.
         */
        _uiState.update {

            it.copy(

                messages =
                    it.messages + userMessage,

                inputText = "",

                selectedImageUri = null,

                isLoading = true,

                promptError = null,

                errorMessage = null
            )
        }


        viewModelScope.launch {

            try {

                /*
                 * Ensure secure initialization has finished
                 * before the API key is requested.
                 */
                initializationJob?.join()


                val response =
                    repository.sendMessage(

                        prompt = prompt,

                        imageUri =
                            imageUriString?.let {
                                Uri.parse(it)
                            }
                    )


                /*
                 * Add Gemini's response as a NEW message.
                 */
                val geminiMessage =
                    ChatMessage(

                        id =
                            UUID.randomUUID().toString(),

                        isUser = false,

                        text = response
                    )


                _uiState.update {

                    it.copy(

                        messages =
                            it.messages + geminiMessage,

                        isLoading = false
                    )
                }

            } catch (exception: Exception) {

                exception.printStackTrace()

                _uiState.update {

                    it.copy(

                        isLoading = false,

                        errorMessage =
                            exception.message
                                ?: exception.javaClass.simpleName
                    )
                }
            }
        }
    }


    /*
     * Image selected through Android's system picker.
     */
    fun onImageSelected(
        uri: String
    ) {

        _uiState.update {

            it.copy(

                selectedImageUri = uri,

                errorMessage = null
            )
        }
    }


    /*
     * Remove pending image.
     */
    fun clearSelectedImage() {

        _uiState.update {

            it.copy(
                selectedImageUri = null
            )
        }
    }


    /*
     * Opens Preferences.
     */
    fun openPreferences() {

        _uiState.update {

            it.copy(

                showPreferences = true,

                nameDraft =
                    it.userName
            )
        }
    }


    /*
     * Closes Preferences.
     */
    fun closePreferences() {

        _uiState.update {

            it.copy(
                showPreferences = false
            )
        }
    }


    /*
     * Updates the temporary name in Preferences.
     */
    fun updateNameDraft(
        value: String
    ) {

        _uiState.update {

            it.copy(
                nameDraft = value
            )
        }
    }


    /*
     * Toggles dark mode.
     *
     * This is persisted immediately.
     */
    fun toggleDarkMode() {

        val newValue =
            !_uiState.value.isDarkMode


        _uiState.update {

            it.copy(
                isDarkMode = newValue
            )
        }


        viewModelScope.launch {

            try {

                userPreferences.saveDarkMode(
                    newValue
                )

            } catch (exception: Exception) {

                _uiState.update {

                    it.copy(
                        errorMessage =
                            exception.message
                                ?: "Unable to save theme preference."
                    )
                }
            }
        }
    }


    /*
     * Saves Preferences.
     */
    fun savePreferences() {

        val state =
            _uiState.value


        val name =
            state.nameDraft.trim()


        val darkMode =
            state.isDarkMode


        viewModelScope.launch {

            try {

                userPreferences.saveUserName(
                    name
                )

                userPreferences.saveDarkMode(
                    darkMode
                )


                _uiState.update {

                    it.copy(

                        userName = name,

                        nameDraft = name,

                        showPreferences = false,

                        errorMessage = null
                    )
                }

            } catch (exception: Exception) {

                exception.printStackTrace()

                _uiState.update {

                    it.copy(

                        errorMessage =
                            exception.message
                                ?: "Unable to save preferences."
                    )
                }
            }
        }
    }


    /*
     * Clears the current error.
     */
    fun clearError() {

        _uiState.update {

            it.copy(
                errorMessage = null,
                promptError = null
            )
        }
    }


    companion object {

        const val MISSING_API_KEY_MESSAGE =
            "GEMINI_API_KEY is missing. Add it to local.properties and rebuild."


        fun factory(
            repository: GeminiRepository,
            userPreferences: UserPreferences,
            hasApiKey: Boolean
        ): ViewModelProvider.Factory {

            return object :
                ViewModelProvider.Factory {

                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(
                    modelClass: Class<T>
                ): T {

                    if (
                        modelClass.isAssignableFrom(
                            ChatViewModel::class.java
                        )
                    ) {

                        return ChatViewModel(

                            repository =
                                repository,

                            userPreferences =
                                userPreferences,

                            hasApiKey =
                                hasApiKey

                        ) as T
                    }


                    throw IllegalArgumentException(
                        "Unknown ViewModel class: ${modelClass.name}"
                    )
                }
            }
        }
    }
}