package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test

class ChatScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val phoneWindowSizeClass =
        WindowSizeClass.calculateFromSize(
            DpSize(400.dp, 800.dp)
        )

    @Test
    fun chatScreen_displaysMessages() {

        composeTestRule.setContent {

            ChatScreen(
                state = ChatUiState(
                    messages = listOf(
                        ChatMessage(
                            id = 1,
                            text = "Hello",
                            isUser = true,
                        ),
                        ChatMessage(
                            id = 2,
                            text = "Hi! How can I help?",
                            isUser = false,
                        ),
                    )
                ),
                windowSizeClass = phoneWindowSizeClass,
                onPromptChange = {},
                onSend = {},
            )
        }

        composeTestRule
            .onNodeWithText("Hello")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Hi! How can I help?")
            .assertIsDisplayed()
    }

    @Test
    fun chatScreen_displaysLoadingIndicator() {

        composeTestRule.setContent {

            ChatScreen(
                state = ChatUiState(
                    isLoading = true
                ),
                windowSizeClass = phoneWindowSizeClass,
                onPromptChange = {},
                onSend = {},
            )
        }

        composeTestRule
            .onNodeWithText("Message Gemini")
            .assertIsDisplayed()
    }

    @Test
    fun chatScreen_allowsEnteringMessage() {

        composeTestRule.setContent {

            ChatScreen(
                state = ChatUiState(),
                windowSizeClass = phoneWindowSizeClass,
                onPromptChange = {},
                onSend = {},
            )
        }

        composeTestRule
            .onNodeWithText("Message Gemini")
            .assertIsDisplayed()
    }
}