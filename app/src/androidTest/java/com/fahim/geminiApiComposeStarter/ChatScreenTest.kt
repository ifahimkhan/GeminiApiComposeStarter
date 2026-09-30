package com.fahim.geminiApiComposeStarter

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.fahim.geminiApiComposeStarter.ui.chat.ChatMessage
import com.fahim.geminiApiComposeStarter.ui.chat.ChatScreen
import com.fahim.geminiApiComposeStarter.ui.chat.ChatUiState
import com.fahim.geminiApiComposeStarter.ui.chat.PromptError
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChatScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    // Variables the fake callbacks write to, so the tests can check them
    private var sendClicked = false
    private var typedText = ""

    private fun showScreen(state: ChatUiState) {
        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = state,
                    isWideScreen = false,
                    isDarkMode = false,
                    onDarkModeChange = {},
                    onClearChat = {},
                    onPromptChange = { typedText = it },
                    onSend = { sendClicked = true },
                    onErrorShown = {},
                    onMicClick = {},
                )
            }
        }
    }

    @Test
    fun emptyChat_showsTitleAndPlaceholder() {
        showScreen(ChatUiState())

        composeTestRule.onNodeWithText("Chat by Manvi N146").assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.response_placeholder))
            .assertIsDisplayed()
    }

    @Test
    fun messages_areShownAsBubbles() {
        showScreen(
            ChatUiState(
                messages = listOf(
                    ChatMessage(id = 1, text = "Hi Gemini", isUser = true),
                    ChatMessage(id = 2, text = "Hello Manvi", isUser = false),
                ),
            ),
        )

        composeTestRule.onNodeWithText("Hi Gemini").assertIsDisplayed()
        composeTestRule.onNodeWithText("Hello Manvi").assertIsDisplayed()
    }

    @Test
    fun typingText_callsOnPromptChange() {
        showScreen(ChatUiState())

        composeTestRule.onNode(hasSetTextAction()).performTextInput("What is Compose?")

        assertEquals("What is Compose?", typedText)
    }

    @Test
    fun clickingSend_callsOnSend() {
        showScreen(ChatUiState(prompt = "Hello"))

        composeTestRule.onNodeWithContentDescription(context.getString(R.string.send))
            .performClick()

        assertTrue(sendClicked)
    }

    @Test
    fun emptyPromptError_isShown() {
        showScreen(ChatUiState(promptError = PromptError.EMPTY))

        composeTestRule.onNodeWithText(context.getString(R.string.field_cannot_be_empty))
            .assertIsDisplayed()
    }
}