package com.fahim.geminiApiComposeStarter.ui.chat.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fahim.geminiApiComposeStarter.R
import com.fahim.geminiApiComposeStarter.data.model.ChatMessage
import com.fahim.geminiApiComposeStarter.data.model.Role
import com.fahim.geminiApiComposeStarter.ui.text.toBoldAnnotatedString

const val USER_BUBBLE_TAG = "user_bubble"
const val MODEL_BUBBLE_TAG = "model_bubble"
const val LOADING_TAG = "loading_indicator"

private val AvatarSize = 28.dp

/**
 * Material 3 chat bubble. User bubbles sit on the right in primaryContainer; Gemini bubbles sit on
 * the left in surfaceVariant with the assistant avatar. [sideInset] keeps bubbles from spanning
 * the full width (larger on tablets).
 */
@Composable
fun MessageBubble(
    message: ChatMessage,
    sideInset: Dp,
    modifier: Modifier = Modifier,
) {
    val isUser = message.role == Role.USER
    // Parse **bold** once per message instead of on every recomposition.
    val formatted = remember(message.text) { message.text.toBoldAnnotatedString() }
    val shape = if (isUser) {
        RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 4.dp)
    } else {
        RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 4.dp, bottomEnd = 18.dp)
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom,
    ) {
        if (isUser) {
            Spacer(Modifier.width(sideInset))
        } else {
            AssistantAvatar()
            Spacer(Modifier.width(8.dp))
        }
        Surface(
            modifier = Modifier
                .weight(1f, fill = false)
                .testTag(if (isUser) USER_BUBBLE_TAG else MODEL_BUBBLE_TAG),
            shape = shape,
            color = if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            tonalElevation = if (isUser) 0.dp else 1.dp,
        ) {
            SelectionContainer {
                Text(
                    text = formatted,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                )
            }
        }
        if (!isUser) Spacer(Modifier.width(sideInset))
    }
}

/** "Gemini is thinking…" bubble shown at the end of the list while a request is running. */
@Composable
fun TypingBubble(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom,
    ) {
        AssistantAvatar()
        Spacer(Modifier.width(8.dp))
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp).testTag(LOADING_TAG),
                    strokeWidth = 2.dp,
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.gemini_thinking),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun AssistantAvatar() {
    Icon(
        painter = painterResource(R.drawable.ic_assistant),
        contentDescription = null,
        modifier = Modifier.size(AvatarSize),
        tint = MaterialTheme.colorScheme.primary,
    )
}
