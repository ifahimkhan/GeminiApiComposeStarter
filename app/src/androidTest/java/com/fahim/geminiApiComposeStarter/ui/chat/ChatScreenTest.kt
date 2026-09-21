package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class ChatScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun chatScreen_displaysTitleAndPromptField() {

        composeTestRule.setContent {

            ChatScreen(
                state = ChatUiState(),
                darkMode = false,
                onDarkModeChange = {},
                onPromptChange = {},
                onSend = {}
            )
        }

        composeTestRule
            .onNodeWithText("Gemini Chat")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Enter your prompt")
            .assertIsDisplayed()
    }
}