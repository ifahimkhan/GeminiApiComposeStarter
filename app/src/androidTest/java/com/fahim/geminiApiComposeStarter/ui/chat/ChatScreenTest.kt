package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChatScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun typeAndSendMessage_displaysInChatList() {
        var state by mutableStateOf(ChatUiState())

        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = state,
                    onPromptChange = { newPrompt ->
                        state = state.copy(prompt = newPrompt)
                    },
                    onSend = {
                        val userMsg = ChatMessage(text = state.prompt, isUser = true)
                        state = state.copy(
                            prompt = "",
                            messages = state.messages + userMsg
                        )
                    }
                )
            }
        }

        composeTestRule
            .onNodeWithText("Enter your prompt here")
            .performTextInput("Hello World")

        composeTestRule
            .onNodeWithContentDescription("Send")
            .performClick()

        composeTestRule
            .onNodeWithText("Hello World")
            .assertIsDisplayed()
    }
}
