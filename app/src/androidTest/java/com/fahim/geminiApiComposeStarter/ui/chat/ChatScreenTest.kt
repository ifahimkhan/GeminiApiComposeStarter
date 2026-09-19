package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.fahim.geminiApiComposeStarter.data.ChatMessage
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ChatScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun setScreen(
        state: ChatUiState = ChatUiState(),
        onPromptChange: (String) -> Unit = {},
        onSend: () -> Unit = {},
        onMicClick: () -> Unit = {},
    ) {
        composeRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = state,
                    widthSizeClass = WindowWidthSizeClass.Compact,
                    onPromptChange = onPromptChange,
                    onSend = onSend,
                    onMicClick = onMicClick,
                    onErrorShown = {},
                    onClearChat = {},
                    onThemeModeChange = {},
                    onDynamicColorChange = {},
                )
            }
        }
    }

    @Test
    fun emptyConversation_showsPlaceholder() {
        setScreen()
        composeRule.onNodeWithText("Say hi to Gemini!", substring = true).assertIsDisplayed()
    }

    @Test
    fun messages_areRenderedInBubbles() {
        setScreen(
            ChatUiState(
                messages = listOf(
                    ChatMessage(1, "Hello Gemini", isUser = true),
                    ChatMessage(2, "Hi there!", isUser = false),
                ),
            ),
        )
        composeRule.onNodeWithText("Hello Gemini").assertIsDisplayed()
        composeRule.onNodeWithText("Hi there!").assertIsDisplayed()
    }

    @Test
    fun loadingState_showsProgressIndicator_andDisablesSend() {
        setScreen(ChatUiState(isLoading = true))
        composeRule.onNodeWithTag(ChatTestTags.LOADING).assertIsDisplayed()
        composeRule.onNodeWithTag(ChatTestTags.SEND_BUTTON).assertIsNotEnabled()
    }

    @Test
    fun idleState_hasNoProgressIndicator_andSendIsEnabled() {
        setScreen()
        composeRule.onNodeWithTag(ChatTestTags.SEND_BUTTON).assertIsEnabled()
        composeRule.onNodeWithTag(ChatTestTags.LOADING).assertDoesNotExist()
    }

    @Test
    fun errorMessage_isShownInSnackbar() {
        setScreen(ChatUiState(errorMessage = "Network down"))
        composeRule.onNodeWithText("Network down").assertIsDisplayed()
    }

    @Test
    fun emptyPromptError_isShownUnderTheField() {
        setScreen(ChatUiState(promptError = PromptError.EMPTY))
        composeRule.onNodeWithText("Field cannot be empty").assertIsDisplayed()
    }

    @Test
    fun typingInField_reportsTextChange() {
        var received = ""
        setScreen(onPromptChange = { received = it })
        composeRule.onNodeWithTag(ChatTestTags.PROMPT_FIELD).performTextInput("hello")
        assertEquals("hello", received)
    }

    @Test
    fun tappingSend_and_tappingMic_invokeCallbacks() {
        var sent = false
        var micTapped = false
        setScreen(onSend = { sent = true }, onMicClick = { micTapped = true })

        composeRule.onNodeWithTag(ChatTestTags.SEND_BUTTON).performClick()
        composeRule.onNodeWithTag(ChatTestTags.MIC_BUTTON).performClick()

        assertTrue(sent)
        assertTrue(micTapped)
    }
}
