package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import com.fahim.geminiApiComposeStarter.R
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ChatScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun show(
        state: ChatUiState = ChatUiState(),
        onPromptChange: (String) -> Unit = {},
        onSend: () -> Unit = {},
    ) {
        composeRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(state = state, onPromptChange = onPromptChange, onSend = onSend)
            }
        }
    }

    @Test
    fun emptyChat_showsPlaceholder() {
        show()
        composeRule.onNodeWithText(context.getString(R.string.response_placeholder))
            .assertExists()
    }

    @Test
    fun messages_areDisplayed() {
        show(
            ChatUiState(
                messages = listOf(
                    ChatMessage(1, "Hi Gemini!", isUser = true),
                    ChatMessage(2, "Hello there", isUser = false),
                ),
            ),
        )
        composeRule.onNodeWithText("Hi Gemini!").assertExists()
        composeRule.onNodeWithText("Hello there").assertExists()
    }

    @Test
    fun sendButton_invokesOnSend() {
        var sendCount = 0
        show(onSend = { sendCount++ })

        composeRule.onNodeWithContentDescription(context.getString(R.string.send))
            .performClick()

        assertEquals(1, sendCount)
    }

    @Test
    fun typingInPromptField_updatesText() {
        composeRule.setContent {
            var prompt by remember { mutableStateOf("") }
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(prompt = prompt),
                    onPromptChange = { prompt = it },
                    onSend = {},
                )
            }
        }

        composeRule.onNode(hasSetTextAction()).performTextInput("Hello")

        composeRule.onNodeWithText("Hello").assertExists()
    }

    @Test
    fun emptyPromptError_isShown() {
        show(ChatUiState(promptError = PromptError.EMPTY))
        composeRule.onNodeWithText(context.getString(R.string.field_cannot_be_empty))
            .assertExists()
    }

    @Test
    fun whileLoading_sendButtonIsDisabled() {
        show(ChatUiState(isLoading = true))
        composeRule.onNodeWithContentDescription(context.getString(R.string.send))
            .assertIsNotEnabled()
    }

    @Test
    fun whenIdle_sendButtonIsEnabled() {
        show(ChatUiState(isLoading = false))
        composeRule.onNodeWithContentDescription(context.getString(R.string.send))
            .assertIsEnabled()
    }
}