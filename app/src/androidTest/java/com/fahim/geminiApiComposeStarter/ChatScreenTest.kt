package com.fahim.geminiApiComposeStarter

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.fahim.geminiApiComposeStarter.ui.chat.ChatScreen
import com.fahim.geminiApiComposeStarter.ui.chat.ChatUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ChatScreenTest {
    @get:Rule val composeRule = createComposeRule()

    @Test fun inputAndSendAreAvailable() {
        var prompt = ""
        var sends = 0
        composeRule.setContent {
            MaterialTheme { ChatScreen(ChatUiState(), { prompt = it }, { sends++ }) }
        }
        composeRule.onNodeWithContentDescription("Message input").performTextInput("Hello")
        composeRule.onNodeWithContentDescription("Send message").performClick()
        assertEquals("Hello", prompt)
        assertEquals(1, sends)
        composeRule.onNodeWithText("Start a conversation with Gemini")
    }
}
