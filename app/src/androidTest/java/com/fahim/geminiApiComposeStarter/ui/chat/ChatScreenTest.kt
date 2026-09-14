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
import com.fahim.geminiApiComposeStarter.domain.ChatMessage
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
class ChatScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val compactWindow = WindowSizeClass.calculateFromSize(DpSize(360.dp, 780.dp))

    @Test
    fun emptyState_showsPlaceholder() {
        composeTestRule.setContent {
            ChatScreen(
                state = ChatUiState(),
                windowSizeClass = compactWindow,
                onPromptChange = {},
                onSend = {},
                onVoiceResult = {},
                onErrorShown = {},
            )
        }

        composeTestRule.onNodeWithText("Response will be displayed here!").assertExists()
    }

    @Test
    fun messages_areRenderedAsBubbles() {
        composeTestRule.setContent {
            ChatScreen(
                state = ChatUiState(
                    messages = listOf(
                        ChatMessage(id = 1, text = "Hello there", isFromUser = true),
                        ChatMessage(id = 2, text = "General Kenobi", isFromUser = false),
                    ),
                ),
                windowSizeClass = compactWindow,
                onPromptChange = {},
                onSend = {},
                onVoiceResult = {},
                onErrorShown = {},
            )
        }

        composeTestRule.onNodeWithText("Hello there").assertExists()
        composeTestRule.onNodeWithText("General Kenobi").assertExists()
    }

    @Test
    fun emptyPromptError_showsSupportingText() {
        composeTestRule.setContent {
            ChatScreen(
                state = ChatUiState(promptError = PromptError.EMPTY),
                windowSizeClass = compactWindow,
                onPromptChange = {},
                onSend = {},
                onVoiceResult = {},
                onErrorShown = {},
            )
        }

        composeTestRule.onNodeWithText("Field cannot be empty").assertExists()
    }

    @Test
    fun typingAndSend_invokesCallbacks() {
        var promptValue = ""
        var sendClicked = false

        composeTestRule.setContent {
            ChatScreen(
                state = ChatUiState(prompt = promptValue),
                windowSizeClass = compactWindow,
                onPromptChange = { promptValue = it },
                onSend = { sendClicked = true },
                onVoiceResult = {},
                onErrorShown = {},
            )
        }

        composeTestRule.onNodeWithText("Enter your prompt here").performTextInput("hi")
        composeTestRule.onNodeWithContentDescription("Send").performClick()

        assert(sendClicked)
    }
}
