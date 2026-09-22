package com.fahim.geminiApiComposeStarter

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.fahim.geminiApiComposeStarter.data.db.ChatMessage
import com.fahim.geminiApiComposeStarter.data.db.MessageRole
import com.fahim.geminiApiComposeStarter.ui.chat.ChatScreen
import com.fahim.geminiApiComposeStarter.ui.chat.ChatUiState
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme
import org.junit.Rule
import org.junit.Test

/**
 * Compose UI tests for [ChatScreen] using [createComposeRule].
 * These tests verify the rendered UI without a real ViewModel or network.
 */
class ChatScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    // ── Helpers ────────────────────────────────────────────────

    private fun setScreen(state: ChatUiState) {
        composeRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = state,
                    onPromptChange = {},
                    onSend = {},
                    onVoiceResult = {},
                    onListeningChange = {},
                    onClearHistory = {},
                    onErrorDismissed = {},
                )
            }
        }
    }

    // ── Tests ──────────────────────────────────────────────────

    @Test
    fun emptyState_showsHintText() {
        setScreen(ChatUiState())
        composeRule.onNodeWithText("Ask Gemini anything!").assertIsDisplayed()
    }

    @Test
    fun messages_areDisplayedInBubbles() {
        setScreen(
            ChatUiState(
                messages = listOf(
                    ChatMessage(id = 1, role = MessageRole.USER, text = "Hello!"),
                    ChatMessage(id = 2, role = MessageRole.GEMINI, text = "Hi there, how can I help?"),
                )
            )
        )
        composeRule.onNodeWithText("Hello!").assertIsDisplayed()
        composeRule.onNodeWithText("Hi there, how can I help?").assertIsDisplayed()
    }

    @Test
    fun loadingState_sendButtonShowsSpinner() {
        setScreen(ChatUiState(isLoading = true))
        // Send button should be disabled during loading
        composeRule.onNodeWithContentDescription("Send").assertDoesNotExist()
    }

    @Test
    fun sendButton_disabledWhenPromptBlank() {
        setScreen(ChatUiState(prompt = ""))
        // The send FilledIconButton should be disabled
        composeRule.onNodeWithContentDescription("Send").assertIsNotEnabled()
    }

    @Test
    fun sendButton_enabledWhenPromptNotBlank() {
        setScreen(ChatUiState(prompt = "Hello"))
        composeRule.onNodeWithContentDescription("Send").assertIsEnabled()
    }

    @Test
    fun topBar_showsUserNameWhenSet() {
        setScreen(ChatUiState(userName = "Abhay"))
        composeRule.onNodeWithText("Hi, Abhay 👋").assertIsDisplayed()
    }

    @Test
    fun topBar_showsDefaultTitleWhenNoUserName() {
        setScreen(ChatUiState(userName = ""))
        composeRule.onNodeWithText("Gemini Chat").assertIsDisplayed()
    }

    @Test
    fun micButton_isDisplayed() {
        setScreen(ChatUiState())
        composeRule.onNodeWithContentDescription("Voice input").assertIsDisplayed()
    }
}
