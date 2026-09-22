package com.fahim.geminiApiComposeStarter

import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.db.ChatMessage
import com.fahim.geminiApiComposeStarter.data.db.MessageRole
import com.fahim.geminiApiComposeStarter.data.prefs.UserPreferences
import com.fahim.geminiApiComposeStarter.data.prefs.UserPrefs
import com.fahim.geminiApiComposeStarter.ui.chat.ChatViewModel
import com.fahim.geminiApiComposeStarter.ui.chat.PromptError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * Unit tests for [ChatViewModel] using a fake [GeminiRepository] and Mockito-Kotlin.
 * All coroutines run on [StandardTestDispatcher] — fully deterministic.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    // ── Fakes / Mocks ──────────────────────────────────────────

    private val historyFlow = MutableStateFlow<List<ChatMessage>>(emptyList())

    /** Fake repository — controlled by each test. */
    private lateinit var fakeRepository: GeminiRepository
    private lateinit var fakePreferences: UserPreferences

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        fakeRepository = mock()
        fakePreferences = mock()

        whenever(fakeRepository.getChatHistory()).thenReturn(historyFlow)
        whenever(fakePreferences.userPrefsFlow).thenReturn(flowOf(UserPrefs()))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── Helpers ────────────────────────────────────────────────

    private fun createViewModel(hasApiKey: Boolean = true) = ChatViewModel(
        repository = fakeRepository,
        userPreferences = fakePreferences,
        hasApiKey = hasApiKey,
    )

    // ── Tests ──────────────────────────────────────────────────

    @Test
    fun `onSend with empty prompt sets EMPTY promptError`() = runTest {
        val vm = createViewModel()
        vm.onSend()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(PromptError.EMPTY, vm.uiState.value.promptError)
    }

    @Test
    fun `onSend with missing API key sets error message`() = runTest {
        val vm = createViewModel(hasApiKey = false)
        vm.onPromptChange("Hello")
        vm.onSend()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(ChatViewModel.MISSING_API_KEY_MESSAGE, vm.uiState.value.errorMessage)
    }

    @Test
    fun `onSend success clears loading and prompt`() = runTest {
        whenever(fakeRepository.sendMessage("Hello")).thenReturn(Result.success("Hi there!"))

        val vm = createViewModel()
        vm.onPromptChange("Hello")
        vm.onSend()
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(vm.uiState.value.errorMessage)
        assertEquals(false, vm.uiState.value.isLoading)
        assertEquals("", vm.uiState.value.prompt)
    }

    @Test
    fun `onSend failure sets error message and clears loading`() = runTest {
        whenever(fakeRepository.sendMessage("Hello"))
            .thenReturn(Result.failure(RuntimeException("Network error")))

        val vm = createViewModel()
        vm.onPromptChange("Hello")
        vm.onSend()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Network error", vm.uiState.value.errorMessage)
        assertEquals(false, vm.uiState.value.isLoading)
    }

    @Test
    fun `onVoiceResult populates prompt and clears listening state`() = runTest {
        val vm = createViewModel()
        vm.onListeningStateChange(true)
        vm.onVoiceResult("Voice prompt text")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Voice prompt text", vm.uiState.value.prompt)
        assertEquals(false, vm.uiState.value.isListening)
    }

    @Test
    fun `history flow updates are reflected in UI state`() = runTest {
        val vm = createViewModel()
        val msg = ChatMessage(id = 1L, role = MessageRole.USER, text = "Hey")
        historyFlow.emit(listOf(msg))
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.uiState.value.messages.contains(msg))
    }

    @Test
    fun `onErrorDismissed clears error message`() = runTest {
        whenever(fakeRepository.sendMessage("Hello"))
            .thenReturn(Result.failure(RuntimeException("err")))

        val vm = createViewModel()
        vm.onPromptChange("Hello")
        vm.onSend()
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(vm.uiState.value.errorMessage)
        vm.onErrorDismissed()
        assertNull(vm.uiState.value.errorMessage)
    }
}
