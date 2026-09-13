package com.fahim.geminiApiComposeStarter.ui.chat

import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.local.ChatDao
import com.fahim.geminiApiComposeStarter.data.local.ChatMessageEntity
import com.fahim.geminiApiComposeStarter.data.preferences.UserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

@OptIn(
    ExperimentalCoroutinesApi::class
)
class ChatViewModelTest {

    private val dispatcher =
        UnconfinedTestDispatcher()

    @Before
    fun setup() {

        Dispatchers.setMain(
            dispatcher
        )
    }

    @After
    fun tearDown() {

        Dispatchers.resetMain()
    }

    @Test
    fun `sending message adds user and Gemini messages`() =
        runTest {

            val repository =
                FakeGeminiRepository(
                    response =
                        Result.success(
                            "Hello from Gemini"
                        )
                )

            val dao =
                FakeChatDao()

            val preferences =
                FakeUserPreferences()

            val viewModel =
                _root_ide_package_.com.fahim.geminiApiComposeStarter.ui.chat.ChatViewModel(
                    repository =
                        repository,

                    chatDao =
                        dao,

                    userPreferencesRepository =
                        preferences,

                    hasApiKey =
                        true,
                )

            viewModel.onPromptChange(
                "Hello"
            )

            viewModel.onSend()

            advanceUntilIdle()

            val messages =
                viewModel
                    .uiState
                    .value
                    .messages

            assertEquals(
                2,
                messages.size
            )

            assertEquals(
                "Hello",
                messages[0].text
            )

            assertEquals(
                MessageSender.USER,
                messages[0].sender
            )

            assertEquals(
                "Hello from Gemini",
                messages[1].text
            )

            assertEquals(
                MessageSender.GEMINI,
                messages[1].sender
            )

            assertFalse(
                viewModel
                    .uiState
                    .value
                    .isLoading
            )
        }

    @Test
    fun `empty prompt shows validation error`() =
        runTest {

            val viewModel =
                _root_ide_package_.com.fahim.geminiApiComposeStarter.ui.chat.ChatViewModel(
                    repository =
                        FakeGeminiRepository(
                            Result.success(
                                "Unused"
                            )
                        ),

                    chatDao =
                        FakeChatDao(),

                    userPreferencesRepository =
                        FakeUserPreferences(),

                    hasApiKey =
                        true,
                )

            viewModel.onSend()

            assertEquals(
                PromptError.EMPTY,
                viewModel
                    .uiState
                    .value
                    .promptError
            )
        }

    @Test
    fun `repository failure exposes error message`() =
        runTest {

            val viewModel =
                _root_ide_package_.com.fahim.geminiApiComposeStarter.ui.chat.ChatViewModel(
                    repository =
                        FakeGeminiRepository(
                            Result.failure(
                                IllegalStateException(
                                    "Network failure"
                                )
                            )
                        ),

                    chatDao =
                        FakeChatDao(),

                    userPreferencesRepository =
                        FakeUserPreferences(),

                    hasApiKey =
                        true,
                )

            viewModel.onPromptChange(
                "Hello"
            )

            viewModel.onSend()

            advanceUntilIdle()

            assertNotNull(
                viewModel
                    .uiState
                    .value
                    .errorMessage
            )

            assertEquals(
                "Network failure",
                viewModel
                    .uiState
                    .value
                    .errorMessage
            )
        }
}

/*
 * Fake Gemini repository.
 *
 * No real network/API call happens
 * during unit tests.
 */
private class FakeGeminiRepository(
    private val response:
    Result<String>,
) : GeminiRepository {

    override suspend fun generateText(
        prompt: String
    ): Result<String> {

        return response
    }
}

/*
 * In-memory fake Room DAO.
 */
private class FakeChatDao :
    ChatDao {

    private val messages =
        MutableStateFlow<
                List<ChatMessageEntity>
                >(
            emptyList()
        )

    private var nextId =
        1L

    override fun getAllMessages():
            Flow<List<ChatMessageEntity>> {

        return messages
    }

    override suspend fun insertMessage(
        message:
        ChatMessageEntity
    ): Long {

        val id =
            nextId++

        val saved =
            message.copy(
                id = id
            )

        messages.value =
            messages.value +
                    saved

        return id
    }

    override suspend fun clearMessages() {

        messages.value =
            emptyList()
    }
}

/*
 * In-memory fake DataStore preference.
 */
private class FakeUserPreferences :
    UserPreferences {

    private val concise =
        MutableStateFlow(
            false
        )

    override val conciseReplies:
            Flow<Boolean> =
        concise

    override suspend fun setConciseReplies(
        enabled: Boolean
    ) {

        concise.value =
            enabled
    }
}