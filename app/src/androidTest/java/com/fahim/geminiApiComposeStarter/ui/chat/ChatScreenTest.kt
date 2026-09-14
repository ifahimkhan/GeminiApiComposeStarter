package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ChatScreenTest {
    @get:Rule val compose = createComposeRule()

    private fun show(state: ChatUiState = ChatUiState(), onPrompt: (String) -> Unit = {}, onSend: () -> Unit = {}) {
        compose.setContent {
            GeminiApiComposeStarterTheme(darkTheme = true) {
                ChatScreen(state, onPrompt, onSend, autoFocus = false)
            }
        }
    }

    @Test fun menuOpensDrawerAndCloseButtonDismissesIt() {
        show()
        compose.onNodeWithContentDescription("Chat history").performClick()
        compose.onNodeWithText("Recents").assertIsDisplayed()
        compose.onNodeWithContentDescription("Close chat history").performClick()
        compose.onNodeWithText("Recents").assertIsNotDisplayed()
    }

    @Test fun swipeRightOpensDrawerAndSwipeLeftClosesIt() {
        show()
        compose.onRoot().performTouchInput {
            swipe(Offset(width * 0.15f, height * 0.4f), Offset(width * 0.9f, height * 0.4f), 400)
        }
        compose.onNodeWithText("Recents").assertIsDisplayed()
        compose.onRoot().performTouchInput {
            swipe(Offset(width * 0.7f, height * 0.4f), Offset(width * 0.05f, height * 0.4f), 400)
        }
        compose.onNodeWithText("Recents").assertIsNotDisplayed()
    }

    @Test fun loadingShowsProgressAndDisablesSend() {
        show(ChatUiState(prompt = "Next", messages = listOf(ChatMessage(1, ChatRole.USER, "Hello")), isLoading = true))
        compose.onNodeWithText("Gemini is thinking").assertIsDisplayed()
        compose.onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)).assertIsDisplayed()
        compose.onNodeWithContentDescription("Send").assertIsNotEnabled()
    }

    @Test fun apiFailureIsVisible() {
        show(ChatUiState(messages = listOf(ChatMessage(1, ChatRole.USER, "Hello")), errorMessage = "Network unavailable"))
        compose.onNodeWithText("Network unavailable").assertIsDisplayed()
    }

    @Test fun suggestionPopulatesPromptAndSendInvokesCallback() {
        var selected = ""
        var sends = 0
        show(ChatUiState(prompt = "Hello"), onPrompt = { selected = it }, onSend = { sends++ })
        compose.onNodeWithText("Write a message").performClick()
        compose.runOnIdle { assertEquals("Help me write a thoughtful thank-you message.", selected) }
        compose.onNodeWithContentDescription("Send").performClick()
        compose.runOnIdle { assertEquals(1, sends) }
    }

    @Test fun newestMessageBecomesVisibleInLongConversation() {
        show(ChatUiState(messages = List(40) { ChatMessage(it.toLong(), ChatRole.USER, "Message $it") }))
        compose.onNodeWithText("Message 39").assertIsDisplayed()
    }

    @Test fun codeBlockCanBeCopiedWhileProseStaysSeparate() {
        show(ChatUiState(messages = listOf(ChatMessage(1, ChatRole.GEMINI,
            "Explanation\n```kotlin\nval answer = 42\n```\nMore text"))))
        compose.onNodeWithText("Explanation").assertIsDisplayed()
        compose.onNodeWithText("val answer = 42").assertIsDisplayed()
        compose.onNodeWithText("Copy").performClick()
        compose.onNodeWithText("Copied").assertIsDisplayed()
    }
}
