package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.fahim.geminiApiComposeStarter.R
import com.fahim.geminiApiComposeStarter.data.model.ChatMessage
import com.fahim.geminiApiComposeStarter.data.model.Role
import com.fahim.geminiApiComposeStarter.ui.text.toBoldAnnotatedString

private val UserBubbleShape = RoundedCornerShape(20.dp, 20.dp, 4.dp, 20.dp)
private val ModelBubbleShape = RoundedCornerShape(20.dp, 20.dp, 20.dp, 4.dp)

/**
 * A single chat bubble: user turns are right-aligned in the primary container colour,
 * Gemini turns are left-aligned next to the assistant icon.
 */
@Composable
fun MessageBubble(
    message: ChatMessage,
    maxWidthFraction: Float,
    modifier: Modifier = Modifier,
) {
    val isUser = message.role == Role.USER
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom,
    ) {
        if (!isUser) AssistantAvatar()
        Box(
            modifier = Modifier.fillMaxWidth(maxWidthFraction),
            contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart,
        ) {
            Surface(
                shape = if (isUser) UserBubbleShape else ModelBubbleShape,
                color = if (isUser) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                },
                modifier = Modifier.testTag(if (isUser) "user_bubble" else "model_bubble"),
            ) {
                // Parsing markdown is cheap but not free; only redo it when the text changes.
                val text = remember(message.text) { message.text.toBoldAnnotatedString() }
                SelectionContainer {
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    )
                }
            }
        }
    }
}

/** Placeholder bubble shown while Gemini is generating a reply. */
@Composable
fun TypingBubble(modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
        AssistantAvatar()
        Surface(shape = ModelBubbleShape, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            CircularProgressIndicator(
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .size(24.dp)
                    .testTag("loading_indicator"),
                strokeWidth = 3.dp,
            )
        }
    }
}

@Composable
private fun AssistantAvatar() {
    Icon(
        painter = painterResource(R.drawable.ic_assistant),
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .padding(end = 8.dp, bottom = 4.dp)
            .size(28.dp),
    )
}
