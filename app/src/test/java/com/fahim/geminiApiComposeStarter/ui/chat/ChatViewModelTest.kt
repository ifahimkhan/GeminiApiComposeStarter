package com.fahim.geminiApiComposeStarter.ui.chat

import com.fahim.geminiApiComposeStarter.data.GeminiException
import com.fahim.geminiApiComposeStarter.data.model.Role
import com.fahim.geminiApiComposeStarter.data.model.ThemeMode
import com.fahim.geminiApiComposeStarter.testing.FakeChatHistoryRepository
import com.fahim.geminiApiComposeStarter.testing.FakeGeminiRepository
import com.fahim.geminiApiComposeStarter.testing.FakeUserPreferencesRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    private lateinit var gemini: FakeGeminiRepository
    private lateinit var history: FakeChatHistoryRepository
    private lateinit var prefs: FakeUserPreferencesRepository
    private lateinit var viewModel: ChatViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        gemini = FakeGeminiRepository()
        history = FakeChatHistoryRepository()
        prefs = FakeUserPreferencesRepository()
        viewModel = ChatViewModel(gemini, history, prefs)
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private val state get() = viewModel.uiState.value

    @Test
    fun blankPrompt_setsPromptError_andSkipsRequest() {
        viewModel.onPromptChange("   ")
        viewModel.onSend()

        assertEquals(PromptError.EMPTY, state.promptError)
        assertTrue(gemini.requests.isEmpty())
    }

    @Test
    fun typing_clearsPromptError() {
        viewModel.onSend()
        viewModel.onPromptChange("h")

        assertNull(state.promptError)
    }

    @Test
    fun prompt_isCappedAtMaxLength() {
        viewModel.onPromptChange("a".repeat(ChatViewModel.MAX_PROMPT_LENGTH + 100))

        assertEquals(ChatViewModel.MAX_PROMPT_LENGTH, state.prompt.length)
    }

    @Test
    fun success_storesBothTurns_andClearsPrompt() {
        gemini.result = Result.success("Hello!")

        viewModel.onPromptChange("  Hi  ")
        viewModel.onSend()

        assertEquals(listOf(Role.USER to "Hi", Role.MODEL to "Hello!"), state.messages.map { it.role to it.text })
        assertEquals("", state.prompt)
        assertFalse(state.isLoading)
    }

    @Test
    fun loadingState_isShownWhileWaiting_andDuplicateSendIsIgnored() {
        val gate = CompletableDeferred<Unit>()
        gemini.gate = gate

        viewModel.onPromptChange("Hi")
        viewModel.onSend()
        assertTrue(state.isLoading)

        viewModel.onPromptChange("again")
        viewModel.onSend()
        assertEquals(1, gemini.requests.size)

        gate.complete(Unit)
        assertFalse(state.isLoading)
    }

    @Test
    fun earlierTurns_areSentAsContext() {
        viewModel.onPromptChange("first")
        viewModel.onSend()
        viewModel.onPromptChange("second")
        viewModel.onSend()

        val (context, prompt) = gemini.requests.last()
        assertEquals("second", prompt)
        assertEquals(listOf("first", "reply"), context.map { it.text })
    }

    @Test
    fun failure_rollsBackUserTurn_restoresPrompt_andShowsSafeMessage() {
        gemini.result = Result.failure(GeminiException("Quota exceeded."))

        viewModel.onPromptChange("Hi")
        viewModel.onSend()

        assertTrue(state.messages.isEmpty())
        assertEquals("Hi", state.prompt)
        assertEquals("Quota exceeded.", state.errorMessage)
        assertFalse(state.isLoading)
    }

    @Test
    fun unknownFailure_hidesRawExceptionMessage() {
        gemini.result = Result.failure(RuntimeException("HTTP 500 {internal details}"))

        viewModel.onPromptChange("Hi")
        viewModel.onSend()

        assertEquals(ChatViewModel.GENERIC_ERROR_MESSAGE, state.errorMessage)
    }

    @Test
    fun errorShown_clearsMessage_soRepeatErrorsShowAgain() {
        gemini.result = Result.failure(GeminiException("Boom"))
        viewModel.onPromptChange("Hi")
        viewModel.onSend()

        viewModel.onErrorShown()
        assertNull(state.errorMessage)

        viewModel.onSend()
        assertEquals("Boom", state.errorMessage)
    }

    @Test
    fun voiceInput_isAppendedToTypedText() {
        viewModel.onPromptChange("Tell me ")
        viewModel.onVoiceInput("a joke")

        assertEquals("Tell me a joke", state.prompt)
    }

    @Test
    fun clearHistory_removesAllMessages() = runTest {
        history.add(Role.USER, "old")
        viewModel.onClearHistory()

        assertTrue(state.messages.isEmpty())
    }

    @Test
    fun preferenceChanges_flowIntoUiState() {
        viewModel.onThemeModeChange(ThemeMode.DARK)
        viewModel.onDynamicColorChange(false)

        assertEquals(ThemeMode.DARK, state.preferences.themeMode)
        assertFalse(state.preferences.dynamicColor)
    }
}
