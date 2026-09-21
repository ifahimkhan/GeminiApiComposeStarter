package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import com.fahim.geminiApiComposeStarter.R
import com.fahim.geminiApiComposeStarter.data.Author
import com.fahim.geminiApiComposeStarter.data.ChatMessage
import com.fahim.geminiApiComposeStarter.data.ThemeMode
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** ChatScreen is stateless, so each test just hands it a state and records the callbacks. */
class ChatScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun string(id: Int) = InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    private val conversation = listOf(
        ChatMessage(0, Author.USER, "What is 2+2?"),
        ChatMessage(1, Author.GEMINI, "It is **4**."),
    )

    private class Calls {
        var sent = 0
        var retried = 0
        var errorShown = 0
        var cleared = 0
        var voice = 0
        var typed: String? = null
        var theme: ThemeMode? = null
    }

    private fun setScreen(state: ChatUiState, calls: Calls = Calls()): Calls {
        composeRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = state,
                    widthSizeClass = WindowWidthSizeClass.Compact,
                    onPromptChange = { calls.typed = it },
                    onSend = { calls.sent++ },
                    onRetry = { calls.retried++ },
                    onErrorShown = { calls.errorShown++ },
                    onThemeModeSelected = { calls.theme = it },
                    onClearConversation = { calls.cleared++ },
                    onVoiceInput = { calls.voice++ },
                )
            }
        }
        return calls
    }

    @Test
    fun emptyConversation_showsHint() {
        setScreen(ChatUiState())

        composeRule.onNodeWithText(string(R.string.empty_chat_hint)).assertIsDisplayed()
    }

    @Test
    fun conversation_showsBothBubbles() {
        setScreen(ChatUiState(messages = conversation))

        composeRule.onNodeWithTag(ChatTestTags.MESSAGE_LIST).assertIsDisplayed()
        composeRule.onNodeWithText("What is 2+2?").assertIsDisplayed()
        // The **bold** markers are rendered, not shown literally.
        composeRule.onNodeWithText("It is 4.").assertIsDisplayed()
    }

    @Test
    fun loading_showsSpinnerAndDisablesSend() {
        setScreen(ChatUiState(messages = conversation.take(1), isLoading = true))

        composeRule.onNodeWithTag(ChatTestTags.LOADING_INDICATOR).assertIsDisplayed()
        composeRule.onNodeWithTag(ChatTestTags.SEND_BUTTON).assertIsNotEnabled()
        composeRule.onNodeWithTag(ChatTestTags.VOICE_BUTTON).assertIsNotEnabled()
    }

    @Test
    fun idle_hidesSpinnerAndEnablesSend() {
        setScreen(ChatUiState(messages = conversation))

        composeRule.onNodeWithTag(ChatTestTags.LOADING_INDICATOR).assertDoesNotExist()
        composeRule.onNodeWithTag(ChatTestTags.SEND_BUTTON).assertIsEnabled()
    }

    @Test
    fun typing_reportsTheText() {
        val calls = setScreen(ChatUiState())

        composeRule.onNodeWithTag(ChatTestTags.PROMPT_FIELD).performTextInput("hello")

        assertEquals("hello", calls.typed)
    }

    @Test
    fun sendButton_invokesOnSend() {
        val calls = setScreen(ChatUiState(prompt = "hello"))

        composeRule.onNodeWithTag(ChatTestTags.SEND_BUTTON).performClick()

        assertEquals(1, calls.sent)
    }

    @Test
    fun voiceButton_invokesOnVoiceInput() {
        val calls = setScreen(ChatUiState())

        composeRule.onNodeWithTag(ChatTestTags.VOICE_BUTTON).performClick()

        assertEquals(1, calls.voice)
    }

    @Test
    fun emptyPromptError_isShown() {
        setScreen(ChatUiState(promptError = PromptError.EMPTY))

        composeRule.onNodeWithText(string(R.string.field_cannot_be_empty)).assertIsDisplayed()
    }

    @Test
    fun failedRequest_showsErrorWithRetry() {
        val calls = setScreen(
            ChatUiState(
                messages = conversation.take(1),
                errorMessage = "Network down",
                errorIsRetryable = true,
            ),
        )

        composeRule.onNodeWithText("Network down").assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.retry)).performClick()
        composeRule.waitForIdle()

        assertEquals(1, calls.errorShown)
        assertEquals(1, calls.retried)
    }

    @Test
    fun nonRetryableError_hasNoRetryAction() {
        setScreen(ChatUiState(errorMessage = "No speech service", errorIsRetryable = false))

        composeRule.onNodeWithText("No speech service").assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.retry)).assertDoesNotExist()
    }

    @Test
    fun menu_clearConversation_invokesCallback() {
        val calls = setScreen(ChatUiState(messages = conversation))

        composeRule.onNodeWithTag(ChatTestTags.MENU_BUTTON).performClick()
        composeRule.onNodeWithText(string(R.string.clear_conversation)).performClick()

        assertEquals(1, calls.cleared)
    }

    @Test
    fun menu_clearConversation_isDisabledWhenEmpty() {
        setScreen(ChatUiState())

        composeRule.onNodeWithTag(ChatTestTags.MENU_BUTTON).performClick()

        composeRule.onNodeWithText(string(R.string.clear_conversation)).assertIsNotEnabled()
    }

    @Test
    fun menu_selectingDarkTheme_invokesCallback() {
        val calls = setScreen(ChatUiState())

        composeRule.onNodeWithTag(ChatTestTags.MENU_BUTTON).performClick()
        composeRule.onNodeWithText(string(R.string.theme_dark)).performClick()

        assertTrue(calls.theme == ThemeMode.DARK)
    }
}
