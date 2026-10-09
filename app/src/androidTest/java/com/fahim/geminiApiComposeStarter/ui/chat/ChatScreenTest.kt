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
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onAllNodesWithTag
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
    val rule = createComposeRule()

    private fun show(
        state: ChatUiState,
        width: WindowWidthSizeClass = WindowWidthSizeClass.Compact,
        onSend: () -> Unit = {},
        onRetry: () -> Unit = {},
        onSuggestionClick: (String) -> Unit = {},
    ) {
        rule.setContent {
            GeminiTheme(dynamicColor = false) {
                ChatScreen(
                    uiState = state, widthSizeClass = width,
                    onInputChange = {}, onSend = onSend, onSuggestionClick = onSuggestionClick,
                    onVoiceResult = {}, onVoiceUnavailable = {}, onRetry = onRetry,
                    onDismissError = {}, onClearHistory = {}, onSettingsOpenChange = {},
                    onThemeModeChange = {}, onDynamicColorChange = {},
                )
            }
        }
    }

    @Test
    fun noMessages_showsWelcomeWithSuggestions() {
        show(ChatUiState())
        rule.onNodeWithTag(ChatTags.EMPTY_STATE).assertIsDisplayed()
        rule.onAllNodesWithTag(ChatTags.SUGGESTION).onFirst().assertIsDisplayed()
    }

    @Test
    fun tappingSuggestion_reportsItsText() {
        var picked: String? = null
        show(ChatUiState(), onSuggestionClick = { picked = it })
        rule.onAllNodesWithTag(ChatTags.SUGGESTION).onFirst().performClick()
        assertTrue(picked!!.isNotBlank())
    }

    @Test
    fun conversation_isShown() {
        show(
            ChatUiState(
                messages = listOf(
                    ChatMessage(1, "Hello Gemini", true, 1),
                    ChatMessage(2, "Hi! What can I do for you?", false, 2),
                )
            )
        )
        rule.onNodeWithText("Hello Gemini").assertIsDisplayed()
        rule.onNodeWithText("Hi! What can I do for you?").assertIsDisplayed()
    }

    @Test
    fun emptyInput_disablesSend() {
        show(ChatUiState())
        rule.onNodeWithTag(ChatTags.SEND).assertIsNotEnabled()
    }

    @Test
    fun typedText_enablesSend_andClickSends() {
        var sends = 0
        rule.setContent {
            var state by remember { mutableStateOf(ChatUiState()) }
            GeminiTheme(dynamicColor = false) {
                ChatScreen(
                    uiState = state, widthSizeClass = WindowWidthSizeClass.Compact,
                    onInputChange = { state = state.copy(input = it) }, onSend = { sends++ },
                    onSuggestionClick = {}, onVoiceResult = {}, onVoiceUnavailable = {},
                    onRetry = {}, onDismissError = {}, onClearHistory = {},
                    onSettingsOpenChange = {}, onThemeModeChange = {}, onDynamicColorChange = {},
                )
            }
        }
        rule.onNodeWithTag(ChatTags.INPUT).performTextInput("Tell me a joke")
        rule.onNodeWithTag(ChatTags.SEND).assertIsEnabled().performClick()
        assertEquals(1, sends)
    }

    @Test
    fun waitingForReply_showsProgress() {
        show(ChatUiState(messages = listOf(ChatMessage(1, "Q", true, 1)), isLoading = true))
        rule.onNodeWithTag(ChatTags.LOADING).assertIsDisplayed()
    }

    @Test
    fun failure_showsBanner_andRetryCallsBack() {
        var retried = false
        show(ChatUiState(errorMessage = "No connection", failedPrompt = "Q"), onRetry = { retried = true })
        rule.onNodeWithTag(ChatTags.ERROR).assertIsDisplayed()
        rule.onNodeWithTag(ChatTags.RETRY).performClick()
        assertTrue(retried)
    }

    @Test
    fun wideScreen_showsSidePanel() {
        show(ChatUiState(), width = WindowWidthSizeClass.Expanded)
        rule.onNodeWithTag(ChatTags.SETTINGS_PANE).assertIsDisplayed()
    }
}
