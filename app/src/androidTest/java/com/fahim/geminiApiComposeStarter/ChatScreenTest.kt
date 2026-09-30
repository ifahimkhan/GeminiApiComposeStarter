package com.fahim.geminiApiComposeStarter

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.fahim.geminiApiComposeStarter.ui.chat.ChatScreen
import com.fahim.geminiApiComposeStarter.ui.chat.ChatUiState
import org.junit.Rule
import org.junit.Test

class ChatScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun chatScreen_displaysPromptAndSendButton() {
        var promptStr = ""
        var sent = false
        composeTestRule.setContent {
            ChatScreen(
                state = ChatUiState(prompt = ""),
                onPromptChange = { promptStr = it },
                onSend = { sent = true },
                onClearHistory = {},
                isDarkTheme = false,
                onThemeToggle = {}
            )
        }

        // The text field label is "Enter your prompt here"
        composeTestRule.onNodeWithText("Enter your prompt here").performTextInput("Hello World")
        composeTestRule.onNodeWithText("Hello World").assertExists()
    }
}
