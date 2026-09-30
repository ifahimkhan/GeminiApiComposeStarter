package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.fahim.geminiApiComposeStarter.data.ChatMessage
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ChatScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun setScreen(
        state: ChatUiState,
        widthSizeClass: WindowWidthSizeClass = WindowWidthSizeClass.Compact,
        onSend: () -> Unit = {},
        onRetry: () -> Unit = {},
    ) {
        composeRule.setContent {
            GeminiTheme(dynamicColor = false) {
                ChatScreen(
                    uiState = state,
                    widthSizeClass = widthSizeClass,
                    onInputChange = {}, onSend = onSend, onVoiceResult = {}, onVoiceUnavailable = {},
                    onRetry = onRetry, onDismissError = {}, onClearHistory = {},
                    onSettingsOpenChange = {}, onThemeModeChange = {}, onDynamicColorChange = {},
                )
            }
        }
    }

    @Test
    fun emptyConversation_showsEmptyState() {
        setScreen(ChatUiState())
        composeRule.onNodeWithTag(ChatTestTags.EMPTY_STATE).assertIsDisplayed()
    }

    @Test
    fun messages_areRendered() {
        setScreen(
            ChatUiState(
                messages = listOf(
                    ChatMessage(1, "Hello there", true, 1),
                    ChatMessage(2, "Hi! How can I help?", false, 2),
                )
            )
        )
        composeRule.onNodeWithText("Hello there").assertIsDisplayed()
        composeRule.onNodeWithText("Hi! How can I help?").assertIsDisplayed()
    }

    @Test
    fun sendButton_disabledWhenInputEmpty() {
        setScreen(ChatUiState(input = ""))
        composeRule.onNodeWithTag(ChatTestTags.SEND).assertIsNotEnabled()
    }

    @Test
    fun typingThenSend_invokesCallback() {
        var sent = 0
        composeRule.setContent {
            var state by remember { mutableStateOf(ChatUiState()) }
            GeminiTheme(dynamicColor = false) {
                ChatScreen(
                    uiState = state,
                    widthSizeClass = WindowWidthSizeClass.Compact,
                    onInputChange = { state = state.copy(input = it) },
                    onSend = { sent++ },
                    onVoiceResult = {}, onVoiceUnavailable = {}, onRetry = {}, onDismissError = {},
                    onClearHistory = {}, onSettingsOpenChange = {}, onThemeModeChange = {},
                    onDynamicColorChange = {},
                )
            }
        }
        composeRule.onNodeWithTag(ChatTestTags.INPUT).performTextInput("What is Compose?")
        composeRule.onNodeWithTag(ChatTestTags.SEND).assertIsEnabled().performClick()
        assertEquals(1, sent)
    }

    @Test
    fun loading_showsProgressIndicator() {
        setScreen(ChatUiState(messages = listOf(ChatMessage(1, "Q", true, 1)), isLoading = true))
        composeRule.onNodeWithTag(ChatTestTags.LOADING).assertIsDisplayed()
        composeRule.onNodeWithTag(ChatTestTags.SEND).assertIsNotEnabled()
    }

    @Test
    fun error_showsCardAndRetryWorks() {
        var retried = false
        setScreen(
            ChatUiState(errorMessage = "Network error", failedPrompt = "Q"),
            onRetry = { retried = true },
        )
        composeRule.onNodeWithTag(ChatTestTags.ERROR).assertIsDisplayed()
        composeRule.onNodeWithTag(ChatTestTags.RETRY).performClick()
        assertTrue(retried)
    }

    @Test
    fun expandedWidth_showsPermanentSettingsPane() {
        setScreen(ChatUiState(), widthSizeClass = WindowWidthSizeClass.Expanded)
        composeRule.onNodeWithTag(ChatTestTags.SETTINGS_PANE).assertIsDisplayed()
    }
}
