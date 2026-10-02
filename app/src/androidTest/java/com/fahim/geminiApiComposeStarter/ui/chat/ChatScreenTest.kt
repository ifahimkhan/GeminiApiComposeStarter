package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fahim.geminiApiComposeStarter.data.model.ChatMessage
import com.fahim.geminiApiComposeStarter.data.model.Role
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChatScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var typed = ""
    private var sendClicks = 0
    private var micClicks = 0

    private fun setScreen(state: ChatUiState, withMic: Boolean = true) {
        composeRule.setContent {
            GeminiApiComposeStarterTheme(dynamicColor = false) {
                ChatScreen(
                    state = state,
                    widthSizeClass = WindowWidthSizeClass.Compact,
                    heightSizeClass = WindowHeightSizeClass.Medium,
                    onPromptChange = { typed = it },
                    onSend = { sendClicks++ },
                    onMicClick = if (withMic) ({ micClicks++ }) else null,
                    onErrorShown = {},
                    onClearHistory = {},
                    onThemeModeChange = {},
                    onDynamicColorChange = {},
                )
            }
        }
    }

    @Test
    fun emptyState_showsPlaceholder() {
        setScreen(ChatUiState())

        composeRule.onNodeWithText("Ask Gemini anything", substring = true).assertIsDisplayed()
    }

    @Test
    fun messages_renderAsBubbles_withBoldMarkersStripped() {
        setScreen(
            ChatUiState(
                messages = listOf(
                    ChatMessage(1, Role.USER, "Hello", 1),
                    ChatMessage(2, Role.MODEL, "**Hi** there", 2),
                ),
            ),
        )

        composeRule.onNodeWithText("Hello").assertIsDisplayed()
        composeRule.onNodeWithText("Hi there").assertIsDisplayed()
    }

    @Test
    fun typingAndSending_reportCallbacks() {
        setScreen(ChatUiState())

        composeRule.onNodeWithTag("prompt_input").performTextInput("Hi")
        composeRule.onNodeWithTag("send_button").performClick()

        assertEquals("Hi", typed)
        assertEquals(1, sendClicks)
    }

    @Test
    fun loading_showsIndicator_andDisablesInput() {
        setScreen(ChatUiState(messages = listOf(ChatMessage(1, Role.USER, "Hi", 1)), isLoading = true))

        composeRule.onNodeWithTag("loading_indicator").assertIsDisplayed()
        composeRule.onNodeWithTag("send_button").assertIsNotEnabled()
        composeRule.onNodeWithTag("prompt_input").assertIsNotEnabled()
    }

    @Test
    fun promptError_isShown() {
        setScreen(ChatUiState(promptError = PromptError.EMPTY))

        composeRule.onNodeWithText("Field cannot be empty").assertIsDisplayed()
    }

    @Test
    fun errorMessage_isShownInSnackbar() {
        setScreen(ChatUiState(errorMessage = "Quota exceeded."))

        composeRule.onNodeWithText("Quota exceeded.").assertIsDisplayed()
    }

    @Test
    fun micButton_reportsClick() {
        setScreen(ChatUiState())

        composeRule.onNodeWithTag("mic_button").performClick()

        assertEquals(1, micClicks)
    }

    @Test
    fun micButton_hiddenWhenNoRecogniserInstalled() {
        setScreen(ChatUiState(), withMic = false)

        composeRule.onNodeWithTag("mic_button").assertDoesNotExist()
    }
}
