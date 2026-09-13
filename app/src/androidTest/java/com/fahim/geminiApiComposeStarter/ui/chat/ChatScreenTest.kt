package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.fahim.geminiApiComposeStarter.data.ChatMessage
import com.fahim.geminiApiComposeStarter.data.ChatSender
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ChatScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun chatScreen_rendersMessagesAndInteracts() {
        var sendClicked = false
        var lastPrompt = ""

        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(
                        prompt = "Hello AI",
                        messages = listOf(
                            ChatMessage(id = 1, sender = ChatSender.USER, content = "User Question"),
                            ChatMessage(id = 2, sender = ChatSender.ASSISTANT, content = "AI Answer"),
                        ),
                    ),
                    onPromptChange = { lastPrompt = it },
                    onSpeechResult = {},
                    onSend = { sendClicked = true },
                    onClearHistory = {},
                )
            }
        }

        // Verify message bubbles are displayed
        composeTestRule.onNodeWithText("User Question").assertIsDisplayed()
        composeTestRule.onNodeWithText("AI Answer").assertIsDisplayed()

        // Verify send interaction
        composeTestRule.onNodeWithText("Send").performClick()
        assertEquals(true, sendClicked)
    }
}
