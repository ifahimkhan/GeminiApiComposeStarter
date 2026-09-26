package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import com.fahim.geminiApiComposeStarter.R
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme
import org.junit.Rule
import org.junit.Test

class ChatScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun string(resId: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(resId)

    private fun setContent(
        state: ChatUiState = ChatUiState(),
        onSend: () -> Unit = {},
        onPromptChange: (String) -> Unit = {},
    ) {
        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = state,
                    darkModeOverride = null,
                    onToggleDarkMode = {},
                    onPromptChange = onPromptChange,
                    onSend = onSend,
                    onVoiceInputUnavailable = {},
                )
            }
        }
    }

    @Test
    fun emptyConversation_showsPlaceholder() {
        setContent()

        composeTestRule.onNodeWithText(string(R.string.response_placeholder)).assertIsDisplayed()
    }

    @Test
    fun messages_areRenderedInTheConversation() {
        setContent(
            state = ChatUiState(
                messages = listOf(
                    ChatMessage(id = 0, text = "What is Jetpack Compose?", isFromUser = true),
                    ChatMessage(id = 1, text = "A modern Android UI toolkit.", isFromUser = false),
                ),
            ),
        )

        composeTestRule.onNodeWithText("What is Jetpack Compose?").assertIsDisplayed()
        composeTestRule.onNodeWithText("A modern Android UI toolkit.").assertIsDisplayed()
    }

    @Test
    fun promptError_showsFieldCannotBeEmptyMessage() {
        setContent(state = ChatUiState(promptError = PromptError.EMPTY))

        composeTestRule.onNodeWithText(string(R.string.field_cannot_be_empty)).assertIsDisplayed()
    }

    @Test
    fun typingAndSending_invokesCallbacksWithTheTypedText() {
        var sendClicked = false
        var typedValue = ""

        setContent(
            onPromptChange = { typedValue = it },
            onSend = { sendClicked = true },
        )

        composeTestRule.onNodeWithText(string(R.string.enter_your_prompt_here)).performTextInput("Hello Gemini")
        composeTestRule.onNodeWithContentDescription(string(R.string.send)).performClick()

        assert(typedValue == "Hello Gemini")
        assert(sendClicked)
    }
}
