package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChatScreenTest {

    // Assignment Step 6 Requirement: Use createComposeRule() for Compose UI testing
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun chatScreen_displaysConversationBubbles() {
        val testMessages = listOf(
            ChatMessage(id = 1L, text = "Hello from User", participant = Participant.USER),
            ChatMessage(id = 2L, text = "Hello from Gemini", participant = Participant.MODEL)
        )

        val state = ChatUiState(
            prompt = "",
            messages = testMessages,
            isLoading = false
        )

        composeTestRule.setContent {
            ChatScreen(
                state = state,
                onPromptChange = {},
                onSend = {}
            )
        }

        // Verify both chat bubbles are rendered on the screen
        composeTestRule.onNodeWithText("Hello from User").assertIsDisplayed()
        composeTestRule.onNodeWithText("Hello from Gemini").assertIsDisplayed()
    }
}