package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.fahim.geminiApiComposeStarter.data.ThemeMode
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
class ChatScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val phoneWindowSizeClass = WindowSizeClass.calculateFromSize(DpSize(360.dp, 800.dp))

    @Test
    fun sentMessagesAreRenderedAsBubbles() {
        composeTestRule.setContent {
            ChatScreen(
                state = ChatUiState(
                    messages = listOf(
                        ChatMessage(1, ChatRole.USER, "Hello Gemini", 0L),
                        ChatMessage(2, ChatRole.MODEL, "Hi! How can I help?", 1L),
                    ),
                ),
                windowSizeClass = phoneWindowSizeClass,
                speechAvailable = false,
                themeMode = ThemeMode.SYSTEM,
                onThemeModeChange = {},
                onPromptChange = {},
                onSend = {},
                onMicClick = {},
            )
        }

        composeTestRule.onNodeWithText("Hello Gemini").assertExists()
        composeTestRule.onNodeWithText("Hi! How can I help?").assertExists()
    }

    @Test
    fun typingAndTappingSendInvokesCallbacks() {
        var typed = ""
        var sendClicked = false

        composeTestRule.setContent {
            ChatScreen(
                state = ChatUiState(prompt = typed),
                windowSizeClass = phoneWindowSizeClass,
                speechAvailable = false,
                themeMode = ThemeMode.SYSTEM,
                onThemeModeChange = {},
                onPromptChange = { typed = it },
                onSend = { sendClicked = true },
                onMicClick = {},
            )
        }

        composeTestRule.onNodeWithText("Enter your prompt here").performTextInput("Hi")
        composeTestRule.onNodeWithContentDescription("Send").performClick()

        assert(sendClicked)
    }
}
