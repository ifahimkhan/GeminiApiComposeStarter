package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.fahim.geminiApiComposeStarter.ui.chat.ChatScreen
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
class ChatScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun userAndGeminiMessages_areDisplayed() {

        val testState = ChatUiState(
            messages = listOf(
                ChatMessage(
                    id = 1L,
                    text = "Hello Gemini",
                    isUser = true
                ),
                ChatMessage(
                    id = 2L,
                    text = "Hello user",
                    isUser = false
                )
            )
        )

        composeRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = testState,
                    windowSizeClass = WindowSizeClass.calculateFromSize(
                        DpSize(
                            width = 360.dp,
                            height = 800.dp
                        )
                    ),
                    onPromptChange = {},
                    onSend = {}
                )
            }
        }

        composeRule
            .onNodeWithText("Hello Gemini")
            .assertIsDisplayed()

        composeRule
            .onNodeWithText("Hello user")
            .assertIsDisplayed()
    }
}