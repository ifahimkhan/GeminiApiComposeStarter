package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ChatScreenTest {

    @get:Rule val composeRule = createComposeRule()

    private fun setScreen(
        state: ChatUiState,
        onPromptChange: (String) -> Unit = {},
        onSend: () -> Unit = {},
    ) {
        composeRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = state,
                    widthSizeClass = WindowWidthSizeClass.Compact,
                    onPromptChange = onPromptChange,
                    onSend = onSend,
                    onVoiceResult = {}, onVoiceUnavailable = {}, onErrorShown = {},
                    onClearChat = {}, onToggleTheme = {},
                )
            }
        }
    }

    @Test fun showsUserAndGeminiMessages() {
        setScreen(
            ChatUiState(
                messages = listOf(
                    ChatMessage(1, "Hello Gemini", isUser = true),
                    ChatMessage(2, "Hello human", isUser = false),
                )
            )
        )
        composeRule.onNodeWithText("Hello Gemini").assertIsDisplayed()
        composeRule.onNodeWithText("Hello human").assertIsDisplayed()
    }

    @Test fun typingAndSend_callsCallbacks() {
        var typed = ""
        var sent = false
        setScreen(ChatUiState(), onPromptChange = { typed = it }, onSend = { sent = true })

        composeRule.onNodeWithTag(ChatTestTags.PROMPT_INPUT).performTextInput("Hi")
        composeRule.onNodeWithTag(ChatTestTags.SEND_BUTTON).performClick()

        assertEquals("Hi", typed)
        assertTrue(sent)
    }

    @Test fun loadingState_showsProgress_andDisablesSend() {
        setScreen(ChatUiState(messages = listOf(ChatMessage(1, "Q", true)), isLoading = true))
        composeRule.onNodeWithTag(ChatTestTags.LOADING).assertIsDisplayed()
        composeRule.onNodeWithTag(ChatTestTags.SEND_BUTTON).assertIsNotEnabled()
    }

    @Test fun emptyState_showsPlaceholder() {
        setScreen(ChatUiState())
        composeRule.onNodeWithText("Ask Gemini anything to start a conversation").assertIsDisplayed()
    }
}
